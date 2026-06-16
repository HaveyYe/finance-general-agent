import { createServer, IncomingMessage, ServerResponse } from 'node:http'
import { randomUUID } from 'node:crypto'
import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import YAML from 'yaml'

type JsonRpcId = string | number | null

interface JsonRpcRequest {
  jsonrpc?: string
  id?: JsonRpcId
  method?: string
  params?: Record<string, unknown>
}

interface JsonRpcError {
  code: number
  message: string
  data?: unknown
}

interface JsonRpcResponse {
  jsonrpc: '2.0'
  id: JsonRpcId
  result?: unknown
  error?: JsonRpcError | null
}

interface RouteService {
  url: string
  timeoutMs?: number
  tools: string[]
  authToken?: string
  authTokenEnv?: string
  authHeaderName?: string
}

interface RouteConfig {
  services: Record<string, RouteService>
}

interface McpTool {
  name: string
  description?: string
  inputSchema?: Record<string, unknown>
}

interface ToolRoute {
  serviceName: string
  service: RouteService
  backendToolName: string
}

interface SessionContext {
  sessionId: string
  createdAt: number
  lastSeenAt: number
  clientContext?: ClientContext
  auditLogs: ToolAuditLog[]
}

interface ClientContext {
  channel?: 'dingtalk' | 'web'
  corpId?: string
  authCode?: string
  userId?: string
  userName?: string
  device?: 'mobile' | 'desktop'
  userAgent?: string
}

interface AgentChatRequest {
  sessionId?: string
  message?: string
  clientContext?: ClientContext
  imageUrl?: string
  expenseApprovalArgs?: Record<string, unknown>
}

interface AgentChatResponse {
  sessionId: string
  role: 'assistant'
  type: 'text' | 'table' | 'chart' | 'card' | 'file'
  text: string
  content?: unknown
  toolCall?: {
    name: string
    arguments: Record<string, unknown>
  }
  toolCalls?: Array<{
    name: string
    arguments: Record<string, unknown>
  }>
  clientContext?: ClientContext
  callChain?: ToolAuditLog[]
}

interface ToolAuditLog {
  id: string
  sessionId?: string
  toolName: string
  backendToolName?: string
  serviceName?: string
  startedAt: string
  durationMs: number
  status: 'success' | 'error'
  arguments?: Record<string, unknown>
  error?: string
}

interface LlmToolCall {
  id: string
  type: 'function'
  function: {
    name: string
    arguments: string
  }
}

interface LlmMessage {
  role: 'system' | 'user' | 'assistant' | 'tool'
  content: string | null
  tool_calls?: LlmToolCall[]
  tool_call_id?: string
}

interface KnowledgeCitation {
  documentName?: string
  chunkNo?: number
  text?: string
  score?: number
}

interface KnowledgeChatData {
  answer?: string
  citations?: KnowledgeCitation[]
  hits?: unknown[]
}

const __dirname = dirname(fileURLToPath(import.meta.url))
const configPath = resolve(__dirname, '../config/routes.yml')
const port = Number(process.env.MCP_GATEWAY_PORT || 9000)
const documentRagUrl = process.env.DOCUMENT_RAG_URL || 'http://localhost:8091'
const maxAuditLogsPerSession = 100
const knowledgeTopK = Number(process.env.KNOWLEDGE_CHAT_TOP_K || 10)
const sessions = new Map<string, SessionContext>()
let toolRoutes = new Map<string, ToolRoute>()

function loadConfig(): RouteConfig {
  const raw = readFileSync(configPath, 'utf8')
  return YAML.parse(raw) as RouteConfig
}

const routeConfig = loadConfig()

const server = createServer(async (req, res) => {
  setCorsHeaders(res)

  if (req.method === 'OPTIONS') {
    res.writeHead(204)
    res.end()
    return
  }

  if (req.method === 'GET' && req.url === '/health') {
    writeJson(res, 200, { ok: true, services: Object.keys(routeConfig.services) })
    return
  }

  if (req.method === 'GET' && req.url?.startsWith('/agent/chat/stream')) {
    await handleAgentChatStream(req, res)
    return
  }

  if (req.method === 'POST' && req.url === '/agent/sessions') {
    const request = (await readJson(req)) as AgentChatRequest
    writeJson(res, 200, {
      sessionId: ensureSession(request.sessionId, clientContextFromRequest(request.clientContext, req)),
    })
    return
  }

  if (req.method === 'GET' && req.url?.startsWith('/agent/sessions/')) {
    writeJson(res, 200, handleSessionAudit(req.url))
    return
  }

  if (req.method === 'POST' && req.url === '/agent/chat') {
    const request = (await readJson(req)) as AgentChatRequest
    writeJson(res, 200, await handleAgentChat(request, req))
    return
  }

  if (req.method !== 'POST' || req.url !== '/mcp') {
    writeJson(res, 404, { error: 'not found' })
    return
  }

  const request = await readJson(req)
  const response = await handleMcp(request)
  writeJson(res, 200, response)
})

server.listen(port, async () => {
  console.log(`mcp-gateway listening on http://localhost:${port}/mcp`)
  const discovered = await listTools()
  console.log(`mcp-gateway discovered ${discovered.tools.length} tools; unavailable services: ${discovered.unavailableServices.length}`)
})

async function handleMcp(request: JsonRpcRequest): Promise<JsonRpcResponse> {
  if (!request || !request.method) {
    return error(null, -32600, 'Invalid Request', 'method is required')
  }

  try {
    if (request.method === 'initialize') {
      return ok(request.id ?? null, initializeResult())
    }
    if (request.method === 'tools/list') {
      return ok(request.id ?? null, await listTools())
    }
    if (request.method === 'tools/call') {
      return await callTool(request)
    }
    if (request.method === 'ping') {
      return ok(request.id ?? null, { pong: true })
    }
    return error(request.id ?? null, -32601, 'Method not found', request.method)
  } catch (e) {
    return error(request.id ?? null, -32000, 'Internal error', errorMessage(e))
  }
}

function initializeResult() {
  const sessionId = randomUUID()
  sessions.set(sessionId, {
    sessionId,
    createdAt: Date.now(),
    lastSeenAt: Date.now(),
    auditLogs: [],
  })

  return {
    protocolVersion: '2024-11-05',
    capabilities: { tools: {} },
    serverInfo: {
      name: 'finance-mcp-gateway',
      version: '0.0.1',
    },
    _meta: { sessionId },
  }
}

async function listTools() {
  const tools: McpTool[] = []
  const nextRoutes = new Map<string, ToolRoute>()
  const unavailableServices: Array<{ service: string; reason: string }> = []

  for (const [serviceName, service] of Object.entries(routeConfig.services)) {
    try {
      const serviceTools = await fetchServiceTools(service)
      for (const tool of serviceTools) {
        const exposedName = nextRoutes.has(tool.name) ? `${serviceName}_${tool.name}` : tool.name
        tools.push(exposedName === tool.name ? tool : { ...tool, name: exposedName })
        nextRoutes.set(exposedName, {
          serviceName,
          service,
          backendToolName: tool.name,
        })
      }
    } catch (e) {
      unavailableServices.push({ service: serviceName, reason: errorMessage(e) })
      for (const fallbackToolName of service.tools || []) {
        if (!nextRoutes.has(fallbackToolName)) {
          nextRoutes.set(fallbackToolName, {
            serviceName,
            service,
            backendToolName: fallbackToolName,
          })
        }
      }
    }
  }

  toolRoutes = nextRoutes
  return { tools, unavailableServices }
}

async function callTool(request: JsonRpcRequest): Promise<JsonRpcResponse> {
  const params = request.params || {}
  const name = String(params.name || '')
  const meta = params._meta as { sessionId?: string } | undefined
  const sessionId = meta?.sessionId
  const startedAt = Date.now()
  if (!name) {
    return error(request.id ?? null, -32602, 'Invalid params', 'tool name is required')
  }

  if (toolRoutes.size === 0 || !toolRoutes.has(name)) {
    await listTools()
  }

  const route = toolRoutes.get(name)
  if (!route) {
    recordToolAudit({
      sessionId,
      toolName: name,
      startedAt,
      status: 'error',
      durationMs: Date.now() - startedAt,
      arguments: sanitizeArguments(params.arguments),
      error: `unsupported tool: ${name}`,
    })
    return error(request.id ?? null, -32602, 'Invalid params', `unsupported tool: ${name}`)
  }

  touchSession(params)

  const forwarded = {
    ...request,
    params: {
      ...params,
      name: route.backendToolName,
    },
  }

  try {
    const response = await postJsonRpc(route.service, forwarded)
    recordToolAudit({
      sessionId,
      toolName: name,
      backendToolName: route.backendToolName,
      serviceName: route.serviceName,
      startedAt,
      status: response.error ? 'error' : 'success',
      durationMs: Date.now() - startedAt,
      arguments: sanitizeArguments(params.arguments),
      error: response.error ? String(response.error.data || response.error.message) : undefined,
    })
    return response
  } catch (e) {
    recordToolAudit({
      sessionId,
      toolName: name,
      backendToolName: route.backendToolName,
      serviceName: route.serviceName,
      startedAt,
      status: 'error',
      durationMs: Date.now() - startedAt,
      arguments: sanitizeArguments(params.arguments),
      error: errorMessage(e),
    })
    return ok(request.id ?? null, {
      content: [
        {
          type: 'text',
          text: `MCP 服务 ${route.serviceName} 暂不可用：${errorMessage(e)}`,
        },
      ],
      structuredContent: {
        service: route.serviceName,
        error: errorMessage(e),
      },
      isError: true,
    })
  }
}

async function handleAgentChat(request: AgentChatRequest, req: IncomingMessage): Promise<AgentChatResponse> {
  const message = (request.message || '').trim()
  const requestContext = clientContextFromRequest(request.clientContext, req)
  const sessionId = ensureSession(request.sessionId, requestContext)
  const turnStartedAt = Date.now()
  const clientContext = sessions.get(sessionId)?.clientContext
  if (!message) {
    return {
      sessionId,
      role: 'assistant',
      type: 'text',
      text: '请输入要查询知识库的问题。',
      clientContext,
      callChain: currentCallChain(sessionId, turnStartedAt),
    }
  }

  if (isGreeting(message)) {
    return {
      sessionId,
      role: 'assistant',
      type: 'text',
      text: buildGeneralGreeting(),
      clientContext,
      callChain: currentCallChain(sessionId, turnStartedAt),
    }
  }

  if (isCapabilityQuestion(message)) {
    return {
      sessionId,
      role: 'assistant',
      type: 'text',
      text: buildCapabilityIntro(),
      clientContext,
      callChain: currentCallChain(sessionId, turnStartedAt),
    }
  }

  if (isGeneralChat(message)) {
    return await tryAnswerGeneral(message, sessionId, turnStartedAt, clientContext)
  }

  if (request.expenseApprovalArgs) {
    return handleRagEnhancedExpenseApproval(
      sessionId,
      turnStartedAt,
      message,
      {
        name: 'approve_expense',
        arguments: request.expenseApprovalArgs,
        responseType: 'card',
      },
      clientContext,
    )
  }

  if (isPolicyInquiryIntent(message)) {
    const ragResponse = await tryAnswerWithKnowledge(message, sessionId, turnStartedAt, clientContext, true)
    if (ragResponse) {
      const citationsCount = (ragResponse.toolCall?.arguments as { citationsCount?: number } | undefined)?.citationsCount
      if (citationsCount && citationsCount > 0) {
        return ragResponse
      }
      return {
        sessionId,
        role: 'assistant',
        type: 'text',
        text: '未在知识库中检索到与该问题直接相关的制度依据。该类报销标准问题需要以公司上传的制度文档为准，我不会凭经验编造答案。建议先到「知识库」上传对应的费用报销制度，或把适用城市、人员级别、金额等信息告诉我，我再帮你判断。',
        toolCall: { name: 'knowledge_chat', arguments: { question: message } },
        clientContext,
        callChain: currentCallChain(sessionId, turnStartedAt),
      }
    }
    return answerGeneralQuestion(message, sessionId, turnStartedAt, clientContext)
  }

  if (isKnowledgeIntent(message)) {
    const ragResponse = await tryAnswerWithKnowledge(message, sessionId, turnStartedAt, clientContext)
    if (ragResponse) {
      return ragResponse
    }
    return answerGeneralQuestion(message, sessionId, turnStartedAt, clientContext)
  }

  if (isConceptualQuestion(message) && !isExplicitToolExecutionIntent(message, request.imageUrl)) {
    const ragResponse = await tryAnswerWithKnowledge(message, sessionId, turnStartedAt, clientContext)
    if (ragResponse) {
      return ragResponse
    }
    return answerGeneralQuestion(message, sessionId, turnStartedAt, clientContext)
  }

  const llmResponse = await tryRunLlmAgent(message, sessionId, turnStartedAt)
  if (llmResponse) {
    return llmResponse
  }

  if (isInvoiceToVoucherIntent(message)) {
    return handleInvoiceToVoucher(sessionId, turnStartedAt, message, request.imageUrl)
  }

  if (!isFinanceToolIntent(message)) {
    return await tryAnswerGeneral(message, sessionId, turnStartedAt, clientContext)
  }

  const decision = inferToolCall(message)
  if (decision.name === 'approve_expense') {
    return handleRagEnhancedExpenseApproval(sessionId, turnStartedAt, message, decision, clientContext)
  }

  const toolResponse = await callTool({
    jsonrpc: '2.0',
    id: Date.now(),
    method: 'tools/call',
    params: {
      name: decision.name,
      arguments: decision.arguments,
      _meta: buildToolMeta(sessionId),
    },
  })

  if (toolResponse.error) {
    return {
      sessionId,
      role: 'assistant',
      type: 'text',
      text: `工具调用失败：${toolResponse.error.data || toolResponse.error.message}`,
      toolCall: decision,
      clientContext,
      callChain: currentCallChain(sessionId, turnStartedAt),
    }
  }

  const toolResult = toolResponse.result as {
    isError?: boolean
    structuredContent?: { result?: { code?: number; message?: string; data?: unknown }; error?: unknown }
  }

  if (toolResult?.isError) {
    return {
      sessionId,
      role: 'assistant',
      type: 'text',
      text: `工具暂不可用：${JSON.stringify(toolResult.structuredContent || {})}`,
      toolCall: decision,
      clientContext,
      callChain: currentCallChain(sessionId, turnStartedAt),
    }
  }

  const result = toolResult?.structuredContent?.result
  const data = unwrapToolData(result)
  const text = buildAgentText(decision.name, result?.message || '查询成功', data)
  await persistInvocationAudit({
    sessionId,
    message,
    decision,
    text,
    status: 'success',
    turnStartedAt,
  })
  return {
    sessionId,
    role: 'assistant',
    type: decision.responseType,
    text,
    content: normalizeAgentContent(decision.name, data),
    toolCall: decision,
    clientContext,
    callChain: currentCallChain(sessionId, turnStartedAt),
  }
}

async function tryAnswerWithKnowledge(
  message: string,
  sessionId: string,
  turnStartedAt: number,
  clientContext?: ClientContext,
  force = false,
): Promise<AgentChatResponse | undefined> {
  const knowledgeStartedAt = Date.now()
  try {
    const response = await fetch(`${documentRagUrl}/knowledge/chat`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ question: message, topK: knowledgeTopK }),
      signal: AbortSignal.timeout(Number(process.env.DOCUMENT_RAG_TIMEOUT_MS || 30000)),
    })
    if (!response.ok) {
      recordToolAudit({
        sessionId,
        toolName: 'knowledge_chat',
        serviceName: 'document-rag',
        startedAt: knowledgeStartedAt,
        durationMs: Date.now() - knowledgeStartedAt,
        status: 'error',
        arguments: { question: message, topK: knowledgeTopK },
        error: `HTTP ${response.status}`,
      })
      if (force) {
        return {
          sessionId,
          role: 'assistant',
          type: 'text',
          text: `知识库暂不可用：HTTP ${response.status}`,
          toolCall: { name: 'knowledge_chat', arguments: { question: message } },
          clientContext,
          callChain: currentCallChain(sessionId, turnStartedAt),
        }
      }
      return undefined
    }
    const data = await response.json() as {
      answer?: string
      citations?: Array<{ documentName?: string; chunkNo?: number; text?: string; score?: number }>
      hits?: unknown[]
    }
    const citations = Array.isArray(data.citations) ? data.citations : []
    recordToolAudit({
      sessionId,
      toolName: 'knowledge_chat',
      serviceName: 'document-rag',
      startedAt: knowledgeStartedAt,
      durationMs: Date.now() - knowledgeStartedAt,
      status: 'success',
      arguments: { question: message, topK: knowledgeTopK },
    })
    if (!citations.length && !force && !isKnowledgeIntent(message)) {
      return undefined
    }
    const text = formatKnowledgeAnswer(data.answer || '知识库没有返回有效答案。', citations)
    return {
      sessionId,
      role: 'assistant',
      type: 'text',
      text,
      toolCall: { name: 'knowledge_chat', arguments: { question: message, citationsCount: citations.length } },
      clientContext,
      callChain: currentCallChain(sessionId, turnStartedAt),
    }
  } catch (e) {
    recordToolAudit({
      sessionId,
      toolName: 'knowledge_chat',
      serviceName: 'document-rag',
      startedAt: knowledgeStartedAt,
      durationMs: Date.now() - knowledgeStartedAt,
      status: 'error',
      arguments: { question: message, topK: knowledgeTopK },
      error: errorMessage(e),
    })
    if (isKnowledgeIntent(message)) {
      return {
        sessionId,
        role: 'assistant',
        type: 'text',
        text: `知识库暂不可用：${errorMessage(e)}`,
        toolCall: { name: 'knowledge_chat', arguments: { question: message } },
        clientContext,
        callChain: currentCallChain(sessionId, turnStartedAt),
      }
    }
    if (force) {
      return {
        sessionId,
        role: 'assistant',
        type: 'text',
        text: `知识库暂不可用：${errorMessage(e)}`,
        toolCall: { name: 'knowledge_chat', arguments: { question: message } },
        clientContext,
        callChain: currentCallChain(sessionId, turnStartedAt),
      }
    }
    return undefined
  }
}

async function handleRagEnhancedExpenseApproval(
  sessionId: string,
  turnStartedAt: number,
  message: string,
  decision: { name: string; arguments: Record<string, unknown>; responseType: AgentChatResponse['type'] },
  clientContext?: ClientContext,
): Promise<AgentChatResponse> {
  const knowledgeStartedAt = Date.now()
  const ruleQuestion = buildExpenseRuleQuestion(message, decision.arguments)
  const knowledge = await fetchKnowledgeChat(ruleQuestion, 6)
  recordToolAudit({
    sessionId,
    toolName: 'knowledge_chat',
    serviceName: 'document-rag',
    startedAt: knowledgeStartedAt,
    durationMs: Date.now() - knowledgeStartedAt,
    status: knowledge.error ? 'error' : 'success',
    arguments: { question: ruleQuestion, topK: 6 },
    error: knowledge.error,
  })

  const ruleCitations = normalizeRuleCitations(knowledge.data?.citations || [])
  const autoApproveRequested = Boolean(decision.arguments.autoApproveEnabled)
  const approvalDecision = {
    ...decision,
    arguments: {
      ...decision.arguments,
      autoApproveEnabled: autoApproveRequested && ruleCitations.length > 0,
      ruleCitations,
    },
  }
  const toolResponse = await callTool({
    jsonrpc: '2.0',
    id: Date.now(),
    method: 'tools/call',
    params: {
      name: approvalDecision.name,
      arguments: approvalDecision.arguments,
      _meta: buildToolMeta(sessionId),
    },
  })

  if (toolResponse.error) {
    return {
      sessionId,
      role: 'assistant',
      type: 'text',
      text: `工具调用失败：${toolResponse.error.data || toolResponse.error.message}`,
      toolCall: approvalDecision,
      clientContext,
      callChain: currentCallChain(sessionId, turnStartedAt),
    }
  }

  const toolResult = toolResponse.result as {
    isError?: boolean
    structuredContent?: { result?: { code?: number; message?: string; data?: unknown }; error?: unknown }
  }
  if (toolResult?.isError) {
    return {
      sessionId,
      role: 'assistant',
      type: 'text',
      text: `工具暂不可用：${JSON.stringify(toolResult.structuredContent || {})}`,
      toolCall: approvalDecision,
      clientContext,
      callChain: currentCallChain(sessionId, turnStartedAt),
    }
  }

  const result = toolResult?.structuredContent?.result
  const approval = enrichExpenseApprovalData(unwrapToolData(result), {
    ruleCitations,
    ruleAnswer: knowledge.data?.answer,
    ruleRetrievalError: knowledge.error,
    autoApproveRequested,
  })
  const text = buildExpenseApprovalText(approval)
  await persistInvocationAudits(
    sessionId,
    message,
    [
      { name: 'knowledge_chat', arguments: { question: ruleQuestion, topK: 6 } },
      approvalDecision,
    ],
    text,
    turnStartedAt,
  )

  return {
    sessionId,
    role: 'assistant',
    type: 'card',
    text,
    content: approval,
    toolCall: approvalDecision,
    toolCalls: [
      { name: 'knowledge_chat', arguments: { question: ruleQuestion, topK: 6 } },
      approvalDecision,
    ],
    clientContext,
    callChain: currentCallChain(sessionId, turnStartedAt),
  }
}

async function fetchKnowledgeChat(question: string, topK: number): Promise<{ data?: KnowledgeChatData; error?: string }> {
  try {
    const response = await fetch(`${documentRagUrl}/knowledge/chat`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ question, topK }),
      signal: AbortSignal.timeout(Number(process.env.DOCUMENT_RAG_TIMEOUT_MS || 30000)),
    })
    if (!response.ok) {
      return { error: `HTTP ${response.status}` }
    }
    return { data: await response.json() as KnowledgeChatData }
  } catch (e) {
    return { error: errorMessage(e) }
  }
}

function buildExpenseRuleQuestion(message: string, args: Record<string, unknown>) {
  const parts = [
    '请根据已上传的报销制度，判断该报销申请适用的费用标准、审批条件和风险控制要求。',
    `报销单号：${stringArg(args.expenseNo) || '-'}`,
    `报销人：${stringArg(args.employeeName) || stringArg(args.employeeId) || '-'}`,
    `部门：${stringArg(args.department) || '-'}`,
    `人员级别：${stringArg(args.employeeLevel) || '-'}`,
    `费用类型：${stringArg(args.expenseType) || '-'}`,
    `城市等级：${stringArg(args.cityTier) || '-'}`,
    `金额：${String(args.amount || '-')}`,
    `原始指令：${message}`,
  ]
  return parts.join('\n')
}

function normalizeRuleCitations(citations: KnowledgeCitation[]) {
  return citations
    .filter((item) => item && item.documentName && item.text)
    .slice(0, 3)
    .map((item) => ({
      documentName: String(item.documentName),
      chunkNo: typeof item.chunkNo === 'number' ? item.chunkNo : undefined,
      text: String(item.text || '').replace(/\s+/g, ' ').trim().slice(0, 240),
      score: typeof item.score === 'number' ? item.score : undefined,
    }))
}

function enrichExpenseApprovalData(
  data: unknown,
  extra: {
    ruleCitations: ReturnType<typeof normalizeRuleCitations>
    ruleAnswer?: string
    ruleRetrievalError?: string
    autoApproveRequested: boolean
  },
) {
  const approval = data && typeof data === 'object' ? { ...(data as Record<string, unknown>) } : {}
  approval.ruleCitations = extra.ruleCitations
  approval.ruleAnswer = extra.ruleAnswer || ''
  approval.ruleRetrievalError = extra.ruleRetrievalError
  approval.autoApproveRequested = extra.autoApproveRequested
  if (!approval.approvalOpinion) {
    approval.approvalOpinion = buildExpenseApprovalOpinion(approval, extra)
  }
  return approval
}

function buildExpenseApprovalOpinion(
  approval: Record<string, unknown>,
  extra: {
    ruleCitations: ReturnType<typeof normalizeRuleCitations>
    ruleRetrievalError?: string
    autoApproveRequested: boolean
  },
) {
  if (extra.ruleRetrievalError) {
    return `审批意见：知识库规则检索失败（${extra.ruleRetrievalError}），本次不自动通过，需人工复核。`
  }
  if (!extra.ruleCitations.length) {
    return '审批意见：未找到可引用的报销制度规则，本次不自动通过，需人工复核。'
  }
  if (approval.autoApproved === true) {
    return '审批意见：制度引用、费用标准、发票和预算均通过，已自动通过。'
  }
  if (approval.approvalStatus === 'APPROVED_ROUTE_READY') {
    return extra.autoApproveRequested
      ? '审批意见：规则校验通过，但未满足自动通过全部条件，建议进入审批流。'
      : '审批意见：规则校验通过，可进入审批流。'
  }
  if (approval.approvalStatus === 'NEED_REVIEW') {
    return '审批意见：存在黄色风险，建议人工复核后再继续审批。'
  }
  if (approval.approvalStatus === 'REJECTED') {
    return '审批意见：存在红色风险，建议退回整改。'
  }
  return '审批意见已生成。'
}

function buildExpenseApprovalText(approval: Record<string, unknown>) {
  const citations = Array.isArray(approval.ruleCitations) ? approval.ruleCitations as Array<{ documentName?: string; chunkNo?: number }> : []
  const riskItems = Array.isArray(approval.riskItems) ? approval.riskItems : []
  const citationText = citations.length
    ? `规则引用 ${citations.map((item) => `${item.documentName || '未知文档'}#${item.chunkNo || '-'}`).join('、')}`
    : '未找到规则引用'
  const autoText = approval.autoApproved === true ? '已自动通过' : '未自动通过'
  return `已完成报销单 ${approval.expenseNo || '-'} 智能审批：状态 ${approval.approvalStatus || '-'}，风险等级 ${approval.riskLevel || '-'}，${autoText}，风险 ${riskItems.length} 项，${citationText}。${approval.approvalOpinion || ''}`
}

function stringArg(value: unknown) {
  return typeof value === 'string' && value.trim() ? value.trim() : undefined
}

function formatKnowledgeAnswer(
  answer: string,
  _citations: Array<{ documentName?: string; chunkNo?: number; text?: string; score?: number }>,
) {
  return answer
}

async function answerGeneralQuestion(
  message: string,
  sessionId: string,
  turnStartedAt: number,
  clientContext?: ClientContext,
): Promise<AgentChatResponse> {
  const apiKey = process.env.GLM_API_KEY || process.env.Z_AI_API_KEY
  if (apiKey) {
    try {
      const completion = await callGlmCompletion(apiKey, [
        {
          role: 'system',
          content: '你是财务数智人，也可以回答通用问题。回答要直接、简洁、中文优先；涉及后台业务数据时不要编造，提醒用户提供具体查询条件或使用工具。',
        },
        { role: 'user', content: message },
      ], [])
      const text = completion.choices?.[0]?.message?.content || ''
      if (text.trim()) {
        return {
          sessionId,
          role: 'assistant',
          type: 'text',
          text,
          clientContext,
          callChain: currentCallChain(sessionId, turnStartedAt),
        }
      }
    } catch (e) {
      console.warn(`general LLM answer failed: ${errorMessage(e)}`)
    }
  }

  return {
    sessionId,
    role: 'assistant',
    type: 'text',
    text: buildLocalGeneralAnswer(message),
    clientContext,
    callChain: currentCallChain(sessionId, turnStartedAt),
  }
}

function buildLegacyGeneralAnswer(message: string) {
  const lowerMessage = message.toLowerCase()
  if (message.includes('你好') || lowerMessage.includes('hello')) {
    return buildGeneralGreeting()
  }
  if (message.includes('发票') && (message.includes('检验') || message.includes('校验') || message.includes('查验') || message.includes('验真'))) {
    return '可以。请提供发票图片、发票号码、代码、开票日期和金额等信息，我会帮你做发票验真、重复报销风险和字段一致性检查。'
  }
  if (message.includes('知识库') || message.includes('文档') || message.includes('上传')) {
    return '可以先到知识库页面上传制度、合同、报销说明等文档，然后直接提问。我会优先检索文档内容并给出答案。'
  }
  if (message.includes('你能做什么') || message.includes('怎么用')) {
    return '你可以直接问我问题，比如“检验这张发票是否重复”“查询2026-05应收看板”“根据上传文档回答住宿标准是多少钱”。如果先在知识库上传文档，我会优先从文档里找依据。'
  }
  if (message.includes('住宿') || message.includes('差旅') || message.includes('报销标准') || message.includes('费用标准')) {
    return '我需要以你上传的制度文档或具体报销规则为准。请确认知识库里已经上传对应制度，或者把适用城市、人员类型、入住方式和金额发给我，我可以帮你判断是否超标。'
  }
  return '这个问题我还需要更多上下文才能准确回答。你可以补充具体对象、时间、金额、单据或相关制度文档；如果是普通问题，也可以直接换一种更明确的问法。'
}

function isGreeting(message: string) {
  const normalized = message.replace(/[!！。,.，\s]/g, '').toLowerCase()
  return ['你好', '您好', 'hello', 'hi', '嗨'].includes(normalized)
}

const FINANCE_VOCAB = /财务|发票|报销|凭证|报表|账龄|应收|应付|预算|税务|税|资产|合同|付款|银行|现金流|利润|资产负债|住宿|差旅|费用|审批|知识库|文档|制度|手册|资料|看板|对账|结算|核销|科目|分录|总账|明细|存货|成本|收入|支出|单据|票据|税务申报|纳税|开票|红冲|作废|验真|查重|抵扣|折旧|摊销|盘点|报废|预算执行|资金|融资|投资|股权|分红|工资|薪酬|社保|公积金|个税|坐席|一等座|二等座|商务座|火车|高铁|动车|飞机|经济舱|头等舱/

function isGeneralChat(message: string) {
  const m = message.trim()
  if (!m) return false
  if (isGreeting(m)) return true
  if (/^(谢谢|感谢|辛苦|多谢|thanks|thank you|thx|再见|拜拜|晚安|早安|早上好|下午好|晚上好|嗨|哈喽)[!！。.~]*/i.test(m)) return true
  if (/^[\s\d+\-*/().×÷%^]+[=?？]?$/.test(m)) return true
  if (/^[\d]+[\s]*[+\-*/×÷][\s]*[\d]+/.test(m)) return true
  if (/几加几|几减几|几乘几|几除以|等于多少|算一下|算一算|计算/.test(m) && !FINANCE_VOCAB.test(m)) return true
  if (/(今天|现在|当前|这会儿)\s*(星期几|周几|几号|什么日期|几点|什么时间)/.test(m)) return true
  if (/^(今天|明天|昨天|后天|大后天)\s*(天气|温度|几度|热不热|冷不冷)/.test(m)) return true
  if (/讲个(笑话|故事)|说个笑话|无聊|陪我聊天|聊聊天|唱首歌/.test(m)) return true
  if (!FINANCE_VOCAB.test(m)) {
    if (/^.{0,8}(是什么意思|是什么|什么含义|怎么理解)/.test(m)) return true
  }
  return false
}

function isCapabilityQuestion(message: string) {
  const m = message.trim()
  if (!m) return false
  if (/你是谁|你叫什么|你的名字|你是机器人|你是AI|你是人工|你是什么|你是干嘛的|介绍一下你自己|介绍一下你|自我介绍/.test(m)) return true
  if (/你能做什么|你可以做什么|你会做什么|你能做啥|你能干嘛|你能干啥|都能做什么|都可以做什么|都能干啥|你会干啥|你会啥|你会什么/.test(m)) return true
  if (/有(哪些|什么)(功能|能力|本事|用处)|功能介绍|能做哪些(事|事情)|能帮我(做|干)什么|能提供什么|支持什么|可以帮我(做|干)什么/.test(m)) return true
  return false
}

function buildCapabilityIntro() {
  return [
    '你好，我是财务数智人助手。我的核心能力是基于「知识库」里上传的文档回答问题。',
    '',
    '我可以帮你做这些事：',
    '1. 知识库问答：上传制度、手册、合同、报销说明等文档后，我可以基于文档内容回答制度、流程、标准类问题（例如住宿标准、报销流程）。',
    '2. 财务查询：查询财务报表（资产负债表、利润表、现金流量表）、凭证、发票、费用报销单、部门预算等。',
    '3. 单据识别：识别发票或单据图片，提取结构化字段。',
    '',
    '建议先到「知识库」上传文档，然后直接提问。其他不相关的问题我也会正常回答，但不会拿知识库硬凑答案。',
  ].join('\n')
}

function buildGeneralGreeting() {
  return '你好！我是财务数智人助手。我可以帮你做这些事：\n- 根据知识库文档回答制度、流程、标准类问题（比如住宿标准、报销流程）\n- 查询应收账龄看板、检验发票是否重复等财务工具\n- 也能和你正常聊天，回答一些日常问题\n\n直接告诉我你需要什么就行。'
}

async function callGeneralLlm(message: string): Promise<string | null> {
  const apiKey = process.env.GLM_API_KEY || process.env.Z_AI_API_KEY
  const apiUrl = process.env.GLM_API_URL
  if (!apiKey || !apiUrl) return null
  try {
    const controller = new AbortController()
    const timer = setTimeout(() => controller.abort(), 12000)
    const resp = await fetch(apiUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${apiKey}` },
      body: JSON.stringify({
        model: process.env.DOCUMENT_RAG_CHAT_MODEL || process.env.GLM_MODEL || 'glm-4-flash',
        temperature: 0.5,
        messages: [
          { role: 'system', content: '你是财务数智人助手。当用户的问题与财务系统操作、知识库文档无关时，用简洁友好的中文自然回答（闲聊、常识、算术、时间日期等都可以直接作答）。不要编造任何财务数据或制度内容。回答控制在 3 句话以内。' },
          { role: 'user', content: message },
        ],
      }),
      signal: controller.signal,
    })
    clearTimeout(timer)
    if (!resp.ok) return null
    const data: any = await resp.json()
    const text = data?.choices?.[0]?.message?.content
    return text ? String(text).trim() : null
  } catch {
    return null
  }
}

async function tryAnswerGeneral(
  message: string,
  sessionId: string,
  turnStartedAt: number,
  clientContext: any,
) {
  const llmText = await callGeneralLlm(message)
  const text = llmText || buildLocalGeneralAnswer(message)
  return {
    sessionId,
    role: 'assistant' as const,
    type: 'text' as const,
    text,
    clientContext,
    callChain: currentCallChain(sessionId, turnStartedAt),
  }
}

function buildLocalGeneralAnswer(message: string) {
  const lowerMessage = message.toLowerCase()
  if (message.includes('你好') || lowerMessage.includes('hello')) {
    return buildGeneralGreeting()
  }
  if (/你是谁|你叫什么|你能做什么|你能帮我|你会什么|介绍一下/.test(message)) {
    return buildGeneralGreeting()
  }
  if (message.includes('发票') && (message.includes('检验') || message.includes('校验') || message.includes('查验') || message.includes('验真'))) {
    return '可以。请提供发票图片、发票号码、代码、开票日期和金额等信息，我会帮你做发票验真、重复报销风险和字段一致性检查。'
  }
  if (message.includes('知识库') || message.includes('文档') || message.includes('上传')) {
    return '可以先到知识库页面上传制度、合同、报销说明等文档，然后直接提问。我会优先检索文档内容并给出答案。'
  }
  if (message.includes('住宿') || message.includes('差旅') || message.includes('报销标准') || message.includes('费用标准')) {
    return '我需要以你上传的制度文档或具体报销规则为准。请确认知识库里已经上传对应制度，或者把适用城市、人员类型、入住方式和金额发给我，我可以帮你判断是否超标。'
  }
  return '这个问题我还需要更多上下文才能准确回答。你可以补充具体对象、时间、金额、单据或相关制度文档；如果是普通问题，也可以直接换一种更明确的问法。'
}

function buildKnowledgeGreeting() {
  return [
    '你好，我是财务数智人。',
    '',
    '当前左侧已打开对话、报销和知识库三个工作区。知识库场景下，我会优先根据你上传的制度、手册和说明文档检索答案。',
    '',
    '你可以这样问：',
    '- “住宿费报销标准是多少？”',
    '- “这份制度里审批流程怎么规定？”',
    '- “这张报销单应该引用哪条规则？”',
    '- “固定资产验收需要哪些材料？”',
    '',
    '如果知识库里没有相关依据，我会明确告诉你没有检索到，不会编造制度内容。',
  ].join('\n')
}

async function tryRunLlmAgent(message: string, sessionId: string, turnStartedAt: number) {
  if (process.env.AGENT_MODE !== 'llm') {
    return undefined
  }
  const apiKey = process.env.GLM_API_KEY || process.env.Z_AI_API_KEY
  if (!apiKey) {
    console.warn('AGENT_MODE=llm but GLM_API_KEY/Z_AI_API_KEY is not configured; using rule-based fallback')
    return undefined
  }

  try {
    return await runLlmAgent(message, sessionId, turnStartedAt, apiKey)
  } catch (e) {
    console.warn(`LLM agent failed; using rule-based fallback: ${errorMessage(e)}`)
    return undefined
  }
}

async function runLlmAgent(
  message: string,
  sessionId: string,
  turnStartedAt: number,
  apiKey: string,
): Promise<AgentChatResponse> {
  const discovered = await listTools()
  const tools = discovered.tools.map((tool) => ({
    type: 'function',
    function: {
      name: tool.name,
      description: tool.description || tool.name,
      parameters: tool.inputSchema || { type: 'object', properties: {} },
    },
  }))
  const messages: LlmMessage[] = [
    {
      role: 'system',
      content: [
        '你是财务数智人。必须使用提供的工具查询或执行财务操作，不得编造后台数据。',
        '涉及写入、审核、报销、凭证生成时，先核对工具返回的状态，再用简洁中文说明结果。',
        '链式任务可连续调用多个工具，例如先 OCR 再创建凭证。',
        '工具使用边界：',
        '- knowledge_chat（知识库）只在用户明确要求基于上传文档/制度/手册回答，或问题涉及公司制度、流程、标准、规范等内容时使用。',
        '- 闲聊、打招呼、身份询问、算术、时间日期、天气等日常问题不要调用任何工具，直接用中文自然回答。',
        '- 应收账龄、发票验真、凭证、报表等结构化查询使用对应的财务工具，不要用 knowledge_chat。',
        '- 如果用户问题与财务、文档都无关，直接回答即可，禁止强行调用 knowledge_chat。',
        '介绍自身能力时要克制、诚实，只说核心能力：知识库文档问答、财务报表与凭证查询、发票与费用报销查询、预算查询、单据图片识别。不要宣传浏览器自动化、视频分析、通用图片分析、数据可视化、UI 对比等与财务无关的通用能力，也不要罗列一堆用户用不到的功能。',
      ].join('\n'),
    },
    { role: 'user', content: message },
  ]
  const invokedTools: Array<{ name: string; arguments: Record<string, unknown> }> = []
  let lastContent: unknown

  for (let round = 0; round < 6; round++) {
    const completion = await callGlmCompletion(apiKey, messages, tools)
    const assistant = completion.choices?.[0]?.message
    if (!assistant) {
      throw new Error('GLM response has no assistant message')
    }
    const toolCalls = assistant.tool_calls || []
    messages.push({
      role: 'assistant',
      content: assistant.content || null,
      tool_calls: toolCalls,
    })

    if (!toolCalls.length) {
      const text = assistant.content || '工具调用已完成。'
      const type = inferStructuredResponseType(lastContent)
      await persistInvocationAudits(sessionId, message, invokedTools, text, turnStartedAt)
      return {
        sessionId,
        role: 'assistant',
        type,
        text,
        content: type === 'text' ? undefined : lastContent,
        toolCall: invokedTools.length ? invokedTools[invokedTools.length - 1] : undefined,
        toolCalls: invokedTools,
        clientContext: sessions.get(sessionId)?.clientContext,
        callChain: currentCallChain(sessionId, turnStartedAt),
      }
    }

    for (const toolCall of toolCalls) {
      const args = normalizeLlmToolArguments(toolCall.function.name, parseToolArguments(toolCall.function.arguments))
      invokedTools.push({ name: toolCall.function.name, arguments: args })
      const response = await callTool({
        jsonrpc: '2.0',
        id: toolCall.id,
        method: 'tools/call',
        params: {
          name: toolCall.function.name,
          arguments: args,
          _meta: buildToolMeta(sessionId),
        },
      })
      lastContent = extractToolResponseData(response)
      messages.push({
        role: 'tool',
        tool_call_id: toolCall.id,
        content: truncateForLlm(JSON.stringify(response)),
      })
    }
  }

  throw new Error('GLM tool-call rounds exceeded limit')
}

async function callGlmCompletion(apiKey: string, messages: LlmMessage[], tools: unknown[]) {
  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), Number(process.env.GLM_TIMEOUT_MS || 60000))
  try {
    const response = await fetch(process.env.GLM_API_URL || 'https://open.bigmodel.cn/api/paas/v4/chat/completions', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${apiKey}`,
      },
      body: JSON.stringify({
        model: process.env.GLM_MODEL || 'glm-4-flash',
        messages,
        ...(tools.length ? { tools, tool_choice: 'auto' } : {}),
        temperature: 0.1,
      }),
      signal: controller.signal,
    })
    if (!response.ok) {
      throw new Error(`GLM HTTP ${response.status}: ${truncateForLlm(await response.text(), 500)}`)
    }
    return (await response.json()) as {
      choices?: Array<{
        message?: {
          content?: string | null
          tool_calls?: LlmToolCall[]
        }
      }>
    }
  } finally {
    clearTimeout(timeout)
  }
}

function parseToolArguments(value: string) {
  try {
    const parsed = JSON.parse(value || '{}')
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed)
      ? parsed as Record<string, unknown>
      : {}
  } catch {
    return {}
  }
}

function normalizeLlmToolArguments(toolName: string, args: Record<string, unknown>) {
  const normalized = { ...args }
  if (toolName === 'query_expense' && typeof normalized.status === 'string') {
    normalized.status = normalizeExpenseStatusValue(normalized.status)
  }
  if (toolName === 'generate_report' && typeof normalized.reportType === 'string') {
    normalized.reportType = normalizeReportTypeValue(normalized.reportType)
  }
  return normalized
}

function normalizeExpenseStatusValue(status: string) {
  if (!status) return status
  const upperStatus = status.toUpperCase()
  if (['PENDING', 'NEED_REVIEW', 'APPROVED', 'REJECTED', 'APPROVED_ROUTE_READY'].includes(upperStatus)) return upperStatus
  if (status.includes('待审批') || status.includes('待审核') || status.includes('待处理')) return 'PENDING'
  if (status.includes('待复核') || status.includes('复核')) return 'NEED_REVIEW'
  if (status.includes('已通过') || status.includes('已审批') || status.includes('通过')) return 'APPROVED'
  if (status.includes('驳回') || status.includes('拒绝')) return 'REJECTED'
  return status
}

function normalizeReportTypeValue(reportType: string) {
  if (!reportType) return reportType
  if (['balance_sheet', 'income_statement', 'cash_flow'].includes(reportType)) return reportType
  if (reportType.includes('资产负债')) return 'balance_sheet'
  if (reportType.includes('利润')) return 'income_statement'
  if (reportType.includes('现金流')) return 'cash_flow'
  return reportType
}

function extractToolResponseData(response: JsonRpcResponse) {
  const result = response.result as {
    structuredContent?: { result?: unknown }
  } | undefined
  return unwrapToolData(result?.structuredContent?.result)
}

function inferStructuredResponseType(content: unknown): AgentChatResponse['type'] {
  if (Array.isArray(content)) {
    return 'table'
  }
  if (content && typeof content === 'object') {
    const data = content as Record<string, unknown>
    if (Array.isArray(data.series) || data.xAxis || data.yAxis) {
      return 'chart'
    }
    return 'card'
  }
  return 'text'
}

function truncateForLlm(value: string, maxLength = 20000) {
  return value.length <= maxLength ? value : `${value.slice(0, maxLength)}...[truncated]`
}

async function handleAgentChatStream(req: IncomingMessage, res: ServerResponse) {
  const url = new URL(req.url || '/agent/chat/stream', `http://${req.headers.host || 'localhost'}`)
  const request: AgentChatRequest = {
    message: url.searchParams.get('message') || '',
    sessionId: url.searchParams.get('sessionId') || undefined,
    clientContext: sanitizeClientContext({
      channel: url.searchParams.get('channel') || undefined,
      userId: url.searchParams.get('userId') || undefined,
      userName: url.searchParams.get('userName') || undefined,
      device: url.searchParams.get('device') || undefined,
    }),
  }

  res.writeHead(200, {
    'Content-Type': 'text/event-stream; charset=utf-8',
    'Cache-Control': 'no-cache, no-transform',
    Connection: 'keep-alive',
  })
  res.flushHeaders()
  writeSse(res, 'status', { phase: 'retrieving', message: '正在检索知识库' })

  try {
    const response = await handleAgentChat(request, req)
    const tokenDelayMs = parseStreamTokenDelayMs()
    for (const token of chunkText(response.text, 6)) {
      writeSse(res, 'token', { token })
      if (tokenDelayMs > 0) await sleep(tokenDelayMs)
    }
    if (response.type !== 'text' && response.content !== undefined) {
      writeSse(res, response.type, { content: response.content })
    }
    writeSse(res, 'done', response)
  } catch (e) {
    writeSse(res, 'stream_error', { message: errorMessage(e) })
  } finally {
    res.end()
  }
}

function chunkText(text: string, size: number) {
  const chunks: string[] = []
  for (let i = 0; i < text.length; i += size) {
    chunks.push(text.slice(i, i + size))
  }
  return chunks.length ? chunks : ['']
}

function parseStreamTokenDelayMs() {
  const value = Number(process.env.AGENT_STREAM_TOKEN_DELAY_MS || 60)
  if (!Number.isFinite(value)) return 60
  return Math.max(0, Math.min(value, 200))
}

function sleep(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

function writeSse(res: ServerResponse, event: string, data: unknown) {
  res.write(`event: ${event}\ndata: ${JSON.stringify(data)}\n\n`)
}

function isKnowledgeIntent(message: string) {
  return (
    message.includes('知识库') ||
    message.includes('文档') ||
    message.includes('上传的') ||
    message.includes('资料') ||
    message.includes('制度') ||
    message.includes('手册') ||
    message.includes('根据') && (message.includes('内容') || message.includes('材料') || message.includes('文件'))
  )
}

function isPolicyInquiryIntent(message: string) {
  if (!FINANCE_VOCAB.test(message)) return false
  if (isExplicitToolExecutionIntent(message)) return false
  if (/(可以|能|能不能|能不能够|能不能|是否|能否).*(报销|入账|报|列入)/.test(message)) return true
  if (/(报销|费用|差旅|住宿|交通|餐补|补贴|交通费|打车|机票|高铁|火车|飞机|出租车|招待).*(标准|限额|上限|额度|范围|规定|比例)/.test(message)) return true
  if (/(标准|限额|上限|额度|规定|范围|比例).*(报销|费用|差旅|住宿|交通|餐补|补贴|招待)/.test(message)) return true
  if (/(坐席|一等座|二等座|商务座|头等舱|经济舱|星级|房型|单间|标间).*/.test(message)) return true
  if (/(住宿|差旅|出差|打车|机票|高铁|火车|飞机|出租车|报销|餐补|补贴|招待费|办公费|培训费)/.test(message) && /[？?吗么呢吧]|是不是|能不能|可不可以|可否|可以吗/.test(message)) return true
  return false
}

function isFinanceToolIntent(message: string) {
  if (isConceptualQuestion(message) && !isExplicitToolExecutionIntent(message)) {
    return false
  }
  return (
    isInvoiceToVoucherIntent(message) ||
    isInvoiceVerificationIntent(message) ||
    isBasicInvoiceCheckIntent(message) ||
    isInvoiceDuplicateIntent(message) ||
    isInvoiceInputIntent(message) ||
    isTaxReturnIntent(message) ||
    isTaxCalculationIntent(message) ||
    isTaxPolicyIntent(message) ||
    isAgingAnalysisIntent(message) ||
    isCollectionAdviceIntent(message) ||
    isStandardArApReconcileIntent(message) ||
    isCollectionPlanIntent(message) ||
    isPaymentPlanIntent(message) ||
    isCounterpartyReconcileIntent(message) ||
    isBankReconcileIntent(message) ||
    isBankTransactionIntent(message) ||
    isStandardVoucherAuditIntent(message) ||
    isVoucherAuditIntent(message) ||
    isVoucherQueryIntent(message) ||
    isComplianceRiskIntent(message) ||
    isCashManagementIntent(message) ||
    isContractAssetIntent(message) ||
    isDataIntegrationIntent(message) ||
    isAuditLogIntent(message) ||
    isExpenseApprovalIntent(message) ||
    isExpenseQueryIntent(message) ||
    isExpenseBudgetRemainingIntent(message) ||
    isExpenseCreateIntent(message) ||
    isVarianceDiagnosisIntent(message) ||
    isFinancialRatioIntent(message) ||
    isFinancialTrendIntent(message) ||
    isFinancialAnomalyIntent(message) ||
    isBudgetControlIntent(message) ||
    isMonthEndCloseIntent(message) ||
    isAccountMappingIntent(message) ||
    message.includes('应收') ||
    message.includes('回款') ||
    message.includes('账龄') ||
    message.includes('凭证') ||
    message.includes('记账') ||
    message.includes('发票') ||
    message.includes('报表') ||
    message.includes('资产负债') ||
    message.includes('利润') ||
    message.includes('现金流') ||
    message.includes('报销') ||
    message.includes('费用') ||
    message.includes('分析')
  )
}

function isConceptualQuestion(message: string) {
  return (
    message.includes('怎么') ||
    message.includes('如何') ||
    message.includes('为什么') ||
    message.includes('规则') ||
    message.includes('标准') ||
    message.includes('是什么') ||
    message.includes('有哪些') ||
    message.includes('有什么') ||
    message.includes('说明一下') ||
    message.includes('介绍一下')
  )
}

function isExplicitToolExecutionIntent(message: string, imageUrl?: string) {
  if (imageUrl) return true
  if (/(INV|EXP|VOU|AR|AP|BANK|CTR|FA)-?\d{4,}/i.test(message)) return true
  if (message.includes('这张') || message.includes('该发票') || message.includes('该报销单') || message.includes('该凭证')) return true
  if (message.includes('发票号') || message.includes('发票号码') || message.includes('报销单号') || message.includes('凭证号')) return true
  if (message.includes('生成') || message.includes('创建') || message.includes('新增') || message.includes('录入')) return true
  if (message.includes('提交审批') || message.includes('发起审批') || message.includes('执行审批')) return true
  if (message.includes('查重') || message.includes('重复报销') || message.includes('重复入账')) return true
  return false
}

async function handleInvoiceToVoucher(
  sessionId: string,
  turnStartedAt: number,
  message: string,
  imageUrl?: string,
): Promise<AgentChatResponse> {
  const ocrCall = {
    name: 'extract_text_from_screenshot',
    arguments: { imageUrl: imageUrl || 'demo://invoice' },
  }
  const ocrResponse = await callTool({
    jsonrpc: '2.0',
    id: Date.now(),
    method: 'tools/call',
    params: {
      name: ocrCall.name,
      arguments: ocrCall.arguments,
      _meta: buildToolMeta(sessionId),
    },
  })

  const invoice = extractStructuredResult<Record<string, unknown>>(ocrResponse)
  const amount = numberValue(invoice.amount, 120000)
  const voucherCall = {
    name: 'create_voucher',
    arguments: {
      voucherDate: String(invoice.invoiceDate || '2026-05-31'),
      summary: `${invoice.sellerName || '供应商'}发票自动记账`,
      entries: [
        { accountCode: '6601', accountName: '销售费用', debitAmount: amount, creditAmount: 0 },
        { accountCode: '2202', accountName: '应付账款', debitAmount: 0, creditAmount: amount },
      ],
    },
  }
  const voucherResponse = await callTool({
    jsonrpc: '2.0',
    id: Date.now() + 1,
    method: 'tools/call',
    params: {
      name: voucherCall.name,
      arguments: voucherCall.arguments,
      _meta: buildToolMeta(sessionId),
    },
  })
  const voucher = extractStructuredResult<Record<string, unknown>>(voucherResponse)
  const text = `已完成发票识别并生成凭证 ${voucher.voucherNo || ''}。识别发票号：${invoice.invoiceNo || '-'}，不含税金额：${formatMoney(amount)}。`
  await persistInvocationAudits(sessionId, message, [ocrCall, voucherCall], text, turnStartedAt)

  return {
    sessionId,
    role: 'assistant',
    type: 'card',
    text,
    content: {
      invoice,
      voucher,
    },
    toolCall: voucherCall,
    toolCalls: [ocrCall, voucherCall],
    clientContext: sessions.get(sessionId)?.clientContext,
    callChain: currentCallChain(sessionId, turnStartedAt),
  }
}

function ensureSession(sessionId?: string, clientContext?: ClientContext) {
  if (sessionId && sessions.has(sessionId)) {
    const session = sessions.get(sessionId)
    if (session) {
      session.lastSeenAt = Date.now()
      if (clientContext) {
        session.clientContext = mergeClientContext(session.clientContext, clientContext)
      }
    }
    return sessionId
  }
  const created = randomUUID()
  sessions.set(created, {
    sessionId: created,
    createdAt: Date.now(),
    lastSeenAt: Date.now(),
    clientContext,
    auditLogs: [],
  })
  return created
}

function buildToolMeta(sessionId: string) {
  const session = sessions.get(sessionId)
  const meta: Record<string, unknown> = { sessionId }
  if (session?.clientContext) {
    meta.clientContext = session.clientContext
  }
  return meta
}

function inferToolCall(message: string): {
  name: string
  arguments: Record<string, unknown>
  responseType: AgentChatResponse['type']
} {
  const lower = message.toLowerCase()
  if (isAuditLogIntent(message)) {
    return {
      name: 'query_audit_logs',
      arguments: {
        sessionId: extractAuditSessionId(message),
        toolName: inferAuditToolName(message),
        status: message.includes('失败') || message.includes('错误') ? 'error' : undefined,
        channel: message.includes('钉钉') ? 'dingtalk' : undefined,
        pageNo: 1,
        pageSize: 10,
      },
      responseType: 'table',
    }
  }
  if (isDataIntegrationIntent(message)) {
    return {
      name: 'query_data_integration',
      arguments: {
        period: extractDataIntegrationPeriod(message),
        systemType: inferIntegrationSystemType(message),
        syncMode: inferIntegrationSyncMode(message),
        includeQualityDetails: true,
      },
      responseType: 'card',
    }
  }
  if (isBasicInvoiceCheckIntent(message)) {
    const invoiceNo = extractInvoiceNo(message)
    return {
      name: 'check_invoice',
      arguments: {
        invoiceCode: extractInvoiceCode(message, invoiceNo),
        invoiceNo,
      },
      responseType: 'card',
    }
  }
  if (isInvoiceDuplicateIntent(message)) {
    return {
      name: 'check_invoice_duplicate',
      arguments: buildInvoiceInputArguments(message),
      responseType: 'card',
    }
  }
  if (isInvoiceInputIntent(message)) {
    return {
      name: 'input_invoice',
      arguments: {
        ...buildInvoiceInputArguments(message),
        source: message.includes('钉钉') ? 'DINGTALK' : (message.includes('OCR') || message.includes('识别') ? 'OCR' : 'MANUAL'),
        autoVerify: message.includes('自动验真') || message.includes('验真'),
      },
      responseType: 'card',
    }
  }
  if (isInvoiceVerificationIntent(message)) {
    return {
      name: 'verify_invoice',
      arguments: {
        invoiceNo: extractInvoiceNo(message),
        invoiceType: inferInvoiceType(message),
        amount: extractInvoiceAmount(message),
        taxAmount: extractInvoiceTaxAmount(message),
        includeDeductionCheck: true,
        includeArchiveCheck: true,
      },
      responseType: 'card',
    }
  }
  if (isExpenseBudgetRemainingIntent(message)) {
    return {
      name: 'query_budget_remaining',
      arguments: {
        department: inferDepartment(message) === 'all' ? '销售部' : inferDepartment(message),
        period: extractPeriod(message),
      },
      responseType: 'card',
    }
  }
  if (isExpenseCreateIntent(message)) {
    const amount = extractAmount(message, 3600)
    return {
      name: 'create_expense',
      arguments: {
        employeeId: extractEmployeeId(message),
        employeeName: message.includes('张三') ? '张三' : '李四',
        department: inferDepartment(message) === 'all' ? '销售部' : inferDepartment(message),
        projectCode: inferProjectCode(message),
        expenseType: inferExpenseType(message),
        expenseDate: inferVoucherDate(message),
        description: inferExpenseDescription(message),
        amount,
        invoiceNos: message.includes('发票') ? [extractInvoiceNo(message)] : [],
        attachments: message.includes('附件') || message.includes('行程单') ? ['expense-attachment-demo.pdf'] : [],
        submitForApproval: message.includes('提交') || message.includes('发起'),
      },
      responseType: 'card',
    }
  }
  if (isExpenseQueryIntent(message)) {
    return {
      name: 'query_expense',
      arguments: {
        employeeId: extractEmployeeIdIfPresent(message),
        status: inferExpenseStatus(message),
      },
      responseType: 'table',
    }
  }
  if (isExpenseApprovalIntent(message)) {
    const amount = extractAmount(message, message.includes('大额') || message.includes('异常') ? 8600 : 3200)
    const risky = message.includes('异常') || message.includes('重复') || message.includes('超标') || message.includes('预算不足') || message.includes('三单')
    return {
      name: 'approve_expense',
      arguments: {
        expenseNo: extractExpenseNo(message),
        employeeId: 'E1002',
        employeeName: '李四',
        employeeLevel: message.includes('经理') ? 'manager' : 'staff',
        department: inferDepartment(message) === 'all' ? '销售部' : inferDepartment(message),
        projectCode: message.includes('研发') ? 'RD-2026-AI' : 'PRJ-2026-SALES',
        expenseType: inferExpenseType(message),
        cityTier: message.includes('一线') ? '一线' : '二线',
        submitDate: '2026-05-16',
        invoiceNo: message.includes('缺票') ? '' : (message.includes('重复') ? 'INV-202605-001' : 'INV-202605-003'),
        purchaseOrderNo: message.includes('采购') || message.includes('三单') ? 'PO-202605-009' : undefined,
        receiptNo: message.includes('采购') || message.includes('三单') ? 'GR-202605-007' : undefined,
        description: risky ? '采购类报销，需检查三单匹配、预算和发票风险' : '差旅交通报销，系统自动审批路由',
        amount,
        invoiceAmount: amount,
        orderAmount: risky ? amount + 1500 : amount,
        receiptAmount: risky ? amount - 900 : amount,
        availableBudget: message.includes('预算不足') ? amount - 1000 : amount + 12000,
        invoiceVerified: !message.includes('验真失败') && !message.includes('假票'),
        duplicateInvoice: message.includes('重复'),
        autoApproveEnabled: isAutoApproveRequested(message),
      },
      responseType: 'card',
    }
  }
  if (isFinancialRatioIntent(message)) {
    return {
      name: 'calculate_financial_ratios',
      arguments: { period: extractPeriod(message) },
      responseType: 'table',
    }
  }
  if (isFinancialTrendIntent(message)) {
    return {
      name: 'analyze_financial_trend',
      arguments: {
        startDate: extractTrendStartPeriod(message),
        endDate: extractTrendEndPeriod(message),
        metric: inferTrendMetric(message),
      },
      responseType: 'chart',
    }
  }
  if (isFinancialAnomalyIntent(message)) {
    return {
      name: 'detect_financial_anomalies',
      arguments: { period: extractPeriod(message) },
      responseType: 'table',
    }
  }
  if (isVarianceDiagnosisIntent(message)) {
    return {
      name: 'diagnose_financial_variance',
      arguments: {
        period: extractPeriod(message),
        metricName: inferVarianceMetric(message),
        department: inferDepartment(message) === 'all' ? '销售部' : inferDepartment(message),
        actualAmount: extractAmount(message, 286000),
        budgetAmount: message.includes('预算') ? 240000 : 240000,
        previousAmount: 255000,
        includeActionPlan: true,
      },
      responseType: 'card',
    }
  }
  if (isBudgetControlIntent(message)) {
    const department = inferDepartment(message)
    return {
      name: 'evaluate_budget_control',
      arguments: {
        period: extractPeriod(message),
        department: department === 'all' ? '销售部' : department,
        projectCode: inferProjectCode(message),
        expenseType: inferExpenseType(message),
        requestedAmount: extractAmount(message, message.includes('大额') || message.includes('超支') ? 98000 : 36000),
        scenario: inferBudgetScenario(message),
        includeForecast: true,
      },
      responseType: 'card',
    }
  }
  if (isMonthEndCloseIntent(message)) {
    return {
      name: 'run_month_end_close',
      arguments: {
        period: extractPeriod(message),
        entityName: inferEntityName(message),
        closeType: inferCloseType(message),
        includeChecklist: true,
        forceClose: message.includes('强制') || message.includes('先关账'),
      },
      responseType: 'card',
    }
  }
  if (isAccountMappingIntent(message)) {
    return {
      name: 'recommend_account_mapping',
      arguments: {
        documentType: inferMappingDocumentType(message),
        businessScenario: inferMappingScenario(message),
        summary: inferMappingSummary(message),
        counterpartyName: inferCounterpartyName(message),
        department: inferDepartment(message) === 'all' ? '销售部' : inferDepartment(message),
        projectCode: inferProjectCode(message),
        amount: extractAmount(message, 42000),
        taxAmount: message.includes('含税') || message.includes('专票') || message.includes('进项') ? Math.round(extractAmount(message, 42000) * 0.13 * 100) / 100 : 0,
        includeVoucherPreview: true,
      },
      responseType: 'card',
    }
  }
  if (isTaxReturnIntent(message)) {
    return {
      name: 'generate_tax_return',
      arguments: { taxType: inferTaxType(message), period: extractPeriod(message) },
      responseType: 'card',
    }
  }
  if (isTaxCalculationIntent(message)) {
    return {
      name: 'calculate_tax',
      arguments: {
        taxType: inferTaxType(message),
        period: extractPeriod(message),
        taxableAmount: extractAmount(message, 1286500),
        taxRate: inferTaxRate(message),
        deductibleAmount: message.includes('抵扣') || message.includes('进项') ? 102430 : 0,
      },
      responseType: 'card',
    }
  }
  if (isTaxPolicyIntent(message)) {
    return {
      name: 'webReader',
      arguments: { url: 'https://www.chinatax.gov.cn/', keyword: message },
      responseType: 'card',
    }
  }
  if (isAgingAnalysisIntent(message)) {
    return {
      name: 'query_ar_ap_aging',
      arguments: {
        type: inferAgingType(message),
        baseDate: extractAgingBaseDate(message),
        partnerId: extractPartnerId(message),
        partnerName: inferExplicitPartnerName(message),
        pageNo: 1,
        pageSize: 20,
      },
      responseType: 'table',
    }
  }
  if (isCollectionAdviceIntent(message)) {
    return {
      name: 'suggest_collection_advice',
      arguments: { customerId: extractCustomerId(message) },
      responseType: 'table',
    }
  }
  if (isStandardArApReconcileIntent(message)) {
    return {
      name: 'reconcile_ar_ap',
      arguments: {
        partnerId: extractPartnerId(message) || 'C1001',
        period: extractPeriod(message),
      },
      responseType: 'card',
    }
  }
  if (isCollectionPlanIntent(message)) {
    return {
      name: 'generate_collection_plan',
      arguments: {
        period: extractPeriod(message),
        customerName: inferCustomerName(message),
        creditLevel: inferCreditLevel(message),
        overdueAmount: extractAmount(message, 218900),
        overdueDays: inferOverdueDays(message),
        owner: '应收会计',
        includeLetter: true,
      },
      responseType: 'card',
    }
  }
  if (isPaymentPlanIntent(message)) {
    const amount = extractAmount(message, 42000)
    return {
      name: 'optimize_payment_plan',
      arguments: {
        period: extractPeriod(message),
        supplierName: inferSupplierName(message),
        payableAmount: amount,
        dueDate: '2026-06-30',
        discountTerm: message.includes('折扣') || message.includes('提前付款') ? '2/10,N/30' : 'N/30',
        cashBalance: message.includes('资金紧张') || message.includes('现金不足') ? amount + 10000 : amount + 250000,
        safetyCashLevel: 120000,
        allowMergePayment: true,
      },
      responseType: 'card',
    }
  }
  if (isCounterpartyReconcileIntent(message)) {
    const internalBalance = extractAmount(message, 218900)
    const hasExplicitDifference = message.includes('差异') || message.includes('不一致') || message.includes('不符')
    return {
      name: 'reconcile_counterparty_balance',
      arguments: {
        period: extractPeriod(message),
        counterpartyName: inferCounterpartyName(message),
        counterpartyType: inferCounterpartyType(message),
        internalBalance,
        counterpartyBalance: hasExplicitDifference ? Math.max(0, internalBalance - 14600) : internalBalance,
        includeConfirmationLetter: true,
      },
      responseType: 'card',
    }
  }
  if (message.includes('应收') || message.includes('回款') || message.includes('账龄')) {
    return { name: 'query_ar_dashboard', arguments: { month: extractPeriod(message) }, responseType: 'chart' }
  }
  if (isBankReconcileIntent(message)) {
    return {
      name: 'reconcile_bank_statement',
      arguments: { accountNo: inferBankAccount(message), period: extractPeriod(message) },
      responseType: 'card',
    }
  }
  if (isBankTransactionIntent(message)) {
    return {
      name: 'query_bank_transactions',
      arguments: { accountNo: inferBankAccount(message), status: message.includes('未达') || message.includes('未匹配') ? 'UNMATCHED' : undefined },
      responseType: 'table',
    }
  }
  if (isStandardVoucherAuditIntent(message)) {
    return {
      name: 'audit_voucher_by_no',
      arguments: {
        voucherNo: extractVoucherNo(message),
        auditor: extractVoucherAuditor(message),
      },
      responseType: 'card',
    }
  }
  if (isVoucherAuditIntent(message)) {
    const amount = extractAmount(message, 126000)
    const shouldCreateRisk = message.includes('异常') || message.includes('三单') || message.includes('超预算') || message.includes('不平')
    return {
      name: 'audit_voucher',
      arguments: {
        voucherNo: extractVoucherNo(message),
        voucherDate: inferVoucherDate(message),
        summary: shouldCreateRisk ? '采购发票自动凭证审核' : '销售回款凭证自动审核',
        businessType: message.includes('采购') || message.includes('三单') ? 'purchase' : 'sales',
        period: extractPeriod(message),
        documentDate: message.includes('跨期') ? '2026-04-30' : inferVoucherDate(message),
        relatedInvoiceNo: message.includes('重复') ? 'INV-202605-001' : 'INV-202605-003',
        purchaseOrderNo: shouldCreateRisk ? 'PO-202605-009' : 'PO-202605-001',
        receiptNo: shouldCreateRisk ? 'GR-202605-007' : 'GR-202605-001',
        invoiceAmount: amount,
        orderAmount: shouldCreateRisk ? amount + 3200 : amount,
        receiptAmount: shouldCreateRisk ? amount - 1800 : amount,
        budgetAmount: message.includes('超预算') ? amount - 10000 : amount + 20000,
        preparerId: 'E1001',
        reviewerId: message.includes('同一人') || message.includes('岗位') ? 'E1001' : 'E2001',
        entries: buildVoucherAuditEntries(amount, shouldCreateRisk || message.includes('不平')),
      },
      responseType: 'card',
    }
  }
  if (isVoucherQueryIntent(message)) {
    const voucherNo = extractExplicitVoucherNo(message)
    if (voucherNo && (message.includes('明细') || message.includes('详情') || message.includes('查看') || message.includes('查询'))) {
      return {
        name: 'get_voucher',
        arguments: { voucherNo },
        responseType: 'card',
      }
    }
    return {
      name: 'query_vouchers',
      arguments: {
        voucherNo,
        period: extractPeriod(message),
        status: inferVoucherStatus(message),
        accountCode: inferVoucherAccountCode(message),
        summaryKeyword: inferVoucherSummaryKeyword(message),
        pageNo: 1,
        pageSize: 10,
      },
      responseType: 'table',
    }
  }
  if (isComplianceRiskIntent(message)) {
    return {
      name: 'assess_compliance_risk',
      arguments: {
        period: extractPeriod(message),
        scenario: inferComplianceScenario(message),
        minSeverity: message.includes('高风险') || message.includes('严重') ? 'HIGH' : 'LOW',
      },
      responseType: 'card',
    }
  }
  if (isCashManagementIntent(message)) {
    return {
      name: 'forecast_cash_flow',
      arguments: {
        startPeriod: extractCashStartPeriod(message),
        months: inferForecastMonths(message),
        scenario: inferCashScenario(message),
        currency: 'CNY',
      },
      responseType: 'card',
    }
  }
  if (isContractAssetIntent(message)) {
    return {
      name: 'query_contract_assets',
      arguments: {
        period: extractContractAssetPeriod(message),
        scope: inferContractAssetScope(message),
        department: inferDepartment(message),
        reminderDays: inferReminderDays(message),
      },
      responseType: 'card',
    }
  }
  if (message.includes('凭证') || message.includes('记账')) {
    return {
      name: 'create_voucher',
      arguments: {
        voucherDate: '2026-05-31',
        summary: '销售回款入账',
        entries: [
          { accountCode: '1002', accountName: '银行存款', debitAmount: 120000, creditAmount: 0 },
          { accountCode: '1122', accountName: '应收账款', debitAmount: 0, creditAmount: 120000 },
        ],
      },
      responseType: 'card',
    }
  }
  if (message.includes('发票') || message.includes('验真')) {
    return { name: 'query_invoice', arguments: { invoiceNo: 'INV-202605' }, responseType: 'table' }
  }
  if (message.includes('报表') || message.includes('资产负债') || message.includes('利润') || message.includes('现金流')) {
    const reportToolName = inferReportToolName(message)
    return {
      name: reportToolName,
      arguments: reportToolName === 'generate_report'
        ? { reportType: inferReportType(message), period: extractPeriod(message) }
        : { period: extractPeriod(message) },
      responseType: 'table',
    }
  }
  if (message.includes('报销') || message.includes('费用')) {
    return { name: 'query_expense', arguments: { status: 'PENDING' }, responseType: 'table' }
  }
  if (message.includes('分析') || lower.includes('top') || message.includes('异常')) {
    return {
      name: 'analyze_financial',
      arguments: { period: extractPeriod(message), metrics: ['流动比率', '回款率'] },
      responseType: 'card',
    }
  }
  return {
    name: 'analyze_financial',
    arguments: { period: extractPeriod(message), metrics: ['流动比率', '回款率'] },
    responseType: 'card',
  }
}

function isInvoiceToVoucherIntent(message: string) {
  return (
    (message.includes('拍照') || message.includes('上传') || message.includes('识别') || message.includes('ocr') || message.includes('OCR')) &&
    (message.includes('发票') || message.includes('单据') || message.includes('记账') || message.includes('凭证'))
  )
}

function isInvoiceVerificationIntent(message: string) {
  return (
    message.includes('发票') &&
    (
      message.includes('验真') ||
      message.includes('查验') ||
      message.includes('检验') ||
      message.includes('校验') ||
      message.includes('验证') ||
      message.includes('真伪') ||
      message.includes('查重') ||
      message.includes('重复') ||
      message.includes('连号') ||
      message.includes('抵扣') ||
      message.includes('归档')
    )
  )
}

function isBasicInvoiceCheckIntent(message: string) {
  return (
    message.includes('发票') &&
    (message.includes('标准查验') || message.includes('基础查验') || message.includes('基础验真'))
  )
}

function isInvoiceDuplicateIntent(message: string) {
  return (
    message.includes('发票') &&
    !message.includes('验真') &&
    !message.includes('查验') &&
    !message.includes('真伪') &&
    !message.includes('抵扣') &&
    !message.includes('归档') &&
    (
      message.includes('查重') ||
      message.includes('重复') ||
      message.includes('重复票') ||
      message.includes('重复报销') ||
      message.includes('重复入账')
    )
  )
}

function isInvoiceInputIntent(message: string) {
  return (
    message.includes('发票') &&
    (
      message.includes('录入') ||
      message.includes('登记') ||
      message.includes('入库') ||
      message.includes('入账前') ||
      message.includes('新增') ||
      message.includes('保存')
    )
  )
}

function isTaxPolicyIntent(message: string) {
  return (
    message.includes('税率') ||
    message.includes('税务') ||
    message.includes('纳税') ||
    message.includes('增值税') ||
    message.includes('所得税') ||
    message.includes('进项税') ||
    message.includes('销项税') ||
    (message.includes('政策') && (message.includes('税') || message.includes('发票') || message.includes('报销'))) ||
    message.toLowerCase().includes('vat')
  )
}

function isTaxCalculationIntent(message: string) {
  return (
    message.includes('算税') ||
    message.includes('税额') ||
    message.includes('应纳税') ||
    message.includes('应交税') ||
    message.includes('应缴税') ||
    (message.includes('计算') && message.includes('税'))
  )
}

function isTaxReturnIntent(message: string) {
  return (
    message.includes('申报表') ||
    message.includes('纳税申报') ||
    message.includes('税务申报') ||
    (message.includes('生成') && message.includes('申报'))
  )
}

function isBankReconcileIntent(message: string) {
  return (
    message.includes('银行对账') ||
    message.includes('对账') ||
    message.includes('未达账') ||
    message.includes('账实差异')
  )
}

function isBankTransactionIntent(message: string) {
  return (
    message.includes('银行流水') ||
    message.includes('流水') ||
    message.includes('银行交易')
  )
}

function isCollectionPlanIntent(message: string) {
  return (
    (message.includes('催收') || message.includes('催款') || message.includes('逾期客户') || message.includes('催收函')) &&
    (message.includes('应收') || message.includes('客户') || message.includes('回款') || message.includes('逾期'))
  )
}

function isAgingAnalysisIntent(message: string) {
  return (
    message.includes('账龄分析') ||
    message.includes('账龄明细') ||
    message.includes('账龄分布') ||
    (message.includes('账龄') && (message.includes('应收') || message.includes('应付') || message.includes('往来')))
  )
}

function isCollectionAdviceIntent(message: string) {
  return (
    message.includes('催收建议') ||
    message.includes('催款建议') ||
    message.includes('催收措辞') ||
    message.includes('催款措辞')
  )
}

function isStandardArApReconcileIntent(message: string) {
  return Boolean(extractPartnerId(message)) && message.includes('对账')
}

function isPaymentPlanIntent(message: string) {
  return (
    message.includes('付款计划') ||
    message.includes('应付计划') ||
    message.includes('付款优化') ||
    message.includes('提前付款') ||
    message.includes('合并付款') ||
    (message.includes('应付') && (message.includes('付款') || message.includes('账期') || message.includes('供应商')))
  )
}

function isCounterpartyReconcileIntent(message: string) {
  return (
    message.includes('往来对账') ||
    message.includes('余额确认') ||
    message.includes('对账函') ||
    message.includes('确认函') ||
    message.includes('客户对账') ||
    message.includes('供应商对账') ||
    ((message.includes('应收') || message.includes('应付') || message.includes('往来')) && message.includes('对账'))
  )
}

function isComplianceRiskIntent(message: string) {
  return (
    message.includes('合规') ||
    message.includes('风控') ||
    message.includes('审计') ||
    message.includes('内控') ||
    message.includes('风险') ||
    message.includes('预警') ||
    message.includes('异常凭证') ||
    message.includes('异常发票')
  )
}

function isCashManagementIntent(message: string) {
  return (
    message.includes('资金管理') ||
    message.includes('资金预测') ||
    message.includes('现金流预测') ||
    message.includes('资金缺口') ||
    message.includes('融资') ||
    message.includes('头寸') ||
    message.includes('调拨') ||
    message.includes('闲置资金') ||
    (message.includes('现金流') && (message.includes('预测') || message.includes('资金') || message.includes('未来')))
  )
}

function isContractAssetIntent(message: string) {
  return (
    message.includes('合同') ||
    message.includes('固定资产') ||
    message.includes('资产折旧') ||
    message.includes('折旧') ||
    message.includes('资产盘点') ||
    message.includes('盘盈') ||
    message.includes('盘亏') ||
    message.includes('无形资产') ||
    message.includes('摊销') ||
    message.includes('收付款节点') ||
    message.includes('续约') ||
    message.includes('续费')
  )
}

function isVoucherAuditIntent(message: string) {
  return (
    message.includes('凭证') &&
    (
      message.includes('审核') ||
      message.includes('复核') ||
      message.includes('异常') ||
      message.includes('三单') ||
      message.includes('借贷平衡') ||
      message.includes('借贷不平') ||
      message.includes('超预算') ||
      message.includes('重复入账') ||
      message.includes('岗位分离')
    )
  )
}

function isStandardVoucherAuditIntent(message: string) {
  return (
    message.includes('凭证') &&
    (message.includes('审核通过') || message.includes('确认审核') || message.includes('人工审核') || message.includes('由') && message.includes('审核')) &&
    !message.includes('自动审核') &&
    !message.includes('异常') &&
    !message.includes('三单') &&
    !message.includes('不平') &&
    !message.includes('超预算') &&
    !message.includes('重复入账') &&
    !message.includes('岗位分离')
  )
}

function isVoucherQueryIntent(message: string) {
  return (
    message.includes('凭证') &&
    (
      message.includes('查询') ||
      message.includes('查看') ||
      message.includes('明细') ||
      message.includes('详情') ||
      message.includes('台账') ||
      message.includes('列表') ||
      message.includes('多少张')
    )
  )
}

function isDataIntegrationIntent(message: string) {
  return (
    message.includes('数据集成') ||
    message.includes('系统对接') ||
    message.includes('ERP') ||
    message.includes('erp') ||
    message.includes('银企直连') ||
    message.includes('电子税务局') ||
    message.includes('税务系统') ||
    message.includes('OA') ||
    message.includes('CRM') ||
    message.includes('HR') ||
    message.includes('采购系统') ||
    message.includes('ETL') ||
    message.includes('etl') ||
    message.includes('数据质量') ||
    message.includes('主数据') ||
    message.includes('同步状态') ||
    message.includes('重试任务')
  )
}

function isAuditLogIntent(message: string) {
  return (
    message.includes('调用日志') ||
    message.includes('审计日志') ||
    message.includes('调用链') ||
    message.includes('工具日志') ||
    message.includes('会话日志') ||
    message.includes('追踪记录')
  )
}

function isExpenseApprovalIntent(message: string) {
  if (isExpenseQueryIntent(message)) {
    return false
  }
  const approvalAction = (
      message.includes('审批') ||
      message.includes('审核') ||
      message.includes('提交') ||
      message.includes('流转') ||
      message.includes('超标') ||
      message.includes('查重') ||
      message.includes('重复') ||
      message.includes('三单')
  )
  return (
    (message.includes('报销') && (approvalAction || message.includes('预算'))) ||
    (message.includes('费用') && approvalAction)
  )
}

function isExpenseQueryIntent(message: string) {
  return (
    (message.includes('报销') || message.includes('报销单')) &&
    (
      message.includes('查') ||
      message.includes('查询') ||
      message.includes('查看') ||
      message.includes('列表') ||
      message.includes('台账') ||
      message.includes('待审') ||
      message.includes('待审批') ||
      message.includes('多少')
    )
  )
}

function isExpenseBudgetRemainingIntent(message: string) {
  return (
    message.includes('预算') &&
    (message.includes('余额') || message.includes('剩余') || message.includes('可用')) &&
    (message.includes('报销') || message.includes('费用') || message.includes('部门'))
  )
}

function isExpenseCreateIntent(message: string) {
  return (
    (message.includes('报销') || message.includes('报销单')) &&
    (
      message.includes('创建') ||
      message.includes('新增') ||
      message.includes('发起') ||
      message.includes('登记') ||
      message.includes('录入')
    )
  )
}

function isVarianceDiagnosisIntent(message: string) {
  return (
    message.includes('为什么') ||
    message.includes('原因') ||
    message.includes('归因') ||
    message.includes('超了') ||
    (message.includes('超支') && !message.includes('预测') && !message.includes('评估')) ||
    message.includes('异常波动') ||
    message.includes('差异分析') ||
    (
      (message.includes('费用') || message.includes('收入') || message.includes('毛利')) &&
      message.includes('分析') &&
      !message.includes('预算执行') &&
      !message.includes('预测')
    )
  )
}

function isFinancialRatioIntent(message: string) {
  return (
    message.includes('财务比率') ||
    message.includes('财务指标健康') ||
    message.includes('指标健康度') ||
    message.includes('流动比率') ||
    message.includes('速动比率') ||
    message.includes('资产负债率') ||
    message.includes('净利率') ||
    message.includes('周转率')
  )
}

function isFinancialTrendIntent(message: string) {
  return (
    message.includes('趋势分析') ||
    message.includes('走势分析') ||
    (
      (message.includes('趋势') || message.includes('走势') || /近\d+个?月/.test(message)) &&
      (
        message.includes('收入') ||
        message.includes('费用') ||
        message.includes('利润') ||
        message.includes('回款') ||
        message.includes('现金') ||
        message.includes('指标')
      )
    )
  )
}

function isFinancialAnomalyIntent(message: string) {
  return (
    message.includes('异常检测') ||
    message.includes('识别异常') ||
    message.includes('异常项') ||
    message.includes('财务异常')
  )
}

function isBudgetControlIntent(message: string) {
  return (
    message.includes('预算') &&
    (
      message.includes('管控') ||
      message.includes('执行') ||
      message.includes('占用') ||
      message.includes('余额') ||
      message.includes('超支') ||
      message.includes('超预算') ||
      message.includes('可用') ||
      message.includes('评估') ||
      message.includes('预测') ||
      message.includes('预警')
    )
  )
}

function isMonthEndCloseIntent(message: string) {
  return (
    message.includes('月结') ||
    message.includes('关账') ||
    message.includes('结账') ||
    message.includes('期末检查') ||
    message.includes('结账检查') ||
    message.includes('关账清单')
  )
}

function isAccountMappingIntent(message: string) {
  return (
    message.includes('科目映射') ||
    message.includes('推荐科目') ||
    message.includes('会计科目') ||
    message.includes('科目推荐') ||
    (message.includes('科目') && (message.includes('推荐') || message.includes('分类') || message.includes('入账'))) ||
    (message.includes('凭证') && message.includes('科目') && !message.includes('审核'))
  )
}

function extractAuditSessionId(message: string) {
  const matched = message.match(/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}|demo-session/i)
  return matched ? matched[0] : undefined
}

function extractVoucherAuditor(message: string) {
  const byMatched = message.match(/由[:：\s]*([A-Za-z0-9_\u4e00-\u9fa5]{2,20}?)审核/)
  if (byMatched) return byMatched[1]
  const auditorMatched = message.match(/审核人[:：\s]*([A-Za-z0-9_\u4e00-\u9fa5]{2,20})/)
  return auditorMatched ? auditorMatched[1] : 'E2001'
}

function inferAuditToolName(message: string) {
  if (message.includes('报销')) return 'approve_expense'
  if (message.includes('应收')) return 'query_ar_dashboard'
  if (message.includes('凭证审核')) return 'audit_voucher'
  if (message.includes('凭证台账') || message.includes('凭证查询')) return 'query_vouchers'
  if (message.includes('凭证')) return 'create_voucher'
  if (message.includes('发票验真') || message.includes('查验')) return 'verify_invoice'
  if (message.includes('发票')) return 'query_invoice'
  if (message.includes('报表')) return 'generate_report'
  if (message.includes('税')) return 'generate_tax_return'
  if (message.includes('预算')) return 'evaluate_budget_control'
  if (message.includes('月结') || message.includes('关账')) return 'run_month_end_close'
  return undefined
}

function extractPeriod(message: string) {
  const matched = message.match(/20\d{2}[-年/.](0?[1-9]|1[0-2])/)
  if (!matched) {
    return '2026-05'
  }
  return matched[0].replace('年', '-').replace('/', '-').replace('.', '-').replace(/-(\d)$/, '-0$1')
}

function extractAgingBaseDate(message: string) {
  const matched = message.match(/20\d{2}[-年/.](0?[1-9]|1[0-2])[-月/.](0?[1-9]|[12]\d|3[01])/)
  if (matched) {
    return matched[0]
      .replace('年', '-')
      .replace('月', '-')
      .replace('/', '-')
      .replace('.', '-')
      .replace(/-(\d)-/, '-0$1-')
      .replace(/-(\d)$/, '-0$1')
  }
  return `${extractPeriod(message)}-31`
}

function extractPartnerId(message: string) {
  const matched = message.match(/\b[CS]\d{4}\b/i)
  return matched ? matched[0].toUpperCase() : undefined
}

function extractCustomerId(message: string) {
  const partnerId = extractPartnerId(message)
  return partnerId && partnerId.startsWith('C') ? partnerId : 'C1001'
}

function inferAgingType(message: string) {
  return message.includes('应付') || message.includes('供应商') ? 'AP' : 'AR'
}

function inferExplicitPartnerName(message: string) {
  const candidates = ['浙江智造设备有限公司', '杭州云启零售有限公司', '宁波远洋贸易有限公司', '上海数科服务有限公司', '深圳蓝海咨询有限公司', '杭州差旅服务有限公司']
  return candidates.find((name) => message.includes(name))
}

function extractTrendStartPeriod(message: string) {
  const periods = extractPeriods(message)
  return periods.length >= 2 ? periods[0] : '2026-01'
}

function extractTrendEndPeriod(message: string) {
  const periods = extractPeriods(message)
  return periods.length > 0 ? periods[periods.length - 1] : '2026-05'
}

function extractPeriods(message: string) {
  return Array.from(message.matchAll(/20\d{2}[-年/.](0?[1-9]|1[0-2])/g))
    .map((item) => item[0].replace('年', '-').replace('/', '-').replace('.', '-').replace(/-(\d)$/, '-0$1'))
}

function extractInvoiceNo(message: string) {
  const matched = message.match(/INV-\d{6}-\d{3}|[A-Z]{2,}-\d{6,}-\d{2,}|\b\d{8,20}\b/i)
  if (matched) {
    return matched[0].toUpperCase()
  }
  if (message.includes('待复核') || message.includes('异常') || message.includes('连号')) {
    return 'INV-202605-003'
  }
  return 'INV-202605-001'
}

function extractInvoiceCode(message: string, invoiceNo = extractInvoiceNo(message)) {
  const matched = message.match(/CODE-INV-\d{6}-\d{3}|(?:发票代码|代码)[:：\s]*([A-Z0-9-]{4,30})/i)
  if (matched) {
    return (matched[1] || matched[0]).toUpperCase()
  }
  return `CODE-${invoiceNo}`
}

function inferInvoiceType(message: string) {
  if (message.includes('普通') || message.includes('普票')) return '增值税普通发票'
  if (message.includes('全电') || message.includes('数电')) return '全电发票'
  if (message.includes('机动车')) return '机动车销售发票'
  return '增值税专用发票'
}

function extractInvoiceAmount(message: string) {
  if (!message.includes('金额')) {
    return undefined
  }
  return extractAmount(message, 0) || undefined
}

function extractInvoiceTaxAmount(message: string) {
  const matched = message.match(/税额\s*(\d+(?:\.\d+)?)\s*(万元|万|元)?/)
  if (!matched) {
    return undefined
  }
  return normalizeAmount(Number(matched[1]), matched[2] || '元')
}

function buildInvoiceInputArguments(message: string) {
  const amount = extractInvoiceAmount(message) || extractMoneyAmount(message) || 86000
  const taxAmount = extractInvoiceTaxAmount(message) || inferInvoiceTaxAmount(message, amount)
  return {
    invoiceCode: extractInvoiceCode(message),
    invoiceNo: extractInvoiceNo(message),
    invoiceDate: inferInvoiceDate(message),
    invoiceType: inferInvoiceType(message),
    buyerName: inferInvoiceBuyerName(message),
    sellerName: inferInvoiceSellerName(message),
    amount,
    taxAmount,
    fileHash: message.includes('同一个PDF') || message.includes('同一PDF') ? `HASH-${extractInvoiceNo(message)}` : undefined,
  }
}

function extractMoneyAmount(message: string) {
  const matched = message.match(/(\d+(?:\.\d+)?)\s*(万元|万|元)/)
  if (!matched) {
    return undefined
  }
  return normalizeAmount(Number(matched[1]), matched[2])
}

function inferInvoiceDate(message: string) {
  const matched = message.match(/20\d{2}[-年/.](0?[1-9]|1[0-2])[-月/.](0?[1-9]|[12]\d|3[01])/)
  if (!matched) {
    return '2026-05-31'
  }
  return matched[0]
    .replace('年', '-')
    .replace('月', '-')
    .replace('/', '-')
    .replace('.', '-')
    .replace(/-(\d)-/, '-0$1-')
    .replace(/-(\d)$/, '-0$1')
}

function inferInvoiceTaxAmount(message: string, amount: number) {
  if (message.includes('普票') || message.includes('普通')) {
    return Math.round(amount * 0.06 * 100) / 100
  }
  return Math.round(amount * 0.13 * 100) / 100
}

function inferInvoiceBuyerName(message: string) {
  const candidates = ['杭州云启科技有限公司', '浙江智造设备有限公司', '上海数科服务有限公司']
  return candidates.find((name) => message.includes(name)) || '杭州云启科技有限公司'
}

function inferInvoiceSellerName(message: string) {
  const candidates = ['浙江智造设备有限公司', '上海数科服务有限公司', '深圳蓝海咨询有限公司', '杭州差旅服务有限公司']
  return candidates.find((name) => message.includes(name)) || inferSupplierName(message)
}

function inferReportType(message: string) {
  if (message.includes('利润')) return 'income_statement'
  if (message.includes('现金流')) return 'cash_flow'
  return 'balance_sheet'
}

function inferReportToolName(message: string) {
  if (message.includes('资产负债')) return 'get_balance_sheet'
  if (message.includes('利润')) return 'get_income_statement'
  if (message.includes('现金流量表') || (message.includes('现金流') && message.includes('报表'))) return 'get_cash_flow_statement'
  return 'generate_report'
}

function extractVoucherNo(message: string) {
  const matched = message.match(/V-\d{8}-\d{3,}|V-[A-Z0-9-]+/i)
  return matched ? matched[0].toUpperCase() : 'V-20260531-AUDIT'
}

function extractExplicitVoucherNo(message: string) {
  const matched = message.match(/V-\d{8}-\d{3,}|V-[A-Z0-9-]+/i)
  return matched ? matched[0].toUpperCase() : undefined
}

function extractExpenseNo(message: string) {
  const matched = message.match(/EXP-\d{6}-\d{3}|EXP-[A-Z0-9-]+/i)
  if (matched) {
    return matched[0].toUpperCase()
  }
  return message.includes('003') ? 'EXP-202605-003' : 'EXP-202605-002'
}

function isAutoApproveRequested(message: string) {
  return (
    message.includes('自动通过') ||
    message.includes('自动审批') ||
    message.includes('直接通过') ||
    message.includes('开关打开') ||
    message.includes('打开开关') ||
    message.includes('auto approve')
  )
}

function inferVoucherDate(message: string) {
  const matched = message.match(/20\d{2}[-年/.](0?[1-9]|1[0-2])[-月/.](0?[1-9]|[12]\d|3[01])/)
  if (!matched) {
    return '2026-05-31'
  }
  return matched[0]
    .replace('年', '-')
    .replace('月', '-')
    .replace('/', '-')
    .replace('.', '-')
    .replace(/-(\d)-/, '-0$1-')
    .replace(/-(\d)$/, '-0$1')
}

function inferVoucherStatus(message: string) {
  if (message.includes('待复核') || message.includes('待审核') || message.includes('异常')) return 'NEED_REVIEW'
  if (message.includes('已审核') || message.includes('通过')) return 'AUDIT_PASSED'
  return undefined
}

function inferVoucherAccountCode(message: string) {
  const matched = message.match(/科目\s*(\d{4})|(\d{4})\s*科目/)
  if (matched) {
    return matched[1] || matched[2]
  }
  if (message.includes('银行存款')) return '1002'
  if (message.includes('应收账款')) return '1122'
  if (message.includes('应付账款')) return '2202'
  if (message.includes('销售费用')) return '6601'
  return undefined
}

function inferVoucherSummaryKeyword(message: string) {
  if (message.includes('回款')) return '回款'
  if (message.includes('采购')) return '采购'
  if (message.includes('报销') || message.includes('差旅')) return '报销'
  if (message.includes('销售')) return '销售'
  return undefined
}

function inferExpenseType(message: string) {
  if (message.includes('住宿')) return '住宿'
  if (message.includes('差旅')) return '差旅'
  if (message.includes('交通')) return '交通'
  if (message.includes('餐饮') || message.includes('招待')) return '餐饮'
  if (message.includes('采购') || message.includes('三单')) return '采购'
  if (message.includes('研发')) return '研发'
  return '交通'
}

function extractEmployeeId(message: string) {
  const matched = message.match(/\bE\d{4}\b/i)
  return matched ? matched[0].toUpperCase() : 'E1002'
}

function extractEmployeeIdIfPresent(message: string) {
  const matched = message.match(/\bE\d{4}\b/i)
  return matched ? matched[0].toUpperCase() : undefined
}

function inferExpenseStatus(message: string) {
  if (message.includes('待审批') || message.includes('待审核') || message.includes('待处理')) return 'PENDING'
  if (message.includes('已审批') || message.includes('已通过') || message.includes('通过')) return 'APPROVED'
  if (message.includes('驳回') || message.includes('拒绝')) return 'REJECTED'
  return undefined
}

function inferExpenseDescription(message: string) {
  if (message.includes('客户') || message.includes('拜访')) return '客户现场拜访费用报销'
  if (message.includes('采购')) return '采购业务费用报销'
  if (message.includes('研发')) return '研发项目费用报销'
  if (message.includes('差旅')) return '员工差旅费用报销'
  return '员工费用报销'
}

function inferProjectCode(message: string) {
  if (message.includes('研发') || message.includes('RD')) return 'RD-2026-AI'
  if (message.includes('制造') || message.includes('采购')) return 'MFG-2026-OPS'
  if (message.includes('行政')) return 'ADM-2026-OFFICE'
  return 'PRJ-2026-SALES'
}

function inferBudgetScenario(message: string) {
  if (message.includes('压力') || message.includes('紧张') || message.includes('超支') || message.includes('超预算')) {
    return 'stress'
  }
  return 'base'
}

function inferVarianceMetric(message: string) {
  if (message.includes('收入')) return '营业收入'
  if (message.includes('毛利')) return '毛利率'
  if (message.includes('管理费用')) return '管理费用'
  if (message.includes('销售费用') || message.includes('费用')) return '销售费用'
  return '销售费用'
}

function inferTrendMetric(message: string) {
  if (message.includes('回款')) return '回款率'
  if (message.includes('销售费用')) return '销售费用'
  if (message.includes('管理费用')) return '管理费用'
  if (message.includes('费用')) return '销售费用'
  if (message.includes('净利润')) return '净利润'
  if (message.includes('利润')) return '利润总额'
  if (message.includes('现金')) return '经营净现金流'
  return '营业收入'
}

function inferEntityName(message: string) {
  const candidates = ['杭州未来制造有限公司', '浙江智造设备有限公司', '上海数科服务有限公司']
  return candidates.find((name) => message.includes(name)) || '杭州未来制造有限公司'
}

function inferCloseType(message: string) {
  if (message.includes('年结') || message.includes('年度')) return 'YEAR_END'
  if (message.includes('季结') || message.includes('季度')) return 'QUARTERLY'
  return 'MONTHLY'
}

function inferMappingDocumentType(message: string) {
  if (message.includes('报销')) return 'expense'
  if (message.includes('银行') || message.includes('回单') || message.includes('流水')) return 'bank_receipt'
  if (message.includes('合同')) return 'contract'
  return 'invoice'
}

function inferMappingScenario(message: string) {
  if (message.includes('采购') || message.includes('入库') || message.includes('三单')) return 'purchase'
  if (message.includes('销售') || message.includes('收入') || message.includes('回款')) return 'sales'
  if (message.includes('差旅') || message.includes('交通') || message.includes('住宿') || message.includes('报销')) return 'expense'
  return 'general'
}

function inferMappingSummary(message: string) {
  if (message.includes('采购')) return '采购原材料专用发票，需推荐入账科目'
  if (message.includes('软件') || message.includes('服务费')) return '软件服务费发票，需推荐管理费用科目'
  if (message.includes('销售') || message.includes('收入')) return '销售收入确认，需推荐收入科目'
  if (message.includes('差旅') || message.includes('交通')) return '员工差旅交通报销，需推荐费用科目'
  return message.slice(0, 80) || '非标单据科目映射推荐'
}

function inferCounterpartyName(message: string) {
  const candidates = ['浙江智造设备有限公司', '上海数科服务有限公司', '深圳蓝海咨询有限公司', '杭州采购服务有限公司']
  return candidates.find((name) => message.includes(name)) || '上海数科服务有限公司'
}

function inferCounterpartyType(message: string) {
  if (message.includes('供应商') || message.includes('应付') || message.includes('付款')) return 'SUPPLIER'
  return 'CUSTOMER'
}

function buildVoucherAuditEntries(amount: number, risky: boolean) {
  return [
    { accountCode: '6601', accountName: '销售费用', debitAmount: amount, creditAmount: risky ? 1200 : 0 },
    { accountCode: '2202', accountName: '应付账款', debitAmount: 0, creditAmount: risky ? amount - 500 : amount },
  ]
}

function inferTaxType(message: string) {
  if (message.includes('所得税') || message.toLowerCase().includes('cit')) {
    return 'CIT'
  }
  return 'VAT'
}

function inferBankAccount(message: string) {
  const matched = message.match(/\d{4}-\d{4}|\d{6,}/)
  return matched ? matched[0] : '6222-0001'
}

function inferCustomerName(message: string) {
  const candidates = ['浙江智造设备有限公司', '上海数科服务有限公司', '深圳蓝海咨询有限公司', '杭州示例客户有限公司']
  return candidates.find((name) => message.includes(name)) || '浙江智造设备有限公司'
}

function inferSupplierName(message: string) {
  const candidates = ['浙江智造设备有限公司', '上海数科服务有限公司', '深圳蓝海咨询有限公司', '杭州采购服务有限公司']
  return candidates.find((name) => message.includes(name)) || '上海数科服务有限公司'
}

function inferCreditLevel(message: string) {
  if (message.includes('C类') || message.includes('高风险') || message.includes('信用差')) return 'C'
  if (message.includes('A类') || message.includes('信用优秀')) return 'A'
  return 'B'
}

function inferOverdueDays(message: string) {
  const matched = message.match(/逾期\s*(\d{1,3})\s*天|(\d{1,3})\s*天以上/)
  const value = matched ? Number(matched[1] || matched[2]) : 35
  return Math.max(1, Math.min(365, value || 35))
}

function extractCashStartPeriod(message: string) {
  const matched = message.match(/20\d{2}[-年/.](0?[1-9]|1[0-2])/)
  return matched ? extractPeriod(message) : '2026-06'
}

function extractContractAssetPeriod(message: string) {
  const matched = message.match(/20\d{2}[-年/.](0?[1-9]|1[0-2])/)
  return matched ? extractPeriod(message) : '2026-06'
}

function extractDataIntegrationPeriod(message: string) {
  const matched = message.match(/20\d{2}[-年/.](0?[1-9]|1[0-2])/)
  return matched ? extractPeriod(message) : '2026-06'
}

function inferForecastMonths(message: string) {
  const matched = message.match(/未来?\s*(\d{1,2})\s*个?月|(\d{1,2})\s*个月/)
  const value = matched ? Number(matched[1] || matched[2]) : 3
  return Math.max(1, Math.min(12, value || 3))
}

function inferCashScenario(message: string) {
  if (message.includes('压力') || message.includes('保守') || message.includes('下滑') || message.toLowerCase().includes('stress')) {
    return 'stress'
  }
  if (message.includes('乐观') || message.includes('增长') || message.toLowerCase().includes('optimistic')) {
    return 'optimistic'
  }
  return 'base'
}

function inferContractAssetScope(message: string) {
  const hasContract = message.includes('合同') || message.includes('收付款节点') || message.includes('续约')
  const hasAsset = message.includes('资产') || message.includes('折旧') || message.includes('盘点') || message.includes('盘盈') || message.includes('盘亏') || message.includes('摊销')
  if (hasContract && hasAsset) {
    return 'all'
  }
  if (hasContract) {
    return 'contract'
  }
  if (hasAsset) {
    return 'asset'
  }
  return 'all'
}

function inferDepartment(message: string) {
  const departments = ['研发部', '制造部', '行政部', '财务部', '销售部']
  return departments.find((department) => message.includes(department)) || 'all'
}

function inferReminderDays(message: string) {
  const matched = message.match(/未来?\s*(\d{1,3})\s*天|(\d{1,3})\s*天内/)
  const value = matched ? Number(matched[1] || matched[2]) : 30
  return Math.max(1, Math.min(180, value || 30))
}

function inferIntegrationSystemType(message: string) {
  const hits: string[] = []
  if (message.includes('ERP') || message.includes('erp') || message.includes('SAP') || message.includes('用友') || message.includes('金蝶')) hits.push('erp')
  if (message.includes('银行') || message.includes('银企直连') || message.includes('付款指令')) hits.push('bank')
  if (message.includes('税务') || message.includes('电子税务局') || message.includes('金税')) hits.push('tax')
  if (message.includes('OA') || message.includes('CRM') || message.includes('HR') || message.includes('采购')) hits.push('business')
  const uniqueHits = Array.from(new Set(hits))
  if (uniqueHits.length === 1) return uniqueHits[0]
  return 'all'
}

function inferIntegrationSyncMode(message: string) {
  if (message.includes('增量')) return 'INCREMENTAL'
  if (message.includes('全量')) return 'FULL'
  if (message.includes('实时')) return 'REALTIME'
  if (message.includes('定时')) return 'SCHEDULED'
  if (message.includes('事件')) return 'EVENT'
  return 'all'
}

function inferComplianceScenario(message: string) {
  if (message.includes('发票')) return 'invoice'
  if (message.includes('报销') || message.includes('费用')) return 'expense'
  if (message.includes('银行') || message.includes('流水') || message.includes('对账')) return 'bank'
  if (message.includes('应收') || message.includes('催收') || message.includes('逾期')) return 'ar'
  return 'all'
}

function inferTaxRate(message: string) {
  const matched = message.match(/(\d+(?:\.\d+)?)\s*%/)
  if (matched) {
    return Number(matched[1])
  }
  if (message.includes('所得税')) {
    return 0.25
  }
  return 0.13
}

function extractAmount(message: string, fallback: number) {
  const moneyMatched = message.match(/(\d+(?:\.\d+)?)\s*(万元|万|元)/)
  if (moneyMatched) {
    return normalizeAmount(Number(moneyMatched[1]), moneyMatched[2])
  }
  const matched = Array.from(message.matchAll(/(\d+(?:\.\d+)?)/g))
    .map((item) => Number(item[1]))
    .find((value) => value > 3000)
  return matched || fallback
}

function normalizeAmount(amount: number, unit: string) {
  if (unit.includes('万')) {
    return amount * 10000
  }
  return amount
}

function buildAgentText(toolName: string, message: string, data: unknown) {
  if (toolName === 'query_ar_dashboard') {
    const row = data as { month?: string; receivableAmount?: number; collectionRate?: number; overdueAmount?: number }
    return `已查询 ${row.month || '当前期间'} 应收看板：应收余额 ${formatMoney(row.receivableAmount)}，回款率 ${row.collectionRate ?? '-'}%，逾期金额 ${formatMoney(row.overdueAmount)}。`
  }
  if (toolName === 'query_ar_ap_aging') {
    const page = data as { total?: number; rows?: Array<{ type?: string; totalAmount?: number; overdueAmount?: number; over90Days?: number }> }
    const rows = Array.isArray(page.rows) ? page.rows : []
    const totalAmount = rows.reduce((sum, row) => sum + (row.totalAmount || 0), 0)
    const overdueAmount = rows.reduce((sum, row) => sum + (row.overdueAmount || 0), 0)
    const over90Days = rows.reduce((sum, row) => sum + (row.over90Days || 0), 0)
    return `已查询${rows[0]?.type === 'AP' ? '应付' : '应收'}账龄，共 ${page.total ?? rows.length} 个往来单位，总额 ${formatMoney(totalAmount)}，逾期 ${formatMoney(overdueAmount)}，90天以上 ${formatMoney(over90Days)}。`
  }
  if (toolName === 'suggest_collection_advice') {
    const advices = Array.isArray(data) ? data as Array<{ customerName?: string; priority?: string }> : []
    const highCount = advices.filter((item) => item.priority === 'HIGH').length
    return `已为 ${advices[0]?.customerName || '客户'} 生成 ${advices.length} 条分级催收建议，其中高优先级 ${highCount} 条。`
  }
  if (toolName === 'reconcile_ar_ap') {
    const rec = data as { reconciliationNo?: string; partnerName?: string; internalBalance?: number; partnerBalance?: number; differenceAmount?: number; status?: string; differenceItems?: unknown[] }
    return `已完成标准往来对账 ${rec.reconciliationNo || '-'}：${rec.partnerName || '-'}，我方余额 ${formatMoney(rec.internalBalance)}，对方余额 ${formatMoney(rec.partnerBalance)}，差异 ${formatMoney(rec.differenceAmount)}，状态 ${rec.status || '-'}，差异明细 ${Array.isArray(rec.differenceItems) ? rec.differenceItems.length : 0} 项。`
  }
  if (toolName === 'generate_collection_plan') {
    const plan = data as { planNo?: string; customerName?: string; overdueAmount?: number; overdueDays?: number; collectionStrategy?: string; escalationLevel?: string; actionItems?: unknown[] }
    return `已生成催收计划 ${plan.planNo || '-'}：客户 ${plan.customerName || '-'}，逾期金额 ${formatMoney(plan.overdueAmount)}，逾期 ${plan.overdueDays ?? 0} 天，策略 ${plan.collectionStrategy || '-'}，升级路径 ${plan.escalationLevel || '-'}，动作 ${Array.isArray(plan.actionItems) ? plan.actionItems.length : 0} 项。`
  }
  if (toolName === 'optimize_payment_plan') {
    const plan = data as { planNo?: string; supplierName?: string; payableAmount?: number; recommendedPayDate?: string; paymentStrategy?: string; discountBenefit?: number; cashAfterPayment?: number; liquidityImpact?: string; mergePaymentRecommended?: boolean }
    return `已生成付款计划 ${plan.planNo || '-'}：供应商 ${plan.supplierName || '-'}，应付金额 ${formatMoney(plan.payableAmount)}，建议付款日 ${plan.recommendedPayDate || '-'}，策略 ${plan.paymentStrategy || '-'}，折扣收益 ${formatMoney(plan.discountBenefit)}，付款后现金 ${formatMoney(plan.cashAfterPayment)}，流动性影响 ${plan.liquidityImpact || '-'}，合并付款 ${plan.mergePaymentRecommended ? '建议' : '不建议'}。`
  }
  if (toolName === 'reconcile_counterparty_balance') {
    const rec = data as { reconcileNo?: string; counterpartyName?: string; counterpartyType?: string; internalBalance?: number; counterpartyBalance?: number; differenceAmount?: number; reconcileStatus?: string; riskLevel?: string; confirmationStatus?: string; differenceItems?: unknown[] }
    return `已完成往来对账 ${rec.reconcileNo || '-'}：对象 ${rec.counterpartyName || '-'}（${rec.counterpartyType || '-'}），我方余额 ${formatMoney(rec.internalBalance)}，对方余额 ${formatMoney(rec.counterpartyBalance)}，差异 ${formatMoney(rec.differenceAmount)}，状态 ${rec.reconcileStatus || '-'}，风险 ${rec.riskLevel || '-'}，确认状态 ${rec.confirmationStatus || '-'}，差异明细 ${Array.isArray(rec.differenceItems) ? rec.differenceItems.length : 0} 项。`
  }
  if (toolName === 'create_voucher') {
    const row = data as { voucherNo?: string; status?: string }
    return `已生成凭证 ${row.voucherNo || ''}，状态：${row.status || message}。`
  }
  if (toolName === 'get_voucher') {
    const voucher = data as { voucherNo?: string; voucherDate?: string; summary?: string; status?: string; debitTotal?: number; creditTotal?: number; entries?: unknown[] }
    return `已查询凭证 ${voucher.voucherNo || '-'}：日期 ${voucher.voucherDate || '-'}，摘要 ${voucher.summary || '-'}，状态 ${voucher.status || '-'}，借方 ${formatMoney(voucher.debitTotal)}，贷方 ${formatMoney(voucher.creditTotal)}，分录 ${Array.isArray(voucher.entries) ? voucher.entries.length : 0} 行。`
  }
  if (toolName === 'query_vouchers') {
    const page = data as { total?: number; rows?: unknown[] }
    return `已查询凭证台账，共 ${page.total ?? 0} 张，当前返回 ${Array.isArray(page.rows) ? page.rows.length : 0} 张。`
  }
  if (toolName === 'audit_voucher') {
    const audit = data as { voucherNo?: string; auditStatus?: string; overallSeverity?: string; redCount?: number; yellowCount?: number; blueCount?: number; debitTotal?: number; creditTotal?: number; auditItems?: unknown[] }
    return `已完成凭证 ${audit.voucherNo || '-'} 自动审核：状态 ${audit.auditStatus || '-'}，综合等级 ${audit.overallSeverity || '-'}，借方 ${formatMoney(audit.debitTotal)}，贷方 ${formatMoney(audit.creditTotal)}，红色 ${audit.redCount ?? 0} 项，黄色 ${audit.yellowCount ?? 0} 项，蓝色 ${audit.blueCount ?? 0} 项，共 ${Array.isArray(audit.auditItems) ? audit.auditItems.length : 0} 个审核发现。`
  }
  if (toolName === 'audit_voucher_by_no') {
    return '指定凭证已通过标准审核接口审核完成。'
  }
  if (toolName === 'query_invoice') {
    const page = data as { total?: number }
    return `已查询发票台账，共 ${page.total ?? 0} 条记录。`
  }
  if (toolName === 'check_invoice') {
    const invoice = data as { invoiceCode?: string; invoiceNo?: string; authentic?: boolean; amountMatched?: boolean; amount?: number; taxAmount?: number; sellerName?: string; checkStatus?: string; riskLevel?: string }
    return `已完成发票 ${invoice.invoiceNo || '-'} 标准查验：状态 ${invoice.checkStatus || '-'}，真伪 ${invoice.authentic ? '通过' : '未通过'}，金额一致 ${invoice.amountMatched ? '是' : '否'}，金额 ${formatMoney(invoice.amount)}，税额 ${formatMoney(invoice.taxAmount)}，销方 ${invoice.sellerName || '-'}，风险 ${invoice.riskLevel || '-'}。`
  }
  if (toolName === 'input_invoice') {
    const invoice = data as { invoiceNo?: string; inputStatus?: string; verifyStatus?: string; duplicate?: boolean; riskLevel?: string; amount?: number; taxAmount?: number }
    return `已录入发票 ${invoice.invoiceNo || '-'}：状态 ${invoice.inputStatus || '-'}，验真状态 ${invoice.verifyStatus || '-'}，不含税金额 ${formatMoney(invoice.amount)}，税额 ${formatMoney(invoice.taxAmount)}，重复风险 ${invoice.duplicate ? '是' : '否'}，风险等级 ${invoice.riskLevel || '-'}。`
  }
  if (toolName === 'check_invoice_duplicate') {
    const rows = Array.isArray(data) ? data as Array<{ duplicateNo?: string; riskLevel?: string }> : []
    return `已完成发票查重，发现 ${rows.length} 条疑似重复记录。${rows.length > 0 ? `最高风险 ${rows[0].riskLevel || '-'}` : '未发现重复风险。'}`
  }
  if (toolName === 'verify_invoice') {
    const invoice = data as { invoiceNo?: string; verifyStatus?: string; taxAuthorityStatus?: string; duplicate?: boolean; deductible?: boolean; deductibleTaxAmount?: number; riskLevel?: string; riskHints?: unknown[] }
    return `已完成发票 ${invoice.invoiceNo || '-'} 验真：状态 ${invoice.verifyStatus || '-'}，税务端 ${invoice.taxAuthorityStatus || '-'}，重复 ${invoice.duplicate ? '是' : '否'}，可抵扣 ${invoice.deductible ? '是' : '否'}，可抵扣税额 ${formatMoney(invoice.deductibleTaxAmount)}，风险等级 ${invoice.riskLevel || '-'}，提示 ${Array.isArray(invoice.riskHints) ? invoice.riskHints.length : 0} 条。`
  }
  if (toolName === 'generate_report') {
    const report = data as { reportType?: string; period?: string }
    return `已生成 ${report.period || '当前期间'} ${report.reportType || '财务报表'}。`
  }
  if (toolName === 'get_balance_sheet') {
    const report = data as { period?: string; totalAssets?: number; totalLiabilities?: number; totalEquity?: number; balanced?: boolean; rows?: unknown[] }
    return `已生成 ${report.period || '当前期间'} 资产负债表：资产 ${formatMoney(report.totalAssets)}，负债 ${formatMoney(report.totalLiabilities)}，所有者权益 ${formatMoney(report.totalEquity)}，${report.balanced ? '报表平衡' : '报表不平衡'}，项目 ${Array.isArray(report.rows) ? report.rows.length : 0} 行。`
  }
  if (toolName === 'get_income_statement') {
    const report = data as { period?: string; revenue?: number; operatingCost?: number; totalProfit?: number; netProfit?: number; rows?: unknown[] }
    return `已生成 ${report.period || '当前期间'} 利润表：营业收入 ${formatMoney(report.revenue)}，营业成本 ${formatMoney(report.operatingCost)}，利润总额 ${formatMoney(report.totalProfit)}，净利润 ${formatMoney(report.netProfit)}，项目 ${Array.isArray(report.rows) ? report.rows.length : 0} 行。`
  }
  if (toolName === 'get_cash_flow_statement') {
    const report = data as { period?: string; operatingNetCashFlow?: number; investingNetCashFlow?: number; financingNetCashFlow?: number; netIncreaseCash?: number; rows?: unknown[] }
    return `已生成 ${report.period || '当前期间'} 现金流量表：经营净现金流 ${formatMoney(report.operatingNetCashFlow)}，投资净现金流 ${formatMoney(report.investingNetCashFlow)}，筹资净现金流 ${formatMoney(report.financingNetCashFlow)}，现金净增加额 ${formatMoney(report.netIncreaseCash)}，项目 ${Array.isArray(report.rows) ? report.rows.length : 0} 行。`
  }
  if (toolName === 'calculate_financial_ratios') {
    const ratios = Array.isArray(data) ? data as Array<{ healthLevel?: string }> : []
    const healthyCount = ratios.filter((item) => item.healthLevel === 'HEALTHY' || item.healthLevel === 'LOW_RISK').length
    return `已计算财务比率，共 ${ratios.length} 项，其中健康或低风险 ${healthyCount} 项，需关注 ${ratios.length - healthyCount} 项。`
  }
  if (toolName === 'analyze_financial_trend') {
    const trend = data as { metric?: string; startPeriod?: string; endPeriod?: string; trendDirection?: string; changeRate?: number; dataPoints?: unknown[] }
    return `已完成 ${trend.metric || '财务指标'} 趋势分析：${trend.startPeriod || '-'} 至 ${trend.endPeriod || '-'}，趋势 ${trend.trendDirection || '-'}，累计变动 ${trend.changeRate ?? '-'}%，包含 ${Array.isArray(trend.dataPoints) ? trend.dataPoints.length : 0} 个数据点。`
  }
  if (toolName === 'detect_financial_anomalies') {
    const anomalies = Array.isArray(data) ? data as Array<{ severity?: string }> : []
    const highCount = anomalies.filter((item) => item.severity === 'HIGH').length
    return `已完成财务异常检测，共发现 ${anomalies.length} 项异常，其中高风险 ${highCount} 项。`
  }
  if (toolName === 'diagnose_financial_variance') {
    const diagnosis = data as { diagnosisNo?: string; period?: string; metricName?: string; department?: string; actualAmount?: number; budgetAmount?: number; budgetVariance?: number; budgetVarianceRate?: number; momVarianceRate?: number; severity?: string; drivers?: unknown[]; actionPlan?: unknown[] }
    return `已完成差异归因 ${diagnosis.diagnosisNo || '-'}：${diagnosis.period || '当前期间'} ${diagnosis.department || '-'} ${diagnosis.metricName || '-'} 实际 ${formatMoney(diagnosis.actualAmount)}，预算 ${formatMoney(diagnosis.budgetAmount)}，超预算 ${formatMoney(diagnosis.budgetVariance)}，超预算率 ${diagnosis.budgetVarianceRate ?? '-'}%，环比 ${diagnosis.momVarianceRate ?? '-'}%，严重度 ${diagnosis.severity || '-'}，驱动因素 ${Array.isArray(diagnosis.drivers) ? diagnosis.drivers.length : 0} 个，行动计划 ${Array.isArray(diagnosis.actionPlan) ? diagnosis.actionPlan.length : 0} 项。`
  }
  if (toolName === 'evaluate_budget_control') {
    const budget = data as { budgetNo?: string; department?: string; expenseType?: string; requestedAmount?: number; availableBudget?: number; executionRate?: number; controlStatus?: string; riskLevel?: string; approvalSuggestion?: string; warnings?: unknown[] }
    return `已完成预算评估 ${budget.budgetNo || '-'}：部门 ${budget.department || '-'}，费用类型 ${budget.expenseType || '-'}，本次占用 ${formatMoney(budget.requestedAmount)}，可用预算 ${formatMoney(budget.availableBudget)}，执行率 ${budget.executionRate ?? '-'}%，管控状态 ${budget.controlStatus || '-'}，风险 ${budget.riskLevel || '-'}，预警 ${Array.isArray(budget.warnings) ? budget.warnings.length : 0} 条。${budget.approvalSuggestion || ''}`
  }
  if (toolName === 'run_month_end_close') {
    const close = data as { closeNo?: string; period?: string; entityName?: string; closeStatus?: string; progressRate?: number; blockerCount?: number; warningCount?: number; readyToClose?: boolean; estimatedCloseDate?: string; checklist?: unknown[] }
    return `已完成 ${close.period || '当前期间'} 月结检查 ${close.closeNo || '-'}：主体 ${close.entityName || '-'}，状态 ${close.closeStatus || '-'}，进度 ${close.progressRate ?? '-'}%，阻断 ${close.blockerCount ?? 0} 项，预警 ${close.warningCount ?? 0} 项，清单 ${Array.isArray(close.checklist) ? close.checklist.length : 0} 项，预计关账日 ${close.estimatedCloseDate || '-'}，${close.readyToClose ? '可进入关账审批' : '暂不可关账'}。`
  }
  if (toolName === 'recommend_account_mapping') {
    const mapping = data as { mappingNo?: string; recommendationMode?: string; confidence?: number; selectedAccountCode?: string; selectedAccountName?: string; autoApplicable?: boolean; manualReviewRequired?: boolean; candidates?: unknown[]; voucherPreview?: unknown[] }
    return `已完成科目映射 ${mapping.mappingNo || '-'}：推荐 ${mapping.selectedAccountCode || '-'} ${mapping.selectedAccountName || '-'}，模式 ${mapping.recommendationMode || '-'}，置信度 ${mapping.confidence ?? '-'}%，候选科目 ${Array.isArray(mapping.candidates) ? mapping.candidates.length : 0} 个，凭证预览 ${Array.isArray(mapping.voucherPreview) ? mapping.voucherPreview.length : 0} 行，${mapping.autoApplicable ? '可自动采用' : '不可自动采用'}，${mapping.manualReviewRequired ? '需要人工复核' : '无需人工复核'}。`
  }
  if (toolName === 'query_expense') {
    const page = data as { total?: number }
    return `已查询费用报销单，共 ${page.total ?? 0} 条记录。`
  }
  if (toolName === 'create_expense') {
    const expense = data as { expenseNo?: string; employeeName?: string; department?: string; amount?: number; status?: string; invoiceCount?: number; attachmentCount?: number; riskHint?: string }
    return `已创建报销单 ${expense.expenseNo || '-'}：报销人 ${expense.employeeName || '-'}，部门 ${expense.department || '-'}，金额 ${formatMoney(expense.amount)}，状态 ${expense.status || '-'}，发票 ${expense.invoiceCount ?? 0} 张，附件 ${expense.attachmentCount ?? 0} 个。${expense.riskHint || ''}`
  }
  if (toolName === 'approve_expense') {
    const expense = data as { expenseNo?: string; approvalStatus?: string; riskLevel?: string; amount?: number; standardLimit?: number; budgetAfterSubmit?: number; approvalRoute?: unknown[]; riskItems?: unknown[] }
    return `已完成报销单 ${expense.expenseNo || '-'} 智能审批：状态 ${expense.approvalStatus || '-'}，风险等级 ${expense.riskLevel || '-'}，金额 ${formatMoney(expense.amount)}，费用标准 ${formatMoney(expense.standardLimit)}，提交后预算 ${formatMoney(expense.budgetAfterSubmit)}，审批节点 ${Array.isArray(expense.approvalRoute) ? expense.approvalRoute.length : 0} 个，风险 ${Array.isArray(expense.riskItems) ? expense.riskItems.length : 0} 项。`
  }
  if (toolName === 'query_budget_remaining') {
    const budget = data as { department?: string; period?: string; budgetAmount?: number; usedAmount?: number; occupiedAmount?: number; remainingAmount?: number; executionRate?: number; riskLevel?: string }
    return `已查询 ${budget.department || '-'} ${budget.period || '当前期间'} 费用预算：预算 ${formatMoney(budget.budgetAmount)}，已使用 ${formatMoney(budget.usedAmount)}，在途占用 ${formatMoney(budget.occupiedAmount)}，剩余 ${formatMoney(budget.remainingAmount)}，执行率 ${budget.executionRate ?? '-'}%，风险 ${budget.riskLevel || '-'}。`
  }
  if (toolName === 'calculate_tax') {
    const tax = data as { taxType?: string; period?: string; taxPayable?: number }
    return `已完成 ${tax.period || '当前期间'} ${tax.taxType || '税种'} 税额计算，应纳税额 ${formatMoney(tax.taxPayable)}。`
  }
  if (toolName === 'generate_tax_return') {
    const taxReturn = data as { returnNo?: string; taxType?: string; period?: string; taxPayable?: number; status?: string }
    return `已生成 ${taxReturn.period || '当前期间'} ${taxReturn.taxType || '税种'} 申报表草稿 ${taxReturn.returnNo || ''}，应纳税额 ${formatMoney(taxReturn.taxPayable)}，状态：${taxReturn.status || 'DRAFT'}。`
  }
  if (toolName === 'query_bank_transactions') {
    const page = data as { total?: number }
    return `已查询银行流水，共 ${page.total ?? 0} 条记录。`
  }
  if (toolName === 'reconcile_bank_statement') {
    const result = data as { period?: string; accountNo?: string; differenceAmount?: number; unmatchedCount?: number }
    return `已完成 ${result.period || '当前期间'} 账号 ${result.accountNo || '-'} 银行对账，差异金额 ${formatMoney(result.differenceAmount)}，未匹配 ${result.unmatchedCount ?? 0} 条。`
  }
  if (toolName === 'assess_compliance_risk') {
    const risk = data as { period?: string; overallLevel?: string; riskScore?: number; totalRiskCount?: number; highRiskCount?: number }
    return `已完成 ${risk.period || '当前期间'} 合规风控评估，综合等级 ${risk.overallLevel || '-'}，风险评分 ${risk.riskScore ?? '-'}，发现 ${risk.totalRiskCount ?? 0} 项风险，其中高风险 ${risk.highRiskCount ?? 0} 项。`
  }
  if (toolName === 'forecast_cash_flow') {
    const cash = data as { startPeriod?: string; liquidityLevel?: string; currentCashBalance?: number; forecastEndingBalance?: number; lowestBalance?: number; alerts?: unknown[] }
    return `已完成 ${cash.startPeriod || '当前期间'} 起的资金预测，当前头寸 ${formatMoney(cash.currentCashBalance)}，预测期末余额 ${formatMoney(cash.forecastEndingBalance)}，最低余额 ${formatMoney(cash.lowestBalance)}，流动性等级 ${cash.liquidityLevel || '-'}，预警 ${Array.isArray(cash.alerts) ? cash.alerts.length : 0} 条。`
  }
  if (toolName === 'query_contract_assets') {
    const asset = data as { period?: string; scope?: string; activeContractCount?: number; dueMilestoneCount?: number; overdueMilestoneCount?: number; assetCount?: number; monthlyDepreciation?: number; intangibleAmortization?: number; inventoryExceptions?: unknown[] }
    if (asset.scope === 'asset') {
      return `已查询 ${asset.period || '当前期间'} 资产看板：资产 ${asset.assetCount ?? 0} 项，本期折旧 ${formatMoney(asset.monthlyDepreciation)}，摊销 ${formatMoney(asset.intangibleAmortization)}，盘点异常 ${Array.isArray(asset.inventoryExceptions) ? asset.inventoryExceptions.length : 0} 项。`
    }
    if (asset.scope === 'contract') {
      return `已查询 ${asset.period || '当前期间'} 合同节点看板：有效合同 ${asset.activeContractCount ?? 0} 份，待提醒节点 ${asset.dueMilestoneCount ?? 0} 个，逾期节点 ${asset.overdueMilestoneCount ?? 0} 个。`
    }
    return `已查询 ${asset.period || '当前期间'} 合同与资产看板：有效合同 ${asset.activeContractCount ?? 0} 份，待提醒节点 ${asset.dueMilestoneCount ?? 0} 个，逾期节点 ${asset.overdueMilestoneCount ?? 0} 个，资产 ${asset.assetCount ?? 0} 项，本期折旧 ${formatMoney(asset.monthlyDepreciation)}，摊销 ${formatMoney(asset.intangibleAmortization)}，盘点异常 ${Array.isArray(asset.inventoryExceptions) ? asset.inventoryExceptions.length : 0} 项。`
  }
  if (toolName === 'query_data_integration') {
    const integration = data as { period?: string; connectorCount?: number; healthyConnectorCount?: number; warningConnectorCount?: number; failedConnectorCount?: number; overallQualityScore?: number; totalRecords?: number; failedRecords?: number; alerts?: unknown[]; retryTasks?: unknown[] }
    return `已查询 ${integration.period || '当前期间'} 数据集成状态：连接器 ${integration.connectorCount ?? 0} 个，健康 ${integration.healthyConnectorCount ?? 0} 个，预警 ${integration.warningConnectorCount ?? 0} 个，失败 ${integration.failedConnectorCount ?? 0} 个，质量评分 ${integration.overallQualityScore ?? '-'}，同步记录 ${integration.totalRecords ?? 0} 条，失败记录 ${integration.failedRecords ?? 0} 条，告警 ${Array.isArray(integration.alerts) ? integration.alerts.length : 0} 条，重试任务 ${Array.isArray(integration.retryTasks) ? integration.retryTasks.length : 0} 个。`
  }
  if (toolName === 'query_audit_logs') {
    const page = data as { total?: number; rows?: unknown[] }
    return `已查询调用审计日志，共 ${page.total ?? 0} 条，当前返回 ${Array.isArray(page.rows) ? page.rows.length : 0} 条。`
  }
  if (toolName === 'webReader') {
    const policy = data as { title?: string; highlights?: string[]; extracted?: { disclaimer?: string } }
    const first = Array.isArray(policy.highlights) && policy.highlights.length > 0 ? policy.highlights[0] : '已读取政策摘要。'
    return `${policy.title || '已读取税务政策摘要'}：${first}${policy.extracted?.disclaimer ? ` ${policy.extracted.disclaimer}` : ''}`
  }
  return message
}

function normalizeAgentContent(toolName: string, data: unknown) {
  if ((toolName === 'query_invoice' || toolName === 'query_expense' || toolName === 'query_bank_transactions' || toolName === 'query_audit_logs' || toolName === 'query_vouchers' || toolName === 'query_ar_ap_aging') && data && typeof data === 'object') {
    const page = data as { rows?: unknown[] }
    return page.rows || []
  }
  if ((toolName === 'generate_report' || toolName === 'get_balance_sheet' || toolName === 'get_income_statement' || toolName === 'get_cash_flow_statement') && data && typeof data === 'object') {
    const report = data as { rows?: unknown[] }
    return report.rows || []
  }
  return data
}

function unwrapToolData(result: unknown) {
  let current = result
  for (let depth = 0; depth < 4; depth += 1) {
    if (!current || typeof current !== 'object' || !('data' in current)) {
      return current
    }
    current = (current as { data?: unknown }).data
  }
  return current
}

function extractStructuredResult<T>(response: JsonRpcResponse): T {
  if (response.error) {
    throw new Error(String(response.error.data || response.error.message))
  }
  const toolResult = response.result as {
    isError?: boolean
    structuredContent?: { result?: { data?: unknown } | unknown }
  }
  if (toolResult?.isError) {
    throw new Error(JSON.stringify(toolResult.structuredContent || {}))
  }
  const result = toolResult?.structuredContent?.result
  return unwrapToolData(result) as T
}

function numberValue(value: unknown, fallback: number) {
  return typeof value === 'number' ? value : fallback
}

function formatMoney(value: unknown) {
  if (typeof value !== 'number') {
    return '-'
  }
  return `¥${value.toLocaleString('zh-CN')}`
}

function clientContextFromRequest(bodyContext: unknown, req: IncomingMessage): ClientContext | undefined {
  const headerContext = sanitizeClientContext({
    channel: headerValue(req, 'x-client-channel'),
    corpId: headerValue(req, 'x-dingtalk-corp-id'),
    authCode: headerValue(req, 'x-dingtalk-auth-code'),
    userId: headerValue(req, 'x-dingtalk-user-id'),
    userName: headerValue(req, 'x-dingtalk-user-name'),
    userAgent: headerValue(req, 'user-agent'),
  })
  return mergeClientContext(sanitizeClientContext(bodyContext), headerContext)
}

function sanitizeClientContext(value: unknown): ClientContext | undefined {
  if (!value || typeof value !== 'object') {
    return undefined
  }
  const source = value as Record<string, unknown>
  const channel = stringValue(source.channel)
  const device = stringValue(source.device)
  const result: ClientContext = {}
  if (channel === 'dingtalk' || channel === 'web') {
    result.channel = channel
  }
  if (device === 'mobile' || device === 'desktop') {
    result.device = device
  }
  result.corpId = stringValue(source.corpId, 128)
  result.authCode = stringValue(source.authCode, 256)
  result.userId = stringValue(source.userId, 128)
  result.userName = stringValue(source.userName, 128)
  result.userAgent = stringValue(source.userAgent, 512)
  return Object.values(result).some(Boolean) ? result : undefined
}

function mergeClientContext(primary?: ClientContext, secondary?: ClientContext) {
  if (!primary && !secondary) {
    return undefined
  }
  const result: ClientContext = { ...(primary || {}) }
  for (const [key, value] of Object.entries(secondary || {})) {
    if (value !== undefined && value !== '') {
      ;(result as Record<string, unknown>)[key] = value
    }
  }
  return result
}

function headerValue(req: IncomingMessage, name: string) {
  const value = req.headers[name]
  if (Array.isArray(value)) {
    return value[0]
  }
  return value
}

function stringValue(value: unknown, maxLength = 256) {
  if (typeof value !== 'string') {
    return undefined
  }
  const trimmed = value.trim()
  if (!trimmed) {
    return undefined
  }
  return trimmed.slice(0, maxLength)
}

function handleSessionAudit(url: string) {
  const matched = url.match(/^\/agent\/sessions\/([^/]+)\/audit$/)
  if (!matched) {
    return { error: 'not found' }
  }
  const sessionId = decodeURIComponent(matched[1])
  const session = sessions.get(sessionId)
  if (!session) {
    return { sessionId, logs: [], error: 'session not found' }
  }
  return {
    sessionId,
    createdAt: new Date(session.createdAt).toISOString(),
    lastSeenAt: new Date(session.lastSeenAt).toISOString(),
    clientContext: session.clientContext,
    logs: session.auditLogs,
  }
}

async function persistInvocationAudit(input: {
  sessionId: string
  message: string
  decision: { name: string; arguments: Record<string, unknown> }
  text: string
  status: 'success' | 'error'
  turnStartedAt: number
  errorMessage?: string
}) {
  if (input.decision.name === 'record_audit_log') {
    return
  }
  const session = sessions.get(input.sessionId)
  const chain = currentCallChain(input.sessionId, input.turnStartedAt)
  const primaryLog = chain.find((log) => log.toolName === input.decision.name) || chain[0]
  try {
    await callTool({
      jsonrpc: '2.0',
      id: Date.now() + 2,
      method: 'tools/call',
      params: {
        name: 'record_audit_log',
        arguments: {
          traceId: primaryLog?.id,
          sessionId: input.sessionId,
          channel: session?.clientContext?.channel || 'web',
          corpId: session?.clientContext?.corpId,
          userId: session?.clientContext?.userId,
          userName: session?.clientContext?.userName,
          requestText: input.message,
          toolName: input.decision.name,
          serviceName: primaryLog?.serviceName || 'finance',
          requestArgs: JSON.stringify(sanitizeArguments(input.decision.arguments) || {}),
          responseSummary: input.text,
          status: input.status,
          errorMessage: input.errorMessage,
          durationMs: primaryLog?.durationMs || 0,
          occurredAt: formatDateTime(new Date()),
        },
        _meta: buildToolMeta(input.sessionId),
      },
    })
  } catch (e) {
    recordToolAudit({
      sessionId: input.sessionId,
      toolName: 'record_audit_log',
      serviceName: 'finance',
      startedAt: Date.now(),
      status: 'error',
      durationMs: 0,
      error: `audit persist skipped: ${errorMessage(e)}`,
    })
  }
}

async function persistInvocationAudits(
  sessionId: string,
  message: string,
  decisions: Array<{ name: string; arguments: Record<string, unknown> }>,
  text: string,
  turnStartedAt: number,
) {
  for (const decision of decisions) {
    await persistInvocationAudit({
      sessionId,
      message,
      decision,
      text,
      status: 'success',
      turnStartedAt,
    })
  }
}

function recordToolAudit(input: Omit<ToolAuditLog, 'id' | 'startedAt'> & { startedAt: number }) {
  if (!input.sessionId) {
    return
  }
  const session = sessions.get(input.sessionId)
  if (!session) {
    return
  }
  session.auditLogs.push({
    id: randomUUID(),
    sessionId: input.sessionId,
    toolName: input.toolName,
    backendToolName: input.backendToolName,
    serviceName: input.serviceName,
    startedAt: new Date(input.startedAt).toISOString(),
    durationMs: input.durationMs,
    status: input.status,
    arguments: input.arguments,
    error: input.error,
  })
  if (session.auditLogs.length > maxAuditLogsPerSession) {
    session.auditLogs.splice(0, session.auditLogs.length - maxAuditLogsPerSession)
  }
}

function currentCallChain(sessionId: string, turnStartedAt: number) {
  const session = sessions.get(sessionId)
  if (!session) {
    return []
  }
  return session.auditLogs.filter((log) => new Date(log.startedAt).getTime() >= turnStartedAt && log.toolName !== 'record_audit_log')
}

function sanitizeArguments(value: unknown): Record<string, unknown> | undefined {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    return undefined
  }
  return sanitizeObject(value as Record<string, unknown>, 0)
}

function sanitizeObject(source: Record<string, unknown>, depth: number): Record<string, unknown> {
  if (depth > 2) {
    return { truncated: true }
  }
  const result: Record<string, unknown> = {}
  for (const [key, value] of Object.entries(source)) {
    if (isSensitiveKey(key)) {
      result[key] = '***'
      continue
    }
    if (Array.isArray(value)) {
      result[key] = value.slice(0, 5).map((item) => {
        if (item && typeof item === 'object' && !Array.isArray(item)) {
          return sanitizeObject(item as Record<string, unknown>, depth + 1)
        }
        return item
      })
      continue
    }
    if (value && typeof value === 'object') {
      result[key] = sanitizeObject(value as Record<string, unknown>, depth + 1)
      continue
    }
    result[key] = value
  }
  return result
}

function isSensitiveKey(key: string) {
  const lower = key.toLowerCase()
  return lower.includes('token') || lower.includes('password') || lower.includes('secret') || lower.includes('authcode')
}

async function fetchServiceTools(service: RouteService): Promise<McpTool[]> {
  await postJsonRpc(service, { jsonrpc: '2.0', id: 1, method: 'initialize', params: {} })
  const response = await postJsonRpc(service, { jsonrpc: '2.0', id: 2, method: 'tools/list', params: {} })
  if (response.error) {
    throw new Error(response.error.data ? String(response.error.data) : response.error.message)
  }
  const result = response.result as { tools?: McpTool[] }
  return result.tools || []
}

async function postJsonRpc(service: RouteService, body: JsonRpcRequest): Promise<JsonRpcResponse> {
  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), service.timeoutMs || 3000)
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  const authToken = resolveServiceAuthToken(service)
  if (authToken) {
    headers[service.authHeaderName || 'Authorization'] = (service.authHeaderName && service.authHeaderName !== 'Authorization') ? authToken : `Bearer ${authToken}`
  }
  try {
    const response = await fetch(service.url, {
      method: 'POST',
      headers,
      body: JSON.stringify(body),
      signal: controller.signal,
    })
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }
    return (await response.json()) as JsonRpcResponse
  } finally {
    clearTimeout(timeout)
  }
}

function resolveServiceAuthToken(service: RouteService) {
  if (service.authToken) {
    return service.authToken
  }
  if (service.authTokenEnv) {
    return process.env[service.authTokenEnv]
  }
  return undefined
}

function touchSession(params: Record<string, unknown>) {
  const meta = params._meta as { sessionId?: string; clientContext?: unknown } | undefined
  if (!meta?.sessionId) {
    return
  }
  const session = sessions.get(meta.sessionId)
  if (session) {
    session.lastSeenAt = Date.now()
    const clientContext = sanitizeClientContext(meta.clientContext)
    if (clientContext) {
      session.clientContext = mergeClientContext(session.clientContext, clientContext)
    }
  }
}

async function readJson(req: IncomingMessage): Promise<JsonRpcRequest> {
  const chunks: Buffer[] = []
  for await (const chunk of req) {
    chunks.push(Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk))
  }
  const raw = Buffer.concat(chunks).toString('utf8')
  if (!raw) {
    return {}
  }
  return JSON.parse(raw) as JsonRpcRequest
}

function setCorsHeaders(res: ServerResponse) {
  res.setHeader('Access-Control-Allow-Origin', '*')
  res.setHeader('Access-Control-Allow-Methods', 'POST, GET, OPTIONS')
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization')
}

function writeJson(res: ServerResponse, status: number, body: unknown) {
  res.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8' })
  res.end(JSON.stringify(body))
}

function formatDateTime(date: Date) {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
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
