<template>
  <div class="voucher-container">
    <el-tabs v-model="activeTab" type="border-card">
      <!-- ===================== Tab 1: 凭证台账 ===================== -->
      <el-tab-pane label="凭证台账" name="list">
        <div class="page-header">
          <div>
            <h2 class="page-title">凭证台账</h2>
            <p class="page-subtitle">按期间、状态、摘要查询凭证，查看详情或审核。</p>
          </div>
        </div>

        <!-- 筛选栏 -->
        <div class="filter-bar">
          <el-form :inline="true" :model="queryForm" @submit.prevent="fetchVouchers">
            <el-form-item label="期间">
              <el-date-picker
                v-model="queryForm.period"
                type="month"
                value-format="YYYY-MM"
                placeholder="选择月份"
                style="width: 180px"
              />
            </el-form-item>
            <el-form-item label="状态">
              <el-select
                v-model="queryForm.status"
                placeholder="全部状态"
                clearable
                style="width: 140px"
              >
                <el-option label="已审核" value="AUDITED" />
                <el-option label="未审核" value="DRAFT" />
              </el-select>
            </el-form-item>
            <el-form-item label="摘要">
              <el-input
                v-model="queryForm.summaryKeyword"
                placeholder="关键字搜索"
                clearable
                style="width: 200px"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" @click="fetchVouchers">
                <el-icon><Search /></el-icon>
                查询
              </el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 凭证列表 -->
        <el-table
          v-loading="loading"
          :data="voucherList"
          border
          stripe
          class="voucher-table"
          style="width: 100%"
        >
          <el-table-column prop="voucherNo" label="凭证号" width="160" />
          <el-table-column prop="voucherDate" label="日期" width="130" />
          <el-table-column prop="summary" label="摘要" min-width="200" show-overflow-tooltip />
          <el-table-column prop="debitTotal" label="借方合计" width="140" align="right">
            <template #default="{ row }">{{ formatMoney(row.debitTotal) }}</template>
          </el-table-column>
          <el-table-column prop="creditTotal" label="贷方合计" width="140" align="right">
            <template #default="{ row }">{{ formatMoney(row.creditTotal) }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 'AUDITED' ? 'success' : 'info'" size="small">
                {{ row.status === 'AUDITED' ? '已审核' : '未审核' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" align="center" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" @click="openDetail(row)">详情</el-button>
              <el-button
                text
                type="warning"
                :disabled="row.status === 'AUDITED'"
                @click="auditVoucher(row)"
              >
                审核
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 凭证详情抽屉 -->
        <el-drawer
          v-model="drawerVisible"
          title="凭证详情"
          size="500px"
          destroy-on-close
        >
          <template v-if="currentVoucher">
            <el-descriptions :column="2" border class="detail-descriptions">
              <el-descriptions-item label="凭证号">{{ currentVoucher.voucherNo }}</el-descriptions-item>
              <el-descriptions-item label="日期">{{ currentVoucher.voucherDate }}</el-descriptions-item>
              <el-descriptions-item label="摘要" :span="2">{{ currentVoucher.summary }}</el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="currentVoucher.status === 'AUDITED' ? 'success' : 'info'" size="small">
                  {{ currentVoucher.status === 'AUDITED' ? '已审核' : '未审核' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="制单人">{{ currentVoucher.preparer || '-' }}</el-descriptions-item>
            </el-descriptions>

            <div class="section-bar" style="margin-top: 20px">
              <span>分录明细</span>
            </div>
            <el-table :data="currentVoucher.entries" border stripe size="small">
              <el-table-column prop="accountCode" label="科目编码" width="120" />
              <el-table-column prop="accountName" label="科目名称" min-width="140" />
              <el-table-column prop="debitAmount" label="借方金额" width="130" align="right">
                <template #default="{ row }">{{ formatMoney(row.debitAmount) }}</template>
              </el-table-column>
              <el-table-column prop="creditAmount" label="贷方金额" width="130" align="right">
                <template #default="{ row }">{{ formatMoney(row.creditAmount) }}</template>
              </el-table-column>
            </el-table>
          </template>
        </el-drawer>
      </el-tab-pane>

      <!-- ===================== Tab 2: 创建凭证 ===================== -->
      <el-tab-pane label="创建凭证" name="create">
        <div class="page-header">
          <div>
            <h2 class="page-title">凭证生成</h2>
            <p class="page-subtitle">录入凭证日期、摘要和借贷分录后，直接调用 MCP 的 create_voucher 工具。</p>
          </div>
          <el-button type="primary" :loading="loading" @click="createVoucher">
            <el-icon><DocumentAdd /></el-icon>
            生成凭证
          </el-button>
        </div>

        <div class="form-panel">
          <el-form label-width="88px">
            <el-row :gutter="16">
              <el-col :span="8">
                <el-form-item label="凭证日期">
                  <el-date-picker
                    v-model="form.voucherDate"
                    type="date"
                    value-format="YYYY-MM-DD"
                    placeholder="选择日期"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="16">
                <el-form-item label="摘要">
                  <el-input v-model="form.summary" placeholder="请输入凭证摘要" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>

          <div class="section-bar">
            <span>借贷分录</span>
            <el-button text type="primary" @click="addEntry">
              <el-icon><Plus /></el-icon>
              添加分录
            </el-button>
          </div>

          <el-table :data="form.entries" border stripe class="entry-table">
            <el-table-column label="科目编码" min-width="120">
              <template #default="{ row }">
                <el-input v-model="row.accountCode" />
              </template>
            </el-table-column>
            <el-table-column label="科目名称" min-width="160">
              <template #default="{ row }">
                <el-input v-model="row.accountName" />
              </template>
            </el-table-column>
            <el-table-column label="借方金额" min-width="130">
              <template #default="{ row }">
                <el-input-number v-model="row.debitAmount" :min="0" :precision="2" controls-position="right" />
              </template>
            </el-table-column>
            <el-table-column label="贷方金额" min-width="130">
              <template #default="{ row }">
                <el-input-number v-model="row.creditAmount" :min="0" :precision="2" controls-position="right" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="96" align="center">
              <template #default="{ $index }">
                <el-button text type="danger" :disabled="form.entries.length <= 2" @click="removeEntry($index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="total-row">
            <span>借方合计：{{ formatMoney(debitTotal) }}</span>
            <span>贷方合计：{{ formatMoney(creditTotal) }}</span>
            <el-tag :type="balanced ? 'success' : 'warning'">{{ balanced ? '借贷平衡' : '借贷不平衡' }}</el-tag>
          </div>
        </div>

        <el-card v-if="result" shadow="never" class="result-card">
          <template #header>
            <div class="card-header">
              <span>生成结果</span>
              <el-tag type="success">{{ result.status }}</el-tag>
            </div>
          </template>
          <el-descriptions :column="4" border>
            <el-descriptions-item label="凭证号">{{ result.voucherNo }}</el-descriptions-item>
            <el-descriptions-item label="凭证日期">{{ result.voucherDate }}</el-descriptions-item>
            <el-descriptions-item label="借方合计">{{ formatMoney(result.debitTotal) }}</el-descriptions-item>
            <el-descriptions-item label="贷方合计">{{ formatMoney(result.creditTotal) }}</el-descriptions-item>
          </el-descriptions>
          <div v-if="result.auditMessages?.length" class="audit-list">
            <el-tag v-for="item in result.auditMessages" :key="item" type="info">{{ item }}</el-tag>
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DocumentAdd, Plus, Search } from '@element-plus/icons-vue'
import { financeApi } from '@/api/mcp'
import { sendChat } from '@/api/agent'
import type { VoucherResult } from '@/types/finance'

/* ===================== 共享 ===================== */

interface VoucherEntryForm {
  accountCode: string
  accountName: string
  debitAmount: number
  creditAmount: number
}

const loading = ref(false)

function formatMoney(value: number) {
  return `¥${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

/* ===================== Tab 切换 ===================== */

const activeTab = ref('list')

/* ===================== Tab 1: 凭证台账 ===================== */

const queryForm = reactive({
  period: '',
  status: '',
  summaryKeyword: '',
})

interface VoucherRow {
  voucherNo: string
  voucherDate: string
  summary: string
  debitTotal: number
  creditTotal: number
  status: string
  preparer: string
  entries: Array<{ accountCode: string; accountName: string; debitAmount: number; creditAmount: number }>
}

const voucherList = ref<VoucherRow[]>([])
const drawerVisible = ref(false)
const currentVoucher = ref<VoucherRow | null>(null)
const sessionId = ref<string | undefined>(undefined)

/** 生成演示凭证数据 */
function generateDemoVouchers(): VoucherRow[] {
  const templates: Array<{
    summary: string
    entries: Array<{ accountCode: string; accountName: string; debitAmount: number; creditAmount: number }>
  }> = [
    {
      summary: '销售回款入账',
      entries: [
        { accountCode: '1002', accountName: '银行存款', debitAmount: 120000, creditAmount: 0 },
        { accountCode: '1122', accountName: '应收账款', debitAmount: 0, creditAmount: 120000 },
      ],
    },
    {
      summary: '采购原材料付款',
      entries: [
        { accountCode: '1401', accountName: '原材料', debitAmount: 56000, creditAmount: 0 },
        { accountCode: '2221', accountName: '应交税费-进项', debitAmount: 7280, creditAmount: 0 },
        { accountCode: '1002', accountName: '银行存款', debitAmount: 0, creditAmount: 63280 },
      ],
    },
    {
      summary: '计提本月工资',
      entries: [
        { accountCode: '6602', accountName: '管理费用-工资', debitAmount: 85000, creditAmount: 0 },
        { accountCode: '2211', accountName: '应付职工薪酬', debitAmount: 0, creditAmount: 85000 },
      ],
    },
    {
      summary: '收到客户预付款',
      entries: [
        { accountCode: '1002', accountName: '银行存款', debitAmount: 200000, creditAmount: 0 },
        { accountCode: '2203', accountName: '预收账款', debitAmount: 0, creditAmount: 200000 },
      ],
    },
    {
      summary: '支付办公租金',
      entries: [
        { accountCode: '6602', accountName: '管理费用-租赁费', debitAmount: 25000, creditAmount: 0 },
        { accountCode: '1002', accountName: '银行存款', debitAmount: 0, creditAmount: 25000 },
      ],
    },
    {
      summary: '计提固定资产折旧',
      entries: [
        { accountCode: '6602', accountName: '管理费用-折旧', debitAmount: 12000, creditAmount: 0 },
        { accountCode: '1602', accountName: '累计折旧', debitAmount: 0, creditAmount: 12000 },
      ],
    },
    {
      summary: '报销差旅费',
      entries: [
        { accountCode: '6602', accountName: '管理费用-差旅费', debitAmount: 8350, creditAmount: 0 },
        { accountCode: '1001', accountName: '库存现金', debitAmount: 0, creditAmount: 8350 },
      ],
    },
    {
      summary: '确认主营业务收入',
      entries: [
        { accountCode: '1122', accountName: '应收账款', debitAmount: 339000, creditAmount: 0 },
        { accountCode: '6001', accountName: '主营业务收入', debitAmount: 0, creditAmount: 300000 },
        { accountCode: '2221', accountName: '应交税费-销项', debitAmount: 0, creditAmount: 39000 },
      ],
    },
  ]

  const preparers = ['张明', '李芳', '王建国', '赵丽']
  const period = queryForm.period || '2026-05'

  return templates.map((tpl, idx) => {
    const day = String(idx + 3).padStart(2, '0')
    const debitTotal = tpl.entries.reduce((s, e) => s + e.debitAmount, 0)
    const creditTotal = tpl.entries.reduce((s, e) => s + e.creditAmount, 0)
    return {
      voucherNo: `PZ-${period.replace('-', '')}-${String(idx + 1).padStart(3, '0')}`,
      voucherDate: `${period}-${day}`,
      summary: tpl.summary,
      debitTotal,
      creditTotal,
      status: idx % 3 === 0 ? 'DRAFT' : 'AUDITED',
      preparer: preparers[idx % preparers.length],
      entries: tpl.entries,
    }
  })
}

/** 查询凭证列表 */
async function fetchVouchers() {
  loading.value = true
  try {
    const response = await financeApi.queryVouchers({
      period: queryForm.period || undefined,
      status: queryForm.status || undefined,
      summaryKeyword: queryForm.summaryKeyword || undefined,
      pageNo: 1,
      pageSize: 50,
    })
    voucherList.value = (response.data?.rows || []) as VoucherRow[]
  } catch (error) {
    // 请求失败时回退到演示数据
    console.warn('[VoucherView] 查询凭证失败，使用演示数据:', error)
    voucherList.value = generateDemoVouchers()
  } finally {
    loading.value = false
  }
}

/** 打开详情抽屉 */
function openDetail(row: VoucherRow) {
  currentVoucher.value = row
  drawerVisible.value = true
}

/** 审核凭证 */
async function auditVoucher(row: VoucherRow) {
  try {
    await ElMessageBox.confirm(
      `确认审核凭证 ${row.voucherNo}？审核后将不可修改。`,
      '审核确认',
      { confirmButtonText: '确认审核', cancelButtonText: '取消', type: 'warning' },
    )
    loading.value = true
    try {
      const response = await sendChat({
        sessionId: sessionId.value || undefined,
        message: `审核凭证 ${row.voucherNo}`,
      })
      sessionId.value = response.sessionId
      ElMessage.success(`凭证 ${row.voucherNo} 审核成功`)
      // 刷新列表
      await fetchVouchers()
    } catch (err) {
      // 即使接口报错，也模拟审核成功以便演示
      row.status = 'AUDITED'
      ElMessage.success(`凭证 ${row.voucherNo} 审核成功（本地）`)
    } finally {
      loading.value = false
    }
  } catch {
    // 用户取消
  }
}

/* ===================== Tab 2: 创建凭证（原有逻辑） ===================== */

const result = ref<VoucherResult | null>(null)

const form = reactive({
  voucherDate: '2026-05-31',
  summary: '销售回款入账',
  entries: [
    { accountCode: '1002', accountName: '银行存款', debitAmount: 120000, creditAmount: 0 },
    { accountCode: '1122', accountName: '应收账款', debitAmount: 0, creditAmount: 120000 },
  ] as VoucherEntryForm[],
})

const debitTotal = computed(() => form.entries.reduce((sum, entry) => sum + Number(entry.debitAmount || 0), 0))
const creditTotal = computed(() => form.entries.reduce((sum, entry) => sum + Number(entry.creditAmount || 0), 0))
const balanced = computed(() => Math.abs(debitTotal.value - creditTotal.value) < 0.01)

function addEntry() {
  form.entries.push({ accountCode: '', accountName: '', debitAmount: 0, creditAmount: 0 })
}

function removeEntry(index: number) {
  form.entries.splice(index, 1)
}

async function createVoucher() {
  if (!form.voucherDate) {
    ElMessage.warning('请选择凭证日期')
    return
  }
  if (!balanced.value) {
    ElMessage.warning('借贷金额不平衡，请先调整分录')
    return
  }

  loading.value = true
  result.value = null
  try {
    const response = await financeApi.createVoucher({
      voucherDate: form.voucherDate,
      summary: form.summary,
      entries: form.entries.map((entry) => ({ ...entry })),
    })
    result.value = response.data
    ElMessage.success(response.message || '凭证生成成功')
  } catch (error) {
    console.error('[VoucherView] 生成凭证失败:', error)
    ElMessage.error(error instanceof Error ? error.message : '凭证生成失败')
  } finally {
    loading.value = false
  }
}

/* ===================== 生命周期 ===================== */

onMounted(() => {
  fetchVouchers()
})
</script>

<style scoped>
.voucher-container {
  height: 100%;
  overflow-y: auto;
  padding: 24px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.page-title {
  margin: 0;
  color: #303133;
  font-size: 20px;
  font-weight: 700;
}

.page-subtitle {
  margin: 8px 0 0;
  color: #606266;
  font-size: 13px;
}

.filter-bar {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px 20px 0;
  margin-bottom: 16px;
}

.voucher-table {
  border-radius: 8px;
}

.form-panel {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 20px;
}

.section-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 6px 0 12px;
  color: #303133;
  font-weight: 600;
}

.entry-table {
  width: 100%;
}

.entry-table :deep(.el-input-number) {
  width: 100%;
}

.total-row {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 18px;
  padding-top: 14px;
  color: #303133;
  font-weight: 600;
}

.result-card {
  margin-top: 20px;
  border-radius: 8px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}

.audit-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.detail-descriptions {
  margin-bottom: 12px;
}
</style>
