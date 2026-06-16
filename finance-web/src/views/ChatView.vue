<!--
  对话页面（核心页面）
  类 ChatGPT 的对话交互界面，支持文本、表格、图表、卡片多种消息类型
-->
<template>
  <div class="chat-page">
    <aside class="history-panel">
      <el-button type="primary" plain class="new-session" @click="chatStore.createSession()" :disabled="chatStore.loading">
        新建会话
      </el-button>
      <div class="history-title">历史会话</div>
      <button
        v-for="session in chatStore.sessions"
        :key="session.id"
        class="history-item"
        :class="{ active: session.id === chatStore.activeSessionId }"
        @click="chatStore.switchSession(session.id)"
      >
        <span class="history-copy">
          <strong>{{ session.title }}</strong>
          <small>{{ formatSessionTime(session.updatedAt) }}</small>
        </span>
        <el-icon class="history-delete" @click.stop="chatStore.deleteSession(session.id)"><Delete /></el-icon>
      </button>
    </aside>

    <div class="chat-container">
    <!-- 顶部标题区域 -->
    <div class="chat-header">
      <div class="header-info">
        <div class="header-icon">
          <el-icon :size="22"><Cpu /></el-icon>
        </div>
        <div>
          <h3 class="header-title">知识库问答</h3>
          <p class="header-desc">基于已上传文档检索答案
            <span v-if="knowledgeStatus === 'online'" style="margin-left: 8px; color: #67c23a; font-size: 12px">● 知识库在线</span>
            <span v-else-if="knowledgeStatus === 'offline'" style="margin-left: 8px; color: #f56c6c; font-size: 12px">● 知识库离线</span>
          </p>
        </div>
      </div>
      <el-button text @click="handleClear" :disabled="chatStore.loading">
        <el-icon><Delete /></el-icon>
        清空对话
      </el-button>
    </div>

    <!-- 消息列表区域（可滚动） -->
    <div class="chat-messages" ref="messagesRef">
      <div class="messages-inner">
        <template v-for="msg in chatStore.messages" :key="msg.id">
          <!-- 用户消息 -->
          <div v-if="msg.role === 'user'" class="message-row user-row">
            <div class="message-bubble user-bubble">
              <div class="bubble-content">{{ msg.content }}</div>
              <div class="bubble-time">{{ formatTime(msg.createdAt) }}</div>
            </div>
            <div class="avatar user-avatar">
              <el-icon :size="18"><User /></el-icon>
            </div>
          </div>

          <!-- AI 消息 -->
          <div v-else class="message-row assistant-row">
            <div class="avatar ai-avatar">
              <el-icon :size="18"><Coin /></el-icon>
            </div>
            <div class="message-bubble ai-bubble">
              <!-- 工具调用信息 -->
              <div v-if="msg.toolCalls && msg.toolCalls.length" class="tool-calls">
                <div
                  v-for="(tool, idx) in msg.toolCalls"
                  :key="idx"
                  class="tool-call-item"
                >
                  <el-icon class="tool-icon"><SetUp /></el-icon>
                  <span>调用了 <strong>{{ (tool as any).name || '工具' }}</strong></span>
                </div>
              </div>

              <!-- 文本消息 -->
              <div v-if="msg.type === 'text'" class="bubble-content">
                <MarkdownText v-if="msg.content" :text="msg.content" />
                <div v-else-if="chatStore.loading && msg.id === lastMessageId" class="typing-indicator">
                  <span class="dot"></span>
                  <span class="dot"></span>
                  <span class="dot"></span>
                  <span class="typing-text">{{ chatStore.streamingStatus || '正在生成回答...' }}</span>
                </div>
              </div>

              <!-- 表格消息 -->
              <div v-else-if="msg.type === 'table'" class="bubble-content">
                <div v-if="msg.content" class="msg-text-intro">{{ msg.content }}</div>
                <el-table
                  v-if="msg.data && msg.data.length"
                  :data="msg.data"
                  stripe
                  border
                  size="small"
                  style="width: 100%; margin-top: 8px;"
                  max-height="400"
                >
                  <el-table-column
                    v-for="col in getTableColumns(msg.data)"
                    :key="col"
                    :prop="col"
                    :label="col"
                    min-width="120"
                    show-overflow-tooltip
                  />
                </el-table>
              </div>

              <!-- 图表消息 -->
              <div v-else-if="msg.type === 'chart'" class="bubble-content">
                <div v-if="msg.content" class="msg-text-intro">{{ msg.content }}</div>
                <FinancialChart
                  v-if="msg.data"
                  class="chart-container"
                  :option="msg.data"
                  @chart-click="handleChartClick"
                  @chart-dbl-click="handleChartDrilldown"
                />
              </div>

              <!-- 卡片消息 -->
              <div v-else-if="msg.type === 'card'" class="bubble-content">
                <div v-if="msg.content" class="msg-text-intro">{{ msg.content }}</div>
                <div class="card-list" v-if="msg.data">
                  <el-card
                    v-for="(card, idx) in (Array.isArray(msg.data) ? msg.data : [msg.data])"
                    :key="idx"
                    shadow="hover"
                    class="info-card"
                  >
                    <template v-for="(val, key) in card" :key="key">
                      <div class="card-field">
                        <span class="field-label">{{ key }}:</span>
                        <span class="field-value">{{ val }}</span>
                      </div>
                    </template>
                  </el-card>
                </div>
              </div>

              <div v-else-if="msg.type === 'file'" class="bubble-content file-message">
                <a :href="fileInfo(msg.data).url" download>{{ fileInfo(msg.data).name }}</a>
              </div>

              <!-- 兜底：直接显示文本 -->
              <div v-else class="bubble-content">
                {{ msg.content }}
              </div>

              <div v-if="msg.content" class="message-actions">
                <el-button text size="small" @click="copyMessage(msg.content)">复制</el-button>
                <el-button text size="small" @click="chatStore.regenerate(msg.id)" :disabled="chatStore.loading">重新生成</el-button>
              </div>
              <div class="bubble-time">{{ formatTime(msg.createdAt) }}</div>
            </div>
          </div>
        </template>

        <!-- 加载状态提示 -->
        <div v-if="showLoadingRow" class="message-row assistant-row">
          <div class="avatar ai-avatar">
            <el-icon :size="18"><Coin /></el-icon>
          </div>
          <div class="message-bubble ai-bubble">
            <div class="typing-indicator">
              <span class="dot"></span>
              <span class="dot"></span>
              <span class="dot"></span>
              <span class="typing-text">{{ chatStore.streamingStatus || '正在思考中...' }}</span>
            </div>
          </div>
        </div>

        <!-- 滚动锚点 -->
        <div ref="scrollAnchor"></div>
      </div>
    </div>

    <!-- 底部输入区域 -->
    <div class="chat-input-area">
      <!-- 输入框和发送按钮 -->
      <div class="input-row">
        <el-input
          v-model="inputText"
          type="textarea"
          :rows="2"
          placeholder="请输入要查询知识库的问题，如：住宿费报销标准是多少..."
          resize="none"
          @keydown.enter.exact.prevent="handleSend"
          :disabled="chatStore.loading"
        />
        <el-button
          :icon="Microphone"
          circle
          size="large"
          title="语音输入"
          @click="startVoice"
          :disabled="chatStore.loading"
        />
        <el-button
          type="primary"
          :icon="Promotion"
          circle
          size="large"
          class="send-btn"
          @click="handleSend"
          :loading="chatStore.loading"
          :disabled="!inputText.trim()"
        />
      </div>
    </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, nextTick, watch } from 'vue'
import { Coin, Microphone, Promotion } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useChatStore } from '@/stores/chat'
import { useSettingsStore } from '@/stores/settings'
import FinancialChart from '@/components/chart/FinancialChart.vue'
import MarkdownText from '@/components/chat/MarkdownText.vue'

/** 对话状态管理 */
const chatStore = useChatStore()

/** 设置状态管理 */
const settingsStore = useSettingsStore()

/** 知识库状态 */
const knowledgeStatus = computed(() => settingsStore.knowledgeStatus)

/** 用户输入文本 */
const inputText = ref('')

/** 消息列表容器引用 */
const messagesRef = ref<HTMLElement>()

/** 滚动锚点引用 */
const scrollAnchor = ref<HTMLElement>()

const lastMessageId = computed(() => chatStore.messages.at(-1)?.id || '')

const showLoadingRow = computed(() => {
  const lastMessage = chatStore.messages.at(-1)
  return chatStore.loading && lastMessage?.role !== 'assistant'
})

/**
 * 组件挂载时初始化欢迎语
 */
onMounted(() => {
  chatStore.initWelcome()
  settingsStore.checkKnowledgeStatus()
  scrollToBottom()
})

/**
 * 监听消息变化，自动滚动到底部
 */
watch(
  () => chatStore.messages.length,
  () => nextTick(scrollToBottom),
)

/**
 * 监听流式内容变化，token 追加时保持滚动到底部
 */
watch(
  () => chatStore.messages.map((message) => message.content).join('\u0000'),
  () => nextTick(scrollToBottom),
)

/**
 * 监听加载状态变化，滚动到底部
 */
watch(
  () => chatStore.loading,
  () => nextTick(scrollToBottom),
)

/**
 * 发送消息
 */
function handleSend() {
  const text = inputText.value.trim()
  if (!text || chatStore.loading) return
  inputText.value = ''
  chatStore.send(text)
}

function handleChartClick(name: string) {
  if (!name) return
  inputText.value = `请详细分析 ${name} 的财务数据和异常原因`
}

function handleChartDrilldown(name: string) {
  if (!name || chatStore.loading) return
  inputText.value = ''
  chatStore.send(`下钻查询 ${name} 的财务明细`)
}

/**
 * 清空对话
 */
function handleClear() {
  chatStore.clearChat()
}

function startVoice() {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
  if (!SpeechRecognition) {
    ElMessage.warning('当前浏览器不支持语音输入')
    return
  }
  const recognition = new SpeechRecognition()
  recognition.lang = 'zh-CN'
  recognition.onresult = (event: SpeechRecognitionEvent) => {
    inputText.value = event.results[0]?.[0]?.transcript || ''
  }
  recognition.start()
}

/**
 * 滚动到消息底部
 */
function scrollToBottom() {
  if (scrollAnchor.value) {
    scrollAnchor.value.scrollIntoView({ behavior: 'smooth' })
  }
}

/**
 * 格式化时间戳
 */
function formatTime(ts: number): string {
  const d = new Date(ts)
  const h = d.getHours().toString().padStart(2, '0')
  const m = d.getMinutes().toString().padStart(2, '0')
  return `${h}:${m}`
}

function formatSessionTime(ts: number): string {
  return new Intl.DateTimeFormat('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(ts)
}

async function copyMessage(content: string) {
  try {
    await navigator.clipboard.writeText(content)
    ElMessage.success('已复制')
  } catch {
    ElMessage.warning('当前环境无法访问剪贴板')
  }
}

function fileInfo(data: unknown) {
  const file = (data && typeof data === 'object' ? data : {}) as { name?: string; url?: string }
  return {
    name: file.name || '下载文件',
    url: file.url || '#',
  }
}

/**
 * 从表格数据中提取列名
 * @param data 表格数据数组
 * @returns 列名数组
 */
function getTableColumns(data: any[]): string[] {
  if (!data || !data.length) return []
  return Object.keys(data[0])
}
</script>

<style scoped>
.chat-page {
  display: grid;
  grid-template-columns: 230px minmax(0, 1fr);
  height: 100%;
  min-height: 0;
  background: #f7f8fa;
}

.history-panel {
  min-height: 0;
  overflow-y: auto;
  padding: 16px 12px;
  background: #eef2f4;
  border-right: 1px solid #dfe6e8;
}

.new-session {
  width: 100%;
  margin-bottom: 16px;
}

.history-title {
  margin: 0 8px 8px;
  color: #647277;
  font-size: 12px;
  font-weight: 700;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  margin-bottom: 6px;
  padding: 10px;
  color: #263438;
  text-align: left;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 10px;
  cursor: pointer;
}

.history-item:hover,
.history-item.active {
  background: #fff;
  border-color: #c8d7da;
}

.history-copy {
  display: grid;
  gap: 4px;
  min-width: 0;
  flex: 1;
}

.history-copy strong {
  overflow: hidden;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-copy small {
  color: #88979b;
  font-size: 10px;
}

.history-delete {
  color: #9aa8ac;
}

/* 对话容器 */
.chat-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f7f8fa;
  min-width: 0;
  min-height: 0;
}

/* ====== 顶部标题区域 ====== */
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
}

.header-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #409eff, #53a8ff);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.header-desc {
  font-size: 12px;
  color: #909399;
  margin: 2px 0 0;
}

/* ====== 消息列表区域 ====== */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  scroll-behavior: smooth;
}

.messages-inner {
  max-width: 900px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 消息行 */
.message-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.user-row {
  justify-content: flex-end;
}

.assistant-row {
  justify-content: flex-start;
}

/* 头像 */
.avatar {
  width: 36px;
  height: 36px;
  min-width: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.user-avatar {
  background: linear-gradient(135deg, #409eff, #53a8ff);
  color: #fff;
}

.ai-avatar {
  background: linear-gradient(135deg, #f6c343 0%, #e6a317 100%);
  color: #fff;
  box-shadow: 0 2px 8px rgba(230, 163, 23, 0.35);
}

/* 消息气泡 */
.message-bubble {
  max-width: 70%;
  border-radius: 12px;
  padding: 12px 16px;
  position: relative;
  word-break: break-word;
}

.user-bubble {
  background: linear-gradient(135deg, #409eff, #337ecc);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.ai-bubble {
  background: #fff;
  color: #303133;
  border-bottom-left-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.bubble-content {
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
}

.bubble-time {
  font-size: 11px;
  margin-top: 6px;
  opacity: 0.5;
  text-align: right;
}

.message-actions {
  display: flex;
  gap: 2px;
  margin-top: 6px;
  border-top: 1px solid #edf0f2;
}

.file-message a {
  color: #19796b;
  font-weight: 700;
}

.user-bubble .bubble-time {
  color: rgba(255, 255, 255, 0.7);
}

/* 文本介绍 */
.msg-text-intro {
  margin-bottom: 8px;
  color: #606266;
}

/* ====== 工具调用信息 ====== */
.tool-calls {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

.tool-call-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: #ecf5ff;
  border: 1px solid #d9ecff;
  border-radius: 6px;
  font-size: 12px;
  color: #409eff;
}

.tool-icon {
  font-size: 14px;
}

/* ====== 图表容器 ====== */
.chart-container {
  width: 100%;
  height: 320px;
  margin-top: 8px;
}

/* ====== 卡片列表 ====== */
.card-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 12px;
  margin-top: 8px;
}

.info-card {
  border-radius: 8px;
}

.info-card :deep(.el-card__body) {
  padding: 12px 16px;
}

.card-field {
  display: flex;
  justify-content: space-between;
  padding: 4px 0;
  font-size: 13px;
}

.field-label {
  color: #909399;
}

.field-value {
  color: #303133;
  font-weight: 500;
}

/* ====== 打字动画 ====== */
.typing-indicator {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 0;
}

.dot {
  width: 6px;
  height: 6px;
  background: #409eff;
  border-radius: 50%;
  animation: bounce 1.4s infinite both;
}

.dot:nth-child(2) { animation-delay: 0.16s; }
.dot:nth-child(3) { animation-delay: 0.32s; }

.typing-text {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
}

@keyframes bounce {
  0%, 80%, 100% {
    transform: scale(0.6);
    opacity: 0.4;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

/* ====== 底部输入区域 ====== */
.chat-input-area {
  padding: 12px 24px 16px;
  background: #fff;
  border-top: 1px solid #ebeef5;
}

.input-row {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.input-row :deep(.el-textarea__inner) {
  border-radius: 12px;
  padding: 10px 14px;
  font-size: 14px;
}

.send-btn {
  width: 44px;
  height: 44px;
  flex-shrink: 0;
}

/* ====== 响应式 ====== */
@media (max-width: 768px) {
  .chat-page {
    grid-template-columns: 1fr;
  }

  .history-panel {
    display: none;
  }

  .message-bubble {
    max-width: 85%;
  }

  .chat-messages {
    padding: 12px;
  }

  .chat-input-area {
    padding: 8px 12px 12px;
  }
}
</style>
