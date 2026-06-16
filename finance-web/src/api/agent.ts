/**
 * Agent 对话 API 封装
 * 封装与后端 /agent/chat 接口的通信
 */
import axios from 'axios'

// 创建 axios 实例，统一配置
const request = axios.create({
  baseURL: '',
  timeout: 60000, // AI 响应可能较慢，设置60秒超时
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求拦截器：可在此添加 token 等认证信息
request.interceptors.request.use(
  (config) => config,
  (error) => Promise.reject(error),
)

// 响应拦截器：统一错误处理
request.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('[Agent API] 请求失败:', error)
    return Promise.reject(error)
  },
)

/** 客户端上下文信息 */
export interface ClientContext {
  channel?: string   // 渠道标识，如 'web'、'dingtalk'
  corpId?: string
  authCode?: string
  userId?: string    // 用户ID
  userName?: string  // 用户名
  device?: 'mobile' | 'desktop'
  userAgent?: string
}

/** 对话请求参数 */
export interface ChatRequest {
  sessionId?: string       // 会话ID，首次可不传
  message: string          // 用户消息
  clientContext?: ClientContext
  imageUrl?: string        // 发票/单据图片 URL 或 dataURL
  expenseApprovalArgs?: Record<string, unknown>
}

/** 对话响应数据 */
export interface ChatResponse {
  sessionId: string                // 会话ID
  role: 'assistant'                // 角色
  type: 'text' | 'table' | 'chart' | 'card' | 'file'  // 消息类型
  text: string                     // 文本内容
  content?: any                    // 结构化数据（表格/图表/卡片）
  toolCall?: object                // 单次工具调用
  toolCalls?: object[]             // 多次工具调用
  callChain?: object[]             // 完整调用链
}

/**
 * 发送对话消息
 * @param data 对话请求参数
 * @returns 对话响应
 */
export async function sendChat(data: ChatRequest): Promise<ChatResponse> {
  const response = await request.post<ChatResponse>('/agent/chat', data)
  return response.data
}

export interface StreamChatHandlers {
  onToken?: (token: string) => void
  onStructured?: (type: ChatResponse['type'], content: unknown) => void
  onStatus?: (message: string) => void
}

export async function streamChat(data: ChatRequest, handlers: StreamChatHandlers = {}): Promise<ChatResponse> {
  const streamSessionId = data.clientContext
    ? await prepareAgentSession(data.sessionId, data.clientContext)
    : data.sessionId
  const params = new URLSearchParams({ message: data.message })
  if (streamSessionId) params.set('sessionId', streamSessionId)

  return new Promise((resolve, reject) => {
    const source = new EventSource(`/agent/chat/stream?${params.toString()}`)
    let completed = false

    source.addEventListener('status', (event) => {
      const payload = parseEventData(event)
      handlers.onStatus?.(String(payload.message || ''))
    })
    source.addEventListener('token', (event) => {
      const payload = parseEventData(event)
      handlers.onToken?.(String(payload.token || ''))
    })
    for (const type of ['table', 'chart', 'card', 'file'] as const) {
      source.addEventListener(type, (event) => {
        const payload = parseEventData(event)
        handlers.onStructured?.(type, payload.content)
      })
    }
    source.addEventListener('done', (event) => {
      completed = true
      source.close()
      resolve(JSON.parse(event.data) as ChatResponse)
    })
    source.addEventListener('stream_error', (event) => {
      completed = true
      source.close()
      const payload = parseEventData(event)
      reject(new Error(String(payload.message || 'SSE stream failed')))
    })
    source.onerror = () => {
      if (!completed) {
        source.close()
        reject(new Error('SSE connection interrupted'))
      }
    }
  })
}

async function prepareAgentSession(sessionId: string | undefined, clientContext: ClientContext) {
  const response = await request.post<{ sessionId: string }>('/agent/sessions', { sessionId, clientContext })
  return response.data.sessionId
}

function parseEventData(event: Event): Record<string, unknown> {
  const data = (event as MessageEvent<string>).data
  try {
    return JSON.parse(data) as Record<string, unknown>
  } catch {
    return {}
  }
}

export default request
