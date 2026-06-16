<template>
  <div class="report-table panel">
    <el-table :data="rows" border>
      <el-table-column prop="item" label="项目" min-width="140" />
      <el-table-column prop="currentAmount" label="本期金额" min-width="140" />
      <el-table-column prop="previousAmount" label="上期金额" min-width="140" />
    </el-table>
    <div v-if="comment" class="ai-comment">{{ comment }}</div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  rows: Array<{ item: string; currentAmount: number; previousAmount: number }>
  comment?: string
}>()
</script>

<style scoped>
.report-table {
  padding: 14px;
}

.ai-comment {
  margin-top: 14px;
  padding: 12px;
  color: #4d3b17;
  background: #fff6df;
  border: 1px solid #ead39b;
  border-radius: 8px;
  line-height: 1.7;
}
</style>
