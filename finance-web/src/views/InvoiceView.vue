<!--
  发票管理页面
  展示发票台账列表，支持查询和拍照识别
-->
<template>
  <div class="invoice-container">
    <!-- 页面标题和操作栏 -->
    <div class="page-header">
      <h2 class="page-title">发票管理</h2>
      <div class="header-actions">
        <el-button type="primary" @click="fetchInvoices" :loading="loading">
          <el-icon><Search /></el-icon>
          查询发票
        </el-button>
        <el-button type="success" @click="handlePhotoRecognize" :loading="recognizeLoading">
          <el-icon><Camera /></el-icon>
          拍照识别记账
        </el-button>
      </div>
    </div>

    <!-- 发票统计卡片 -->
    <div class="invoice-stats">
      <div class="stat-item" v-for="(item, idx) in statsData" :key="idx">
        <div class="stat-item-label">{{ item.label }}</div>
        <div class="stat-item-value" :style="{ color: item.color }">{{ item.value }}</div>
      </div>
    </div>

    <InvoiceUploader
      ref="uploaderRef"
      :session-id="sessionId"
      @session="sessionId = $event"
      @recognized="handleUploadRecognized"
      @verify="handleVerifySingle"
      @voucher="handleVoucherSingle"
      @batch-verify="handleBatchVerify"
      @batch-voucher="handleBatchVoucher"
    />

    <!-- 发票列表表格 -->
    <el-card shadow="hover" class="table-card">
      <template #header>
        <div class="card-header">
          <span>发票台账</span>
          <el-tag type="info" size="small">共 {{ tableData.length }} 条</el-tag>
        </div>
      </template>

      <el-table
        v-if="tableData.length"
        :data="tableData"
        stripe
        border
        style="width: 100%"
        max-height="500"
        v-loading="loading"
      >
        <el-table-column
          v-for="col in tableColumns"
          :key="col"
          :prop="col"
          :label="getColumnLabel(col)"
          min-width="130"
          show-overflow-tooltip
        />
      </el-table>

      <el-empty v-else description="暂无发票数据，请点击查询" />
    </el-card>

    <!-- AI 识别结果 -->
    <div v-if="recognizeResult" class="recognize-result">
      <el-card shadow="hover">
        <template #header>
          <div class="card-header">
            <el-icon><Cpu /></el-icon>
            <span>AI 识别结果</span>
          </div>
        </template>
        <div class="recognize-text">{{ recognizeResult }}</div>
      </el-card>
    </div>

    <!-- OCR 结构化结果 -->
    <div v-if="ocrResults.length" class="ocr-structured-section">
      <el-card shadow="hover">
        <template #header>
          <div class="card-header">
            <div class="card-header-left">
              <el-icon><Document /></el-icon>
              <span>OCR 结构化结果</span>
              <el-tag type="info" size="small">共 {{ ocrResults.length }} 张</el-tag>
            </div>
            <div class="card-header-right">
              <el-button type="primary" size="small" @click="handleBatchVerify" :loading="batchVerifyLoading">
                <el-icon><CircleCheck /></el-icon>
                批量验真
              </el-button>
              <el-button type="success" size="small" @click="handleBatchVoucher" :loading="batchVoucherLoading">
                <el-icon><Tickets /></el-icon>
                批量生成凭证
              </el-button>
            </div>
          </div>
        </template>

        <el-table :data="ocrResults" stripe border style="width: 100%" max-height="400">
          <el-table-column prop="invoiceNo" label="发票号码" min-width="160" show-overflow-tooltip />
          <el-table-column prop="invoiceDate" label="开票日期" min-width="120" show-overflow-tooltip />
          <el-table-column prop="amount" label="金额" min-width="120" show-overflow-tooltip />
          <el-table-column prop="sellerName" label="销方" min-width="150" show-overflow-tooltip />
          <el-table-column prop="buyerName" label="购方" min-width="150" show-overflow-tooltip />
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row, $index }">
              <el-button
                type="primary"
                link
                size="small"
                @click="handleVerifySingle(row, $index)"
                :loading="verifyLoadingMap[$index]"
              >
                验真
              </el-button>
              <el-button
                type="success"
                link
                size="small"
                @click="handleVoucherSingle(row, $index)"
                :loading="voucherLoadingMap[$index]"
              >
                生成凭证
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { sendChat } from '@/api/agent'
import { financeApi } from '@/api/mcp'
import { ElMessage } from 'element-plus'
import { Search, Camera, Cpu, Document, CircleCheck, Tickets } from '@element-plus/icons-vue'
import InvoiceUploader from '@/components/invoice/InvoiceUploader.vue'
import type { ChatResponse } from '@/api/agent'
import type { BatchOcrResult } from '@/components/invoice/InvoiceUploader.vue'

/** 是否加载中 */
const loading = ref(false)

/** 拍照识别加载中 */
const recognizeLoading = ref(false)

/** 批量验真加载中 */
const batchVerifyLoading = ref(false)

/** 批量生成凭证加载中 */
const batchVoucherLoading = ref(false)

/** 单行验真加载状态 */
const verifyLoadingMap = ref<Record<number, boolean>>({})

/** 单行生成凭证加载状态 */
const voucherLoadingMap = ref<Record<number, boolean>>({})

/** 表格数据 */
const tableData = ref<any[]>([])

/** 表格列名 */
const tableColumns = ref<string[]>([])

const columnLabels: Record<string, string> = {
  invoiceNo: '发票号码',
  invoiceCode: '发票代码',
  invoiceDate: '开票日期',
  invoiceType: '发票类型',
  direction: '发票方向',
  partnerId: '往来方编号',
  partnerName: '往来方名称',
  buyerName: '购方名称',
  sellerName: '销方名称',
  amount: '不含税金额',
  taxAmount: '税额',
  totalAmount: '价税合计',
  status: '状态',
  verifyStatus: '验真状态',
  inputStatus: '入账状态',
  source: '来源',
  fileHash: '文件指纹',
  duplicate: '是否重复',
  riskLevel: '风险等级',
}

/** 识别结果文本 */
const recognizeResult = ref('')

/** 统计数据 */
const statsData = ref([
  { label: '发票总数', value: '--', color: '#409eff' },
  { label: '发票总额', value: '--', color: '#67c23a' },
  { label: '待认证', value: '--', color: '#e6a23c' },
  { label: '已记账', value: '--', color: '#909399' },
])

/** 会话ID（用于保持上下文） */
const sessionId = ref('')

/** InvoiceUploader 组件引用 */
const uploaderRef = ref<InstanceType<typeof InvoiceUploader>>()

/** OCR 结构化结果（从 InvoiceUploader 获取） */
const ocrResults = computed(() => uploaderRef.value?.batchResults ?? [])

function formatCurrency(value: number) {
  return `¥${Number(value || 0).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`
}

function updateInvoiceStats(rows: any[], total?: number) {
  const invoiceTotal = total ?? rows.length
  const totalAmount = rows.reduce((sum, row) => sum + Number(row.totalAmount || 0), 0)
  const pendingVerifyCount = rows.filter(row => row.verifyStatus !== '已验真').length
  const recordedCount = rows.filter(row => row.inputStatus === 'RECORDED' || row.inputStatus === '已记账').length

  statsData.value[0].value = String(invoiceTotal)
  statsData.value[1].value = formatCurrency(totalAmount)
  statsData.value[2].value = String(pendingVerifyCount)
  statsData.value[3].value = String(recordedCount)
}

/**
 * 查询发票台账
 */
async function fetchInvoices() {
  loading.value = true
  tableData.value = []
  tableColumns.value = []

  try {
    const response = await financeApi.queryInvoices()
    const page = response.data
    const data = page?.rows || []
    tableData.value = data
    if (data.length) {
      tableColumns.value = Object.keys(data[0])
    }
    updateInvoiceStats(data, page?.total)
  } catch (error) {
    console.error('[InvoiceView] 查询失败:', error)
    ElMessage.error('查询发票失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

/**
 * 拍照识别发票并记账
 */
async function handlePhotoRecognize() {
  recognizeLoading.value = true
  recognizeResult.value = ''

  try {
    const response = await sendChat({
      sessionId: sessionId.value || undefined,
      message: '拍照识别发票并记账',
    })

    sessionId.value = response.sessionId
    recognizeResult.value = response.text || '识别完成'

    ElMessage.success('发票识别处理完成')
  } catch (error) {
    console.error('[InvoiceView] 识别失败:', error)
    ElMessage.error('发票识别失败，请稍后重试')
  } finally {
    recognizeLoading.value = false
  }
}

function handleUploadRecognized(response: ChatResponse) {
  sessionId.value = response.sessionId
  recognizeResult.value = `${response.text}\n\n${JSON.stringify(response.content, null, 2)}`
  ElMessage.success('OCR 识别和凭证生成已完成')
}

function getColumnLabel(column: string) {
  return columnLabels[column] || column
}

/**
 * 单张发票验真
 */
async function handleVerifySingle(row: BatchOcrResult, index: number) {
  verifyLoadingMap.value[index] = true
  try {
    const invoiceDesc = row.invoiceNo
      ? `发票号码 ${row.invoiceNo}`
      : `金额 ${row.amount} 的发票`

    const response = await sendChat({
      sessionId: sessionId.value || undefined,
      message: `请验真发票：${invoiceDesc}，开票日期：${row.invoiceDate}，金额：${row.amount}，销方：${row.sellerName}，购方：${row.buyerName}`,
    })

    sessionId.value = response.sessionId
    recognizeResult.value = response.text || '验真完成'
    ElMessage.success(`发票 ${row.invoiceNo || index + 1} 验真请求已发送`)
  } catch (error) {
    console.error('[InvoiceView] 验真失败:', error)
    ElMessage.error('发票验真失败，请稍后重试')
  } finally {
    verifyLoadingMap.value[index] = false
  }
}

/**
 * 单张发票生成凭证
 */
async function handleVoucherSingle(row: BatchOcrResult, index: number) {
  voucherLoadingMap.value[index] = true
  try {
    const invoiceDesc = row.invoiceNo
      ? `发票号码 ${row.invoiceNo}`
      : `金额 ${row.amount} 的发票`

    const response = await sendChat({
      sessionId: sessionId.value || undefined,
      message: `请为发票生成会计凭证：${invoiceDesc}，开票日期：${row.invoiceDate}，金额：${row.amount}，销方：${row.sellerName}，购方：${row.buyerName}`,
    })

    sessionId.value = response.sessionId
    recognizeResult.value = response.text || '凭证生成完成'
    ElMessage.success(`发票 ${row.invoiceNo || index + 1} 凭证生成请求已发送`)
  } catch (error) {
    console.error('[InvoiceView] 生成凭证失败:', error)
    ElMessage.error('生成凭证失败，请稍后重试')
  } finally {
    voucherLoadingMap.value[index] = false
  }
}

/**
 * 批量验真
 */
async function handleBatchVerify() {
  const results = ocrResults.value
  if (!results.length) {
    ElMessage.warning('暂无 OCR 结果，请先上传发票')
    return
  }

  batchVerifyLoading.value = true
  try {
    const invoiceList = results
      .map((r, i) => `${i + 1}. 发票号码：${r.invoiceNo}，开票日期：${r.invoiceDate}，金额：${r.amount}，销方：${r.sellerName}，购方：${r.buyerName}`)
      .join('\n')

    const response = await sendChat({
      sessionId: sessionId.value || undefined,
      message: `请批量验真以下发票：\n${invoiceList}`,
    })

    sessionId.value = response.sessionId
    recognizeResult.value = response.text || '批量验真完成'
    ElMessage.success(`已提交 ${results.length} 张发票的批量验真请求`)
  } catch (error) {
    console.error('[InvoiceView] 批量验真失败:', error)
    ElMessage.error('批量验真失败，请稍后重试')
  } finally {
    batchVerifyLoading.value = false
  }
}

/**
 * 批量生成凭证
 */
async function handleBatchVoucher() {
  const results = ocrResults.value
  if (!results.length) {
    ElMessage.warning('暂无 OCR 结果，请先上传发票')
    return
  }

  batchVoucherLoading.value = true
  try {
    const invoiceList = results
      .map((r, i) => `${i + 1}. 发票号码：${r.invoiceNo}，开票日期：${r.invoiceDate}，金额：${r.amount}，销方：${r.sellerName}，购方：${r.buyerName}`)
      .join('\n')

    const response = await sendChat({
      sessionId: sessionId.value || undefined,
      message: `请批量生成会计凭证，以下是需要处理的发票：\n${invoiceList}`,
    })

    sessionId.value = response.sessionId
    recognizeResult.value = response.text || '批量生成凭证完成'
    ElMessage.success(`已提交 ${results.length} 张发票的批量凭证生成请求`)
  } catch (error) {
    console.error('[InvoiceView] 批量生成凭证失败:', error)
    ElMessage.error('批量生成凭证失败，请稍后重试')
  } finally {
    batchVoucherLoading.value = false
  }
}

/** 组件挂载时自动查询 */
onMounted(() => {
  fetchInvoices()
})
</script>

<style scoped>
.invoice-container {
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

.header-actions {
  display: flex;
  gap: 12px;
}

/* 统计卡片 */
.invoice-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.stat-item {
  background: #fff;
  border-radius: 10px;
  padding: 16px 20px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.stat-item-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-item-value {
  font-size: 24px;
  font-weight: 700;
}

/* 表格卡片 */
.table-card {
  border-radius: 12px;
  margin-bottom: 24px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
  font-size: 15px;
}

.card-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 识别结果 */
.recognize-result {
  margin-bottom: 24px;
}

.recognize-text {
  font-size: 14px;
  line-height: 1.8;
  color: #606266;
  white-space: pre-wrap;
}

/* OCR 结构化结果 */
.ocr-structured-section {
  margin-bottom: 24px;
}

/* 响应式 */
@media (max-width: 768px) {
  .invoice-container {
    padding: 12px;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .invoice-stats {
    grid-template-columns: repeat(2, 1fr);
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>
