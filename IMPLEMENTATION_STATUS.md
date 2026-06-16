# Finance General Agent Implementation Status

## Module Boundary

MCP 能力已统一放在 `mcp/` 子目录：

- `mcp/finance-mcp`: Java/Spring Boot 财务 Dubbo MCP，45 个工具。
- `mcp/zai-mcp-server`: TypeScript 多模态 MCP，8 个工具。
- `mcp/web-reader-server`: TypeScript 网页读取 MCP，1 个工具。
- `mcp/integrated-browser-server`: TypeScript + Playwright 浏览器 MCP，22 个工具。
- `mcp/mcp-gateway`: TypeScript MCP Gateway，聚合 76 个工具并提供 Agent Chat/SSE。
- `mcp/scripts`: Gateway、浏览器、网页读取、财务持久化冒烟测试。

## Implemented

- 财务 Dubbo Provider 按 `finance-api`、`finance-core`、`finance-infra`、`finance-starter` 分层。
- Gateway 支持 MCP 聚合、静态路由、服务发现、降级、一次重试、会话上下文、调用链审计和可选 GLM Function Calling。
- 凭证、发票、报销、预算、银行流水、应收应付和审计日志通过 JDBC 持久化或查询。
- 资产负债表、利润表和现金流量表通过 SQL 聚合生成。
- 报销审批结果会回写报销单状态。
- web-reader 支持智谱远程 MCP、真实 HTTP/HTTPS 抓取和本地摘要降级。
- integrated-browser-server 使用真实 Playwright 页面和会话。
- Vue 3 前端包含对话、驾驶舱、发票、报表、SSE、历史消息、复制/重新生成、文件上传、语音输入、图表和钉钉入口上下文。

## Verified

```bash
mvn -q test
mvn -q -DskipTests package
cd finance-web && npm run build

bash mcp/scripts/smoke-test.sh
bash mcp/scripts/browser-smoke-test.sh
bash mcp/scripts/web-reader-smoke-test.sh
bash mcp/scripts/finance-persistence-smoke-test.sh
```

最近一次运行结果：

- Gateway 聚合 76 个工具，0 个不可用服务。
- 22 个 Playwright 浏览器工具通过真实页面交互测试。
- web-reader 通过真实 HTTP 页面读取和 Markdown 转换测试。
- 凭证、发票、报销创建与查询、报销审批状态回写通过 JDBC 持久化测试。
- OCR 到凭证、三大报表、报销创建、银行流水自然语言编排通过运行时测试。

## External Integration Boundary

以下能力已提供代码入口和降级逻辑，但需要企业环境信息才能做真实联调：

- 用实际 Dubbo 接口地址、接口全限定名和 DTO 替换当前财务 Demo Provider。
- 配置 `GLM_API_KEY` 验证真实 LLM Function Calling。
- 配置 `Z_AI_API_KEY` 验证真实多模态和智谱 web-reader。
- 接入钉钉服务端授权码换取用户身份。
- 在生产 MySQL、Redis 环境验证 `production` profile。

当前数据访问实现使用 Spring JDBC，而非设计文档建议的 MyBatis-Plus；接口边界和数据库表结构不受此实现差异影响。
