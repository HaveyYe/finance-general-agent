#!/usr/bin/env bash
set -euo pipefail

browser_url="${BROWSER_MCP_URL:-http://localhost:8090/mcp}"
session_id="browser-smoke-$$"
html="<html><head><title>Browser MCP Smoke</title></head><body><input id='name'><select id='type'><option value='invoice'>Invoice</option><option value='expense'>Expense</option></select><input id='agree' type='checkbox'></body></html>"
page_url="data:text/html;base64,$(printf '%s' "${html}" | base64 | tr -d '\n')"

call_tool() {
  local name="$1"
  local args="$2"
  jq -nc --arg name "${name}" --argjson args "${args}" \
    '{jsonrpc:"2.0",id:1,method:"tools/call",params:{name:$name,arguments:$args}}' \
    | curl -fsS "${browser_url}" -H 'Content-Type: application/json' --data-binary @-
}

close_session() {
  call_tool close_session "$(jq -nc --arg sessionId "${session_id}" '{sessionId:$sessionId}')" >/dev/null 2>&1 || true
}
trap close_session EXIT

tools="$(curl -fsS "${browser_url}" -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}')"
jq -e '.result.tools | length == 22' <<<"${tools}" >/dev/null

call_tool navigate "$(jq -nc --arg sessionId "${session_id}" --arg url "${page_url}" '{sessionId:$sessionId,url:$url}')" >/dev/null
call_tool fill_form "$(jq -nc --arg sessionId "${session_id}" '{sessionId:$sessionId,fields:[{selector:"#name",value:"FinanceAgent"}]}')" >/dev/null
call_tool select_option "$(jq -nc --arg sessionId "${session_id}" '{sessionId:$sessionId,selector:"#type",value:"expense"}')" >/dev/null
call_tool check "$(jq -nc --arg sessionId "${session_id}" '{sessionId:$sessionId,selector:"#agree"}')" >/dev/null

expression='({name:document.querySelector("#name").value,type:document.querySelector("#type").value,agree:document.querySelector("#agree").checked})'
state="$(call_tool evaluate "$(jq -nc --arg sessionId "${session_id}" --arg expression "${expression}" '{sessionId:$sessionId,expression:$expression}')")"
jq -e '.result.isError == false
  and .result.structuredContent.result.result.name == "FinanceAgent"
  and .result.structuredContent.result.result.type == "expense"
  and .result.structuredContent.result.result.agree == true' <<<"${state}" >/dev/null

image="$(call_tool screenshot "$(jq -nc --arg sessionId "${session_id}" '{sessionId:$sessionId,fullPage:true}')")"
jq -e '.result.isError == false
  and .result.structuredContent.result.mimeType == "image/png"
  and (.result.structuredContent.result.dataUrl | startswith("data:image/png;base64,"))' <<<"${image}" >/dev/null

echo "Browser MCP smoke test passed: 22 tools and real Playwright form interaction."
