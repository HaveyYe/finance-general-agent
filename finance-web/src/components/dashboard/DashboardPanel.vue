<template>
  <section class="panel dashboard-panel">
    <div class="panel-head">
      <div>
        <div class="panel-title">{{ title }}</div>
        <div v-if="subtitle" class="panel-subtitle">{{ subtitle }}</div>
      </div>
      <el-tag v-if="tag" effect="plain">{{ tag }}</el-tag>
    </div>
    <slot />
  </section>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, watch } from 'vue'

const props = defineProps<{
  title: string
  subtitle?: string
  tag?: string
  refreshIntervalMs?: number
}>()

const emit = defineEmits<{
  refresh: []
}>()

let timer: number | undefined

function configureTimer() {
  if (timer) window.clearInterval(timer)
  timer = undefined
  if (props.refreshIntervalMs && props.refreshIntervalMs > 0) {
    timer = window.setInterval(() => emit('refresh'), props.refreshIntervalMs)
  }
}

onMounted(configureTimer)
watch(() => props.refreshIntervalMs, configureTimer)

onBeforeUnmount(() => {
  if (timer) window.clearInterval(timer)
})
</script>

<style scoped>
.dashboard-panel {
  padding: 16px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.panel-title {
  font-weight: 760;
}

.panel-subtitle {
  margin-top: 4px;
  color: var(--muted);
  font-size: 12px;
}
</style>
