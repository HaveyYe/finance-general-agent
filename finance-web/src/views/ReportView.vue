<!--
  报表页面
  展示三大财务报表：资产负债表、利润表、现金流量表
  每个Tab独立调用AI获取报表数据
-->
<template>
  <div class="report-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2 class="page-title">财务报表</h2>
      <div class="header-actions">
        <el-date-picker
          v-model="reportPeriod"
          type="month"
          value-format="YYYY-MM"
          placeholder="选择报表期间"
          style="width: 180px"
        />
        <el-button
          type="success"
          :icon="Download"
          :disabled="!reportData.length"
          @click="exportToExcel"
        >
          导出 Excel
        </el-button>
      </div>
    </div>

    <!-- 报表 Tab 切换 -->
    <el-tabs v-model="activeTab" type="border-card" class="report-tabs" @tab-change="handleTabChange">
      <!-- 资产负债表 -->
      <el-tab-pane label="资产负债表" name="balance">
        <template #label>
          <div class="tab-label">
            <span class="tab-icon">📋</span>
            <span>资产负债表</span>
          </div>
        </template>
      </el-tab-pane>

      <!-- 利润表 -->
      <el-tab-pane label="利润表" name="profit">
        <template #label>
          <div class="tab-label">
            <span class="tab-icon">📊</span>
            <span>利润表</span>
          </div>
        </template>
      </el-tab-pane>

      <!-- 现金流量表 -->
      <el-tab-pane label="现金流量表" name="cashflow">
        <template #label>
          <div class="tab-label">
            <span class="tab-icon">💵</span>
            <span>现金流量表</span>
          </div>
        </template>
      </el-tab-pane>
    </el-tabs>

    <!-- 报表内容区域 -->
    <div class="report-content">
      <!-- 加载状态 -->
      <div v-if="loading" class="loading-area">
        <el-icon class="loading-icon is-loading"><Loading /></el-icon>
        <span>AI 正在生成报表...</span>
      </div>

      <!-- 报表表格 -->
      <el-table
        v-else-if="reportData.length"
        :data="reportData"
        stripe
        border
        style="width: 100%"
        max-height="500"
      >
        <el-table-column
          v-for="col in reportColumns"
          :key="col"
          :prop="col"
          :label="col"
          :min-width="col === reportColumns[0] ? 180 : 130"
          show-overflow-tooltip
        />
      </el-table>

      <!-- 空状态 -->
      <el-empty v-else description="请点击Tab切换查看对应报表" />
    </div>

    <!-- AI 分析评语 -->
    <div v-if="aiAnalysis" class="ai-analysis">
      <el-card shadow="hover">
        <template #header>
          <div class="card-header">
            <el-icon><Cpu /></el-icon>
            <span>AI 财务分析</span>
          </div>
        </template>
        <div class="analysis-text">{{ aiAnalysis }}</div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { sendChat } from '@/api/agent'
import { Download } from '@element-plus/icons-vue'

/** 报表期间 */
const reportPeriod = ref('2026-05')

/** 当前激活的Tab */
const activeTab = ref('balance')

/** 是否加载中 */
const loading = ref(false)

/** 报表数据 */
const reportData = ref<any[]>([])

/** 报表列名 */
const reportColumns = ref<string[]>([])

/** AI 分析评语 */
const aiAnalysis = ref('')

/** 会话ID */
const sessionId = ref('')

/**
 * 根据Tab名称和当前期间生成查询消息
 * @param tabName Tab名称
 */
function getTabMessage(tabName: string) {
  const names: Record<string, string> = {
    balance: '资产负债表',
    profit: '利润表',
    cashflow: '现金流量表',
  }
  return `生成 ${reportPeriod.value} ${names[tabName]}`
}

/**
 * 切换Tab时获取对应报表数据
 * @param tab Tab名称
 */
async function handleTabChange(tab: string | number) {
  const tabName = String(tab)
  const message = getTabMessage(tabName)
  if (!message) return

  loading.value = true
  reportData.value = []
  reportColumns.value = []
  aiAnalysis.value = ''

  try {
    const response = await sendChat({
      sessionId: sessionId.value || undefined,
      message,
    })

    sessionId.value = response.sessionId

    // 处理文本分析
    if (response.text) {
      aiAnalysis.value = response.text
    }

    // 处理表格数据
    if (response.type === 'table' && response.content) {
      const data = Array.isArray(response.content) ? response.content : []
      reportData.value = data
      if (data.length) {
        reportColumns.value = Object.keys(data[0])
      }
    }

    // 处理图表数据（如果有，作为分析展示）
    if (response.type === 'chart' && response.text) {
      aiAnalysis.value = response.text
    }
  } catch (error) {
    console.error('[ReportView] 获取报表失败:', error)
    aiAnalysis.value = '获取报表数据失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}

/**
 * 导出当前报表数据为 CSV 文件
 */
function exportToExcel() {
  if (!reportData.value.length) return
  const headers = reportColumns.value
  const csv = [
    headers.join(','),
    ...reportData.value.map(row =>
      headers.map(h => `"${String(row[h] ?? '').replace(/"/g, '""')}"`).join(',')
    ),
  ].join('\n')
  const BOM = '\uFEFF'
  const blob = new Blob([BOM + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${activeTab.value}_${reportPeriod.value}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

/** 监听期间变化，自动刷新当前报表 */
watch(reportPeriod, () => {
  handleTabChange(activeTab.value)
})
</script>

<style scoped>
.report-container {
  padding: 24px;
  overflow-y: auto;
  height: 100%;
}

/* 页面标题 */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
  margin: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* Tab 样式 */
.report-tabs {
  border-radius: 12px;
  margin-bottom: 20px;
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 6px;
}

.tab-icon {
  font-size: 16px;
}

/* 报表内容 */
.report-content {
  margin-bottom: 24px;
}

/* 加载状态 */
.loading-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  color: #909399;
  gap: 12px;
}

.loading-icon {
  font-size: 28px;
  color: #409eff;
}

/* 卡片头部 */
.card-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  font-size: 15px;
}

/* AI 分析区域 */
.ai-analysis {
  margin-bottom: 24px;
}

.analysis-text {
  font-size: 14px;
  line-height: 1.8;
  color: #606266;
  white-space: pre-wrap;
}

/* 响应式 */
@media (max-width: 768px) {
  .report-container {
    padding: 12px;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .header-actions {
    width: 100%;
    flex-wrap: wrap;
  }
}
</style>
