<template>
  <div class="expense-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">智能报销助手</h2>
        <p class="page-subtitle">基于知识库制度引用、发票状态和预算条件生成审批意见。</p>
      </div>
      <div class="header-actions">
        <el-button @click="$router.push('/knowledge')">
          <el-icon><Collection /></el-icon>
          规则知识库
        </el-button>
        <el-button type="success" @click="openCreateDialog">
          <el-icon><Plus /></el-icon>
          新增报销单
        </el-button>
        <el-button type="primary" :loading="loading" @click="loadExpenses">
          <el-icon><Refresh /></el-icon>
          刷新待审
        </el-button>
      </div>
    </div>

    <section class="workspace-grid">
      <div class="panel expense-list">
        <div class="panel-title-row">
          <div class="title-with-count">
            <span>待审报销单</span>
            <el-tag type="warning" size="small">待审批 {{ statusCounts.pending }} 条</el-tag>
            <el-tag size="small">当前 {{ expenses.length }} 条</el-tag>
          </div>
          <el-segmented v-model="statusFilter" :options="statusOptions" @change="loadExpenses" />
        </div>
        <el-table
          :data="expenses"
          border
          stripe
          height="420"
          highlight-current-row
          v-loading="loading"
          empty-text="暂无报销单"
          @current-change="selectExpense"
        >
          <el-table-column prop="expenseNo" label="单号" min-width="140" />
          <el-table-column prop="employeeName" label="报销人" width="92" />
          <el-table-column prop="department" label="部门" width="92" />
          <el-table-column prop="expenseType" label="类型" width="92" />
          <el-table-column label="金额" width="112" align="right">
            <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="118">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.status)" size="small">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="riskHint" label="提示" min-width="180" show-overflow-tooltip />
        </el-table>
      </div>

      <div class="panel approval-panel">
        <div class="panel-title-row">
          <span>审批条件</span>
          <el-switch
            v-model="form.autoApproveEnabled"
            active-text="低风险自动通过"
            inactive-text="只生成意见"
          />
        </div>

        <el-empty v-if="!selected" description="请选择一张报销单" />
        <el-form v-else label-width="96px" class="approval-form">
          <el-row :gutter="12">
            <el-col :span="12">
              <el-form-item label="报销单号">
                <el-input v-model="form.expenseNo" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="报销金额">
                <el-input-number v-model="form.amount" :min="0" :precision="2" controls-position="right" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="人员级别">
                <el-select v-model="form.employeeLevel">
                  <el-option label="普通员工" value="staff" />
                  <el-option label="经理" value="manager" />
                  <el-option label="高管" value="executive" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="城市等级">
                <el-select v-model="form.cityTier">
                  <el-option label="一线" value="一线" />
                  <el-option label="二线" value="二线" />
                  <el-option label="其他城市" value="其他城市" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="发票号">
                <el-input v-model="form.invoiceNo" placeholder="缺票时留空" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="发票金额">
                <el-input-number v-model="form.invoiceAmount" :min="0" :precision="2" controls-position="right" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="可用预算">
                <el-input-number v-model="form.availableBudget" :precision="2" controls-position="right" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="票据状态">
                <el-checkbox v-model="form.invoiceVerified">验真通过</el-checkbox>
                <el-checkbox v-model="form.duplicateInvoice">重复票</el-checkbox>
              </el-form-item>
            </el-col>
          </el-row>

          <el-button type="primary" class="approve-button" :loading="approving" @click="approveExpense">
            <el-icon><Checked /></el-icon>
            {{ approving ? '正在检索制度规则并生成审批意见…' : '生成审批意见' }}
          </el-button>
        </el-form>
      </div>
    </section>

    <section v-if="approval" class="panel result-panel">
      <div class="result-summary">
        <div>
          <div class="panel-title">审批结果</div>
          <p>{{ approval.approvalOpinion || responseText }}</p>
        </div>
        <div class="status-tags">
          <el-tag :type="statusTag(approval.approvalStatus)" effect="dark">{{ approval.approvalStatus }}</el-tag>
          <el-tag :type="approval.autoApproved ? 'success' : 'info'">
            {{ approval.autoApproved ? '已自动通过' : '未自动通过' }}
          </el-tag>
          <el-tag>{{ approval.riskLevel }}</el-tag>
        </div>
      </div>

      <div class="result-grid">
        <div>
          <div class="sub-title">风险项</div>
          <el-table :data="approval.riskItems || []" border stripe empty-text="无风险项">
            <el-table-column prop="severity" label="等级" width="88" />
            <el-table-column prop="title" label="风险" min-width="140" />
            <el-table-column prop="suggestion" label="建议" min-width="220" show-overflow-tooltip />
          </el-table>
        </div>

        <div>
          <div class="sub-title">规则引用</div>
          <div v-if="approval.ruleCitations?.length" class="citation-list">
            <div v-for="item in approval.ruleCitations" :key="`${item.documentName}-${item.chunkNo}`" class="citation-item">
              <div class="citation-head">
                <span>{{ item.documentName }}</span>
                <el-tag size="small">片段 {{ item.chunkNo || '-' }}</el-tag>
              </div>
              <p>{{ item.text }}</p>
            </div>
          </div>
          <el-alert v-else title="未找到可引用的报销制度规则，本次不会自动通过。" type="warning" show-icon :closable="false" />
        </div>
      </div>
    </section>

    <el-dialog
      v-model="showCreateDialog"
      title="新增报销单"
      width="640px"
      :close-on-click-modal="false"
      :close-on-press-escape="!creating && !advising"
      :show-close="!creating && !advising"
      :before-close="handleDialogClose"
      destroy-on-close
    >
      <el-form :model="createForm" label-width="96px" class="create-form">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="员工号" required>
              <el-input v-model="createForm.employeeId" placeholder="如 ZCY001" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名">
              <el-input v-model="createForm.employeeName" placeholder="报销人姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门">
              <el-input v-model="createForm.department" placeholder="如 销售部" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="费用类型">
              <el-select v-model="createForm.expenseType" placeholder="选择费用类型">
                <el-option label="差旅" value="差旅" />
                <el-option label="业务招待" value="业务招待" />
                <el-option label="办公费" value="办公费" />
                <el-option label="培训费" value="培训费" />
                <el-option label="交通费" value="交通费" />
                <el-option label="其他" value="其他" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="报销金额" required>
              <el-input-number v-model="createForm.amount" :min="0" :precision="2" controls-position="right" class="full-width" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="费用日期">
              <el-date-picker v-model="createForm.expenseDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" class="full-width" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目编码">
              <el-input v-model="createForm.projectCode" placeholder="如 PRJ-2026-SALES" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发票号码">
              <el-input v-model="createForm.invoiceNosText" placeholder="多个用逗号分隔" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="报销说明">
              <el-input v-model="createForm.description" type="textarea" :rows="2" placeholder="费用事由说明" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="提交审批">
              <el-switch v-model="createForm.submitForApproval" active-text="创建后进入待审批" inactive-text="仅保存草稿" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <div v-if="createResult" class="create-result">
        <el-alert
          :title="`报销单 ${createResult.expenseNo} 已创建（状态：${createResult.status}）`"
          :type="createResult.status === 'PENDING' ? 'success' : 'info'"
          show-icon
          :closable="false"
        >
          <div>{{ createResult.riskHint }}</div>
        </el-alert>

        <div class="advice-section">
          <div class="sub-title">📋 制度规则建议</div>
          <template v-if="advising || ruleAdvice">
            <p class="advice-text">{{ adviceDisplayText }}<span v-if="advising" class="cursor-blink">▌</span></p>
            <div v-if="!advising && ruleAdvice?.citations?.length" class="citation-list">
              <div v-for="(item, idx) in ruleAdvice.citations" :key="idx" class="citation-item">
                <div class="citation-head">
                  <span>{{ item.documentName }}</span>
                  <el-tag size="small">片段 {{ item.chunkNo || '-' }}</el-tag>
                </div>
                <p>{{ item.text }}</p>
              </div>
            </div>
          </template>
          <el-alert
            v-else-if="adviceFailed"
            title="未在知识库中找到与该费用类型直接相关的制度规则。建议先到知识库上传报销制度文档。"
            type="warning"
            show-icon
            :closable="false"
          />
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button v-if="!creating && !advising" @click="closeCreateDialog">关闭</el-button>
          <el-button
            v-if="!createResult"
            type="primary"
            :loading="creating"
            @click="submitCreate"
          >
            <el-icon><MagicStick /></el-icon>
            创建并获取制度建议
          </el-button>
          <el-button v-else type="primary" disabled :loading="advising">
            <el-icon><MagicStick /></el-icon>
            {{ advising ? '生成规则建议中...' : '已创建' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Checked, Collection, MagicStick, Plus, Refresh } from '@element-plus/icons-vue'
import { financeApi } from '@/api/mcp'
import { askKnowledge, type KnowledgeChatResponse } from '@/api/knowledge'
import type { ExpenseApprovalResult, ExpenseRow } from '@/types/finance'

const statusOptions = computed(() => [
  { label: `全部 ${statusCounts.value.all}`, value: '' },
  { label: `待审批 ${statusCounts.value.pending}`, value: 'PENDING' },
  { label: `待复核 ${statusCounts.value.needReview}`, value: 'NEED_REVIEW' },
  { label: `已通过 ${statusCounts.value.approved}`, value: 'APPROVED' },
])

const loading = ref(false)
const approving = ref(false)
const statusFilter = ref('')
const statusCounts = ref({
  all: 0,
  pending: 0,
  needReview: 0,
  approved: 0,
})
const expenses = ref<ExpenseRow[]>([])
const selected = ref<ExpenseRow | null>(null)
const approval = ref<ExpenseApprovalResult | null>(null)
const responseText = ref('')

const showCreateDialog = ref(false)
const creating = ref(false)
const advising = ref(false)
const adviceFailed = ref(false)
const adviceDisplayText = ref('')
const createResult = ref<ExpenseRow | null>(null)
const ruleAdvice = ref<KnowledgeChatResponse | null>(null)
let streamTimer: ReturnType<typeof setInterval> | null = null
const createForm = reactive({
  employeeId: 'ZCY001',
  employeeName: '',
  department: '销售部',
  projectCode: 'PRJ-2026-SALES',
  expenseType: '差旅',
  expenseDate: new Date().toISOString().slice(0, 10),
  description: '',
  amount: 0,
  invoiceNosText: '',
  submitForApproval: true,
})

const form = reactive({
  expenseNo: '',
  employeeId: '',
  employeeName: '',
  department: '',
  projectCode: '',
  expenseType: '',
  description: '',
  amount: 0,
  invoiceAmount: 0,
  availableBudget: 0,
  employeeLevel: 'staff',
  cityTier: '二线',
  invoiceNo: 'INV-202605-003',
  invoiceVerified: true,
  duplicateInvoice: false,
  autoApproveEnabled: true,
})

onMounted(() => {
  void initializePage()
})

async function initializePage() {
  await Promise.all([loadExpenseCounts(), loadExpenses()])
}

async function loadExpenseCounts() {
  try {
    const [all, pending, needReview, approved] = await Promise.all([
      financeApi.queryExpense(''),
      financeApi.queryExpense('PENDING'),
      financeApi.queryExpense('NEED_REVIEW'),
      financeApi.queryExpense('APPROVED'),
    ])
    statusCounts.value = {
      all: all.data.total || all.data.rows?.length || 0,
      pending: pending.data.total || pending.data.rows?.length || 0,
      needReview: needReview.data.total || needReview.data.rows?.length || 0,
      approved: approved.data.total || approved.data.rows?.length || 0,
    }
  } catch (error) {
    ElMessage.error(errorMessage(error))
  }
}

async function loadExpenses() {
  loading.value = true
  try {
    await loadExpenseCounts()
    const response = await financeApi.queryExpense(statusFilter.value)
    expenses.value = response.data.rows || []
    if (!selected.value && expenses.value.length) {
      selectExpense(expenses.value[0])
    }
  } catch (error) {
    ElMessage.error(errorMessage(error))
  } finally {
    loading.value = false
  }
}

function selectExpense(row?: ExpenseRow) {
  if (!row) return
  selected.value = row
  approval.value = null
  responseText.value = ''
  form.expenseNo = row.expenseNo
  form.employeeId = row.employeeId
  form.employeeName = row.employeeName
  form.department = row.department || '销售部'
  form.projectCode = row.projectCode || 'PRJ-2026-SALES'
  form.expenseType = row.expenseType || '差旅'
  form.description = row.description || row.riskHint || '员工费用报销'
  form.amount = Number(row.amount || 0)
  form.invoiceAmount = Number(row.amount || 0)
  form.availableBudget = Number(row.amount || 0) + 12000
  form.invoiceNo = row.invoiceCount === 0 ? '' : 'INV-202605-003'
  form.invoiceVerified = true
  form.duplicateInvoice = false
}

async function approveExpense() {
  if (!selected.value) return
  approving.value = true
  approval.value = null
  try {
    const response = await financeApi.approveExpense({
      expenseNo: form.expenseNo,
      employeeId: form.employeeId,
      employeeName: form.employeeName,
      employeeLevel: form.employeeLevel,
      department: form.department,
      projectCode: form.projectCode,
      expenseType: form.expenseType,
      cityTier: form.cityTier,
      submitDate: new Date().toISOString().slice(0, 10),
      invoiceNo: form.invoiceNo,
      description: form.description,
      amount: form.amount,
      invoiceAmount: form.invoiceAmount,
      availableBudget: form.availableBudget,
      invoiceVerified: form.invoiceVerified,
      duplicateInvoice: form.duplicateInvoice,
      autoApproveEnabled: form.autoApproveEnabled,
    })
    responseText.value = response.text
    approval.value = response.content as ExpenseApprovalResult
    ElMessage.success(approval.value?.autoApproved ? '低风险单据已自动通过' : '审批意见已生成')
    await loadExpenses()
  } catch (error) {
    ElMessage.error(errorMessage(error))
  } finally {
    approving.value = false
  }
}

function statusTag(status?: string) {
  if (status === 'APPROVED' || status === 'APPROVED_ROUTE_READY') return 'success'
  if (status === 'REJECTED') return 'danger'
  if (status === 'NEED_REVIEW') return 'warning'
  return 'info'
}

function formatMoney(value: number) {
  return `¥${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function openCreateDialog() {
  createResult.value = null
  ruleAdvice.value = null
  adviceDisplayText.value = ''
  adviceFailed.value = false
  createForm.amount = 0
  createForm.description = ''
  createForm.invoiceNosText = ''
  createForm.submitForApproval = true
  showCreateDialog.value = true
}

function stopAdviceStream() {
  if (streamTimer) {
    clearInterval(streamTimer)
    streamTimer = null
  }
}

function closeCreateDialog() {
  if (creating.value || advising.value) return
  stopAdviceStream()
  showCreateDialog.value = false
}

function handleDialogClose(done: () => void) {
  if (creating.value || advising.value) return
  stopAdviceStream()
  done()
}

async function submitCreate() {
  if (!createForm.employeeId.trim()) {
    ElMessage.warning('请填写员工号')
    return
  }
  if (!(createForm.amount > 0)) {
    ElMessage.warning('报销金额必须大于 0')
    return
  }
  creating.value = true
  createResult.value = null
  ruleAdvice.value = null
  try {
    const response = await financeApi.createExpense({
      employeeId: createForm.employeeId.trim(),
      employeeName: createForm.employeeName.trim(),
      department: createForm.department.trim(),
      projectCode: createForm.projectCode.trim(),
      expenseType: createForm.expenseType,
      expenseDate: createForm.expenseDate,
      description: createForm.description.trim(),
      amount: createForm.amount,
      invoiceNos: createForm.invoiceNosText
        .split(/[,，\s]+/)
        .map((s) => s.trim())
        .filter(Boolean),
      submitForApproval: createForm.submitForApproval,
    })
    const row = response.data
    if (response.code !== 200 || !row) {
      throw new Error(response.message || '创建报销单失败')
    }
    createResult.value = row
    ElMessage.success(`报销单 ${row.expenseNo} 已创建`)
    advising.value = true
    adviceDisplayText.value = ''
    adviceFailed.value = false
    try {
      const question = buildRuleQuery(createForm.expenseType, createForm.description, createForm.amount)
      const result = await askKnowledge(question)
      await streamAdviceText(result.answer || '')
      ruleAdvice.value = result
      if (!result.answer) {
        adviceFailed.value = true
      }
    } catch {
      ruleAdvice.value = null
      adviceFailed.value = true
    } finally {
      advising.value = false
    }
    await loadExpenses()
  } catch (error) {
    ElMessage.error(errorMessage(error))
  } finally {
    creating.value = false
  }
}

function streamAdviceText(fullText: string): Promise<void> {
  return new Promise((resolve) => {
    if (!fullText) {
      resolve()
      return
    }
    const chunks: string[] = []
    const lines = fullText.split('\n')
    for (const line of lines) {
      const segments = line.match(/[\u4e00-\u9fa5]|[a-zA-Z0-9]+|[^\u4e00-\u9fa5a-zA-Z0-9]/g) || []
      for (const seg of segments) chunks.push(seg)
      chunks.push('\n')
    }
    let index = 0
    const BATCH = Math.max(1, Math.ceil(chunks.length / 120))
    if (streamTimer) clearInterval(streamTimer)
    streamTimer = setInterval(() => {
      const end = Math.min(index + BATCH, chunks.length)
      for (let i = index; i < end; i += 1) {
        adviceDisplayText.value += chunks[i]
      }
      index = end
      if (index >= chunks.length) {
        if (streamTimer) {
          clearInterval(streamTimer)
          streamTimer = null
        }
        resolve()
      }
    }, 16)
  })
}

function buildRuleQuery(expenseType: string, description: string, _amount: number) {
  const typeLabel = expenseType || '费用报销'
  const desc = description ? `：${description.trim().slice(0, 20)}` : ''
  return `根据知识库制度文档，${typeLabel}${desc}的报销标准、限额和单据要求是什么？`
}

function errorMessage(error: unknown) {
  const response = (error as { response?: { data?: { error?: string } } }).response
  return response?.data?.error || (error instanceof Error ? error.message : String(error))
}
</script>

<style scoped>
.expense-container {
  height: 100%;
  overflow-y: auto;
  padding: 24px;
}

.page-header,
.panel-title-row,
.result-summary,
.header-actions,
.status-tags {
  display: flex;
  align-items: center;
}

.page-header,
.panel-title-row,
.result-summary {
  justify-content: space-between;
  gap: 16px;
}

.header-actions,
.status-tags,
.title-with-count {
  gap: 10px;
}

.title-with-count {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

.workspace-grid {
  display: grid;
  grid-template-columns: minmax(480px, 1.1fr) minmax(360px, 0.9fr);
  gap: 16px;
}

.panel {
  padding: 18px;
}

.panel-title,
.panel-title-row,
.sub-title {
  color: #303133;
  font-weight: 700;
}

.panel-title-row {
  margin-bottom: 14px;
}

.approval-form :deep(.el-input-number),
.approval-form :deep(.el-select) {
  width: 100%;
}

.approve-button {
  width: 100%;
}

.result-panel {
  margin-top: 16px;
}

.result-summary p {
  margin: 8px 0 0;
  color: #606266;
}

.result-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 0.8fr);
  gap: 16px;
  margin-top: 16px;
}

.sub-title {
  margin-bottom: 10px;
}

.citation-list {
  display: grid;
  gap: 10px;
}

.citation-item {
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 12px;
  background: #fbfcfe;
}

.citation-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: #303133;
  font-weight: 700;
}

.citation-item p {
  margin: 8px 0 0;
  color: #606266;
  line-height: 1.6;
}

@media (max-width: 1080px) {
  .workspace-grid,
  .result-grid {
    grid-template-columns: 1fr;
  }
}

.create-form :deep(.el-select),
.create-form :deep(.el-input-number),
.create-form :deep(.el-date-editor),
.full-width {
  width: 100%;
}

.create-result {
  margin-top: 8px;
}

.advice-section {
  margin-top: 14px;
  min-height: 60px;
  padding: 4px 0;
}

.advice-text {
  margin: 8px 0;
  color: #303133;
  line-height: 1.7;
  white-space: pre-wrap;
  min-height: 24px;
}

.cursor-blink {
  display: inline-block;
  color: #409eff;
  animation: blink 1s step-end infinite;
  margin-left: 2px;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
