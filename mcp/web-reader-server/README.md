# web-reader-server

网页/政策读取 MCP 服务，用于财务 Agent 的税务政策查询协同场景。

读取顺序：

1. 配置 `Z_AI_API_KEY` 时优先调用智谱远程 web-reader MCP。
2. 远程服务不可用时，直接通过 HTTP/HTTPS 抓取目标网页并转换为轻量 Markdown。
3. 真实网页读取仍失败时，回退到本地政策摘要，保证联调链路可用。

## Run

```bash
npm install
npm run dev
```

默认地址：

```text
http://localhost:8089/mcp
```

## Test

```bash
curl -s http://localhost:8089/mcp \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}'
```

```bash
curl -s http://localhost:8089/mcp \
  -H 'Content-Type: application/json' \
  -d @examples/web-reader-tax.sample.json
```

从仓库根目录验证真实网页读取回退：

```bash
bash mcp/scripts/web-reader-smoke-test.sh
```
