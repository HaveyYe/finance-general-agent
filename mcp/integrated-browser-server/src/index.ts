import { randomUUID } from 'node:crypto'
import { createServer, IncomingMessage, ServerResponse } from 'node:http'
import { Browser, BrowserContext, chromium, Page } from 'playwright'

type JsonRpcId = string | number | null

interface JsonRpcRequest {
  id?: JsonRpcId
  method?: string
  params?: {
    name?: string
    arguments?: Record<string, unknown>
  }
}

interface BrowserSession {
  sessionId: string
  context: BrowserContext
  page: Page
  actions: Array<Record<string, unknown>>
  createdAt: string
  lastSeenAt: string
}

const port = Number(process.env.INTEGRATED_BROWSER_MCP_PORT || 8090)
const actionTimeoutMs = Number(process.env.BROWSER_ACTION_TIMEOUT_MS || 10000)
const navigationTimeoutMs = Number(process.env.BROWSER_NAVIGATION_TIMEOUT_MS || 15000)
const sessions = new Map<string, BrowserSession>()
let browserPromise: Promise<Browser> | undefined

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
    writeJson(res, 200, await handle(await readJson(req)))
  } catch (e) {
    writeJson(res, 200, error(null, -32000, 'Browser operation failed', errorMessage(e)))
  }
})

server.listen(port, () => {
  console.log(`integrated-browser-server listening on http://localhost:${port}/mcp`)
})

async function handle(request: JsonRpcRequest) {
  if (!request.method) return error(null, -32600, 'Invalid Request', 'method is required')
  if (request.method === 'initialize') {
    return ok(request.id ?? null, {
      protocolVersion: '2024-11-05',
      capabilities: { tools: {} },
      serverInfo: { name: 'integrated-browser-server', version: '0.0.1' },
    })
  }
  if (request.method === 'tools/list') return ok(request.id ?? null, { tools: toolsList() })
  if (request.method === 'tools/call') return callTool(request)
  if (request.method === 'ping') return ok(request.id ?? null, { pong: true })
  return error(request.id ?? null, -32601, 'Method not found', request.method)
}

async function callTool(request: JsonRpcRequest) {
  const name = request.params?.name || ''
  const args = request.params?.arguments || {}
  try {
    const handlers: Record<string, () => Promise<unknown>> = {
      navigate: () => navigate(args),
      click: () => click(args),
      type: () => typeText(args),
      snapshot: () => snapshot(args),
      screenshot: () => screenshot(args),
      scroll: () => scroll(args),
      fill_form: () => fillForm(args),
      upload_file: () => uploadFile(args),
      wait_for: () => waitFor(args),
      select_option: () => selectOption(args),
      check: () => setChecked(args, true),
      uncheck: () => setChecked(args, false),
      press_key: () => pressKey(args),
      hover: () => hover(args),
      evaluate: () => evaluate(args),
      get_text: () => getText(args),
      get_attribute: () => getAttribute(args),
      back: () => historyAction(args, 'back'),
      forward: () => historyAction(args, 'forward'),
      reload: () => historyAction(args, 'reload'),
      close_session: () => closeSession(args),
      list_sessions: () => listSessions(),
    }
    const handler = handlers[name]
    if (!handler) return error(request.id ?? null, -32602, 'Invalid params', `unsupported tool: ${name}`)
    return ok(request.id ?? null, toolResult(await handler()))
  } catch (e) {
    return ok(request.id ?? null, {
      content: [{ type: 'text', text: `浏览器工具 ${name} 调用失败：${errorMessage(e)}` }],
      structuredContent: { error: errorMessage(e), tool: name },
      isError: true,
    })
  }
}

async function navigate(args: Record<string, unknown>) {
  const session = await getSession(args.sessionId)
  const url = normalizeUrl(requiredString(args.url, 'url'))
  await session.page.goto(url, { waitUntil: 'domcontentloaded', timeout: navigationTimeoutMs })
  recordAction(session, 'navigate', { url })
  return sessionSummary(session)
}

async function click(args: Record<string, unknown>) {
  const session = await getSession(args.sessionId)
  const selector = requiredString(args.selector, 'selector')
  await session.page.locator(selector).first().click({ timeout: timeoutValue(args.timeoutMs) })
  recordAction(session, 'click', { selector })
  return sessionSummary(session)
}

async function typeText(args: Record<string, unknown>) {
  const session = await getSession(args.sessionId)
  const selector = requiredString(args.selector, 'selector')
  const text = requiredString(args.text, 'text')
  await session.page.locator(selector).first().fill(text, { timeout: timeoutValue(args.timeoutMs) })
  recordAction(session, 'type', { selector, textLength: text.length })
  return sessionSummary(session)
}

async function snapshot(args: Record<string, unknown>) {
  const session = await getSession(args.sessionId)
  const body = session.page.locator('body')
  return {
    ...await sessionSummary(session),
    text: truncate(await body.innerText({ timeout: actionTimeoutMs }), 20000),
    html: truncate(await body.innerHTML({ timeout: actionTimeoutMs }), 40000),
  }
}

async function screenshot(args: Record<string, unknown>) {
  const session = await getSession(args.sessionId)
  const fullPage = args.fullPage !== false
  const image = await session.page.screenshot({ fullPage, type: 'png' })
  recordAction(session, 'screenshot', { fullPage })
  return {
    ...await sessionSummary(session),
    mimeType: 'image/png',
    dataUrl: `data:image/png;base64,${image.toString('base64')}`,
  }
}

async function scroll(args: Record<string, unknown>) {
  const session = await getSession(args.sessionId)
  const deltaX = numberValue(args.deltaX, 0)
  const deltaY = numberValue(args.deltaY, 500)
  await session.page.mouse.wheel(deltaX, deltaY)
  recordAction(session, 'scroll', { deltaX, deltaY })
  return { ...await sessionSummary(session), scrollY: await session.page.evaluate(() => window.scrollY) }
}

async function fillForm(args: Record<string, unknown>) {
  const session = await getSession(args.sessionId)
  const fields = Array.isArray(args.fields) ? args.fields : []
  if (!fields.length) throw new Error('fields is required')
  for (const item of fields) {
    if (!item || typeof item !== 'object') continue
    const field = item as Record<string, unknown>
    await session.page.locator(requiredString(field.selector, 'fields[].selector')).first()
      .fill(String(field.value ?? ''), { timeout: timeoutValue(args.timeoutMs) })
  }
  recordAction(session, 'fill_form', { fieldCount: fields.length })
  return sessionSummary(session)
}

async function uploadFile(args: Record<string, unknown>) {
  const session = await getSession(args.sessionId)
  const selector = requiredString(args.selector, 'selector')
  const paths = Array.isArray(args.paths) ? args.paths.map(String) : [requiredString(args.path, 'path')]
  await session.page.locator(selector).first().setInputFiles(paths, { timeout: timeoutValue(args.timeoutMs) })
  recordAction(session, 'upload_file', { selector, fileCount: paths.length })
  return sessionSummary(session)
}

async function waitFor(args: Record<string, unknown>) {
  const session = await getSession(args.sessionId)
  const selector = optionalString(args.selector)
  const timeout = timeoutValue(args.timeoutMs)
  if (selector) {
    await session.page.locator(selector).first().waitFor({ state: waitState(args.state), timeout })
  } else {
    await session.page.waitForTimeout(Math.min(timeout, 30000))
  }
  recordAction(session, 'wait_for', { selector, timeout })
  return sessionSummary(session)
}

async function selectOption(args: Record<string, unknown>) {
  const session = await getSession(args.sessionId)
  const selector = requiredString(args.selector, 'selector')
  const values = Array.isArray(args.values) ? args.values.map(String) : [requiredString(args.value, 'value')]
  const selected = await session.page.locator(selector).first().selectOption(values, { timeout: timeoutValue(args.timeoutMs) })
  recordAction(session, 'select_option', { selector, selected })
  return { ...await sessionSummary(session), selected }
}

async function setChecked(args: Record<string, unknown>, checked: boolean) {
  const session = await getSession(args.sessionId)
  const selector = requiredString(args.selector, 'selector')
  await session.page.locator(selector).first().setChecked(checked, { timeout: timeoutValue(args.timeoutMs) })
  recordAction(session, checked ? 'check' : 'uncheck', { selector })
  return sessionSummary(session)
}

async function pressKey(args: Record<string, unknown>) {
  const session = await getSession(args.sessionId)
  const key = requiredString(args.key, 'key')
  const selector = optionalString(args.selector)
  if (selector) await session.page.locator(selector).first().press(key, { timeout: timeoutValue(args.timeoutMs) })
  else await session.page.keyboard.press(key)
  recordAction(session, 'press_key', { selector, key })
  return sessionSummary(session)
}

async function hover(args: Record<string, unknown>) {
  const session = await getSession(args.sessionId)
  const selector = requiredString(args.selector, 'selector')
  await session.page.locator(selector).first().hover({ timeout: timeoutValue(args.timeoutMs) })
  recordAction(session, 'hover', { selector })
  return sessionSummary(session)
}

async function evaluate(args: Record<string, unknown>) {
  const session = await getSession(args.sessionId)
  const expression = requiredString(args.expression, 'expression')
  const result = await session.page.evaluate(expression)
  recordAction(session, 'evaluate', { expression: truncate(expression, 200) })
  return { ...await sessionSummary(session), result }
}

async function getText(args: Record<string, unknown>) {
  const session = await getSession(args.sessionId)
  const selector = requiredString(args.selector, 'selector')
  const all = args.all === true
  const text = all
    ? await session.page.locator(selector).allInnerTexts()
    : await session.page.locator(selector).first().innerText({ timeout: timeoutValue(args.timeoutMs) })
  return { ...await sessionSummary(session), selector, text }
}

async function getAttribute(args: Record<string, unknown>) {
  const session = await getSession(args.sessionId)
  const selector = requiredString(args.selector, 'selector')
  const attribute = requiredString(args.attribute, 'attribute')
  const value = await session.page.locator(selector).first().getAttribute(attribute, { timeout: timeoutValue(args.timeoutMs) })
  return { ...await sessionSummary(session), selector, attribute, value }
}

async function historyAction(args: Record<string, unknown>, action: 'back' | 'forward' | 'reload') {
  const session = await getSession(args.sessionId)
  if (action === 'back') await session.page.goBack({ waitUntil: 'domcontentloaded', timeout: navigationTimeoutMs })
  if (action === 'forward') await session.page.goForward({ waitUntil: 'domcontentloaded', timeout: navigationTimeoutMs })
  if (action === 'reload') await session.page.reload({ waitUntil: 'domcontentloaded', timeout: navigationTimeoutMs })
  recordAction(session, action)
  return sessionSummary(session)
}

async function closeSession(args: Record<string, unknown>) {
  const sessionId = requiredString(args.sessionId, 'sessionId')
  const session = sessions.get(sessionId)
  if (!session) return { sessionId, closed: false, reason: 'session not found' }
  sessions.delete(sessionId)
  await session.context.close()
  return { sessionId, closed: true }
}

async function listSessions() {
  return {
    sessions: await Promise.all([...sessions.values()].map(sessionSummary)),
    total: sessions.size,
  }
}

async function getSession(value: unknown) {
  const sessionId = optionalString(value) || randomUUID()
  const existing = sessions.get(sessionId)
  if (existing) {
    existing.lastSeenAt = new Date().toISOString()
    return existing
  }
  const browser = await getBrowser()
  const context = await browser.newContext({ ignoreHTTPSErrors: true })
  const page = await context.newPage()
  page.setDefaultTimeout(actionTimeoutMs)
  page.setDefaultNavigationTimeout(navigationTimeoutMs)
  const now = new Date().toISOString()
  const session = { sessionId, context, page, actions: [], createdAt: now, lastSeenAt: now }
  sessions.set(sessionId, session)
  return session
}

function getBrowser() {
  if (!browserPromise) {
    const headless = process.env.BROWSER_HEADLESS !== 'false'
    const executablePath = optionalString(process.env.BROWSER_EXECUTABLE_PATH)
    browserPromise = chromium.launch(executablePath ? { headless, executablePath } : { headless })
      .catch(async (initialError) => {
        const channel = optionalString(process.env.BROWSER_CHANNEL) || 'chrome'
        console.warn(`bundled Chromium unavailable, falling back to channel "${channel}": ${errorMessage(initialError)}`)
        return chromium.launch({ headless, channel })
      })
  }
  return browserPromise
}

function recordAction(session: BrowserSession, action: string, detail: Record<string, unknown> = {}) {
  session.lastSeenAt = new Date().toISOString()
  session.actions.push({ action, ...detail, at: session.lastSeenAt })
}

async function sessionSummary(session: BrowserSession) {
  return {
    sessionId: session.sessionId,
    url: session.page.url(),
    title: await session.page.title(),
    actionCount: session.actions.length,
    createdAt: session.createdAt,
    lastSeenAt: session.lastSeenAt,
  }
}

function toolsList() {
  const session = { sessionId: { type: 'string', description: '浏览器会话ID，可选；首次调用自动生成' } }
  const selector = { selector: { type: 'string', description: 'Playwright 定位器，例如 CSS 或 text=文本' } }
  return [
    tool('navigate', '导航到目标网页', { ...session, url: { type: 'string' } }, ['url']),
    tool('click', '点击页面元素', { ...session, ...selector, timeoutMs: { type: 'number' } }, ['selector']),
    tool('type', '清空并输入文本', { ...session, ...selector, text: { type: 'string' }, timeoutMs: { type: 'number' } }, ['selector', 'text']),
    tool('snapshot', '获取当前页面文本和 DOM 快照', session, []),
    tool('screenshot', '获取当前页面 PNG 截图', { ...session, fullPage: { type: 'boolean' } }, []),
    tool('scroll', '滚动当前页面', { ...session, deltaX: { type: 'number' }, deltaY: { type: 'number' } }, []),
    tool('fill_form', '批量填写表单字段', { ...session, fields: { type: 'array', items: { type: 'object' } }, timeoutMs: { type: 'number' } }, ['fields']),
    tool('upload_file', '上传一个或多个文件', { ...session, ...selector, path: { type: 'string' }, paths: { type: 'array', items: { type: 'string' } } }, ['selector']),
    tool('wait_for', '等待元素状态或等待指定毫秒', { ...session, ...selector, state: { type: 'string' }, timeoutMs: { type: 'number' } }, []),
    tool('select_option', '选择下拉框选项', { ...session, ...selector, value: { type: 'string' }, values: { type: 'array', items: { type: 'string' } } }, ['selector']),
    tool('check', '选中复选框或单选框', { ...session, ...selector }, ['selector']),
    tool('uncheck', '取消选中复选框', { ...session, ...selector }, ['selector']),
    tool('press_key', '向页面或元素发送键盘按键', { ...session, ...selector, key: { type: 'string' } }, ['key']),
    tool('hover', '悬停在页面元素上', { ...session, ...selector }, ['selector']),
    tool('evaluate', '在页面上下文执行 JavaScript 表达式', { ...session, expression: { type: 'string' } }, ['expression']),
    tool('get_text', '读取一个或多个元素文本', { ...session, ...selector, all: { type: 'boolean' } }, ['selector']),
    tool('get_attribute', '读取元素属性', { ...session, ...selector, attribute: { type: 'string' } }, ['selector', 'attribute']),
    tool('back', '浏览器后退', session, []),
    tool('forward', '浏览器前进', session, []),
    tool('reload', '重新加载当前页面', session, []),
    tool('close_session', '关闭浏览器会话并释放上下文', session, ['sessionId']),
    tool('list_sessions', '列出当前浏览器会话', {}, []),
  ]
}

function tool(name: string, description: string, properties: Record<string, unknown>, required: string[]) {
  return { name, description, inputSchema: { type: 'object', properties, required } }
}

function toolResult(result: unknown) {
  return {
    content: [{ type: 'text', text: JSON.stringify(result) }],
    structuredContent: { result },
    isError: false,
  }
}

function requiredString(value: unknown, name: string) {
  const result = optionalString(value)
  if (!result) throw new Error(`${name} is required`)
  return result
}

function optionalString(value: unknown) {
  return typeof value === 'string' && value.trim() ? value.trim() : undefined
}

function numberValue(value: unknown, fallback: number) {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : fallback
}

function timeoutValue(value: unknown) {
  return Math.min(Math.max(numberValue(value, actionTimeoutMs), 0), 30000)
}

function waitState(value: unknown): 'attached' | 'detached' | 'visible' | 'hidden' {
  return ['attached', 'detached', 'visible', 'hidden'].includes(String(value))
    ? String(value) as 'attached' | 'detached' | 'visible' | 'hidden'
    : 'visible'
}

function normalizeUrl(value: string) {
  if (value === 'about:blank' || value.startsWith('data:') || value.startsWith('file:')) return value
  try {
    return new URL(value).toString()
  } catch {
    return `https://${value.replace(/^\/+/, '')}`
  }
}

function truncate(value: string, maxLength: number) {
  return value.length <= maxLength ? value : `${value.slice(0, maxLength)}...[truncated]`
}

async function readJson(req: IncomingMessage): Promise<JsonRpcRequest> {
  const chunks: Buffer[] = []
  for await (const chunk of req) chunks.push(Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk))
  const raw = Buffer.concat(chunks).toString('utf8')
  return raw ? JSON.parse(raw) as JsonRpcRequest : {}
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

function ok(id: JsonRpcId, result: unknown) {
  return { jsonrpc: '2.0', id, result, error: null }
}

function error(id: JsonRpcId, code: number, message: string, data?: unknown) {
  return { jsonrpc: '2.0', id, error: { code, message, data } }
}

function errorMessage(e: unknown) {
  return e instanceof Error ? e.message : String(e)
}

async function shutdown() {
  await Promise.allSettled([...sessions.values()].map((session) => session.context.close()))
  sessions.clear()
  if (browserPromise) await (await browserPromise).close()
  server.close()
}

process.once('SIGINT', () => void shutdown())
process.once('SIGTERM', () => void shutdown())
