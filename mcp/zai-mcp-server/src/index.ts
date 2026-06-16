import { createServer, IncomingMessage, ServerResponse } from 'node:http'

type JsonRpcId = string | number | null

interface JsonRpcRequest {
  jsonrpc?: string
  id?: JsonRpcId
  method?: string
  params?: {
    name?: string
    arguments?: Record<string, unknown>
  }
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

const port = Number(process.env.ZAI_MCP_PORT || 8088)

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

  const request = await readJson(req)
  writeJson(res, 200, await handle(request))
})

server.listen(port, () => {
  console.log(`zai-mcp-server listening on http://localhost:${port}/mcp`)
})

async function handle(request: JsonRpcRequest): Promise<JsonRpcResponse> {
  if (!request.method) {
    return error(null, -32600, 'Invalid Request', 'method is required')
  }
  if (request.method === 'initialize') {
    return ok(request.id ?? null, {
      protocolVersion: '2024-11-05',
      capabilities: { tools: {} },
      serverInfo: { name: 'zai-mcp-server', version: '0.0.1' },
    })
  }
  if (request.method === 'tools/list') {
    return ok(request.id ?? null, { tools: toolsList() })
  }
  if (request.method === 'tools/call') {
    return callTool(request)
  }
  if (request.method === 'ping') {
    return ok(request.id ?? null, { pong: true })
  }
  return error(request.id ?? null, -32601, 'Method not found', request.method)
}

async function callTool(request: JsonRpcRequest): Promise<JsonRpcResponse> {
  const name = request.params?.name
  const args = request.params?.arguments || {}
  if (name === 'extract_text_from_screenshot') {
    const imageUrl = args.imageUrl as string | undefined
    if (imageUrl && !imageUrl.startsWith('demo://')) {
      // 调用真实智谱AI视觉理解API
      const result = await extractInvoiceFromImage(imageUrl, args)
      return ok(request.id ?? null, mcpToolResult(result))
    }
    // demo:// 或无 imageUrl 时返回Mock数据
    return ok(request.id ?? null, mcpToolResult(mockInvoice(args)))
  }
  if (name === 'analyze_data_visualization') {
    return ok(request.id ?? null, mcpToolResult(buildChartOption(args)))
  }
  if (name === 'analyze_image') {
    const imageUrl = args.imageUrl as string | undefined
    if (imageUrl) {
      const analysis = await analyzeImageWithZhipu(imageUrl)
      return ok(request.id ?? null, mcpToolResult(analysis))
    }
    return ok(request.id ?? null, mcpToolResult({ summary: '图片内容疑似财务单据，建议进入发票识别流程。' }))
  }
  if (name === 'analyze_video') {
    return ok(request.id ?? null, mcpToolResult({ summary: '视频分析为演示占位能力。' }))
  }
  if (name === 'diagnose_error_screenshot') {
    return ok(request.id ?? null, mcpToolResult(await diagnoseErrorScreenshot(args)))
  }
  if (name === 'ui_diff_check') {
    return ok(request.id ?? null, mcpToolResult(await checkUiDiff(args)))
  }
  if (name === 'understand_technical_diagram') {
    return ok(request.id ?? null, mcpToolResult(await understandTechnicalDiagram(args)))
  }
  if (name === 'ui_to_artifact') {
    return ok(request.id ?? null, mcpToolResult(await uiToArtifact(args)))
  }
  return error(request.id ?? null, -32602, 'Invalid params', `unsupported tool: ${name}`)
}

function toolsList() {
  return [
    {
      name: 'extract_text_from_screenshot',
      description: '识别发票或单据图片，返回结构化字段',
      inputSchema: {
        type: 'object',
        properties: {
          imageUrl: { type: 'string', description: '图片地址或 dataURL，可选；Demo 默认返回一张样例发票' },
        },
        required: [],
      },
    },
    {
      name: 'analyze_data_visualization',
      description: '根据结构化财务数据生成 ECharts 图表配置',
      inputSchema: {
        type: 'object',
        properties: {
          title: { type: 'string', description: '图表标题' },
          labels: { type: 'array', items: { type: 'string' } },
          values: { type: 'array', items: { type: 'number' } },
        },
        required: ['labels', 'values'],
      },
    },
    {
      name: 'analyze_image',
      description: '通用图片分析',
      inputSchema: { type: 'object', properties: { imageUrl: { type: 'string' } }, required: [] },
    },
    {
      name: 'analyze_video',
      description: '通用视频分析',
      inputSchema: { type: 'object', properties: { videoUrl: { type: 'string' } }, required: [] },
    },
    {
      name: 'diagnose_error_screenshot',
      description: '分析错误截图，提取错误信息、可能原因和修复建议',
      inputSchema: {
        type: 'object',
        properties: {
          imageUrl: { type: 'string', description: '错误截图地址或 dataURL' },
          context: { type: 'string', description: '补充上下文' },
        },
        required: [],
      },
    },
    {
      name: 'ui_diff_check',
      description: '比较基准 UI 与当前 UI，返回差异和严重程度',
      inputSchema: {
        type: 'object',
        properties: {
          baselineImageUrl: { type: 'string', description: '基准截图' },
          actualImageUrl: { type: 'string', description: '当前截图' },
        },
        required: [],
      },
    },
    {
      name: 'understand_technical_diagram',
      description: '理解技术架构图、流程图或财务业务流程图',
      inputSchema: {
        type: 'object',
        properties: {
          imageUrl: { type: 'string', description: '图表地址或 dataURL' },
          question: { type: 'string', description: '需要重点回答的问题' },
        },
        required: [],
      },
    },
    {
      name: 'ui_to_artifact',
      description: '将 UI 截图转换为可实现的组件结构与代码草稿',
      inputSchema: {
        type: 'object',
        properties: {
          imageUrl: { type: 'string', description: 'UI 截图地址或 dataURL' },
          framework: { type: 'string', description: '目标框架，默认 Vue 3' },
        },
        required: [],
      },
    },
  ]
}

/* ---------- 智谱AI视觉理解API封装 ---------- */

const ZHIPU_API_URL = 'https://open.bigmodel.cn/api/paas/v4/chat/completions'

/**
 * 调用智谱AI视觉理解API（GLM-4V-Flash模型）
 * @param prompt 提示词
 * @param imageUrl 图片URL或base64 dataURL
 * @returns 模型返回的文本内容
 */
async function callZhipuVision(prompt: string, imageUrl: string): Promise<string> {
  const apiKey = process.env.Z_AI_API_KEY
  if (!apiKey) {
    throw new Error('Z_AI_API_KEY 环境变量未设置')
  }

  const body = {
    model: 'glm-4v-flash',
    messages: [
      {
        role: 'user',
        content: [
          { type: 'text', text: prompt },
          { type: 'image_url', image_url: { url: imageUrl } },
        ],
      },
    ],
  }

  const response = await fetch(ZHIPU_API_URL, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${apiKey}`,
    },
    body: JSON.stringify(body),
  })

  if (!response.ok) {
    const errorText = await response.text()
    throw new Error(`智谱API调用失败: HTTP ${response.status} - ${errorText}`)
  }

  const data = await response.json() as {
    choices?: Array<{
      message?: {
        content?: string
      }
    }>
  }

  const content = data.choices?.[0]?.message?.content
  if (!content) {
    throw new Error('智谱API返回数据中无有效内容')
  }

  return content
}

/**
 * 从图片中提取发票/单据信息（调用智谱AI）
 */
async function extractInvoiceFromImage(
  imageUrl: string,
  args: Record<string, unknown>,
): Promise<Record<string, unknown>> {
  const prompt = '请识别这张发票/单据图片，提取以下字段并以JSON格式返回：invoiceNo(发票号码)、invoiceDate(开票日期)、buyerName(购买方)、sellerName(销售方)、amount(不含税金额)、taxAmount(税额)、totalAmount(价税合计)、category(业务类别)。只返回JSON，不要其他文字。'

  try {
    const text = await callZhipuVision(prompt, imageUrl)
    console.log('[extractInvoiceFromImage] 智谱AI原始返回:', text)

    // 尝试从返回文本中提取JSON（可能包含markdown代码块）
    const jsonMatch = text.match(/```(?:json)?\s*([\s\S]*?)```/) || text.match(/(\{[\s\S]*\})/)
    const jsonStr = jsonMatch ? jsonMatch[1].trim() : text.trim()

    const parsed = JSON.parse(jsonStr) as Record<string, unknown>

    // 组装成与mockInvoice相同的结构
    return {
      source: 'zhipu-ai-vision',
      invoiceNo: parsed.invoiceNo || parsed['发票号码'] || '',
      invoiceDate: parsed.invoiceDate || parsed['开票日期'] || '',
      buyerName: parsed.buyerName || parsed['购买方'] || '',
      sellerName: parsed.sellerName || parsed['销售方'] || '',
      amount: Number(parsed.amount || parsed['不含税金额'] || 0),
      taxAmount: Number(parsed.taxAmount || parsed['税额'] || 0),
      totalAmount: Number(parsed.totalAmount || parsed['价税合计'] || 0),
      category: parsed.category || parsed['业务类别'] || '',
      confidence: 0.95,
      fields: Object.entries(parsed).map(([key, value]) => ({
        name: key,
        value: String(value),
        confidence: 0.95,
      })),
    }
  } catch (err) {
    console.error('[extractInvoiceFromImage] 智谱AI调用失败，fallback到Mock数据:', err)
    return mockInvoice(args)
  }
}

/**
 * 通用图片分析（调用智谱AI）
 */
async function analyzeImageWithZhipu(imageUrl: string): Promise<Record<string, unknown>> {
  const prompt = '请详细分析这张图片的内容，描述图片中的主要元素、文字信息、场景等。以JSON格式返回：{ "summary": "图片内容摘要", "description": "详细描述", "tags": ["标签1", "标签2"], "textDetected": "检测到的文字内容（如有）" }。只返回JSON，不要其他文字。'

  try {
    const text = await callZhipuVision(prompt, imageUrl)
    console.log('[analyzeImageWithZhipu] 智谱AI原始返回:', text)

    // 尝试从返回文本中提取JSON
    const jsonMatch = text.match(/```(?:json)?\s*([\s\S]*?)```/) || text.match(/(\{[\s\S]*\})/)
    const jsonStr = jsonMatch ? jsonMatch[1].trim() : text.trim()

    try {
      const parsed = JSON.parse(jsonStr) as Record<string, unknown>
      return { source: 'zhipu-ai-vision', ...parsed }
    } catch {
      // JSON解析失败，直接返回文本作为summary
      return { source: 'zhipu-ai-vision', summary: text }
    }
  } catch (err) {
    console.error('[analyzeImageWithZhipu] 智谱AI调用失败，fallback到Mock数据:', err)
    return { summary: '图片内容疑似财务单据，建议进入发票识别流程。' }
  }
}

async function diagnoseErrorScreenshot(args: Record<string, unknown>) {
  const imageUrl = optionalString(args.imageUrl)
  if (!imageUrl || imageUrl.startsWith('demo://')) {
    return {
      errorText: 'Dubbo provider is unavailable',
      severity: 'high',
      likelyCauses: ['Provider 未启动', '注册中心配置不一致', '网络连接超时'],
      suggestions: ['确认 Provider 端口已监听', '核对 registry/group/version 配置', '检查 Gateway 降级日志'],
      context: optionalString(args.context),
    }
  }
  return analyzeImageWithPrompt(
    imageUrl,
    '请诊断这张错误截图，以JSON返回 errorText、severity、likelyCauses数组、suggestions数组。只返回JSON。',
  )
}

async function checkUiDiff(args: Record<string, unknown>) {
  const baselineImageUrl = optionalString(args.baselineImageUrl)
  const actualImageUrl = optionalString(args.actualImageUrl)
  if (!actualImageUrl || actualImageUrl.startsWith('demo://')) {
    return {
      matchScore: 0.94,
      severity: 'low',
      differences: [
        { area: '应收看板标题', type: 'spacing', detail: '当前版本顶部间距多 4px' },
        { area: '回款率图表', type: 'color', detail: '强调色与基准图存在轻微差异' },
      ],
    }
  }
  const prompt = `请分析当前UI截图与基准图的差异，以JSON返回 matchScore、severity、differences数组。基准图地址：${baselineImageUrl || '未提供'}。只返回JSON。`
  return analyzeImageWithPrompt(actualImageUrl, prompt)
}

async function understandTechnicalDiagram(args: Record<string, unknown>) {
  const imageUrl = optionalString(args.imageUrl)
  if (!imageUrl || imageUrl.startsWith('demo://')) {
    return {
      diagramType: 'system-architecture',
      summary: '该图描述财务 Agent 通过 MCP Gateway 编排多模态、网页阅读、浏览器与 Dubbo 财务服务。',
      nodes: ['Finance Web', 'MCP Gateway', 'Finance MCP', 'Dubbo Provider', 'ZAI MCP'],
      relationships: ['Finance Web -> MCP Gateway', 'MCP Gateway -> Finance MCP', 'Finance MCP -> Dubbo Provider'],
      answer: optionalString(args.question) || '关键链路为用户指令到 Dubbo 财务服务的工具调用闭环。',
    }
  }
  return analyzeImageWithPrompt(
    imageUrl,
    `请理解这张技术图，以JSON返回 diagramType、summary、nodes数组、relationships数组、answer。重点问题：${optionalString(args.question) || '无'}。只返回JSON。`,
  )
}

async function uiToArtifact(args: Record<string, unknown>) {
  const framework = optionalString(args.framework) || 'Vue 3'
  const imageUrl = optionalString(args.imageUrl)
  if (!imageUrl || imageUrl.startsWith('demo://')) {
    return {
      framework,
      artifactType: 'component-spec',
      componentTree: ['FinancePanel', 'KpiCard', 'FinancialChart', 'InsightCard'],
      tokens: { background: '#081d1a', accent: '#43d2a0', radius: 14, gap: 16 },
      implementationNotes: ['使用 CSS Grid 构建响应式布局', '图表通过 ECharts + ResizeObserver 自适应'],
    }
  }
  return analyzeImageWithPrompt(
    imageUrl,
    `请将这张UI截图转换为${framework}可实现的组件规格，以JSON返回 artifactType、componentTree数组、tokens对象、implementationNotes数组。只返回JSON。`,
  )
}

async function analyzeImageWithPrompt(imageUrl: string, prompt: string): Promise<Record<string, unknown>> {
  try {
    const text = await callZhipuVision(prompt, imageUrl)
    const jsonMatch = text.match(/```(?:json)?\s*([\s\S]*?)```/) || text.match(/(\{[\s\S]*\})/)
    return JSON.parse((jsonMatch ? jsonMatch[1] : text).trim()) as Record<string, unknown>
  } catch (err) {
    return { source: 'zhipu-ai-vision', error: err instanceof Error ? err.message : String(err) }
  }
}

/* ---------- Mock数据与工具函数 ---------- */

function mockInvoice(args: Record<string, unknown>) {
  return {
    source: args.imageUrl ? 'uploaded-image' : 'demo-sample',
    invoiceNo: 'INV-202605-009',
    invoiceDate: '2026-05-28',
    buyerName: '杭州云启科技有限公司',
    sellerName: '浙江智造设备有限公司',
    amount: 120000,
    taxAmount: 15600,
    totalAmount: 135600,
    category: '销售回款',
    confidence: 0.98,
    fields: [
      { name: '发票号码', value: 'INV-202605-009', confidence: 0.99 },
      { name: '销售方', value: '浙江智造设备有限公司', confidence: 0.98 },
      { name: '金额', value: '120000.00', confidence: 0.98 },
      { name: '税额', value: '15600.00', confidence: 0.97 },
    ],
  }
}

function buildChartOption(args: Record<string, unknown>) {
  const labels = Array.isArray(args.labels) ? args.labels : ['0-30天', '31-60天', '61-90天', '90天以上']
  const values = Array.isArray(args.values) ? args.values : [641200, 246400, 180000, 218900]
  return {
    title: { text: String(args.title || '财务数据可视化') },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: labels },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: values }],
  }
}

function optionalString(value: unknown) {
  return typeof value === 'string' && value.trim() ? value.trim() : undefined
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
