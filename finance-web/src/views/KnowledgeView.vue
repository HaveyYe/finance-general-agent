<template>
  <div class="knowledge-container">
    <section class="top-grid">
      <div class="upload-panel">
        <div class="panel-title">文档上传</div>
        <el-upload
          drag
          action="#"
          :auto-upload="false"
          :show-file-list="false"
          :disabled="uploading"
          accept=".pdf,.docx,.xlsx,.xls,.txt,.md,.csv,.json,.html,.htm,.xml,.yml,.yaml,.png,.jpg,.jpeg,.webp,.gif,.bmp"
          @change="handleUpload"
        >
          <el-icon class="upload-icon"><UploadFilled /></el-icon>
          <div class="upload-text">选择或拖拽文档</div>
          <div class="upload-hint">PDF、DOCX、XLSX/XLS、文本、网页、图片</div>
        </el-upload>
        <div class="upload-type-row">
          <el-select v-model="docType" placeholder="文档类型" size="small" clearable style="width: 140px">
            <el-option label="报销规则" value="报销规则" />
            <el-option label="发票模板" value="发票模板" />
            <el-option label="财务制度" value="财务制度" />
            <el-option label="合同模板" value="合同模板" />
            <el-option label="其他" value="其他" />
          </el-select>
        </div>
        <el-alert v-if="uploadError" :title="uploadError" type="error" show-icon :closable="false" />
      </div>

      <div class="document-panel">
        <div class="panel-title-row">
          <span class="panel-title">文档列表</span>
          <div class="panel-actions">
            <el-button text type="primary" :loading="reindexing" :disabled="!documents.length || loadingDocuments" @click="rebuildIndex">
              <el-icon><Refresh /></el-icon>
              重建索引
            </el-button>
            <el-button text type="primary" :loading="loadingDocuments" @click="loadDocuments">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
        <el-table
          :data="documents"
          stripe
          border
          height="278"
          empty-text="暂无文档"
          v-loading="loadingDocuments"
        >
          <el-table-column prop="originalName" label="文件名" min-width="180" show-overflow-tooltip />
          <el-table-column prop="docType" label="文档类型" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.docType" size="small">{{ row.docType }}</el-tag>
              <span v-else style="color: #909399">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="extension" label="类型" width="82" />
          <el-table-column label="大小" width="96">
            <template #default="{ row }">{{ formatSize(row.size) }}</template>
          </el-table-column>
          <el-table-column prop="chunkCount" label="片段" width="72" />
          <el-table-column label="上传时间" width="168">
            <template #default="{ row }">{{ formatTime(row.uploadedAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="84" align="center">
            <template #default="{ row }">
              <el-button text type="danger" @click="removeDocument(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </section>

    <section class="qa-panel">
      <div class="panel-title">文档问答</div>
      <div class="ask-row">
        <el-input
          v-model="question"
          type="textarea"
          :rows="3"
          resize="none"
          placeholder="输入要询问文档的问题"
          :disabled="asking"
          @keydown.enter.exact.prevent="ask"
        />
        <el-button type="primary" :loading="asking" :disabled="!question.trim()" @click="ask">
          <el-icon><Promotion /></el-icon>
          提问
        </el-button>
      </div>

      <div v-if="answer" class="answer-box">
        <div class="answer-text">{{ answer.answer }}</div>
        <div v-if="answer.citations.length" class="citation-list">
          <div v-for="item in answer.citations" :key="`${item.documentId}-${item.chunkNo}`" class="citation-item">
            <div class="citation-head">
              <span>{{ item.documentName }}</span>
              <el-tag size="small">片段 {{ item.chunkNo }}</el-tag>
              <el-tag size="small" type="info" v-if="item.score">{{ (item.score * 100).toFixed(0) }}%</el-tag>
            </div>
            <p>{{ item.text }}</p>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Promotion, Refresh, UploadFilled } from '@element-plus/icons-vue'
import {
  askKnowledge,
  deleteKnowledgeDocument,
  listKnowledgeDocuments,
  reindexKnowledgeDocuments,
  uploadKnowledgeDocument,
  type KnowledgeChatResponse,
  type KnowledgeDocument,
} from '@/api/knowledge'

const documents = ref<KnowledgeDocument[]>([])
const loadingDocuments = ref(false)
const uploading = ref(false)
const reindexing = ref(false)
const asking = ref(false)
const uploadError = ref('')
const question = ref('')
const docType = ref('')
const answer = ref<KnowledgeChatResponse | null>(null)

onMounted(() => {
  void loadDocuments()
})

async function loadDocuments() {
  loadingDocuments.value = true
  try {
    documents.value = await listKnowledgeDocuments()
  } catch (error) {
    ElMessage.error(errorMessage(error))
  } finally {
    loadingDocuments.value = false
  }
}

async function handleUpload(uploadFile: { raw?: File }) {
  if (!uploadFile.raw) return
  uploading.value = true
  uploadError.value = ''
  try {
    const document = await uploadKnowledgeDocument(uploadFile.raw, docType.value || undefined)
    ElMessage.success(`已索引 ${document.originalName}`)
    await loadDocuments()
  } catch (error) {
    uploadError.value = errorMessage(error)
  } finally {
    uploading.value = false
  }
}

async function removeDocument(documentId: string) {
  await ElMessageBox.confirm('删除后会同步移除索引。', '删除文档', { type: 'warning' })
  await deleteKnowledgeDocument(documentId)
  ElMessage.success('已删除')
  await loadDocuments()
}

async function rebuildIndex() {
  await ElMessageBox.confirm('会重新抽取已上传文档并按章节、问答、表格重建索引。文档不会被删除。', '重建知识库索引', { type: 'warning' })
  reindexing.value = true
  try {
    const result = await reindexKnowledgeDocuments()
    const failed = result.documents.filter((item) => item.error)
    if (failed.length) {
      ElMessage.warning(`索引重建完成，${failed.length} 个文档失败`)
    } else {
      ElMessage.success(`已重建 ${result.documents.length} 个文档索引`)
    }
    await loadDocuments()
  } catch (error) {
    ElMessage.error(errorMessage(error))
  } finally {
    reindexing.value = false
  }
}

async function ask() {
  const text = question.value.trim()
  if (!text) return
  asking.value = true
  answer.value = null
  try {
    answer.value = await askKnowledge(text)
  } catch (error) {
    ElMessage.error(errorMessage(error))
  } finally {
    asking.value = false
  }
}

function formatSize(size: number) {
  if (size > 1024 * 1024) return `${(size / 1024 / 1024).toFixed(1)} MB`
  if (size > 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${size} B`
}

function formatTime(value: string) {
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

function errorMessage(error: unknown) {
  const response = (error as { response?: { data?: { error?: string } } }).response
  return response?.data?.error || (error instanceof Error ? error.message : String(error))
}
</script>

<style scoped>
.knowledge-container {
  display: flex;
  flex-direction: column;
  gap: 18px;
  height: 100%;
  padding: 24px;
  overflow-y: auto;
}

.top-grid {
  display: grid;
  grid-template-columns: minmax(300px, 380px) minmax(0, 1fr);
  gap: 18px;
}

.upload-panel,
.document-panel,
.qa-panel {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 18px;
}

.panel-title,
.panel-title-row {
  color: #303133;
  font-size: 16px;
  font-weight: 700;
}

.panel-title {
  margin-bottom: 14px;
}

.panel-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.panel-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.upload-icon {
  color: #409eff;
  font-size: 42px;
}

.upload-text {
  margin-top: 8px;
  color: #303133;
  font-weight: 700;
}

.upload-hint {
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
}

.upload-type-row {
  margin-top: 10px;
  display: flex;
  justify-content: center;
}

.qa-panel {
  display: grid;
  gap: 16px;
}

.ask-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 110px;
  gap: 12px;
  align-items: stretch;
}

.ask-row :deep(.el-button) {
  height: 76px;
}

.answer-box {
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 16px;
  background: #fbfcfe;
}

.answer-text {
  color: #303133;
  line-height: 1.75;
  white-space: pre-wrap;
}

.citation-list {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

.citation-item {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px;
  background: #fff;
}

.citation-head {
  display: flex;
  align-items: center;
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

@media (max-width: 1040px) {
  .top-grid {
    grid-template-columns: 1fr;
  }
}
</style>
