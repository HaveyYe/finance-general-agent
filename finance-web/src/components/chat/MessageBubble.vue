<template>
  <div class="message-row" :class="message.role">
    <div class="bubble panel">
      <div class="bubble-meta">{{ message.role === 'user' ? '你' : '知识库助手' }}</div>
      <MarkdownText v-if="message.type === 'text'" :text="message.content" />

      <FinancialChart
        v-else-if="message.type === 'chart' && message.data"
        :option="message.data"
        @chart-click="(name) => emit('followUp', `请详细分析 ${name} 的财务数据和异常原因`)"
        @chart-dbl-click="(name) => emit('followUp', `下钻查询 ${name} 的财务明细`)"
      />

      <el-table v-else-if="message.type === 'table' && tableRows.length" :data="tableRows" size="small" border>
        <el-table-column v-for="column in tableColumns" :key="column" :prop="column" :label="column" min-width="120" />
      </el-table>

      <el-descriptions v-else-if="message.type === 'card' && cardEntries.length" :column="1" border size="small">
        <el-descriptions-item v-for="[key, value] in cardEntries" :key="key" :label="key">
          {{ formatValue(value) }}
        </el-descriptions-item>
      </el-descriptions>

      <a v-else-if="message.type === 'file'" :href="fileInfo.url" download>{{ fileInfo.name }}</a>

      <div v-if="message.role === 'assistant'" class="message-actions">
        <el-button text size="small" @click="emit('copy', message.content)">复制</el-button>
        <el-button text size="small" @click="emit('regenerate', message.id)">重新生成</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import FinancialChart from '@/components/chart/FinancialChart.vue'
import MarkdownText from '@/components/chat/MarkdownText.vue'
import type { ChatMessage } from '@/stores/chat'

const props = defineProps<{
  message: ChatMessage
}>()

const emit = defineEmits<{
  followUp: [text: string]
  copy: [text: string]
  regenerate: [id: string]
}>()

const tableRows = computed<Record<string, unknown>[]>(() =>
  Array.isArray(props.message.data) ? props.message.data : [],
)

const tableColumns = computed(() => {
  const first = tableRows.value[0]
  return first ? Object.keys(first) : []
})

const cardEntries = computed<[string, unknown][]>(() => {
  const data = props.message.data
  if (!data || typeof data !== 'object' || Array.isArray(data)) return []
  return Object.entries(data as Record<string, unknown>).filter(([, value]) => !value || typeof value !== 'object')
})

const fileInfo = computed(() => {
  const data = (props.message.data && typeof props.message.data === 'object' ? props.message.data : {}) as {
    name?: string
    url?: string
  }
  return { name: data.name || '下载文件', url: data.url || '#' }
})

function formatValue(value: unknown) {
  return Array.isArray(value) ? value.join('；') : String(value ?? '-')
}
</script>

<style scoped>
.message-row {
  display: flex;
  margin: 12px 0;
}

.message-row.user {
  justify-content: flex-end;
}

.bubble {
  width: min(760px, 100%);
  padding: 14px;
}

.message-row.user .bubble {
  width: fit-content;
  max-width: min(680px, 100%);
  background: #e6f4f0;
  border-color: #b6d9d0;
}

.bubble-meta {
  margin-bottom: 8px;
  color: var(--muted);
  font-size: 12px;
}

.message-actions {
  display: flex;
  gap: 4px;
  margin-top: 8px;
  border-top: 1px solid var(--line);
}
</style>
