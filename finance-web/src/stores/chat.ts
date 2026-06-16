import { defineStore } from 'pinia'
import { ref } from 'vue'
import { streamChat, type ClientContext } from '@/api/agent'
import { resolveDingTalkContext } from '@/api/dingtalk'
import type { ToolAuditLog } from '@/types/finance'

export type ChatMessageType = 'text' | 'table' | 'chart' | 'card' | 'file'

export interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  type: ChatMessageType
  data?: any
  toolCalls?: object[]
  callChain?: ToolAuditLog[]
  createdAt: number
}

export interface ChatSession {
  id: string
  gatewaySessionId?: string
  title: string
  createdAt: number
  updatedAt: number
  messages: ChatMessage[]
}

const STORAGE_KEY = 'finance-agent-rag-chat-sessions-v2'
const LEGACY_WELCOME_CONTENT = '我是知识库问答助手。你可以先在知识库上传制度、手册、说明文档，然后直接提问；我会从知识库中检索答案。'
const WELCOME_CONTENT = '你好，我是财务数智人。左侧已打开对话、报销和知识库三个工作区：我可以在对话里回答财务和制度问题，在报销里协助处理报销单并引用规则，在知识库里上传、管理和检索制度文档。你可以直接问我财务、报销或知识库相关问题。'

function generateId() {
  return `${Date.now().toString(36)}${Math.random().toString(36).substring(2)}`
}

function welcomeMessage(): ChatMessage {
  return {
    id: generateId(),
    role: 'assistant',
    content: WELCOME_CONTENT,
    type: 'text',
    createdAt: Date.now(),
  }
}

function normalizeWelcomeContent(message: ChatMessage) {
  if (message.role === 'assistant' && message.type === 'text' && message.content === LEGACY_WELCOME_CONTENT) {
    message.content = WELCOME_CONTENT
  }
}

export const useChatStore = defineStore('chat', () => {
  const sessionId = ref('')
  const activeSessionId = ref('')
  const sessions = ref<ChatSession[]>([])
  const messages = ref<ChatMessage[]>([])
  const loading = ref(false)
  const streamingStatus = ref('')
  const initialized = ref(false)
  const clientContext = ref<ClientContext>({
    channel: 'web',
    userId: 'local-user',
    userName: '本地用户',
  })

  function initWelcome() {
    if (!initialized.value) {
      restoreSessions()
      initialized.value = true
      void hydrateClientContext()
    }
    if (!activeSessionId.value) createSession()
    if (messages.value.length === 0) {
      messages.value.push(welcomeMessage())
      persistCurrentSession()
    }
  }

  async function hydrateClientContext() {
    clientContext.value = await resolveDingTalkContext()
  }

  function createSession() {
    persistCurrentSession()
    const now = Date.now()
    const session: ChatSession = {
      id: generateId(),
      title: '新对话',
      createdAt: now,
      updatedAt: now,
      messages: [welcomeMessage()],
    }
    sessions.value.unshift(session)
    activateSession(session)
    persist()
  }

  function switchSession(id: string) {
    if (id === activeSessionId.value || loading.value) return
    persistCurrentSession()
    const target = sessions.value.find((item) => item.id === id)
    if (target) activateSession(target)
  }

  function deleteSession(id: string) {
    if (loading.value) return
    sessions.value = sessions.value.filter((item) => item.id !== id)
    if (activeSessionId.value === id) {
      const next = sessions.value[0]
      if (next) activateSession(next)
      else createSession()
    }
    persist()
  }

  async function send(message: string) {
    await execute(message, true)
  }

  async function regenerate(assistantMessageId: string) {
    if (loading.value) return
    const index = messages.value.findIndex((message) => message.id === assistantMessageId)
    if (index < 0) return
    let prompt = ''
    for (let i = index - 1; i >= 0; i--) {
      if (messages.value[i].role === 'user') {
        prompt = messages.value[i].content
        break
      }
    }
    if (!prompt) return
    messages.value.splice(index, 1)
    await execute(prompt, false)
  }

  async function execute(message: string, appendUser: boolean) {
    const prompt = message.trim()
    if (!prompt || loading.value) return
    initWelcome()

    if (appendUser) {
      messages.value.push({
        id: generateId(),
        role: 'user',
        content: prompt,
        type: 'text',
        createdAt: Date.now(),
      })
      updateSessionTitle(prompt)
    }

    const assistant: ChatMessage = {
      id: generateId(),
      role: 'assistant',
      content: '',
      type: 'text',
      createdAt: Date.now(),
    }
    messages.value.push(assistant)
    const assistantIndex = messages.value.length - 1
    loading.value = true
    streamingStatus.value = '正在检索知识库...'

    try {
      const response = await streamChat(
        {
          sessionId: sessionId.value || undefined,
          message: prompt,
          clientContext: clientContext.value,
        },
        {
          onStatus: (status) => { streamingStatus.value = status },
          onToken: (token) => {
            const current = messages.value[assistantIndex]
            if (current) current.content += token
          },
          onStructured: (type, content) => {
            const current = messages.value[assistantIndex]
            if (!current) return
            current.type = type
            current.data = content
          },
        },
      )
      sessionId.value = response.sessionId
      const current = messages.value[assistantIndex]
      if (current) {
        current.content = response.text || current.content
        current.type = response.type || current.type
        current.data = response.content ?? current.data
        current.toolCalls = response.toolCalls || (response.toolCall ? [response.toolCall] : undefined)
        current.callChain = response.callChain as ToolAuditLog[] | undefined
      }
    } catch (error) {
      const current = messages.value[assistantIndex]
      if (current) {
        current.content = `抱歉，流式请求失败：${error instanceof Error ? error.message : String(error)}`
        current.type = 'text'
      }
    } finally {
      loading.value = false
      streamingStatus.value = ''
      persistCurrentSession()
    }
  }

  function clearChat() {
    sessionId.value = ''
    messages.value = [welcomeMessage()]
    const current = currentSession()
    if (current) current.title = '新对话'
    persistCurrentSession()
  }

  function activateSession(session: ChatSession) {
    activeSessionId.value = session.id
    sessionId.value = session.gatewaySessionId || ''
    messages.value = session.messages
  }

  function currentSession() {
    return sessions.value.find((item) => item.id === activeSessionId.value)
  }

  function updateSessionTitle(prompt: string) {
    const current = currentSession()
    if (!current || current.title !== '新对话') return
    const title = recognizeSessionTitle(prompt)
    if (title) current.title = title
  }

  function recognizeSessionTitle(prompt: string): string {
    const MAX = 24
    const firstLine = prompt.split(/\r?\n/).map((l) => l.trim()).find(Boolean) || ''
    const stripped = firstLine
      .replace(/^(你好|您好|hi|hello|请问|麻烦|帮我|麻烦问一下|咨询一下)[，,。、!！?？\s]*/i, '')
      .replace(/[？?。.，,、!！;；\s]+$/g, '')
      .trim()
    const text = stripped || firstLine
    if (!text) return ''
    return text.length > MAX ? `${text.slice(0, MAX)}…` : text
  }

  function persistCurrentSession() {
    const current = currentSession()
    if (!current) return
    current.gatewaySessionId = sessionId.value || undefined
    current.messages = messages.value
    current.updatedAt = Date.now()
    persist()
  }

  function persist() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({
      activeSessionId: activeSessionId.value,
      sessions: sessions.value,
    }))
  }

  function restoreSessions() {
    try {
      const raw = localStorage.getItem(STORAGE_KEY)
      if (!raw) return
      const state = JSON.parse(raw) as { activeSessionId?: string; sessions?: ChatSession[] }
      sessions.value = Array.isArray(state.sessions) ? state.sessions : []
      sessions.value.forEach((session) => session.messages.forEach(normalizeWelcomeContent))
      const target = sessions.value.find((item) => item.id === state.activeSessionId) || sessions.value[0]
      if (target) activateSession(target)
      persist()
    } catch {
      localStorage.removeItem(STORAGE_KEY)
    }
  }

  return {
    sessionId,
    activeSessionId,
    sessions,
    messages,
    loading,
    streamingStatus,
    clientContext,
    initWelcome,
    createSession,
    switchSession,
    deleteSession,
    send,
    regenerate,
    clearChat,
  }
})
