<template>
  <div class="quick-actions">
    <el-button v-for="item in quickActions" :key="item.label" @click="emit('pick', item.message)">
      {{ item.icon }} {{ item.label }}
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { quickActions } from '@/config/quickActions'

const emit = defineEmits<{
  pick: [text: string]
}>()

</script>

<style scoped>
.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
