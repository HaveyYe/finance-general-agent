<template>
  <div class="markdown-text">
    <template v-for="(segment, index) in segments" :key="index">
      <pre v-if="segment.code" class="code-block"><code>{{ segment.text }}</code></pre>
      <div v-else class="text-block">{{ segment.text }}</div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  text: string
}>()

const segments = computed(() => {
  const parts = props.text.split(/```(?:[\w-]+)?\n?/)
  return parts
    .map((text, index) => ({ text, code: index % 2 === 1 }))
    .filter((segment) => segment.text.length > 0)
})
</script>

<style scoped>
.text-block {
  line-height: 1.7;
  white-space: pre-wrap;
}

.code-block {
  overflow-x: auto;
  margin: 8px 0;
  padding: 12px;
  color: #d8eee8;
  background: #17312d;
  border-radius: 8px;
  font-family: "SFMono-Regular", Consolas, monospace;
  font-size: 12px;
  white-space: pre;
}
</style>
