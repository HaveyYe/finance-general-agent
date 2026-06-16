import axios from 'axios'
import type { ChatResponse, ClientContext } from '@/api/agent'
import type {
  AnalysisResult,
  ArDashboard,
  ExpenseApprovalResult,
  ExpenseRow,
  InvoiceRow,
  McpToolResult,
  PageResult,
  ReportResult,
  VoucherResult,
} from '@/types/finance'

interface JsonRpcResponse<T> {
  jsonrpc: string
  id: number
  result?: {
    structuredContent: {
      result: McpToolResult<T> | {
        success?: boolean
        message?: string
        data?: McpToolResult<T>
        raw?: McpToolResult<T>
      }
    }
    isError: boolean
  }
  error?: {
    code: number
    message: string
    data?: string
  } | null
}

let nextId = 3000

async function callTool<T>(name: string, args: Record<string, unknown>): Promise<McpToolResult<T>> {
  const { data } = await axios.post<JsonRpcResponse<T>>('/mcp', {
    jsonrpc: '2.0',
    id: nextId++,
    method: 'tools/call',
    params: {
      name,
      arguments: args,
    },
  })

  if (data.error) {
    throw new Error(data.error.data || data.error.message)
  }
  const result = normalizeToolResult<T>(data.result?.structuredContent.result)
  if (!result) {
    throw new Error('MCP 返回为空')
  }
  return result
}

function normalizeToolResult<T>(result?: JsonRpcResponse<T>['result']['structuredContent']['result']): McpToolResult<T> | undefined {
  if (!result) return undefined
  if ('code' in result && typeof result.code === 'number') {
    return result as McpToolResult<T>
  }
  if ('data' in result && result.data && typeof result.data === 'object' && 'code' in result.data) {
    return result.data as McpToolResult<T>
  }
  if ('raw' in result && result.raw && typeof result.raw === 'object' && 'code' in result.raw) {
    return result.raw as McpToolResult<T>
  }
  return {
    code: result.success === false ? 500 : 200,
    message: result.message || '查询成功',
    data: result.data as T,
  }
}

export const financeApi = {
  queryArDashboard(month: string) {
    return callTool<ArDashboard>('query_ar_dashboard', { month })
  },
  createVoucher(payload?: Record<string, unknown>) {
    return callTool<VoucherResult>('create_voucher', payload || {
      voucherDate: '2026-05-31',
      summary: '销售回款入账',
      entries: [
        { accountCode: '1002', accountName: '银行存款', debitAmount: 120000, creditAmount: 0 },
        { accountCode: '1122', accountName: '应收账款', debitAmount: 0, creditAmount: 120000 },
      ],
    })
  },
  getVoucher(voucherNo: string) {
    return callTool<VoucherResult>('get_voucher', { voucherNo })
  },
  queryVouchers(args: Record<string, unknown> = {}) {
    return callTool<PageResult<VoucherResult>>('query_vouchers', args)
  },
  queryInvoice(invoiceNo = 'INV-202605') {
    return callTool<PageResult<InvoiceRow>>('query_invoice', invoiceNo ? { invoiceNo } : {})
  },
  queryInvoices(args: Record<string, unknown> = {}) {
    return callTool<PageResult<InvoiceRow>>('query_invoice', args)
  },
  generateReport(reportType = 'balance_sheet', period = '2026-05') {
    return callTool<ReportResult>('generate_report', { reportType, period })
  },
  analyzeFinancial(period = '2026-05') {
    return callTool<AnalysisResult>('analyze_financial', {
      period,
      metrics: ['流动比率', '回款率'],
    })
  },
  queryExpense(status = '') {
    return callTool<PageResult<ExpenseRow>>('query_expense', status ? { status } : {})
  },
  createExpense(payload: Record<string, unknown>) {
    return callTool<ExpenseRow>('create_expense', payload)
  },
  async approveExpense(payload: Record<string, unknown>, clientContext?: ClientContext) {
    const { data } = await axios.post<ChatResponse & { content?: ExpenseApprovalResult }>('/agent/chat', {
      message: `审批报销单 ${payload.expenseNo || ''}${payload.autoApproveEnabled ? '，开关打开，低风险自动通过' : ''}`,
      expenseApprovalArgs: payload,
      clientContext,
    }, { timeout: 90000 })
    return data
  },
}
