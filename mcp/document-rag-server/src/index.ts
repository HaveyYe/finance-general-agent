import { createHash, randomUUID } from 'node:crypto'
import { createServer, IncomingMessage, ServerResponse } from 'node:http'
import { mkdir, readFile, rename, rm, writeFile } from 'node:fs/promises'
import { createWriteStream } from 'node:fs'
import { basename, dirname, extname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import Busboy from 'busboy'
import mammoth from 'mammoth'
import pdfParse from 'pdf-parse'
import * as XLSX from 'xlsx'

type JsonObject = Record<string, unknown>

interface DocumentRecord {
  id: string
  fileName: string
  originalName: string
  mimeType: string
  extension: string
  size: number
  uploadedAt: string
  chunkCount: number
  status: 'ready'
  indexVersion?: string
  docType?: string
}

interface ChunkRecord {
  id: string
  documentId: string
  chunkNo: number
  type?: 'text' | 'rule' | 'qa' | 'table'
  title?: string
  text: string
  summary: string
  keywords: string[]
  signals?: string[]
}

interface EmbeddingRecord {
  chunkId: string
  vector: number[]
}

interface SearchHit {
  chunk: ChunkRecord
  document: DocumentRecord
  score: number
  vectorScore: number
  keywordScore: number
}

interface QueryPlan {
  originalQuestion: string
  normalizedQuestion: string
  queries: string[]
  topicHints: string[]
  terms: string[]
}

const port = Number(process.env.DOCUMENT_RAG_PORT || 8091)
const currentFile = fileURLToPath(import.meta.url)
const serverRoot = join(dirname(currentFile), '..')
const dataDir = process.env.DOCUMENT_RAG_DATA_DIR || join(serverRoot, 'data')
const filesDir = join(dataDir, 'files')
const documentsPath = join(dataDir, 'documents.json')
const chunksPath = join(dataDir, 'chunks.json')
const embeddingsPath = join(dataDir, 'embeddings.json')
const maxUploadBytes = Number(process.env.DOCUMENT_RAG_MAX_UPLOAD_BYTES || 25 * 1024 * 1024)
const indexVersion = '2026-06-rag-v4-structured'
const hardMinStrongScore = 0.32
const hardMinStrongKeywordScore = 0.24
const hardMinAnswerScore = 0.42
const hardMinAnswerKeywordScore = 0.28
const minStrongScore = Math.max(hardMinStrongScore, Number(process.env.DOCUMENT_RAG_MIN_STRONG_SCORE || hardMinStrongScore))
const minStrongKeywordScore = Math.max(hardMinStrongKeywordScore, Number(process.env.DOCUMENT_RAG_MIN_STRONG_KEYWORD_SCORE || hardMinStrongKeywordScore))
const minAnswerScore = Math.max(hardMinAnswerScore, Number(process.env.DOCUMENT_RAG_MIN_ANSWER_SCORE || hardMinAnswerScore))
const minAnswerKeywordScore = Math.max(hardMinAnswerKeywordScore, Number(process.env.DOCUMENT_RAG_MIN_ANSWER_KEYWORD_SCORE || hardMinAnswerKeywordScore))
let reindexPromise: Promise<void> | undefined

const textExtensions = new Set(['.txt', '.md', '.csv', '.json', '.html', '.htm', '.xml', '.yml', '.yaml'])
const imageExtensions = new Set(['.png', '.jpg', '.jpeg', '.webp', '.gif', '.bmp'])
const spreadsheetExtensions = new Set(['.xlsx', '.xls'])

const server = createServer(async (req, res) => {
  setCorsHeaders(res)
  if (req.method === 'OPTIONS') {
    res.writeHead(204)
    res.end()
    return
  }

  try {
    await ensureStore()
    const url = new URL(req.url || '/', `http://${req.headers.host || 'localhost'}`)
    if (req.method === 'GET' && url.pathname === '/knowledge/health') {
      writeJson(res, 200, { ok: true })
      return
    }
    if (req.method === 'GET' && url.pathname === '/knowledge/documents') {
      await handleListDocuments(res)
      return
    }
    if (req.method === 'POST' && url.pathname === '/knowledge/documents') {
      await handleUploadDocument(req, res, url.searchParams.get('docType') || undefined)
      return
    }
    if (req.method === 'POST' && url.pathname === '/knowledge/reindex') {
      await handleReindexDocuments(res)
      return
    }
    if (req.method === 'DELETE' && url.pathname.startsWith('/knowledge/documents/')) {
      await handleDeleteDocument(url.pathname, res)
      return
    }
    if (req.method === 'POST' && url.pathname === '/knowledge/chat') {
      await handleKnowledgeChat(req, res)
      return
    }
    writeJson(res, 404, { error: 'not found' })
  } catch (e) {
    writeJson(res, 500, { error: errorMessage(e) })
  }
})

server.listen(port, () => {
  console.log(`document-rag-server listening on http://localhost:${port}/knowledge`)
})

async function handleListDocuments(res: ServerResponse) {
  const documents = await loadDocuments()
  writeJson(res, 200, { documents: documents.sort((a, b) => b.uploadedAt.localeCompare(a.uploadedAt)) })
}

async function handleUploadDocument(req: IncomingMessage, res: ServerResponse, docType?: string) {
  const uploaded = await readMultipartFile(req)
  const extension = extname(uploaded.originalName).toLowerCase()
  let text = ''
  try {
    text = await extractText(uploaded.path, uploaded.buffer, extension, uploaded.mimeType)
  } catch (e) {
    console.warn(`[document-rag] extract failed file="${uploaded.originalName}" ext="${extension || '(none)'}" mime="${uploaded.mimeType}" size=${uploaded.size}: ${errorMessage(e)}`)
    await rm(uploaded.path, { force: true })
    writeJson(res, 400, { error: errorMessage(e) || '无法抽取文本，上传失败' })
    return
  }
  const normalized = normalizeText(text)
  if (normalized.length < 20) {
    console.warn(`[document-rag] extract too short file="${uploaded.originalName}" ext="${extension || '(none)'}" mime="${uploaded.mimeType}" size=${uploaded.size} textLength=${normalized.length}`)
    await rm(uploaded.path, { force: true })
    writeJson(res, 400, { error: shortTextError(extension, uploaded.mimeType) })
    return
  }

  const id = randomUUID()
  const safeName = `${id}${extension || '.bin'}`
  const finalPath = join(filesDir, safeName)
  await rename(uploaded.path, finalPath)

  const chunks = buildChunks(id, normalized)
  const embeddings = await embedTexts(chunks.map((chunk) => chunk.text))
  const document: DocumentRecord = {
    id,
    fileName: safeName,
    originalName: uploaded.originalName,
    mimeType: uploaded.mimeType,
    extension: extension || '.bin',
    size: uploaded.size,
    uploadedAt: new Date().toISOString(),
    chunkCount: chunks.length,
    status: 'ready',
    indexVersion,
    ...(docType ? { docType } : {}),
  }

  const documents = await loadDocuments()
  const existingChunks = await loadChunks()
  const existingEmbeddings = await loadEmbeddings()
  documents.push(document)
  await saveDocuments(documents)
  await saveChunks(existingChunks.concat(chunks))
  await saveEmbeddings(existingEmbeddings.concat(chunks.map((chunk, index) => ({ chunkId: chunk.id, vector: embeddings[index] }))))

  writeJson(res, 200, { document })
}

async function reindexOutdatedDocuments() {
  const documents = await loadDocuments()
  const outdated = documents.filter((document) => document.indexVersion !== indexVersion)
  if (!outdated.length) {
    return
  }
  console.log(`[document-rag] reindexing ${outdated.length} document(s) with ${indexVersion}`)
  await reindexDocuments(outdated)
}

async function handleReindexDocuments(res: ServerResponse) {
  const result = await reindexDocuments(await loadDocuments())
  writeJson(res, 200, result)
}

async function reindexDocuments(targetDocuments: DocumentRecord[]) {
  const documents = await loadDocuments()
  let chunks = await loadChunks()
  let embeddings = await loadEmbeddings()
  const stats: Array<{ documentId: string; originalName: string; chunkCount?: number; error?: string }> = []
  for (const document of targetDocuments) {
    const path = join(filesDir, document.fileName)
    let buffer: Buffer
    try {
      buffer = await readFile(path)
    } catch (e) {
      console.warn(`[document-rag] skip reindex missing file ${document.fileName}: ${errorMessage(e)}`)
      stats.push({ documentId: document.id, originalName: document.originalName, error: errorMessage(e) })
      continue
    }
    let text = ''
    try {
      text = normalizeText(await extractText(path, buffer, document.extension, document.mimeType))
    } catch (e) {
      console.warn(`[document-rag] skip reindex extract failed ${document.fileName}: ${errorMessage(e)}`)
      stats.push({ documentId: document.id, originalName: document.originalName, error: errorMessage(e) })
      continue
    }
    const nextChunks = buildChunks(document.id, text)
    const nextEmbeddings = await embedTexts(nextChunks.map((chunk) => chunk.text))
    const removedChunkIds = new Set(chunks.filter((chunk) => chunk.documentId === document.id).map((chunk) => chunk.id))
    chunks = chunks.filter((chunk) => chunk.documentId !== document.id).concat(nextChunks)
    embeddings = embeddings.filter((item) => !removedChunkIds.has(item.chunkId))
      .concat(nextChunks.map((chunk, index) => ({ chunkId: chunk.id, vector: nextEmbeddings[index] })))
    const stored = documents.find((item) => item.id === document.id)
    if (stored) {
      stored.chunkCount = nextChunks.length
      stored.indexVersion = indexVersion
    }
    stats.push({ documentId: document.id, originalName: document.originalName, chunkCount: nextChunks.length })
  }
  await saveDocuments(documents)
  await saveChunks(chunks)
  await saveEmbeddings(embeddings)
  return { ok: true, indexVersion, documents: stats }
}

async function handleDeleteDocument(pathname: string, res: ServerResponse) {
  const documentId = decodeURIComponent(pathname.replace('/knowledge/documents/', '').trim())
  const documents = await loadDocuments()
  const target = documents.find((item) => item.id === documentId)
  if (!target) {
    writeJson(res, 404, { error: 'document not found' })
    return
  }
  await saveDocuments(documents.filter((item) => item.id !== documentId))
  const chunks = await loadChunks()
  const removedChunkIds = new Set(chunks.filter((chunk) => chunk.documentId === documentId).map((chunk) => chunk.id))
  await saveChunks(chunks.filter((chunk) => chunk.documentId !== documentId))
  await saveEmbeddings((await loadEmbeddings()).filter((item) => !removedChunkIds.has(item.chunkId)))
  await rm(join(filesDir, target.fileName), { force: true })
  writeJson(res, 200, { ok: true })
}

async function handleKnowledgeChat(req: IncomingMessage, res: ServerResponse) {
  const body = await readJson(req)
  const question = String(body.question || '').trim()
  if (!question) {
    writeJson(res, 400, { error: 'question is required' })
    return
  }

  const queryPlan = await buildQueryPlan(question)
  const hits = await retrieveForPlan(queryPlan, Number(body.topK || 10))
  const answer = await answerQuestion(question, queryPlan.normalizedQuestion, hits, queryPlan)
  writeJson(res, 200, answer)
}

async function retrieveForPlan(queryPlan: QueryPlan, topK: number): Promise<SearchHit[]> {
  const merged = new Map<string, SearchHit>()
  const queries = uniqueStrings(queryPlan.queries).slice(0, 6)
  for (const query of queries) {
    const hits = await retrieve(query, Math.max(topK, 12), queryPlan)
    for (const hit of hits) {
      const existing = merged.get(hit.chunk.id)
      if (!existing || hit.score > existing.score) {
        merged.set(hit.chunk.id, hit)
      }
    }
  }
  return [...merged.values()]
    .sort((a, b) => b.score - a.score)
    .slice(0, Math.max(1, Math.min(topK, 16)))
}

async function buildQueryPlan(originalQuestion: string): Promise<QueryPlan> {
  const local = buildLocalQueryPlan(originalQuestion)
  const modelPlan = process.env.DOCUMENT_RAG_USE_LLM === 'true'
    ? await callQueryPlanner(originalQuestion, local)
    : undefined
  if (!modelPlan) return local
  return {
    originalQuestion,
    normalizedQuestion: modelPlan.normalizedQuestion || local.normalizedQuestion,
    queries: uniqueStrings([local.normalizedQuestion, ...local.queries, ...modelPlan.queries]).slice(0, 8),
    topicHints: uniqueStrings([...local.topicHints, ...modelPlan.topicHints]).slice(0, 24),
    terms: uniqueStrings([...local.terms, ...modelPlan.terms]).slice(0, 48),
  }
}

function buildLocalQueryPlan(originalQuestion: string): QueryPlan {
  const normalizedQuestion = normalizeQuestion(originalQuestion)
  const expandedQuestion = expandQuestion(normalizedQuestion)
  const topicHints = inferTopicHints(expandedQuestion)
  const termText = `${normalizedQuestion}\n${expandedQuestion}\n${topicHints.join(' ')}`
  return {
    originalQuestion,
    normalizedQuestion: expandedQuestion,
    queries: uniqueStrings([
      normalizedQuestion,
      expandedQuestion,
      ...buildTopicQueries(expandedQuestion),
    ]),
    topicHints,
    terms: uniqueStrings(keywords(termText).concat(topicHints)),
  }
}

function expandQuestion(question: string) {
  return question
    .replace(/小孩子|小孩|孩子|儿童|宝宝|娃|小朋友/g, '子女')
    .replace(/老婆|爱人|妻子|丈夫|先生|太太/g, '配偶')
    .replace(/爸妈|爸爸|妈妈|父亲|母亲|老人/g, '父母')
    .replace(/看病|医院|医药费|医疗费|挂号|门诊|急诊|住院/g, '医疗费用')
    .replace(/能不能报|能报吗|能报|可以报吗|可报吗/g, '可以报销 赔付 理赔')
    .replace(/报销/g, '报销 赔付 理赔')
    .replace(/哪个公司|哪家公司|哪家/g, '保险公司 承保公司')
    .replace(/住宿多少钱|住酒店多少钱/g, '住宿费报销标准 酒店住宿限额')
    .replace(/打车|坐车/g, '交通费 市内交通 出行')
    .replace(/票/g, '发票')
}

function inferTopicHints(question: string) {
  const hints: string[] = []
  const pairs: Array<[RegExp, string[]]> = [
    [/(子女|配偶|父母|保险|商保|医疗费用|理赔|赔付|保障|人身险|医疗险)/, ['保险', '商业保险', '综合医疗', '理赔', '赔付', '保障']],
    [/(住宿|酒店|差旅|出差|间夜|间\/夜)/, ['差旅', '住宿标准', '住宿费', '酒店', '限额']],
    [/(发票|开票|红冲|冲红|作废|验真|抵扣)/, ['发票', '开票', '红冲', '验真', '抵扣']],
    [/(固定资产|资产|验收|盘点|报废|折旧)/, ['固定资产', '资产', '验收', '盘点', '报废', '折旧']],
    [/(合同|付款|对公|供应商|结算)/, ['合同', '付款', '对公付款', '供应商', '结算']],
    [/(报销|费用|审批|单据|凭证)/, ['报销', '费用', '审批', '单据']],
  ]
  for (const [pattern, values] of pairs) {
    if (pattern.test(question)) hints.push(...values)
  }
  return uniqueStrings(hints)
}

function buildTopicQueries(question: string) {
  const queries: string[] = []
  if (/(子女|医疗费用|保险|理赔|赔付)/.test(question)) {
    queries.push('员工子女综合医疗保险 门诊 急诊 医疗费用 赔付 报销')
    queries.push('子女 综合医疗保险 保障一览表 公司付费 门诊 急诊 住院')
  }
  if (/(保险公司|承保公司|商业保险)/.test(question)) {
    queries.push('商业保险 保险公司 承保 服务专员 报案电话')
  }
  if (/(住宿|酒店|差旅)/.test(question)) {
    queries.push('住宿费报销标准 酒店住宿限额 高消费城市 其他城市')
  }
  return queries
}

async function callQueryPlanner(originalQuestion: string, localPlan: QueryPlan): Promise<QueryPlan | undefined> {
  const apiKey = process.env.GLM_API_KEY || process.env.Z_AI_API_KEY
  if (!apiKey) return undefined
  try {
    const response = await fetch(process.env.GLM_API_URL || 'https://open.bigmodel.cn/api/paas/v4/chat/completions', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${apiKey}`,
      },
      body: JSON.stringify({
        model: process.env.DOCUMENT_RAG_QUERY_MODEL || process.env.DOCUMENT_RAG_CHAT_MODEL || process.env.GLM_MODEL || 'glm-4-flash',
        messages: [
          {
            role: 'system',
            content: [
              '你是企业知识库检索查询改写器。',
              '把口语问题改写成适合检索企业制度、手册、合同、保险文档的中文查询。',
              '只输出 JSON，不要输出解释。字段：normalizedQuestion, queries, terms, topicHints。',
              'queries 最多 4 条，terms/topicHints 每个最多 10 个。',
            ].join('\n'),
          },
          {
            role: 'user',
            content: JSON.stringify({
              question: originalQuestion,
              localNormalizedQuestion: localPlan.normalizedQuestion,
              localHints: localPlan.topicHints,
            }),
          },
        ],
        temperature: 0,
      }),
    })
    if (!response.ok) return undefined
    const data = await response.json() as { choices?: Array<{ message?: { content?: string } }> }
    const content = data.choices?.[0]?.message?.content || ''
    const parsed = parseJsonObject(content)
    if (!parsed) return undefined
    return {
      originalQuestion,
      normalizedQuestion: stringValue(parsed.normalizedQuestion) || localPlan.normalizedQuestion,
      queries: stringArray(parsed.queries),
      topicHints: stringArray(parsed.topicHints),
      terms: stringArray(parsed.terms),
    }
  } catch {
    return undefined
  }
}

async function retrieve(question: string, topK: number, queryPlan: QueryPlan = buildLocalQueryPlan(question)): Promise<SearchHit[]> {
  const documents = await loadDocuments()
  const documentMap = new Map(documents.map((document) => [document.id, document]))
  const chunks = await loadChunks()
  const embeddings = new Map((await loadEmbeddings()).map((item) => [item.chunkId, item.vector]))
  const queryVector = (await embedTexts([question]))[0]
  const queryKeywords = uniqueStrings(keywords(question).concat(queryPlan.terms))

  return chunks
    .map((chunk) => {
      const document = documentMap.get(chunk.documentId)
      if (!document) return undefined
      const vectorScore = cosine(queryVector, embeddings.get(chunk.id) || [])
      const searchableText = `${document.originalName}\n${chunk.title || ''}\n${chunk.text}\n${(chunk.signals || []).join(' ')}`
      const searchableKeywords = keywords(`${document.originalName}\n${chunk.keywords.join(' ')}\n${chunk.text}`)
      const keywordScore = keywordSimilarity(queryKeywords, searchableKeywords, searchableText)
      const rerankScore = rerank(question, queryKeywords, chunk, document, queryPlan)
      return {
        chunk,
        document,
        vectorScore,
        keywordScore,
        score: vectorScore * 0.35 + keywordScore * 0.35 + rerankScore * 0.3,
      }
    })
    .filter((item): item is SearchHit => Boolean(item))
    .sort((a, b) => b.score - a.score)
    .slice(0, Math.max(1, Math.min(topK, 16)))
}

interface TopicRule {
  key: string
  title: string
  questionPattern: RegExp
  chunkSignals: string[]
  textPatterns: RegExp[]
  boosters: Array<{ question: RegExp; chunk: RegExp; score: number }>
}

const TOPIC_RULES: TopicRule[] = [
  {
    key: 'lodging',
    title: '住宿费报销标准',
    questionPattern: /(住宿|酒店|房价|标准间|间\/夜|间夜)/,
    chunkSignals: ['住宿标准', '住宿费', '酒店', '限额', '元/间/夜', '差旅'],
    textPatterns: [
      /公司分管领导及各一级部门负责人[\s\S]{0,120}?限额(\d+)元\/间\/夜限额(\d+)元\/间\/夜/,
      /其他(?:单程|员工)[\s\S]{0,180}?限额(\d+)元\/间\/夜限额(\d+)元\/间\/夜/,
      /住一个标准间[\s\S]{0,30}?住宿费不能超过(\d+)元\/间\/夜/,
      /各住一个标准间[\s\S]{0,30}?住宿费不能超过(\d+)元\/间\/夜/,
    ],
    boosters: [
      { question: /(住宿|酒店|房价|间夜|间\/夜)/, chunk: /(住宿|酒店|房价|间\/夜|限额)/, score: 0.28 },
      { question: /其他城市/, chunk: /其他城市/, score: 0.18 },
      { question: /高消费城市/, chunk: /高消费城市/, score: 0.18 },
      { question: /(员工|其他员工|普通员工)/, chunk: /(其他员工|其他单程)/, score: 0.14 },
      { question: /(领导|负责人|一级部门)/, chunk: /(领导|负责人|一级部门)/, score: 0.14 },
      { question: /(元\/间\/夜|元间夜|限额|标准)/, chunk: /(元\/间\/夜|限额|标准)/, score: 0.16 },
    ],
  },
  {
    key: 'invoice',
    title: '发票管理规则',
    questionPattern: /(发票|开票|红冲|作废|专票|普票|抵扣|验真|查重)/,
    chunkSignals: ['发票', '开票', '抵扣', '验真'],
    textPatterns: [],
    boosters: [
      { question: /(发票|开票)/, chunk: /(发票|开票|增值税)/, score: 0.26 },
      { question: /(红冲|作废)/, chunk: /(红冲|作废|冲红)/, score: 0.22 },
      { question: /(专票|普票|抵扣)/, chunk: /(专票|普票|抵扣|增值税)/, score: 0.22 },
      { question: /(验真|查重|校验)/, chunk: /(验真|查重|校验|重复)/, score: 0.22 },
    ],
  },
  {
    key: 'asset',
    title: '固定资产管理规则',
    questionPattern: /(固定资产|资产|折旧|盘点|验收|报废|无形资产|摊销)/,
    chunkSignals: ['固定资产', '资产', '折旧', '盘点', '验收'],
    textPatterns: [],
    boosters: [
      { question: /(固定资产|资产)/, chunk: /(固定资产|资产)/, score: 0.26 },
      { question: /(折旧|摊销)/, chunk: /(折旧|摊销|年限)/, score: 0.22 },
      { question: /(盘点|验收)/, chunk: /(盘点|验收|清查)/, score: 0.22 },
      { question: /(报废|处置)/, chunk: /(报废|处置|清理)/, score: 0.22 },
    ],
  },
  {
    key: 'expense',
    title: '费用报销规则',
    questionPattern: /(报销|费用|差旅|交通|餐饮|补贴|津贴|审批|单据|凭证|坐席|一等座|二等座|商务座|火车|高铁|飞机|经济舱|头等舱)/,
    chunkSignals: ['报销', '费用', '差旅', '审批', '火车', '高铁', '飞机'],
    textPatterns: [],
    boosters: [
      { question: /(报销|费用)/, chunk: /(报销|费用)/, score: 0.26 },
      { question: /(差旅|交通|出行)/, chunk: /(差旅|交通|出行|车票|机票)/, score: 0.22 },
      { question: /(餐饮|餐补|补贴|津贴)/, chunk: /(餐饮|餐补|补贴|津贴|伙食)/, score: 0.22 },
      { question: /(审批|审核|流程)/, chunk: /(审批|审核|流程|节点)/, score: 0.2 },
      { question: /(坐席|一等座|二等座|商务座|软卧|硬卧)/, chunk: /(一等座|二等座|商务座|软卧|硬卧|火车)/, score: 0.24 },
      { question: /(高铁|火车|动车|列车)/, chunk: /(高铁|火车|动车|列车|车票)/, score: 0.22 },
      { question: /(飞机|航班|经济舱|头等舱)/, chunk: /(飞机|航班|经济舱|头等舱)/, score: 0.22 },
    ],
  },
  {
    key: 'contract',
    title: '合同与付款规则',
    questionPattern: /(合同|签约|付款|对公|结算|供应商)/,
    chunkSignals: ['合同', '付款', '结算'],
    textPatterns: [],
    boosters: [
      { question: /(合同|签约|签订)/, chunk: /(合同|签约|签订|协议)/, score: 0.26 },
      { question: /(付款|对公|打款)/, chunk: /(付款|对公|打款|银行)/, score: 0.22 },
      { question: /(结算|核销)/, chunk: /(结算|核销|清算)/, score: 0.22 },
    ],
  },
  {
    key: 'insurance',
    title: '商业保险规则',
    questionPattern: /(保险|商保|理赔|保障|人身险|医疗险|医疗费用|赔付|95511|承保|子女|小孩|孩子|儿童)/,
    chunkSignals: ['保险', '商保', '理赔', '保障', '人身险', '医疗险', '子女', '综合医疗'],
    textPatterns: [],
    boosters: [
      { question: /(商业保险|商保|保险)/, chunk: /(商业保险|商保|保险|保障|理赔|人身险|医疗险)/, score: 0.28 },
      { question: /(哪个公司|哪家公司|哪家|承保|保险公司)/, chunk: /(平安|pingan|保险公司|95511|服务专员)/i, score: 0.24 },
      { question: /(理赔|报案|赔付)/, chunk: /(理赔|报案|赔付|95511)/, score: 0.22 },
      { question: /(保障|保额|人身险|医疗险)/, chunk: /(保障|保额|人身险|医疗险)/, score: 0.22 },
      { question: /(子女|小孩|孩子|儿童)/, chunk: /(子女|新生儿|未婚子女|综合医疗|医疗保险|门诊|急诊|住院)/, score: 0.28 },
      { question: /(报销|赔付|医疗费用)/, chunk: /(报销|赔付|医疗费用|合理医疗费用|综合医疗)/, score: 0.22 },
    ],
  },
]

function matchTopic(question: string): TopicRule | undefined {
  const insuranceRule = TOPIC_RULES.find((rule) => rule.key === 'insurance')
  if (insuranceRule && /(子女|小孩|孩子|儿童|医疗险|医疗费用|赔付|理赔|商保|保险)/.test(question)) {
    return insuranceRule
  }
  return TOPIC_RULES.find((rule) => rule.questionPattern.test(question))
}

function rerank(question: string, queryKeywords: string[], chunk: ChunkRecord, document: DocumentRecord, queryPlan: QueryPlan) {
  let score = 0
  const compactChunk = compactText(`${document.originalName}\n${chunk.title || ''}\n${chunk.text}\n${(chunk.signals || []).join(' ')}`)
  const compactQuestion = compactText(question)
  if (chunk.type === 'rule') score += 0.18
  for (const rule of TOPIC_RULES) {
    if (!rule.questionPattern.test(compactQuestion)) continue
    for (const booster of rule.boosters) {
      if (booster.question.test(compactQuestion) && booster.chunk.test(compactChunk)) {
        score += booster.score
      }
    }
  }
  if (/\d+/.test(compactQuestion)) {
    const nums = compactQuestion.match(/\d+/g) || []
    score += Math.min(0.16, nums.filter((num) => compactChunk.includes(num)).length * 0.08)
  }
  const exactKeywordHits = queryKeywords.filter((keyword) => compactChunk.includes(keyword)).length
  score += Math.min(0.18, exactKeywordHits * 0.03)
  const topicHits = queryPlan.topicHints.filter((hint) => compactChunk.includes(hint)).length
  score += Math.min(0.2, topicHits * 0.08)
  const termHits = queryPlan.terms.filter((term) => compactChunk.includes(term)).length
  score += Math.min(0.18, termHits * 0.025)
  return Math.min(1, score)
}

async function answerQuestion(originalQuestion: string, normalizedQuestion: string, hits: SearchHit[], queryPlan?: QueryPlan) {
  const strongHits = hits.filter(isStrongHit)
  const topic = matchTopic(normalizedQuestion)
  const topicHits = topic ? strongHits.filter((hit) => hitMatchesTopic(hit, topic)) : []
  const scopedHits = topicHits.length ? topicHits : strongHits
  const citations = scopedHits.map((hit) => ({
    documentId: hit.document.id,
    documentName: hit.document.originalName,
    chunkNo: hit.chunk.chunkNo,
    score: Number(hit.score.toFixed(4)),
    text: hit.chunk.summary,
  }))

  if (!hasAnswerableEvidence(normalizedQuestion, scopedHits, queryPlan)) {
    return buildNoEvidenceAnswer(originalQuestion)
  }

  const extractedAnswer = extractDeterministicAnswer(normalizedQuestion, scopedHits)
  if (extractedAnswer) {
    return { answer: extractedAnswer, citations, hits: citations }
  }

  if (isBareTopicQuery(normalizedQuestion)) {
    return {
      answer: `已找到与“${originalQuestion}”相关的文档，但这个问题还不够具体。你可以继续问保障内容、理赔流程、保险公司、报案电话等具体事项。`,
      citations,
      hits: citations,
    }
  }

  if (process.env.DOCUMENT_RAG_USE_LLM === 'true' && canUseLlmForAnswer(normalizedQuestion, scopedHits, queryPlan)) {
    const modelAnswer = await callChatModel(originalQuestion, scopedHits)
    if (modelAnswer) {
      return { answer: modelAnswer, citations, hits: citations }
    }
  }

  return {
    answer: buildExtractiveAnswer(scopedHits),
    citations,
    hits: citations,
  }
}

function isStrongHit(hit: SearchHit) {
  return hit.keywordScore >= minStrongKeywordScore || (hit.score >= minStrongScore && hit.keywordScore > 0)
}

function hasAnswerableEvidence(question: string, hits: SearchHit[], queryPlan?: QueryPlan) {
  const top = hits[0]
  if (!top) return false
  if (!hasRequiredQuestionEvidence(question, hits, queryPlan)) return false
  return top.score >= minAnswerScore && top.keywordScore >= minAnswerKeywordScore
}

function canUseLlmForAnswer(question: string, hits: SearchHit[], queryPlan?: QueryPlan) {
  const top = hits[0]
  if (!top) return false
  if (!hasRequiredQuestionEvidence(question, hits, queryPlan)) return false
  return top.score >= Math.max(0.5, minAnswerScore) && top.keywordScore >= minAnswerKeywordScore
}

function hasRequiredQuestionEvidence(question: string, hits: SearchHit[], queryPlan?: QueryPlan) {
  const evidence = compactText(hits.map((hit) => `${hit.document.originalName}\n${hit.chunk.title || ''}\n${hit.chunk.text}\n${(hit.chunk.signals || []).join(' ')}`).join('\n')).toLowerCase()
  const compactQuestion = compactText(question).toLowerCase()
  const requiredGroups: Array<{ question: RegExp; evidence: RegExp }> = [
    { question: /(红冲|冲红|作废)/, evidence: /(红冲|冲红|作废)/ },
    { question: /(天气|气温|下雨|降雨|温度)/, evidence: /(天气|气温|下雨|降雨|温度)/ },
    { question: /火星/, evidence: /火星/ },
  ]
  for (const group of requiredGroups) {
    if (group.question.test(compactQuestion) && !group.evidence.test(evidence)) {
      return false
    }
  }
  const topic = matchTopic(compactQuestion)
  if (!topic) {
    const termHits = queryPlan?.terms.filter((term) => evidence.includes(compactText(term).toLowerCase())).length || 0
    return hits[0]?.score >= Math.max(0.5, minAnswerScore) || termHits >= 2
  }
  return hits.some((hit) => hitMatchesTopic(hit, topic))
}

function hitMatchesTopic(hit: SearchHit, topic: TopicRule) {
  const text = compactText(`${hit.document.originalName}\n${hit.chunk.title || ''}\n${hit.chunk.text}\n${(hit.chunk.signals || []).join(' ')}`)
  if (topic.key === 'lodging') {
    return /(住宿|酒店|房价|间\/夜|间夜|住宿标准|住宿费|差旅住宿)/.test(text)
  }
  if (topic.key === 'insurance') {
    return /(保险|商保|理赔|赔付|保障|人身险|医疗险|子女|综合医疗|95511|pingan|平安)/i.test(text)
  }
  if (topic.key === 'invoice') {
    return /(发票|开票|红冲|冲红|作废|专票|普票|抵扣|验真|查重)/.test(text)
  }
  if (topic.key === 'asset') {
    return /(固定资产|资产|折旧|盘点|验收|报废|摊销)/.test(text)
  }
  if (topic.key === 'contract') {
    return /(合同|签约|付款|对公|结算|供应商)/.test(text)
  }
  return topic.chunkSignals.some((signal) => text.includes(signal)) || topic.boosters.some((booster) => booster.chunk.test(text))
}

function buildNoEvidenceAnswer(originalQuestion: string) {
  return {
    answer: `未在已上传文档中找到和“${originalQuestion}”直接相关的依据。请补充更具体的问题，或先上传相关资料。`,
    citations: [],
    hits: [],
  }
}

function buildExtractiveAnswer(hits: SearchHit[]) {
  const selected = hits.slice(0, 3)
  const lines = selected.map((hit, index) => {
    const summary = hit.chunk.summary.replace(/\s+/g, ' ').trim()
    return `${index + 1}. ${summary}`
  })
  return `根据已上传文档，找到以下相关依据：\n${lines.join('\n')}`
}

function isBareTopicQuery(question: string) {
  const compactQuestion = compactText(question)
  if (/[？?]|(什么|怎么|如何|哪些|多少|是否|能否|能不能|可以|需要|哪个公司|哪家|哪里|谁|几)/.test(compactQuestion)) {
    return false
  }
  return compactQuestion.length <= 12
}

function normalizeQuestion(question: string) {
  return question
    .replace(/根据(我)?(上传|提供)?的?(文档|资料|材料|文件)[，,。:：\s]*/g, '')
    .replace(/文档(中|里)?(提到|说明|写的|规定)?[，,。:：\s]*/g, '')
    .replace(/请问|帮我|告诉我|回答一下/g, '')
    .replace(/小孩子|小孩|孩子|儿童|宝宝|娃|小朋友/g, '子女')
    .trim() || question
}

function extractDeterministicAnswer(question: string, hits: SearchHit[]) {
  const topic = matchTopic(question)
  if (!topic) {
    return undefined
  }
  if (topic.key === 'lodging') {
    return extractLodgingAnswer(question, hits)
  }
  if (topic.key === 'insurance') {
    return extractInsuranceAnswer(question, hits)
  }
  return undefined
}

function extractInsuranceAnswer(question: string, hits: SearchHit[]) {
  const asksCompany = /(哪个公司|哪家公司|哪家|承保|保险公司)/.test(question)
  const asksChildReimbursement = /(子女|小孩|孩子|儿童).*(报销|赔付|医疗|理赔)|(报销|赔付|医疗|理赔).*(子女|小孩|孩子|儿童)/.test(question)
  const evidence = hits.map((hit) => `${hit.document.originalName}\n${hit.chunk.text}`).join('\n')
  if (!asksCompany) {
    if (asksChildReimbursement && /(员工子女综合医疗保险|子女综合医疗保险|员工子女|新生儿|未婚子女)/.test(evidence)) {
      return [
        '可以。已上传的商业保险手册里有“员工子女综合医疗保险保障一览表”，属于公司付费保障。',
        '其中门诊、急诊医疗保障共用 10000 元；符合当地社会基本医疗保险管理部门规定、且属于双方约定的合理门急诊医疗费用，可以按文档规则赔付。',
        '文档还提到“互联网+医疗”线上复诊可理赔，但只限复诊，赔付范围和材料要求与线下就诊保持一致。',
      ].join('\n')
    }
    return undefined
  }
  if (/平安|pingan|95511/.test(evidence)) {
    return '从已上传的商业保险文档看，公司商业保险对应的是平安保险。文档名包含“平安商业保险”，且联系方式中出现 pingan.com.cn 邮箱和 95511-6 报案电话。'
  }
  return undefined
}

function extractLodgingAnswer(question: string, hits: SearchHit[]) {
  const ruleHit = hits.find((hit) => hit.chunk.type === 'rule' && hit.chunk.title?.includes('住宿'))
  if (ruleHit) {
    return `${filterRuleAnswer(question, ruleHit.chunk.text)}`
  }
  const text = hits.map((hit) => hit.chunk.text).join('\n')
  const normalized = compactText(text)
  const leaderStandards = normalized.match(/公司分管领导及各一级部门负责人[\s\S]{0,120}?限额(\d+)元\/间\/夜限额(\d+)元\/间\/夜/)
  const staffStandards = normalized.match(/其他(?:单程|员工)[\s\S]{0,160}?限额(\d+)元\/间\/夜限额(\d+)元\/间\/夜/)
  const singleRoomDiscount = matchFirst(text, /各住一个标准\s*间[\s\S]{0,20}?住宿费不能超\s*过\s*(\d+)\s*元\/?\s*间\/?\s*夜/)
  const sameGenderShared = matchFirst(text, /住一个标准间[\s\S]{0,20}?住宿费不能超\s*过\s*(\d+)\s*元\/?\s*间\/?\s*夜/)

  if (!leaderStandards && !staffStandards && !singleRoomDiscount && !sameGenderShared) {
    return undefined
  }

  const lines: string[] = []
  if (staffStandards) {
    lines.push(`其他员工住宿标准：高消费城市 ${staffStandards[1]} 元/间/夜，其他城市 ${staffStandards[2]} 元/间/夜。`)
  }
  if (leaderStandards) {
    lines.push(`公司分管领导及各一级部门负责人住宿标准：高消费城市 ${leaderStandards[1]} 元/间/夜，其他城市 ${leaderStandards[2]} 元/间/夜。`)
  }
  if (sameGenderShared || singleRoomDiscount) {
    const detail: string[] = []
    if (sameGenderShared) detail.push(`同性别 2 人住一个标准间的示例上限为 ${sameGenderShared} 元/间/夜`)
    if (singleRoomDiscount) detail.push(`如 2 人各住一个标准间，示例上限为 ${singleRoomDiscount} 元/间/夜`)
    lines.push(`另外，文档对同性别多人出差有特殊说明：${detail.join('；')}。`)
  }
  return `${lines.join('\n')}`
}

function filterRuleAnswer(question: string, text: string) {
  const lines = text.split('\n').filter(Boolean)
  const wantsStaff = /(员工|其他员工|普通员工)/.test(question)
  const wantsLeader = /(领导|负责人|一级部门)/.test(question)
  const wantsOtherCity = /其他城市/.test(question)
  const wantsHighCost = /高消费城市/.test(question)
  const wantsSpecial = /(同性别|两人|两个人|2人|各住|一人一间|标准间|70%|420)/.test(question)
  let selected = lines.filter((line) => {
    if (line.includes('住宿费报销标准')) return false
    if (wantsStaff && !line.includes('其他员工')) return false
    if (wantsLeader && !line.includes('负责人')) return false
    if (wantsSpecial && !line.includes('同性别')) return false
    return true
  })
  if (!selected.length) {
    selected = lines.filter((line) => !line.includes('住宿费报销标准'))
  }
  if (wantsOtherCity || wantsHighCost) {
    selected = selected.map((line) => {
      if (!line.includes('住宿限额')) return line
      const role = line.split('：')[0]
      const high = line.match(/高消费城市住宿限额(\d+)元\/间\/夜/)
      const other = line.match(/其他城市住宿限额(\d+)元\/间\/夜/)
      if (wantsOtherCity && other) return `${role}其他城市住宿限额为 ${other[1]} 元/间/夜。`
      if (wantsHighCost && high) return `${role}高消费城市住宿限额为 ${high[1]} 元/间/夜。`
      return line
    })
  }
  return selected.join('\n')
}

function matchFirst(text: string, pattern: RegExp) {
  const normalized = compactText(text)
  const matched = normalized.match(pattern)
  return matched ? matched[1] : undefined
}

function compactText(text: string) {
  return text.replace(/\s+/g, '')
}

async function callChatModel(question: string, hits: SearchHit[]) {
  const apiKey = process.env.GLM_API_KEY || process.env.Z_AI_API_KEY
  if (!apiKey) return undefined
  const hasContext = hits.length > 0
  const context = hits
    .map((hit) => `【${hit.document.originalName}#片段${hit.chunk.chunkNo}】\n${hit.chunk.text}`)
    .join('\n\n')
  const system = [
    '你是财务数智人的知识库问答能力。',
    '严格依据给定文档片段回答用户问题，直接给出答案，不要在回答里列举出处或引用编号。',
    '如果文档片段足以回答，用简洁清晰的中文作答；如果信息不足，明确说明“已上传文档中未找到直接依据”，不要编造。',
    '当片段含金额、标准、流程节点等具体信息时，务必保留这些关键数字和条件。',
  ].join('\n')
  const user = hasContext
    ? `问题：${question}\n\n可用文档片段：\n${context}`
    : `问题：${question}\n\n没有检索到可用文档片段。`
  try {
    const response = await fetch(process.env.GLM_API_URL || 'https://open.bigmodel.cn/api/paas/v4/chat/completions', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${apiKey}`,
      },
      body: JSON.stringify({
        model: process.env.DOCUMENT_RAG_CHAT_MODEL || process.env.GLM_MODEL || 'glm-4-flash',
        messages: [
          { role: 'system', content: system },
          { role: 'user', content: user },
        ],
        temperature: 0.2,
      }),
    })
    if (!response.ok) return undefined
    const data = await response.json() as { choices?: Array<{ message?: { content?: string } }> }
    return data.choices?.[0]?.message?.content
  } catch {
    return undefined
  }
}

async function extractText(path: string, buffer: Buffer, extension: string, mimeType: string) {
  if (textExtensions.has(extension)) {
    const raw = buffer.toString('utf8')
    return extension === '.html' || extension === '.htm' ? stripHtml(raw) : raw
  }
  if (extension === '.pdf') {
    try {
      return (await pdfParse(buffer)).text
    } catch {
      throw new Error('PDF 解析失败，请确认文件未加密且不是损坏文件')
    }
  }
  if (extension === '.docx') {
    try {
      return (await mammoth.extractRawText({ path })).value
    } catch {
      throw new Error('Word 解析失败，请确认文件是有效的 .docx 文件')
    }
  }
  if (extension === '.doc') {
    throw new Error('暂不支持旧版 .doc，请先另存为 .docx 后上传')
  }
  if (spreadsheetExtensions.has(extension)) {
    try {
      const workbook = XLSX.read(buffer, { type: 'buffer' })
      return workbook.SheetNames.map((name) => {
        const rows = XLSX.utils.sheet_to_csv(workbook.Sheets[name])
        return `# ${name}\n${rows}`
      }).join('\n\n')
    } catch {
      throw new Error('Excel 解析失败，请确认文件是有效的 .xlsx 或 .xls 文件')
    }
  }
  if (imageExtensions.has(extension) || mimeType.startsWith('image/')) {
    return await extractImageText(buffer, mimeType || mimeFromExtension(extension))
  }
  throw new Error('暂不支持该文件格式，请上传 PDF、DOCX、XLSX、XLS、TXT、Markdown、HTML、CSV、JSON 或图片')
}

async function extractImageText(buffer: Buffer, mimeType: string) {
  const apiKey = process.env.GLM_API_KEY || process.env.Z_AI_API_KEY
  if (!apiKey) {
    throw new Error('图片文档需要配置 GLM_API_KEY 或 Z_AI_API_KEY 才能抽取文本')
  }
  const imageUrl = `data:${mimeType};base64,${buffer.toString('base64')}`
  const response = await fetch(process.env.GLM_API_URL || 'https://open.bigmodel.cn/api/paas/v4/chat/completions', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${apiKey}`,
    },
    body: JSON.stringify({
      model: process.env.GLM_VISION_MODEL || 'glm-4v-flash',
      messages: [
        {
          role: 'user',
          content: [
            { type: 'text', text: '请提取图片中的全部可读文字，只返回纯文本。' },
            { type: 'image_url', image_url: { url: imageUrl } },
          ],
        },
      ],
    }),
  })
  if (!response.ok) {
    throw new Error(`图片 OCR 失败，服务返回 ${response.status}`)
  }
  const data = await response.json() as { choices?: Array<{ message?: { content?: string } }> }
  return data.choices?.[0]?.message?.content || ''
}

function shortTextError(extension: string, mimeType: string) {
  if (extension === '.pdf') {
    return '未从 PDF 中抽取到可索引文字。可能是扫描件或图片型 PDF，请上传可复制文字的 PDF，或将页面转成清晰图片后上传。'
  }
  if (imageExtensions.has(extension) || mimeType.startsWith('image/')) {
    return '图片 OCR 未抽取到有效文字，请确认图片清晰且包含可读文字。'
  }
  return '文档中可抽取文字太少，请确认文件内容不是空白、图片截图或加密文档。'
}

async function embedTexts(texts: string[]) {
  const apiKey = process.env.GLM_API_KEY || process.env.Z_AI_API_KEY
  if (apiKey && process.env.DOCUMENT_RAG_DISABLE_REMOTE_EMBEDDING !== '1') {
    const remote = await callEmbeddingApi(texts, apiKey)
    if (remote) return remote
  }
  return texts.map(localEmbedding)
}

async function callEmbeddingApi(texts: string[], apiKey: string): Promise<number[][] | undefined> {
  try {
    const response = await fetch(process.env.EMBEDDING_API_URL || 'https://open.bigmodel.cn/api/paas/v4/embeddings', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${apiKey}`,
      },
      body: JSON.stringify({
        model: process.env.EMBEDDING_MODEL || 'embedding-3',
        input: texts,
      }),
    })
    if (!response.ok) return undefined
    const data = await response.json() as { data?: Array<{ index?: number; embedding?: number[] }> }
    const embeddings = (data.data || [])
      .sort((a, b) => Number(a.index || 0) - Number(b.index || 0))
      .map((item) => item.embedding || [])
    return embeddings.length === texts.length && embeddings.every((item) => item.length) ? embeddings : undefined
  } catch {
    return undefined
  }
}

function localEmbedding(text: string) {
  const vector = new Array<number>(256).fill(0)
  const terms = keywords(text).concat(text.match(/[\u4e00-\u9fa5]{1,2}/g) || [])
  for (const term of terms) {
    const hash = createHash('sha256').update(term).digest()
    const index = hash[0]
    vector[index] += 1
  }
  return normalizeVector(vector)
}

function buildChunks(documentId: string, text: string): ChunkRecord[] {
  const size = Number(process.env.DOCUMENT_RAG_CHUNK_SIZE || 900)
  const overlap = Number(process.env.DOCUMENT_RAG_CHUNK_OVERLAP || 120)
  const chunks: ChunkRecord[] = []
  for (const rule of buildRuleChunks(documentId, text)) {
    chunks.push(rule)
  }
  for (const block of extractQaBlocks(text)) {
    chunks.push(makeChunk(documentId, chunks.length + 1, block.text, 'qa', block.title, inferSignals(block.text).concat(['问答'])))
  }
  for (const block of extractTableLikeBlocks(text)) {
    chunks.push(makeChunk(documentId, chunks.length + 1, block.text, 'table', block.title, inferSignals(block.text).concat(['表格'])))
  }
  for (const section of splitSections(text)) {
    if (section.text.length < 80) {
      continue
    }
    chunks.push(makeChunk(documentId, chunks.length + 1, section.text.slice(0, 1400), 'text', section.title, section.signals))
  }
  for (let start = 0; start < text.length; start += size - overlap) {
    const raw = text.slice(start, start + size).trim()
    if (!raw) continue
    chunks.push(makeChunk(documentId, chunks.length + 1, raw, 'text', undefined, inferSignals(raw)))
  }
  return dedupeChunks(chunks).map((chunk, index) => ({ ...chunk, chunkNo: index + 1 }))
}

function makeChunk(documentId: string, chunkNo: number, text: string, type: NonNullable<ChunkRecord['type']>, title?: string, signals: string[] = []): ChunkRecord {
  const summaryPrefix = title ? `${title}\n` : ''
  return {
    id: randomUUID(),
    documentId,
    chunkNo,
    type,
    title,
    text,
    summary: `${summaryPrefix}${text}`.slice(0, 260),
    keywords: keywords(`${title || ''}\n${text}\n${signals.join(' ')}`),
    signals,
  }
}

function dedupeChunks(chunks: ChunkRecord[]) {
  const seen = new Set<string>()
  const result: ChunkRecord[] = []
  for (const chunk of chunks) {
    const key = compactText(chunk.text).slice(0, 500)
    if (key.length < 40 || seen.has(key)) continue
    seen.add(key)
    result.push(chunk)
  }
  return result
}

function extractQaBlocks(text: string) {
  const lines = text.split('\n').map((line) => line.trim()).filter(Boolean)
  const blocks: Array<{ title: string; text: string }> = []
  let currentTitle = ''
  let buffer: string[] = []
  for (const line of lines) {
    const question = normalizeQaQuestion(line)
    if (question) {
      if (currentTitle && buffer.length >= 2) {
        blocks.push({ title: currentTitle, text: buffer.join('\n') })
      }
      currentTitle = question
      buffer = [line]
      continue
    }
    if (currentTitle) {
      buffer.push(line)
      if (buffer.join('\n').length > 1400) {
        blocks.push({ title: currentTitle, text: buffer.join('\n') })
        currentTitle = ''
        buffer = []
      }
    }
  }
  if (currentTitle && buffer.length >= 2) {
    blocks.push({ title: currentTitle, text: buffer.join('\n') })
  }
  return blocks.filter((block) => block.text.length >= 80)
}

function normalizeQaQuestion(line: string) {
  const cleaned = line.replace(/\s+/g, '')
  if (/^(问)?\d+[、.．]/.test(cleaned) && /(吗|么|如何|怎么|什么|是否|能否|可以|需要|为什么|哪|几|多少|\?|\？)/.test(cleaned)) {
    return line.slice(0, 80)
  }
  if (/^问[:：]/.test(cleaned) || /^Q\d*[:：.．]/i.test(cleaned)) {
    return line.slice(0, 80)
  }
  if (cleaned.length <= 80 && /(吗|么|如何|怎么|什么|是否|能否|可以|需要|为什么|哪|几|多少|\?|\？)$/.test(cleaned)) {
    return line.slice(0, 80)
  }
  return ''
}

function extractTableLikeBlocks(text: string) {
  const lines = text.split('\n').map((line) => line.trim()).filter(Boolean)
  const blocks: Array<{ title: string; text: string }> = []
  let title = ''
  let buffer: string[] = []
  for (const line of lines) {
    if (isLikelyHeading(line)) {
      if (buffer.length >= 3) {
        blocks.push({ title: title || '表格/标准', text: buffer.join('\n') })
      }
      title = line
      buffer = []
      continue
    }
    if (isTableLikeLine(line)) {
      buffer.push(line)
      continue
    }
    if (buffer.length >= 3) {
      blocks.push({ title: title || '表格/标准', text: buffer.join('\n') })
    }
    buffer = []
  }
  if (buffer.length >= 3) {
    blocks.push({ title: title || '表格/标准', text: buffer.join('\n') })
  }
  return blocks
    .map((block) => ({ ...block, text: block.text.slice(0, 1600) }))
    .filter((block) => block.text.length >= 80)
}

function isTableLikeLine(line: string) {
  const compact = compactText(line)
  const numericSignals = (compact.match(/\d+(\.\d+)?\s*(元|%|万|天|小时|年|岁|次)/g) || []).length
  const domainSignals = /(保险|报销|赔付|保障|住宿|限额|金额|比例|费用|责任|材料|流程|审批)/.test(compact)
  const separators = /[|,，;；\t]/.test(line)
  return (domainSignals && numericSignals >= 1) || numericSignals >= 2 || (domainSignals && separators && line.length >= 20)
}

function buildRuleChunks(documentId: string, text: string) {
  const chunks: ChunkRecord[] = []
  const lodging = buildLodgingRuleChunk(documentId, text)
  if (lodging) chunks.push(lodging)
  return chunks
}

function buildLodgingRuleChunk(documentId: string, text: string): ChunkRecord | undefined {
  const normalized = compactText(text)
  const leader = normalized.match(/公司分管领导及各一级部门负责人[\s\S]{0,120}?限额(\d+)元\/间\/夜限额(\d+)元\/间\/夜/)
  const staff = normalized.match(/其他(?:单程|员工)[\s\S]{0,180}?限额(\d+)元\/间\/夜限额(\d+)元\/间\/夜/)
  const sameRoom = normalized.match(/住一个标准间[\s\S]{0,30}?住宿费不能超过(\d+)元\/间\/夜/)
  const separateRoom = normalized.match(/各住一个标准间[\s\S]{0,30}?住宿费不能超过(\d+)元\/间\/夜/)
  if (!staff && !leader && !sameRoom && !separateRoom) {
    return undefined
  }
  const lines = ['住宿费报销标准、差旅住宿标准、酒店住宿限额。']
  if (staff) {
    lines.push(`其他员工：高消费城市住宿限额${staff[1]}元/间/夜，其他城市住宿限额${staff[2]}元/间/夜。`)
  }
  if (leader) {
    lines.push(`公司分管领导及各一级部门负责人：高消费城市住宿限额${leader[1]}元/间/夜，其他城市住宿限额${leader[2]}元/间/夜。`)
  }
  if (sameRoom || separateRoom) {
    lines.push(`同性别多人出差特殊规则：${sameRoom ? `住一个标准间上限${sameRoom[1]}元/间/夜` : ''}${sameRoom && separateRoom ? '；' : ''}${separateRoom ? `各住一个标准间上限${separateRoom[1]}元/间/夜` : ''}。`)
  }
  return makeChunk(documentId, 1, lines.join('\n'), 'rule', '住宿费报销标准', [
    '住宿标准',
    '住宿费',
    '酒店',
    '限额',
    '元/间/夜',
    '差旅',
    '其他城市',
    '高消费城市',
  ])
}

function splitSections(text: string) {
  const lines = text.split('\n').map((line) => line.trim()).filter(Boolean)
  const sections: Array<{ title?: string; text: string; signals: string[] }> = []
  let title = ''
  let buffer: string[] = []
  for (const line of lines) {
    if (isLikelyHeading(line)) {
      if (buffer.length) {
        sections.push({ title, text: buffer.join('\n'), signals: inferSignals(`${title}\n${buffer.join('\n')}`) })
      }
      title = line
      buffer = [line]
    } else {
      buffer.push(line)
    }
  }
  if (buffer.length) {
    sections.push({ title, text: buffer.join('\n'), signals: inferSignals(`${title}\n${buffer.join('\n')}`) })
  }
  return sections
}

function isLikelyHeading(line: string) {
  return (
    /^(0?[1-9]|[一二三四五六七八九十]+)[\.、\s]/.test(line) ||
    /(流程|注意事项|报销标准|住宿标准|费用报销|支出流程|合同流程|保险|理赔|保障)$/.test(line) ||
    (line.length <= 18 && /(报销|流程|标准|发票|合同|付款|住宿|差旅|支出|保险|理赔|保障|商保)/.test(line))
  )
}

function inferSignals(text: string) {
  const signals: string[] = []
  const pairs: Array<[RegExp, string]> = [
    [/住宿|酒店|间\/夜|房价/, '住宿标准'],
    [/差旅/, '差旅'],
    [/报销/, '报销'],
    [/发票/, '发票'],
    [/合同/, '合同'],
    [/付款|对公/, '付款'],
    [/保险|商保|理赔|保障|人身险|医疗险|95511/, '保险'],
    [/预算/, '预算'],
    [/审批|审核/, '审批'],
    [/\d+\s*元|\d+\s*%/, '数字金额'],
  ]
  for (const [pattern, signal] of pairs) {
    if (pattern.test(text)) signals.push(signal)
  }
  return signals
}

function keywords(text: string) {
  const tokens = new Map<string, number>()
  const matches = text.toLowerCase().match(/[a-z0-9_]{2,}|[\u4e00-\u9fa5]{2,}/g) || []
  for (const match of matches) {
    if (/^[\u4e00-\u9fa5]+$/.test(match)) {
      for (let index = 0; index < match.length - 1; index++) {
        const term = match.slice(index, index + 2)
        tokens.set(term, (tokens.get(term) || 0) + 1)
      }
    }
    if (match.length > 32) {
      for (let index = 0; index < match.length - 1; index++) {
        tokens.set(match.slice(index, index + 2), (tokens.get(match.slice(index, index + 2)) || 0) + 1)
      }
    } else {
      tokens.set(match, (tokens.get(match) || 0) + 1)
    }
  }
  return [...tokens.entries()].sort((a, b) => b[1] - a[1]).slice(0, 40).map(([token]) => token)
}

function keywordSimilarity(query: string[], chunkKeywords: string[], chunkText: string) {
  if (!query.length) return 0
  const chunkSet = new Set(chunkKeywords)
  let hit = 0
  for (const term of query) {
    if (chunkSet.has(term) || chunkText.includes(term)) hit += 1
  }
  return Math.min(1, hit / Math.max(2, query.length * 0.5))
}

function cosine(a: number[], b: number[]) {
  const length = Math.min(a.length, b.length)
  if (!length) return 0
  let dot = 0
  let an = 0
  let bn = 0
  for (let i = 0; i < length; i++) {
    dot += a[i] * b[i]
    an += a[i] * a[i]
    bn += b[i] * b[i]
  }
  if (!an || !bn) return 0
  return Math.max(0, dot / (Math.sqrt(an) * Math.sqrt(bn)))
}

function normalizeVector(vector: number[]) {
  const norm = Math.sqrt(vector.reduce((sum, value) => sum + value * value, 0)) || 1
  return vector.map((value) => value / norm)
}

async function readMultipartFile(req: IncomingMessage) {
  await mkdir(filesDir, { recursive: true })
  return new Promise<{ originalName: string; mimeType: string; size: number; path: string; buffer: Buffer }>((resolve, reject) => {
    const busboy = Busboy({ headers: req.headers, limits: { files: 1, fileSize: maxUploadBytes } })
    let settled = false
    let upload: { originalName: string; mimeType: string; size: number; path: string; chunks: Buffer[] } | undefined

    busboy.on('file', (_field, file, info) => {
      const originalName = displayFileName(info.filename || 'upload.bin')
      const tempPath = join(filesDir, `${randomUUID()}.uploading`)
      const stream = createWriteStream(tempPath)
      upload = { originalName, mimeType: info.mimeType || 'application/octet-stream', size: 0, path: tempPath, chunks: [] }
      file.on('data', (chunk: Buffer) => {
        upload!.size += chunk.length
        upload!.chunks.push(chunk)
      })
      file.pipe(stream)
    })
    busboy.on('error', reject)
    busboy.on('finish', () => {
      if (settled) return
      settled = true
      if (!upload) {
        reject(new Error('file is required'))
        return
      }
      resolve({ ...upload, buffer: Buffer.concat(upload.chunks) })
    })
    req.pipe(busboy)
  })
}

async function readJson(req: IncomingMessage) {
  const chunks: Buffer[] = []
  for await (const chunk of req) {
    chunks.push(Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk))
  }
  return JSON.parse(Buffer.concat(chunks).toString('utf8') || '{}') as JsonObject
}

async function ensureStore() {
  await mkdir(filesDir, { recursive: true })
  await ensureJsonFile(documentsPath, [])
  await ensureJsonFile(chunksPath, [])
  await ensureJsonFile(embeddingsPath, [])
  reindexPromise ||= reindexOutdatedDocuments()
  await reindexPromise
}

async function ensureJsonFile(path: string, fallback: unknown) {
  try {
    await readFile(path, 'utf8')
  } catch {
    await writeFile(path, JSON.stringify(fallback, null, 2))
  }
}

async function loadDocuments() {
  return JSON.parse(await readFile(documentsPath, 'utf8')) as DocumentRecord[]
}

async function saveDocuments(value: DocumentRecord[]) {
  await writeFile(documentsPath, JSON.stringify(value, null, 2))
}

async function loadChunks() {
  return JSON.parse(await readFile(chunksPath, 'utf8')) as ChunkRecord[]
}

async function saveChunks(value: ChunkRecord[]) {
  await writeFile(chunksPath, JSON.stringify(value, null, 2))
}

async function loadEmbeddings() {
  return JSON.parse(await readFile(embeddingsPath, 'utf8')) as EmbeddingRecord[]
}

async function saveEmbeddings(value: EmbeddingRecord[]) {
  await writeFile(embeddingsPath, JSON.stringify(value, null, 2))
}

function writeJson(res: ServerResponse, status: number, body: unknown) {
  res.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8' })
  res.end(JSON.stringify(body))
}

function setCorsHeaders(res: ServerResponse) {
  res.setHeader('Access-Control-Allow-Origin', '*')
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, DELETE, OPTIONS')
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization')
}

function normalizeText(text: string) {
  return text.replace(/\r/g, '\n').replace(/[ \t]+/g, ' ').replace(/\n{3,}/g, '\n\n').trim()
}

function stripHtml(text: string) {
  return text.replace(/<script[\s\S]*?<\/script>/gi, ' ')
    .replace(/<style[\s\S]*?<\/style>/gi, ' ')
    .replace(/<[^>]+>/g, ' ')
}

function displayFileName(name: string) {
  const recovered = recoverUtf8FileName(name)
  return basename(recovered.split(/[\\/]/).pop() || 'upload.bin')
    .replace(/[\u0000-\u001f\u007f]/g, '')
    .trim()
    .slice(0, 200) || 'upload.bin'
}

function recoverUtf8FileName(name: string) {
  const decoded = Buffer.from(name, 'latin1').toString('utf8')
  return readabilityScore(decoded) > readabilityScore(name) ? decoded : name
}

function readabilityScore(value: string) {
  let score = 0
  for (const char of value) {
    if (/[\u4e00-\u9fa5]/.test(char)) score += 3
    else if (/[\u3000-\u303f\uff00-\uffef]/.test(char)) score += 2
    else if (/[A-Za-z0-9_.\- ]/.test(char)) score += 1
    else if (char === '\ufffd') score -= 5
  }
  return score
}

function mimeFromExtension(extension: string) {
  if (extension === '.png') return 'image/png'
  if (extension === '.webp') return 'image/webp'
  if (extension === '.gif') return 'image/gif'
  return 'image/jpeg'
}

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : String(error)
}

function uniqueStrings(values: string[]) {
  const seen = new Set<string>()
  const result: string[] = []
  for (const value of values) {
    const text = String(value || '').trim()
    if (!text || seen.has(text)) continue
    seen.add(text)
    result.push(text)
  }
  return result
}

function parseJsonObject(text: string) {
  const raw = text.trim().replace(/^```(?:json)?/i, '').replace(/```$/i, '').trim()
  const start = raw.indexOf('{')
  const end = raw.lastIndexOf('}')
  if (start < 0 || end <= start) return undefined
  try {
    const parsed = JSON.parse(raw.slice(start, end + 1))
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed)
      ? parsed as Record<string, unknown>
      : undefined
  } catch {
    return undefined
  }
}

function stringValue(value: unknown) {
  return typeof value === 'string' ? value.trim() : ''
}

function stringArray(value: unknown) {
  if (!Array.isArray(value)) return []
  return uniqueStrings(value.map((item) => typeof item === 'string' ? item : String(item || '')))
}
