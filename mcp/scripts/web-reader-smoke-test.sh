#!/usr/bin/env bash
set -euo pipefail

web_reader_url="${WEB_READER_MCP_URL:-http://localhost:8089/mcp}"
fixture_dir="$(mktemp -d)"
fixture_port="${WEB_READER_FIXTURE_PORT:-18089}"
fixture_pid=""

cleanup() {
  if [[ -n "${fixture_pid}" ]]; then
    kill "${fixture_pid}" >/dev/null 2>&1 || true
    wait "${fixture_pid}" >/dev/null 2>&1 || true
  fi
  rm -rf "${fixture_dir}"
}
trap cleanup EXIT

for command in curl jq python3; do
  if ! command -v "${command}" >/dev/null 2>&1; then
    echo "missing required command: ${command}" >&2
    exit 1
  fi
done

cat >"${fixture_dir}/policy.html" <<'HTML'
<!doctype html>
<html lang="zh-CN">
  <head><title>差旅报销测试政策</title></head>
  <body>
    <h1>差旅报销标准</h1>
    <p>住宿费报销标准为每日 500 元。</p>
    <p>增值税适用税率示例为 6%。</p>
  </body>
</html>
HTML

python3 -m http.server "${fixture_port}" --bind 127.0.0.1 --directory "${fixture_dir}" >/dev/null 2>&1 &
fixture_pid="$!"

for _ in {1..20}; do
  curl -fsS "http://127.0.0.1:${fixture_port}/policy.html" >/dev/null 2>&1 && break
  sleep 0.1
done

response="$(curl -fsS "${web_reader_url}" \
  -H 'Content-Type: application/json' \
  -d "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"webReader\",\"arguments\":{\"url\":\"http://127.0.0.1:${fixture_port}/policy.html\",\"keyword\":\"差旅报销\"}}}")"

jq -e '.result.isError == false
  and .result.structuredContent.result.title == "差旅报销测试政策"
  and .result.structuredContent.result.extracted.policyType == "网页直接抓取内容"
  and (.result.structuredContent.result.markdown | contains("住宿费报销标准为每日 500 元"))
  and (.result.structuredContent.result.extracted.standardRates | index("6%")) != null' <<<"${response}" >/dev/null

echo "Web Reader MCP smoke test passed: real HTTP page fetched and converted to structured Markdown."
