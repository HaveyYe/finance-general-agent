# MCP Modules

MCP 相关模块统一放在本目录，避免根目录平铺过多服务。

本目录也是 Maven 聚合模块。Java MCP 服务由 `mcp/pom.xml` 纳入根工程统一构建；Node/TS MCP 服务保持各自的 `package.json` 管理。

## Modules

- `finance-mcp`: 财务 Dubbo MCP 服务，提供 45 个财务语义工具和 `dubbo_call` 泛化兜底。
- `zai-mcp-server`: 多模态 MCP 服务，提供设计文档定义的 8 个视觉、OCR、图表理解和 UI 分析工具。
- `web-reader-server`: 网页/政策读取 MCP 服务，提供 1 个工具，支持智谱远程 MCP、直接 HTTP/HTTPS 抓取和本地摘要降级。
- `integrated-browser-server`: Playwright 浏览器自动化 MCP 服务，提供设计文档定义的 22 个网页操作工具。
- `mcp-gateway`: MCP 统一入口，聚合共 76 个工具，按工具名路由并提供 `/agent/chat` 编排接口。

Gateway 默认使用确定性规则编排，配置 `AGENT_MODE=llm` 与 `GLM_API_KEY` 后启用智谱 GLM Function Calling 多轮工具调用。

完整实现状态和外部联调边界见根目录 `IMPLEMENTATION_STATUS.md`。

## Smoke Test

按下方顺序启动全部服务后，可从仓库根目录执行：

```bash
bash mcp/scripts/smoke-test.sh
bash mcp/scripts/browser-smoke-test.sh
bash mcp/scripts/web-reader-smoke-test.sh
bash mcp/scripts/finance-persistence-smoke-test.sh
```

脚本分别验证 Gateway 与 Agent 编排、22 个真实 Playwright 浏览器工具、真实网页读取回退，以及凭证/发票/报销通过 Dubbo 写入 JDBC 后可再次查询。

## Run Order

先在仓库根目录启动财务 Dubbo 服务：

```bash
mvn -q -DskipTests package
java -jar finance-service/finance-starter/target/finance-starter-0.0.1-SNAPSHOT.jar
```

再启动 MCP 服务：

```bash
mvn -pl mcp/finance-mcp spring-boot:run

cd mcp/zai-mcp-server
npm install
npm run dev

cd ../web-reader-server
npm install
npm run dev

cd ../integrated-browser-server
npm install
npm run dev

cd ../mcp-gateway
npm install
npm run dev
```
