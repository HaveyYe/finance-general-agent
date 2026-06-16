<template>
  <div class="chat-input panel">
    <el-input
      v-model="draft"
      type="textarea"
      :autosize="{ minRows: 1, maxRows: 4 }"
      placeholder="输入财务指令，例如：帮我查一下本月应收账款"
      @keydown.enter.exact.prevent="submit"
    />
    <div class="input-actions">
      <el-button :icon="Microphone" @click="startVoice">语音</el-button>
      <el-button type="primary" :icon="Promotion" :loading="loading" @click="submit">发送</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Microphone, Promotion } from '@element-plus/icons-vue'

defineProps<{
  loading: boolean
}>()

const emit = defineEmits<{
  send: [text: string]
}>()

const draft = ref('')

function submit() {
  const value = draft.value.trim()
  if (!value) return
  emit('send', value)
  draft.value = ''
}

function startVoice() {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
  if (!SpeechRecognition) {
    draft.value = '当前浏览器不支持语音输入，请直接输入文字指令'
    return
  }
  const recognition = new SpeechRecognition()
  recognition.lang = 'zh-CN'
  recognition.onresult = (event: SpeechRecognitionEvent) => {
    draft.value = event.results[0]?.[0]?.transcript || ''
  }
  recognition.start()
}
</script>

<style scoped>
.chat-input {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  padding: 12px;
}

.input-actions {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}

@media (max-width: 620px) {
  .chat-input {
    grid-template-columns: 1fr;
  }

  .input-actions {
    justify-content: flex-end;
  }
}
</style>
