#!/usr/bin/env bash
set -euo pipefail

finance_url="${FINANCE_MCP_URL:-http://localhost:8086/mcp}"
run_id="$(date +%s)-$$"

call_tool() {
  local name="$1"
  local args="$2"
  jq -nc --arg name "${name}" --argjson args "${args}" \
    '{jsonrpc:"2.0",id:1,method:"tools/call",params:{name:$name,arguments:$args}}' \
    | curl -fsS "${finance_url}" -H 'Content-Type: application/json' --data-binary @- \
    | jq -r '.result.content[0].text' \
    | jq -c .
}

voucher="$(call_tool create_voucher '{
  "voucherDate":"2026-06-04",
  "summary":"JDBC persistence smoke test",
  "entries":[
    {"accountCode":"1002","accountName":"银行存款","debitAmount":100,"creditAmount":0},
    {"accountCode":"1122","accountName":"应收账款","debitAmount":0,"creditAmount":100}
  ]
}')"
voucher_no="$(jq -r '.data.voucherNo' <<<"${voucher}")"
voucher_read="$(call_tool get_voucher "$(jq -nc --arg voucherNo "${voucher_no}" '{voucherNo:$voucherNo}')")"
jq -e '.code == 200 and .data.debitTotal == .data.creditTotal and (.data.entries | length) == 2' <<<"${voucher_read}" >/dev/null

invoice_no="INV-SMOKE-${run_id}"
seller_name="MCP测试供应商-${run_id}"
call_tool input_invoice "$(jq -nc --arg invoiceNo "${invoice_no}" --arg sellerName "${seller_name}" '{
  invoiceCode:("CODE-" + $invoiceNo),
  invoiceNo:$invoiceNo,
  invoiceDate:"2026-06-04",
  invoiceType:"增值税专用发票",
  buyerName:"杭州云启科技有限公司",
  sellerName:$sellerName,
  amount:100,
  taxAmount:13,
  source:"MCP_SMOKE",
  fileHash:("HASH-" + $invoiceNo),
  autoVerify:true
}')" >/dev/null
invoice_read="$(call_tool query_invoice "$(jq -nc --arg invoiceNo "${invoice_no}" '{invoiceNo:$invoiceNo}')")"
jq -e --arg invoiceNo "${invoice_no}" '.code == 200 and .data.total == 1 and .data.rows[0].invoiceNo == $invoiceNo' <<<"${invoice_read}" >/dev/null

employee_id="SMOKE-${run_id}"
expense="$(call_tool create_expense "$(jq -nc --arg employeeId "${employee_id}" '{
  employeeId:$employeeId,
  employeeName:"持久化测试员",
  department:"财务部",
  expenseType:"办公费",
  expenseDate:"2026-06-04",
  description:"JDBC persistence smoke test",
  amount:100,
  invoiceNos:["SMOKE-INVOICE"],
  attachments:["SMOKE-ATTACHMENT"],
  submitForApproval:true
}')")"
expense_no="$(jq -r '.data.expenseNo' <<<"${expense}")"
expense_read="$(call_tool query_expense "$(jq -nc --arg employeeId "${employee_id}" '{employeeId:$employeeId,status:"PENDING"}')")"
jq -e --arg expenseNo "${expense_no}" '.code == 200 and (.data.rows | map(.expenseNo) | index($expenseNo)) != null' <<<"${expense_read}" >/dev/null

approval="$(call_tool approve_expense "$(jq -nc --arg expenseNo "${expense_no}" --arg employeeId "${employee_id}" '{
  expenseNo:$expenseNo,
  employeeId:$employeeId,
  employeeName:"持久化测试员",
  department:"财务部",
  expenseType:"办公费",
  invoiceNo:"SMOKE-INVOICE",
  description:"JDBC approval status smoke test",
  amount:100,
  invoiceAmount:100,
  availableBudget:1000,
  invoiceVerified:true,
  duplicateInvoice:false
}')")"
jq -e '.code == 200 and .data.approvalStatus == "APPROVED_ROUTE_READY"' <<<"${approval}" >/dev/null
expense_after_approval="$(call_tool query_expense "$(jq -nc --arg employeeId "${employee_id}" '{employeeId:$employeeId,status:"APPROVED_ROUTE_READY"}')")"
jq -e --arg expenseNo "${expense_no}" '.code == 200 and (.data.rows | map(.expenseNo) | index($expenseNo)) != null' <<<"${expense_after_approval}" >/dev/null

echo "Finance JDBC persistence smoke test passed: ${voucher_no}, ${invoice_no}, ${expense_no}."
