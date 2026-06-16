import axios from 'axios'

export interface KnowledgeDocument {
  id: string
  fileName: string
  originalName: string
  mimeType: string
  extension: string
  size: number
  uploadedAt: string
  chunkCount: number
  status: 'ready'
  docType?: string
}

export interface KnowledgeCitation {
  documentId: string
  documentName: string
  chunkNo: number
  score: number
  text: string
}

export interface KnowledgeChatResponse {
  answer: string
  citations: KnowledgeCitation[]
  hits: KnowledgeCitation[]
}

export interface KnowledgeReindexResponse {
  ok: boolean
  indexVersion: string
  documents: Array<{
    documentId: string
    originalName: string
    chunkCount?: number
    error?: string
  }>
}

export async function listKnowledgeDocuments() {
  const { data } = await axios.get<{ documents: KnowledgeDocument[] }>('/knowledge/documents')
  return data.documents
}

export async function uploadKnowledgeDocument(file: File, docType?: string) {
  const form = new FormData()
  form.append('file', file)
  const url = docType
    ? `/knowledge/documents?docType=${encodeURIComponent(docType)}`
    : '/knowledge/documents'
  const { data } = await axios.post<{ document: KnowledgeDocument }>(url, form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000,
  })
  return data.document
}

export async function deleteKnowledgeDocument(documentId: string) {
  await axios.delete(`/knowledge/documents/${encodeURIComponent(documentId)}`)
}

export async function reindexKnowledgeDocuments() {
  const { data } = await axios.post<KnowledgeReindexResponse>('/knowledge/reindex', {}, { timeout: 300000 })
  return data
}

export async function askKnowledge(question: string) {
  const { data } = await axios.post<KnowledgeChatResponse>('/knowledge/chat', { question }, { timeout: 120000 })
  return data
}
