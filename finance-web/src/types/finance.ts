export type MessageRole = 'user' | 'assistant'
export type MessageType = 'text' | 'table' | 'chart' | 'card' | 'file'

export interface ChatMessage {
  id: string
  role: MessageRole
  type: MessageType
  text?: string
  content?: unknown
  callChain?: ToolAuditLog[]
  createdAt: number
}

export interface ToolAuditLog {
  id: string
  sessionId?: string
  toolName: string
  backendToolName?: string
  serviceName?: string
  startedAt: string
  durationMs: number
  status: 'success' | 'error'
  arguments?: Record<string, unknown>
  error?: string
}

export interface McpToolResult<T> {
  code: number
  message: string
  data: T
}

export interface ArDashboard {
  month: string
  receivableAmount: number
  collectedAmount: number
  overdueAmount: number
  collectionRate: number
  agingBuckets: Array<{ bucket: string; amount: number }>
  advices: string[]
}

export interface CollectionPlanResult {
  planNo: string
  period: string
  customerName: string
  creditLevel: string
  overdueAmount: number
  overdueDays: number
  collectionStrategy: string
  escalationLevel: string
  freezeCreditSuggestion: string
  collectionLetterDraft: string
  actionItems: Array<{
    actionDate: string
    actionType: string
    owner: string
    content: string
    status: string
  }>
  advices: string[]
}

export interface PaymentPlanResult {
  planNo: string
  period: string
  supplierName: string
  payableAmount: number
  recommendedPayDate: string
  paymentStrategy: string
  discountBenefit: number
  cashAfterPayment: number
  liquidityImpact: string
  mergePaymentRecommended: boolean
  scheduleItems: string[]
  riskHints: string[]
  nextActions: string[]
}

export interface CounterpartyReconcileResult {
  reconcileNo: string
  period: string
  counterpartyName: string
  counterpartyType: string
  internalBalance: number
  counterpartyBalance: number
  differenceAmount: number
  reconcileStatus: string
  riskLevel: string
  confirmationStatus: string
  confirmationLetterDraft?: string
  differenceItems: Array<{
    itemNo: string
    differenceType: string
    amount: number
    reason: string
    owner: string
    suggestion: string
  }>
  advices: string[]
  nextActions: string[]
}

export interface VoucherResult {
  voucherNo: string
  voucherDate: string
  status: string
  debitTotal: number
  creditTotal: number
  auditMessages: string[]
}

export interface AccountMappingRecommendationResult {
  mappingNo: string
  documentType: string
  businessScenario: string
  summary: string
  recommendationMode: string
  confidence: number
  autoApplicable: boolean
  manualReviewRequired: boolean
  selectedAccountCode: string
  selectedAccountName: string
  selectedTaxAccountCode?: string
  selectedTaxAccountName?: string
  payableOrReceivableAccountCode: string
  payableOrReceivableAccountName: string
  candidates: Array<{
    accountCode: string
    accountName: string
    accountType: string
    confidence: number
    reason: string
  }>
  voucherPreview: Array<{
    accountCode: string
    accountName: string
    debitAmount: number
    creditAmount: number
    explanation: string
  }>
  evidence: string[]
  riskHints: string[]
  nextActions: string[]
}

export interface VoucherAuditResult {
  voucherNo: string
  voucherDate: string
  auditStatus: string
  overallSeverity: string
  debitTotal: number
  creditTotal: number
  redCount: number
  yellowCount: number
  blueCount: number
  auditItems: Array<{
    itemId: string
    category: string
    severity: string
    title: string
    description: string
    suggestion: string
  }>
  advices: string[]
  nextActions: string[]
}

export interface InvoiceRow {
  invoiceNo: string
  invoiceDate: string
  buyerName: string
  sellerName: string
  amount: number
  taxAmount: number
  verifyStatus: string
}

export interface InvoiceVerificationResult {
  invoiceNo: string
  invoiceType: string
  verifyStatus: string
  taxAuthorityStatus: string
  taxAuthorityMatched: boolean
  duplicate: boolean
  duplicateRefs: string[]
  serialRisk: boolean
  deductible: boolean
  deductibleTaxAmount: number
  archiveStatus: string
  riskLevel: string
  riskHints: string[]
  nextActions: string[]
}

export interface PageResult<T> {
  total: number
  pageNo: number
  pageSize: number
  rows: T[]
}

export interface ReportResult {
  reportType: string
  period: string
  rows: Array<{ item: string; currentAmount: number; previousAmount: number }>
  aiComment: string
}

export interface AnalysisResult {
  period: string
  metrics: Array<{ name: string; value: number; unit: string; evaluation: string }>
  anomalies: string[]
  advices: string[]
}

export interface VarianceDiagnosisResult {
  diagnosisNo: string
  period: string
  metricName: string
  department: string
  actualAmount: number
  budgetAmount: number
  previousAmount: number
  budgetVariance: number
  budgetVarianceRate: number
  momVariance: number
  momVarianceRate: number
  severity: string
  conclusion: string
  drivers: Array<{
    driverName: string
    impactAmount: number
    contributionRate: number
    owner: string
    explanation: string
  }>
  evidence: string[]
  actionPlan: string[]
  followUpQuestions: string[]
}

export interface BudgetControlResult {
  budgetNo: string
  period: string
  department: string
  projectCode: string
  expenseType: string
  annualBudget: number
  periodBudget: number
  actualUsed: number
  committedAmount: number
  requestedAmount: number
  availableBudget: number
  executionRate: number
  forecastAmount: number
  forecastVariance: number
  controlStatus: string
  riskLevel: string
  approvalSuggestion: string
  controlItems: Array<{
    itemCode: string
    itemName: string
    status: string
    conclusion: string
    suggestion: string
  }>
  warnings: string[]
  nextActions: string[]
}

export interface MonthEndCloseResult {
  closeNo: string
  period: string
  entityName: string
  closeType: string
  closeStatus: string
  progressRate: number
  totalTaskCount: number
  completedTaskCount: number
  blockerCount: number
  warningCount: number
  readyToClose: boolean
  estimatedCloseDate: string
  checklist: Array<{
    module: string
    itemCode: string
    itemName: string
    status: string
    owner: string
    dueDate: string
    evidence: string
    suggestion: string
  }>
  blockers: string[]
  warnings: string[]
  nextActions: string[]
}

export interface ExpenseRow {
  expenseNo: string
  employeeId: string
  employeeName: string
  department?: string
  projectCode?: string
  expenseType?: string
  expenseDate: string
  amount: number
  status: string
  description?: string
  invoiceCount?: number
  attachmentCount?: number
  riskHint: string
}

export interface RuleCitation {
  documentName: string
  chunkNo?: number
  text: string
  score?: number
}

export interface ExpenseApprovalResult {
  expenseNo: string
  approvalStatus: string
  riskLevel: string
  amount: number
  standardLimit: number
  budgetAfterSubmit: number
  autoApproved?: boolean
  autoApproveRequested?: boolean
  approvalOpinion?: string
  ruleAnswer?: string
  ruleRetrievalError?: string
  ruleCitations?: RuleCitation[]
  approvalRoute: Array<{
    nodeCode: string
    nodeName: string
    approver: string
    status: string
    reason: string
  }>
  riskItems: Array<{
    riskId: string
    severity: string
    title: string
    description: string
    suggestion: string
  }>
  advices: string[]
  nextActions: string[]
}

export interface TaxCalculationResult {
  taxType: string
  period: string
  taxableAmount: number
  taxRate: number
  deductibleAmount: number
  taxPayable: number
  calculationSteps: string[]
  riskHints: string[]
}

export interface TaxReturnResult {
  returnNo: string
  taxType: string
  period: string
  salesAmount: number
  outputTax: number
  inputTax: number
  taxPayable: number
  status: string
  rows: Array<{ item: string; amount: number; remark: string }>
  riskHints: string[]
}

export interface BankTransactionRow {
  transactionNo: string
  accountNo: string
  transactionDate: string
  counterparty: string
  summary: string
  debitAmount: number
  creditAmount: number
  balance: number
  status: string
  matchedVoucherNo?: string
  riskHint: string
}

export interface BankReconcileResult {
  accountNo: string
  period: string
  bankBalance: number
  ledgerBalance: number
  differenceAmount: number
  totalCount: number
  matchedCount: number
  unmatchedCount: number
  unmatchedTransactions: BankTransactionRow[]
  advices: string[]
}

export interface ComplianceRiskResult {
  period: string
  scenario: string
  overallLevel: string
  riskScore: number
  totalRiskCount: number
  highRiskCount: number
  riskItems: Array<{
    riskId: string
    module: string
    severity: string
    title: string
    description: string
    relatedDocNo: string
    amount: number
    owner: string
    dueDate: string
    status: string
    suggestion: string
  }>
  advices: string[]
  nextActions: string[]
}

export interface CashFlowForecastResult {
  startPeriod: string
  scenario: string
  currency: string
  currentCashBalance: number
  safetyCashLevel: number
  forecastEndingBalance: number
  lowestBalance: number
  liquidityLevel: string
  forecastRows: Array<{
    period: string
    openingBalance: number
    inflowAmount: number
    outflowAmount: number
    netCashFlow: number
    endingBalance: number
    warningLevel: string
    explanation: string
  }>
  accountPositions: Array<{
    accountNo: string
    accountName: string
    balance: number
    availableAmount: number
    status: string
    advice: string
  }>
  alerts: string[]
  transferAdvices: string[]
  financingSuggestions: string[]
  externalFactors: string[]
}

export interface ContractAssetResult {
  period: string
  scope: string
  activeContractCount: number
  dueMilestoneCount: number
  overdueMilestoneCount: number
  assetCount: number
  contractAmount: number
  monthlyDepreciation: number
  intangibleAmortization: number
  contracts: Array<{
    contractNo: string
    counterparty: string
    contractType: string
    amount: number
    startDate: string
    endDate: string
    status: string
    confidence: string
    riskHint: string
  }>
  milestones: Array<{
    milestoneNo: string
    contractNo: string
    milestoneType: string
    dueDate: string
    amount: number
    owner: string
    status: string
    reminderLevel: string
    action: string
  }>
  depreciationRows: Array<{
    assetNo: string
    assetName: string
    category: string
    department: string
    depreciationMethod: string
    originalValue: number
    accumulatedDepreciation: number
    monthlyDepreciation: number
    netBookValue: number
    voucherSuggestion: string
  }>
  inventoryExceptions: Array<{
    taskNo: string
    assetNo: string
    assetName: string
    location: string
    bookStatus: string
    actualStatus: string
    exceptionType: string
    handler: string
    action: string
  }>
  intangibleReminders: Array<{
    itemNo: string
    itemName: string
    itemType: string
    originalValue: number
    monthlyAmortization: number
    expiryDate: string
    reminder: string
  }>
  advices: string[]
}

export interface DataIntegrationResult {
  period: string
  systemType: string
  connectorCount: number
  healthyConnectorCount: number
  warningConnectorCount: number
  failedConnectorCount: number
  overallQualityScore: number
  totalRecords: number
  successRecords: number
  failedRecords: number
  connectors: Array<{
    connectorId: string
    systemType: string
    systemName: string
    adapterType: string
    syncMode: string
    lastSyncTime: string
    status: string
    records: number
    failedRecords: number
    latency: string
    message: string
  }>
  etlJobs: Array<{
    jobId: string
    jobName: string
    triggerType: string
    currentStep: string
    status: string
    progress: number
    checkpoint: string
    nextRunTime: string
    message: string
  }>
  qualityMetrics: Array<{
    dimension: string
    score: number
    threshold: number
    status: string
    issue: string
    action: string
  }>
  masterDataMappings: Array<{
    mappingType: string
    sourceSystem: string
    sourceCount: number
    mappedCount: number
    conflictCount: number
    matchRate: number
    strategy: string
  }>
  retryTasks: Array<{
    taskId: string
    connectorId: string
    failedStep: string
    reason: string
    retryCount: number
    nextRetryTime: string
    owner: string
  }>
  alerts: string[]
  advices: string[]
}
