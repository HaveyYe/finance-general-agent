<template>
  <div class="chat-window">
    <aside class="session-list panel">
      <div class="session-head">历史会话</div>
      <button class="session-item active">今天 · 财务问答</button>
      <button class="session-item">昨天 · 应收分析</button>
      <button class="session-item">更早 · 报表生成</button>
    </aside>

    <section class="conversation">
      <div ref="messageBox" class="message-list">
        <MessageBubble
          v-for="message in chat.messages"
          :key="message.id"
          :message="message"
          @follow-up="chat.send"
          @copy="copyMessage"
          @regenerate="chat.regenerate"
        />
        <div v-if="chat.loading" class="typing panel">正在调用 MCP 工具...</div>
      </div>

      <div class="composer">
        <ChatInput :loading="chat.loading" @send="chat.send" />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import ChatInput from '@/components/chat/ChatInput.vue'
import MessageBubble from '@/components/chat/MessageBubble.vue'
import { useChatStore } from '@/stores/chat'
import { ElMessage } from 'element-plus'

const chat = useChatStore()
const messageBox = ref<HTMLDivElement>()

async function copyMessage(text: string) {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制')
  } catch {
    ElMessage.warning('当前环境无法访问剪贴板')
  }
}

watch(
  () => chat.messages.length,
  async () => {
    await nextTick()
    messageBox.value?.scrollTo({ top: messageBox.value.scrollHeight, behavior: 'smooth' })
  },
)
</script>

<style scoped>
.chat-window {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 14px;
  height: calc(100vh - 104px);
}

.session-list {
  padding: 12px;
}

.session-head {
  margin-bottom: 10px;
  font-weight: 720;
}

.session-item {
  width: 100%;
  min-height: 38px;
  margin-bottom: 8px;
  padding: 0 10px;
  text-align: left;
  color: var(--text);
  background: transparent;
  border: 1px solid var(--line);
  border-radius: 8px;
  cursor: pointer;
}

.session-item.active {
  background: #e6f4f0;
  border-color: #a9d3c8;
}

.conversation {
  display: grid;
  grid-template-rows: minmax(0, 1fr) auto;
  min-width: 0;
}

.message-list {
  min-height: 0;
  overflow: auto;
  padding-right: 4px;
}

.typing {
  width: fit-content;
  padding: 10px 12px;
  color: var(--muted);
}

.composer {
  display: grid;
  gap: 10px;
}

@media (max-width: 900px) {
  .chat-window {
    grid-template-columns: 1fr;
    height: auto;
  }

  .session-list {
    display: none;
  }
}
</style>
