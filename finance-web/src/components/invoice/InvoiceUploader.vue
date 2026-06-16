<template>
  <div class="invoice-uploader panel">
    <el-upload
      drag
      multiple
      action="#"
      accept="image/*,.pdf"
      :auto-upload="false"
      :show-file-list="false"
      :disabled="busy"
      @change="recognizeInvoice"
    >
      <el-icon class="upload-icon"><UploadFilled /></el-icon>
      <div class="upload-text">拖拽、拍照或选择发票文件</div>
      <div class="upload-hint">支持批量上传，上传后自动执行 OCR 识别并生成会计凭证</div>
    </el-upload>

    <div v-if="status !== 'idle'" class="recognition-box">
      <div class="scan-stage" :class="status">
        <div class="scan-line" />
      </div>
      <el-steps :active="activeStep" finish-status="success" simple>
        <el-step title="上传" />
        <el-step title="OCR 识别" />
        <el-step title="生成凭证" />
      </el-steps>
      <el-alert v-if="errorMessage" :title="errorMessage" type="error" show-icon :closable="false" />
      <div v-if="fields.length" class="recognized-fields">
        <el-card v-for="field in fields" :key="field.name" shadow="never">
          <small>{{ field.name }}</small>
          <strong>{{ field.value }}</strong>
        </el-card>
      </div>
      <el-button v-if="status === 'done'" type="primary" @click="emit('recognized', result!)">查看识别与凭证结果</el-button>
    </div>

    <!-- 批量 OCR 结果表格 -->
    <div v-if="batchResults.length" class="batch-results">
      <div class="batch-actions">
        <el-button type="primary" @click="emit('batchVerify')">
          <el-icon><CircleCheck /></el-icon>
          批量验真
        </el-button>
        <el-button type="success" @click="emit('batchVoucher')">
          <el-icon><Document /></el-icon>
          批量生成凭证
        </el-button>
      </div>
      <el-table :data="batchResults" stripe border style="width: 100%" max-height="400">
        <el-table-column prop="invoiceNo" label="发票号码" min-width="160" show-overflow-tooltip />
        <el-table-column prop="invoiceDate" label="开票日期" min-width="120" show-overflow-tooltip />
        <el-table-column prop="amount" label="金额" min-width="120" show-overflow-tooltip />
        <el-table-column prop="sellerName" label="销方" min-width="150" show-overflow-tooltip />
        <el-table-column prop="buyerName" label="购方" min-width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row, $index }">
            <el-button type="primary" link size="small" @click="emit('verify', row, $index)">验真</el-button>
            <el-button type="success" link size="small" @click="emit('voucher', row, $index)">生成凭证</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { UploadFilled, CircleCheck, Document } from '@element-plus/icons-vue'
import { sendChat, type ChatResponse, type ClientContext } from '@/api/agent'

/** 批量 OCR 识别结果项 */
export interface BatchOcrResult {
  invoiceNo: string
  invoiceDate: string
  amount: string
  sellerName: string
  buyerName: string
  fileName?: string
  raw?: ChatResponse
}

const props = defineProps<{
  sessionId?: string
  clientContext?: ClientContext
}>()

const emit = defineEmits<{
  recognized: [result: ChatResponse]
  session: [sessionId: string]
  verify: [row: BatchOcrResult, index: number]
  voucher: [row: BatchOcrResult, index: number]
  batchVerify: []
  batchVoucher: []
}>()

const status = ref<'idle' | 'uploading' | 'recognizing' | 'done' | 'error'>('idle')
const result = ref<ChatResponse>()
const errorMessage = ref('')

/** 批量 OCR 结果数组 */
const batchResults = ref<BatchOcrResult[]>([])

const busy = computed(() => status.value === 'uploading' || status.value === 'recognizing')
const activeStep = computed(() => {
  if (status.value === 'uploading') return 1
  if (status.value === 'recognizing') return 2
  if (status.value === 'done') return 3
  return 0
})

const fields = computed(() => {
  const content = result.value?.content as { invoice?: { fields?: Array<{ name: string; value: string }> } } | undefined
  return content?.invoice?.fields || []
})

/**
 * 从 OCR 响应中提取结构化发票字段
 */
function extractStructuredFields(response: ChatResponse, fileName?: string): BatchOcrResult {
  const content = response.content as Record<string, any> | undefined
  const invoice = content?.invoice || {}
  const fields: Record<string, string> = {}

  // 尝试从 fields 数组提取
  if (Array.isArray(invoice.fields)) {
    for (const f of invoice.fields) {
      if (f.name && f.value != null) {
        fields[f.name] = String(f.value)
      }
    }
  }

  // 尝试直接从 content 提取常见字段
  const extractField = (keys: string[]): string => {
    for (const key of keys) {
      const val = invoice[key] ?? content?.[key] ?? fields[key]
      if (val != null && String(val).trim()) return String(val)
    }
    return ''
  }

  return {
    invoiceNo: extractField(['invoiceNo', 'invoice_no', '发票号码', '发票代码', 'fpdm', 'fphm']),
    invoiceDate: extractField(['invoiceDate', 'invoice_date', '开票日期', 'kprq']),
    amount: extractField(['amount', 'totalAmount', 'total_amount', '金额', '合计金额', 'je', 'jshj']),
    sellerName: extractField(['sellerName', 'seller_name', '销方名称', '销售方名称', 'xfmc']),
    buyerName: extractField(['buyerName', 'buyer_name', '购方名称', '购买方名称', 'gfmc']),
    fileName,
    raw: response,
  }
}

async function recognizeInvoice(uploadFile: { raw?: File; name?: string }) {
  if (!uploadFile.raw) return
  status.value = 'uploading'
  result.value = undefined
  errorMessage.value = ''

  try {
    const imageUrl = await readAsDataUrl(uploadFile.raw)
    status.value = 'recognizing'
    result.value = await sendChat({
      sessionId: props.sessionId,
      message: '根据上传的发票生成一张会计凭证',
      clientContext: props.clientContext,
      imageUrl,
    })
    emit('session', result.value.sessionId)

    // 提取结构化字段并加入批量结果
    const ocrItem = extractStructuredFields(result.value, uploadFile.name)
    batchResults.value.push(ocrItem)

    status.value = 'done'
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : String(error)
    status.value = 'error'
  }
}

function readAsDataUrl(file: File) {
  return new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = () => reject(reader.error || new Error('文件读取失败'))
    reader.readAsDataURL(file)
  })
}

/** 暴露 batchResults 供父组件访问 */
defineExpose({
  batchResults,
})
</script>

<style scoped>
.invoice-uploader {
  padding: 16px;
}

.upload-icon {
  color: var(--brand);
  font-size: 42px;
}

.upload-text {
  margin-top: 8px;
  font-weight: 720;
}

.upload-hint {
  margin-top: 4px;
  color: var(--muted);
  font-size: 12px;
}

.recognition-box {
  display: grid;
  gap: 14px;
  margin-top: 16px;
}

.scan-stage {
  position: relative;
  height: 90px;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: repeating-linear-gradient(0deg, #f9fbfa, #f9fbfa 18px, #eef4f2 19px);
}

.scan-line {
  position: absolute;
  left: 0;
  right: 0;
  height: 2px;
  background: var(--brand);
  animation: scan 1.4s infinite;
}

.done .scan-line {
  top: 100%;
  background: var(--ok);
  animation: none;
}

.recognized-fields {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 8px;
}

.recognized-fields small,
.recognized-fields strong {
  display: block;
}

.recognized-fields small {
  margin-bottom: 4px;
  color: var(--muted);
}

/* 批量结果 */
.batch-results {
  margin-top: 20px;
}

.batch-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

@keyframes scan {
  from { top: 0; }
  to { top: 100%; }
}
</style>
