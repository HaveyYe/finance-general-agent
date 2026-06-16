import { createServer, IncomingMessage, ServerResponse } from 'node:http'

type JsonRpcId = string | number | null

interface JsonRpcRequest {
  jsonrpc?: string
  method?: string
  params?: {
    name?: string
    arguments?: Record<string, unknown>
  }
  id?: JsonRpcId
}

interface JsonRpcResponse {
  jsonrpc: '2.0'
  id: JsonRpcId
  result?: unknown
  error?: {
    code: number
    message: string
    data?: unknown
  } | null
}

// ── Remote web-reader MCP configuration ──────────────────────────
const REMOTE_MCP_URL = 'https://open.bigmodel.cn/api/mcp/web_reader/mcp'
const REMOTE_API_KEY = process.env.Z_AI_API_KEY || ''

let mcpRequestId = 0

interface PolicySummary {
  sourceUrl: string
  title: string
  markdown: string
  highlights: string[]
  extracted: {
    policyType: string
    standardRates: string[]
    keywords: string[]
    effectiveHint: string
    disclaimer: string
  }
}

const port = Number(process.env.WEB_READER_MCP_PORT || 8089)

const server = createServer(async (req, res) => {
  setCorsHeaders(res)

  if (req.method === 'OPTIONS') {
    res.writeHead(204)
    res.end()
    return
  }

  if (req.method !== 'POST' || req.url !== '/mcp') {
    writeJson(res, 404, { error: 'not found' })
    return
  }

  try {
    const request = await readJson(req)
    writeJson(res, 200, await handle(request))
  } catch (e) {
    writeJson(res, 200, error(null, -32700, 'Parse error', errorMessage(e)))
  }
})

server.listen(port, () => {
  console.log(`web-reader-server listening on http://localhost:${port}/mcp`)
})

async function handle(request: JsonRpcRequest): Promise<JsonRpcResponse> {
  if (!request.method) {
    return error(null, -32600, 'Invalid Request', 'method is required')
  }
  if (request.method === 'initialize') {
    return ok(request.id ?? null, {
      protocolVersion: '2024-11-05',
      capabilities: { tools: {} },
      serverInfo: { name: 'web-reader-server', version: '0.0.1' },
    })
  }
  if (request.method === 'tools/list') {
    return ok(request.id ?? null, { tools: toolsList() })
  }
  if (request.method === 'tools/call') {
    return await callTool(request)
  }
  if (request.method === 'ping') {
    return ok(request.id ?? null, { pong: true })
  }
  return error(request.id ?? null, -32601, 'Method not found', request.method)
}

async function callTool(request: JsonRpcRequest): Promise<JsonRpcResponse> {
  const name = request.params?.name
  const args = request.params?.arguments || {}
  if (name === 'webReader') {
    const keyword = normalizeKeyword(args.keyword)
    const url = normalizeUrl(args.url)

    // Try remote web-reader MCP first
    const remoteResult = await callRemoteWebReader(url, keyword)
    if (remoteResult) {
      const policy = buildPolicyFromRemote(url, keyword, remoteResult)
      return ok(request.id ?? null, mcpToolResult(policy))
    }

    // Keep URL reading available when the remote MCP key/service is unavailable.
    const directResult = await fetchWebPage(url)
    if (directResult) {
      const policy = buildPolicyFromDirectFetch(url, keyword, directResult)
      return ok(request.id ?? null, mcpToolResult(policy))
    }

    console.warn('[web-reader] Remote and direct fetch failed, falling back to local policy summary')
    return ok(request.id ?? null, mcpToolResult(readPolicy(args)))
  }
  return error(request.id ?? null, -32602, 'Invalid params', `unsupported tool: ${name}`)
}

function toolsList() {
  return [
    {
      name: 'webReader',
      description: '读取网页或政策关键词，返回适合财务 Agent 使用的结构化摘要。依次尝试智谱 web-reader、直接 HTTP/HTTPS 抓取和本地政策摘要。',
      inputSchema: {
        type: 'object',
        properties: {
          url: { type: 'string', description: '目标网页 URL，可选；Demo 默认使用税务总局官网入口' },
          keyword: { type: 'string', description: '政策或税务关键词，例如“增值税税率”“差旅报销税前扣除”' },
        },
        required: [],
      },
    },
  ]
}

// ── Remote web-reader MCP integration ────────────────────────────

/**
 * Call the remote Zhipu web-reader MCP service to fetch real web page content.
 * Follows MCP protocol: initialize → tools/list → tools/call
 * Returns the fetched markdown content string, or null on failure.
 */
async function callRemoteWebReader(url: string, keyword: string): Promise<string | null> {
  if (!REMOTE_API_KEY) {
    console.warn('[web-reader] Z_AI_API_KEY not configured, skipping remote fetch')
    return null
  }

  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), 30_000)

  try {
    // Step 1: initialize
    const initResp = await remoteMcpRequest({
      jsonrpc: '2.0',
      id: ++mcpRequestId,
      method: 'initialize',
      params: {
        protocolVersion: '2024-11-05',
        capabilities: {},
        clientInfo: { name: 'web-reader-server', version: '0.0.1' },
      },
    }, controller.signal)

    if (initResp.error) {
      console.error('[web-reader] Remote initialize failed:', JSON.stringify(initResp.error))
      return null
    }

    // Step 2: tools/list (discover available tools)
    const listResp = await remoteMcpRequest({
      jsonrpc: '2.0',
      id: ++mcpRequestId,
      method: 'tools/list',
    }, controller.signal)

    if (listResp.error) {
      console.error('[web-reader] Remote tools/list failed:', JSON.stringify(listResp.error))
      return null
    }

    // Determine the tool name from the remote service
    const remoteToolName = resolveRemoteToolName(listResp.result)

    // Step 3: tools/call with url, keyword, return_format
    const callResp = await remoteMcpRequest({
      jsonrpc: '2.0',
      id: ++mcpRequestId,
      method: 'tools/call',
      params: {
        name: remoteToolName,
        arguments: {
          url,
          keyword,
          return_format: 'markdown',
        },
      },
    }, controller.signal)

    if (callResp.error) {
      console.error('[web-reader] Remote tools/call failed:', JSON.stringify(callResp.error))
      return null
    }

    // Extract the text content from the MCP tool result
    return extractRemoteContent(callResp.result)
  } catch (e) {
    if (controller.signal.aborted) {
      console.error('[web-reader] Remote fetch timed out (30s)')
    } else {
      console.error('[web-reader] Remote fetch error:', errorMessage(e))
    }
    return null
  } finally {
    clearTimeout(timeout)
  }
}

/** Send a single JSON-RPC request to the remote MCP endpoint */
async function remoteMcpRequest(
  body: Record<string, unknown>,
  signal?: AbortSignal,
): Promise<{ result?: unknown; error?: unknown }> {
  const resp = await fetch(REMOTE_MCP_URL, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${REMOTE_API_KEY}`,
    },
    body: JSON.stringify(body),
    signal,
  })

  if (!resp.ok) {
    throw new Error(`Remote MCP HTTP ${resp.status}: ${resp.statusText}`)
  }

  const json = (await resp.json()) as { result?: unknown; error?: unknown }
  return json
}

/** Resolve the remote tool name from tools/list result */
function resolveRemoteToolName(listResult: unknown): string {
  try {
    const tools = (listResult as { tools?: { name: string }[] })?.tools
    if (Array.isArray(tools) && tools.length > 0) {
      return tools[0].name
    }
  } catch {
    // fallback
  }
  return 'web_reader'
}

/** Extract the markdown text content from a tools/call result */
function extractRemoteContent(callResult: unknown): string | null {
  try {
    const result = callResult as { content?: { type: string; text?: string }[] }
    if (Array.isArray(result?.content)) {
      // Concatenate all text parts
      const text = result.content
        .filter((c) => c.type === 'text' && typeof c.text === 'string')
        .map((c) => c.text!)
        .join('\n')
      if (text.trim()) return text.trim()
    }
  } catch {
    // fallback
  }
  return null
}

/** Build a PolicySummary from the remote-fetched markdown content */
function buildPolicyFromRemote(
  sourceUrl: string,
  keyword: string,
  markdown: string,
): PolicySummary {
  // Extract a title from the first heading or first line
  const titleMatch = markdown.match(/^#{1,3}\s+(.+)$/m)
  const title = titleMatch
    ? titleMatch[1].replace(/[*_`\[\]]/g, '').trim()
    : (keyword ? `「${keyword}」网页内容摘要` : '网页内容摘要')

  // Split into highlight lines (non-empty, non-heading lines)
  const lines = markdown.split('\n').filter((l) => l.trim() && !l.startsWith('#'))
  const highlights = lines.slice(0, 5).map((l) => l.replace(/^[-*]\s*/, '').trim())

  // Extract keywords from the content
  const contentKeywords = keyword
    ? splitKeywords(keyword)
    : Array.from(new Set(markdown.match(/[\u4e00-\u9fa5]{2,4}/g) || [])).slice(0, 6)

  return {
    sourceUrl,
    title,
    markdown,
    highlights,
    extracted: {
      policyType: '网页抓取内容',
      standardRates: [],
      keywords: contentKeywords,
      effectiveHint: '以下内容来自远程网页抓取，请以原始网页为准。',
      disclaimer: '内容由智谱 web-reader 远程服务抓取，可能存在格式偏差或内容不完整，建议对照原始网页核实。',
    },
  }
}

// ── Direct HTTP/HTTPS fallback ───────────────────────────────────

interface DirectFetchResult {
  title: string
  markdown: string
}

async function fetchWebPage(url: string): Promise<DirectFetchResult | null> {
  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), 15_000)
  try {
    const parsed = new URL(url)
    if (!['http:', 'https:'].includes(parsed.protocol)) {
      return null
    }
    const response = await fetch(parsed, {
      headers: {
        Accept: 'text/html,application/xhtml+xml,text/plain;q=0.9,*/*;q=0.5',
        'User-Agent': 'finance-general-agent-web-reader/0.0.1',
      },
      redirect: 'follow',
      signal: controller.signal,
    })
    if (!response.ok) {
      throw new Error(`Direct fetch HTTP ${response.status}: ${response.statusText}`)
    }
    const contentType = response.headers.get('content-type') || ''
    const text = (await response.text()).slice(0, 2_000_000)
    if (!text.trim()) {
      return null
    }
    if (contentType.includes('text/plain')) {
      const markdown = normalizeWhitespace(text).slice(0, 80_000)
      return { title: parsed.hostname, markdown }
    }
    const title = extractHtmlTitle(text) || parsed.hostname
    const markdown = htmlToMarkdown(text).slice(0, 80_000)
    return markdown ? { title, markdown } : null
  } catch (e) {
    console.warn('[web-reader] Direct fetch failed:', errorMessage(e))
    return null
  } finally {
    clearTimeout(timeout)
  }
}

function buildPolicyFromDirectFetch(
  sourceUrl: string,
  keyword: string,
  result: DirectFetchResult,
): PolicySummary {
  const lines = result.markdown.split('\n').map((line) => line.trim()).filter(Boolean)
  const keywordLines = keyword
    ? lines.filter((line) => splitKeywords(keyword).some((item) => line.includes(item)))
    : []
  const highlights = [...keywordLines, ...lines]
    .filter((line, index, all) => all.indexOf(line) === index)
    .slice(0, 5)
  return {
    sourceUrl,
    title: result.title,
    markdown: result.markdown,
    highlights,
    extracted: {
      policyType: '网页直接抓取内容',
      standardRates: Array.from(new Set(result.markdown.match(/\b\d{1,2}(?:\.\d+)?%/g) || [])).slice(0, 8),
      keywords: keyword ? splitKeywords(keyword) : [],
      effectiveHint: '以下内容来自目标网页实时抓取，请结合页面发布日期和有效期判断。',
      disclaimer: '内容由本地 web-reader 直接抓取并做轻量文本转换，正式财务决策前需对照原始网页核实。',
    },
  }
}

function extractHtmlTitle(html: string) {
  const match = html.match(/<title\b[^>]*>([\s\S]*?)<\/title>/i)
  return match ? decodeHtml(stripTags(match[1])).trim() : ''
}

function htmlToMarkdown(html: string) {
  const withoutNoise = html
    .replace(/<!--[\s\S]*?-->/g, ' ')
    .replace(/<(script|style|noscript|svg|canvas|template)\b[\s\S]*?<\/\1>/gi, ' ')
  const withStructure = withoutNoise
    .replace(/<h1\b[^>]*>([\s\S]*?)<\/h1>/gi, '\n# $1\n')
    .replace(/<h2\b[^>]*>([\s\S]*?)<\/h2>/gi, '\n## $1\n')
    .replace(/<h3\b[^>]*>([\s\S]*?)<\/h3>/gi, '\n### $1\n')
    .replace(/<li\b[^>]*>([\s\S]*?)<\/li>/gi, '\n- $1')
    .replace(/<(br|hr)\b[^>]*\/?>/gi, '\n')
    .replace(/<\/(p|div|section|article|header|footer|main|nav|table|tr)>/gi, '\n')
  return normalizeWhitespace(decodeHtml(stripTags(withStructure)))
}

function stripTags(value: string) {
  return value.replace(/<[^>]+>/g, ' ')
}

function decodeHtml(value: string) {
  const named: Record<string, string> = {
    amp: '&',
    lt: '<',
    gt: '>',
    quot: '"',
    apos: "'",
    nbsp: ' ',
  }
  return value
    .replace(/&#(\d+);/g, (_, code: string) => String.fromCodePoint(Number(code)))
    .replace(/&#x([0-9a-f]+);/gi, (_, code: string) => String.fromCodePoint(parseInt(code, 16)))
    .replace(/&([a-z]+);/gi, (entity, name: string) => named[name.toLowerCase()] ?? entity)
}

function normalizeWhitespace(value: string) {
  return value
    .replace(/\r/g, '')
    .split('\n')
    .map((line) => line.replace(/[ \t]+/g, ' ').trim())
    .filter(Boolean)
    .join('\n')
}

// ── Local policy summary fallback ────────────────────────────────

function readPolicy(args: Record<string, unknown>): PolicySummary {
  const keyword = normalizeKeyword(args.keyword)
  const sourceUrl = normalizeUrl(args.url)
  const scenario = inferScenario(keyword)
  const summary = policySummaryByScenario(scenario)

  return {
    sourceUrl,
    title: summary.title,
    markdown: [
      `## ${summary.title}`,
      '',
      ...summary.highlights.map((item) => `- ${item}`),
      '',
      `> ${summary.disclaimer}`,
    ].join('\n'),
    highlights: summary.highlights,
    extracted: {
      policyType: summary.policyType,
      standardRates: summary.standardRates,
      keywords: keyword ? splitKeywords(keyword) : summary.keywords,
      effectiveHint: summary.effectiveHint,
      disclaimer: summary.disclaimer,
    },
  }
}

function policySummaryByScenario(scenario: string) {
  const disclaimer = '演示数据，仅用于验证 MCP 编排链路；正式业务需接入税务官网实时读取、内部税务知识库或人工复核流程。'
  if (scenario === 'expense') {
    return {
      title: '费用报销与税前扣除政策摘要（Demo）',
      policyType: '企业所得税/费用报销',
      standardRates: [],
      keywords: ['费用报销', '税前扣除', '发票合规'],
      effectiveHint: '以企业实际适用政策、票据类型、内部制度和主管税务机关口径为准。',
      disclaimer,
      highlights: [
        '费用报销通常需要业务真实性、合规票据、审批链路和支付凭据共同支撑。',
        '差旅、招待、福利、培训等费用在税前扣除口径上存在差异，需要按费用类型拆分识别。',
        'Agent 可先做票据完整性、金额一致性、审批状态检查，再生成待人工复核清单。',
      ],
    }
  }
  if (scenario === 'invoice') {
    return {
      title: '发票合规与进项税处理摘要（Demo）',
      policyType: '增值税/发票管理',
      standardRates: ['13%', '9%', '6%', '免税/零税率场景需单独判断'],
      keywords: ['发票', '进项税', '认证抵扣'],
      effectiveHint: '以发票类型、购销业务实质、认证状态和当前有效政策为准。',
      disclaimer,
      highlights: [
        '专票抵扣通常需要发票合规、用途允许抵扣、认证/勾选状态有效。',
        '普票、电子发票、旅客运输凭证等不同票据的入账和税务处理规则不同。',
        'Agent 可把 OCR 识别结果、发票台账、凭证分录和税务风险提示串联展示。',
      ],
    }
  }
  return {
    title: '增值税税率政策摘要（Demo）',
    policyType: '增值税',
    standardRates: ['13%', '9%', '6%'],
    keywords: ['增值税', '税率', '政策'],
    effectiveHint: '常见税率仅作演示索引用；实际适用需根据商品服务编码、纳税人类型、政策有效期判断。',
    disclaimer,
    highlights: [
      '常见增值税税率可按货物/劳务、有形动产租赁、交通运输/建筑/不动产、现代服务等类别区分。',
      '小规模纳税人、免税、简易计税、零税率等场景需要单独识别，不应只按关键词套用税率。',
      '建议在正式实现中将税务官网网页读取结果、政策知识库和企业税码规则合并检索。',
    ],
  }
}

function inferScenario(keyword: string) {
  if (keyword.includes('报销') || keyword.includes('扣除') || keyword.includes('差旅') || keyword.includes('招待')) {
    return 'expense'
  }
  if (keyword.includes('发票') || keyword.includes('进项') || keyword.includes('抵扣') || keyword.includes('验真')) {
    return 'invoice'
  }
  return 'vat'
}

function normalizeKeyword(value: unknown) {
  return typeof value === 'string' ? value.trim() : ''
}

function normalizeUrl(value: unknown) {
  if (typeof value !== 'string' || !value.trim()) {
    return 'https://www.chinatax.gov.cn/'
  }
  try {
    const url = new URL(value)
    return url.toString()
  } catch {
    return 'https://www.chinatax.gov.cn/'
  }
}

function splitKeywords(keyword: string) {
  return Array.from(new Set(keyword.split(/[，,。；;\s]+/).filter(Boolean))).slice(0, 8)
}

function mcpToolResult(result: unknown) {
  return {
    content: [{ type: 'text', text: JSON.stringify(result) }],
    structuredContent: { result },
    isError: false,
  }
}

async function readJson(req: IncomingMessage): Promise<JsonRpcRequest> {
  const chunks: Buffer[] = []
  for await (const chunk of req) {
    chunks.push(Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk))
  }
  const raw = Buffer.concat(chunks).toString('utf8')
  return raw ? (JSON.parse(raw) as JsonRpcRequest) : {}
}

function setCorsHeaders(res: ServerResponse) {
  res.setHeader('Access-Control-Allow-Origin', '*')
  res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS')
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization')
}

function writeJson(res: ServerResponse, status: number, body: unknown) {
  res.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8' })
  res.end(JSON.stringify(body))
}

function ok(id: JsonRpcId, result: unknown): JsonRpcResponse {
  return { jsonrpc: '2.0', id, result, error: null }
}

function error(id: JsonRpcId, code: number, message: string, data?: unknown): JsonRpcResponse {
  return { jsonrpc: '2.0', id, error: { code, message, data } }
}

function errorMessage(e: unknown) {
  return e instanceof Error ? e.message : String(e)
}
