#!/usr/bin/env bash
set -euo pipefail

gateway_url="${MCP_GATEWAY_URL:-http://localhost:9000}"
expected_tools="${MCP_EXPECTED_TOOLS:-76}"

for command in curl jq; do
  if ! command -v "${command}" >/dev/null 2>&1; then
    echo "missing required command: ${command}" >&2
    exit 1
  fi
done

health="$(curl -fsS "${gateway_url}/health")"
jq -e '.ok == true' <<<"${health}" >/dev/null

tools="$(curl -fsS "${gateway_url}/mcp" \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}')"
tool_count="$(jq -r '.result.tools | length' <<<"${tools}")"
unavailable_count="$(jq -r '.result.unavailableServices | length' <<<"${tools}")"

if [[ "${tool_count}" -ne "${expected_tools}" ]]; then
  echo "unexpected tool count: expected ${expected_tools}, got ${tool_count}" >&2
  exit 1
fi
if [[ "${unavailable_count}" -ne 0 ]]; then
  jq '.result.unavailableServices' <<<"${tools}" >&2
  exit 1
fi

tool_response="$(curl -fsS "${gateway_url}/mcp" \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"query_ar_dashboard","arguments":{"month":"2026-05"}}}')"
jq -e '.error == null and .result.isError == false and .result.structuredContent.result.code == 200' \
  <<<"${tool_response}" >/dev/null

agent_response="$(curl -fsS "${gateway_url}/agent/chat" \
  -H 'Content-Type: application/json' \
  -d '{"message":"帮我查一下本月应收账款","clientContext":{"channel":"web","userId":"mcp-smoke-test","userName":"MCP Smoke Test"}}')"
jq -e '.toolCall.name == "query_ar_dashboard" and (.callChain | length) >= 1' \
  <<<"${agent_response}" >/dev/null

echo "MCP smoke test passed: ${tool_count} tools, ${unavailable_count} unavailable services."
