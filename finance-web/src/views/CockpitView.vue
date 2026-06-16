<!--
  驾驶舱页面
  展示应收看板数据，包含统计卡片和账龄分布图表
-->
<template>
  <div class="cockpit-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2 class="page-title">应收看板驾驶舱</h2>
      <el-button type="primary" @click="fetchData" :loading="loading">
        <el-icon><Refresh /></el-icon>
        刷新数据
      </el-button>
    </div>

    <!-- 统计卡片区域 -->
    <div class="stat-cards">
      <div class="stat-card" v-for="(card, idx) in statCards" :key="idx">
        <div class="stat-card-icon" :style="{ background: card.color }">
          <span class="stat-emoji">{{ card.emoji }}</span>
        </div>
        <div class="stat-card-info">
          <div class="stat-label">{{ card.label }}</div>
          <div class="stat-value">{{ card.value }}</div>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="chart-section">
      <DashboardPanel
        title="账龄分布"
        subtitle="每 60 秒自动刷新"
        tag="实时"
        :refresh-interval-ms="60000"
        @refresh="fetchData"
      >
        <FinancialChart
          v-if="chartOption"
          class="cockpit-chart"
          :option="chartOption"
          @chart-click="handleChartClick"
          @chart-dbl-click="handleChartDrilldown"
        />
        <el-empty v-else description="暂无图表数据" />
      </DashboardPanel>
    </div>

    <!-- 数据表格区域 -->
    <div class="table-section">
      <el-card shadow="hover">
        <template #header>
          <div class="card-header">
            <span>应收明细</span>
          </div>
        </template>
        <el-table
          v-if="tableData.length"
          :data="tableData"
          stripe
          border
          style="width: 100%"
          max-height="400"
        >
          <el-table-column
            v-for="col in tableColumns"
            :key="col"
            :prop="col"
            :label="col"
            min-width="120"
            show-overflow-tooltip
          />
        </el-table>
        <el-empty v-else description="暂无数据，请点击刷新" />
      </el-card>
    </div>

    <!-- AI 分析评语 -->
    <div v-if="aiComment" class="ai-comment-section">
      <el-card shadow="hover">
        <template #header>
          <div class="card-header">
            <el-icon><Cpu /></el-icon>
            <span>AI 分析评语</span>
          </div>
        </template>
        <div class="ai-comment-text">{{ aiComment }}</div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { sendChat } from '@/api/agent'
import { ElMessage } from 'element-plus'
import DashboardPanel from '@/components/dashboard/DashboardPanel.vue'
import FinancialChart from '@/components/chart/FinancialChart.vue'

/** 是否加载中 */
const loading = ref(false)

/** 统计卡片数据 */
const statCards = ref([
  { label: '应收总额', value: '--', emoji: '💰', color: 'linear-gradient(135deg, #409eff, #53a8ff)' },
  { label: '回款率', value: '--', emoji: '📈', color: 'linear-gradient(135deg, #67c23a, #85ce61)' },
  { label: '逾期金额', value: '--', emoji: '⚠️', color: 'linear-gradient(135deg, #e6a23c, #ebb563)' },
  { label: '账龄分布', value: '--', emoji: '📊', color: 'linear-gradient(135deg, #f56c6c, #f89898)' },
])

/** 图表配置 */
const chartOption = ref<any>(null)

/** 表格数据 */
const tableData = ref<any[]>([])

/** 表格列名 */
const tableColumns = ref<string[]>([])

/** AI 分析评语 */
const aiComment = ref('')

/**
 * 获取驾驶舱数据
 * 调用 /agent/chat 接口获取应收看板数据
 */
async function fetchData() {
  loading.value = true
  chartOption.value = null
  tableData.value = []
  tableColumns.value = []
  aiComment.value = ''

  try {
    const response = await sendChat({ message: '查询应收看板' })

    // 处理文本评语
    if (response.text) {
      aiComment.value = response.text
    }

    // 处理表格数据
    if (response.type === 'table' && response.content) {
      const data = Array.isArray(response.content) ? response.content : []
      tableData.value = data
      if (data.length) {
        tableColumns.value = Object.keys(data[0])
      }
    }

    // 处理图表数据
    if (response.type === 'chart' && response.content) {
      chartOption.value = response.content
    }

    // 处理卡片数据（尝试从 content 中提取统计信息）
    if (response.type === 'card' && response.content) {
      const cards = Array.isArray(response.content) ? response.content : [response.content]
      cards.forEach((card: any, idx: number) => {
        if (idx < statCards.value.length) {
          const entries = Object.entries(card)
          if (entries.length > 0) {
            statCards.value[idx].value = String(entries[0][1])
          }
        }
      })
    }
  } catch (error) {
    console.error('[CockpitView] 获取数据失败:', error)
  } finally {
    loading.value = false
  }
}

function handleChartClick(name: string) {
  if (name) ElMessage.info(`可在对话页追问：请详细分析 ${name} 的回款风险`)
}

function handleChartDrilldown(name: string) {
  if (name) ElMessage.info(`可在对话页下钻查询 ${name} 的应收明细`)
}

/** 组件挂载时自动获取数据 */
onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.cockpit-container {
  padding: 24px;
  overflow-y: auto;
  height: 100%;
}

/* 页面标题 */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
  margin: 0;
}

/* 统计卡片 */
.stat-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  transition: transform 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-card-icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-emoji {
  font-size: 24px;
}

.stat-card-info {
  flex: 1;
  min-width: 0;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
}

/* 图表区域 */
.chart-section {
  margin-bottom: 24px;
}

.chart-card {
  border-radius: 12px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  font-size: 15px;
}

.cockpit-chart {
  width: 100%;
  height: 360px;
}

/* 表格区域 */
.table-section {
  margin-bottom: 24px;
}

/* AI 评语区域 */
.ai-comment-section {
  margin-bottom: 24px;
}

.ai-comment-text {
  font-size: 14px;
  line-height: 1.8;
  color: #606266;
  white-space: pre-wrap;
}

/* 响应式 */
@media (max-width: 768px) {
  .cockpit-container {
    padding: 12px;
  }

  .stat-cards {
    grid-template-columns: repeat(2, 1fr);
  }

  .cockpit-chart {
    height: 260px;
  }
}
</style>
