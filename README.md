# finance-general-agent

财务通用 Agent 工作区，按模块组织代码。

## Modules

- `finance-service`: Demo 财务 Dubbo 服务模块，提供应收、凭证、发票、报表、分析、报销和审计日志接口。
- `finance-service` 已包含应收应付 Demo：应收账龄看板、智能催收计划、催收函草稿、应付付款计划优化和往来余额对账。
- `finance-service` 已实现设计文档标准 `ArApService`：应收/应付账龄分页、按客户催收建议和按往来单位标准对账。
- `finance-service` 已包含科目映射 Demo：规则优先、AI兜底、Top-3推荐科目、置信度和凭证预览。
- `finance-service` 已包含凭证台账查询 Demo：按凭证号、期间、状态、科目和摘要查询单张凭证或分页凭证列表。
- `finance-service` 已包含凭证自动审核 Demo：借贷平衡、三单匹配、科目逻辑、期间匹配、重复入账和内控合规红黄蓝分级。
- `finance-service` 已实现设计文档标准凭证审核签名，并提供可一键初始化的核心表与大批量 Demo 数据。
- `finance-service` 已包含标准 `InvoiceService` 与发票生命周期 Demo：发票录入、标准查验、台账查询、独立查重、增强验真、连号风险、进项抵扣和电子归档检查。
- `finance-service` 已包含三大报表 Demo：资产负债表平衡校验、利润表收入成本利润汇总、现金流量表经营/投资/筹资现金流汇总。
- `finance-service` 已包含费用报销智能审批 Demo：费用标准、发票验真查重、采购三单匹配、预算管控、审批流路由和异常拦截。
- `finance-service` 已实现设计文档标准费用流程：创建报销单、`approveExpense` 审批和部门预算余额查询。
- `finance-service` 已包含预算管控 Demo：预算执行率、预算占用、预测超支、风险分级和审批建议。
- `finance-service` 已包含月末结账 Demo：跨发票、凭证、银行、应收应付、税务、报表和合规模块生成关账清单、阻断项和预警项。
- `finance-service` 已包含智能税务 Demo：税额计算与税务申报表草稿生成。
- `finance-service` 已包含财务差异归因 Demo：超预算/环比异常分析、驱动因素拆解、证据链和行动计划。
- `finance-service` 已包含细粒度财务分析 Demo：财务比率健康度、指标趋势分析和财务异常检测。
- `finance-service` 已包含合规风控 Demo：汇总发票、报销、银行对账和应收风险，输出风险评分和处理建议。
- `finance-service` 已包含资金管理 Demo：滚动现金流预测、资金缺口预警、账户调拨建议和融资建议。
- `finance-service` 已包含合同与资产 Demo：合同节点提醒、固定资产折旧、资产盘点异常和无形资产摊销提醒。
- `finance-service` 已包含数据集成 Demo：ERP、银企直连、税务、OA/CRM/HR/采购连接器状态、ETL任务、数据质量和重试告警。
- `finance-service` 已包含 Agent 调用审计 Demo：记录钉钉/网页入口上下文、用户指令、MCP工具、Dubbo服务、入参摘要、响应摘要、状态和耗时。
- `finance-service` 的凭证、发票、费用报销、预算、银行流水、应收应付、三大报表和审计日志已使用 JDBC 仓储或 SQL 聚合，默认加载 H2 Demo 数据，生产配置可切换 MySQL。
- `mcp`: MCP 聚合目录，所有 MCP Server、MCP Gateway 和 MCP Demo 能力都放在该子文件夹下。
- `mcp/finance-mcp`: 财务 Dubbo MCP 服务模块，提供财务语义工具和 `dubbo_call` 泛化调用。
- `mcp/zai-mcp-server`: 多模态 MCP 服务，提供 8 个发票 OCR、数据可视化、图像与 UI 分析工具。
- `mcp/web-reader-server`: 网页/政策读取 MCP 服务，支持智谱远程 MCP、直接 HTTP/HTTPS 抓取和本地摘要降级。
- `mcp/integrated-browser-server`: Playwright 浏览器自动化 MCP 服务，提供 22 个导航、表单、读取与会话管理工具。
- `mcp/mcp-gateway`: MCP 统一入口，聚合多个 MCP Server 的工具清单并按工具名路由。
- `finance-web`: 财务数智人 Web 前端，提供对话、驾驶舱、发票、报表页面。
- `streamlit-app`: Streamlit 版轻量入口，复用 `mcp-gateway` 的 Agent 对话和结构化结果。

## Build

```bash
mvn -q -DskipTests package
```

## Run MCP Module

先启动 Demo Dubbo 服务：

```bash
mvn -q -DskipTests package
java -jar finance-service/finance-starter/target/finance-starter-0.0.1-SNAPSHOT.jar
```

再启动 MCP 模块：

```bash
mvn -pl mcp/finance-mcp spring-boot:run
```

启动多模态 MCP Demo：

```bash
cd mcp/zai-mcp-server
npm install
npm run dev
```

启动网页/政策读取 MCP Demo：

```bash
cd mcp/web-reader-server
npm install
npm run dev
```

启动 MCP Gateway：

```bash
cd mcp/mcp-gateway
npm install
npm run dev
```

最后启动 Web：

```bash
cd finance-web
npm install
npm run dev
```

启动 Streamlit 版入口：

```bash
scripts/start-all.sh --streamlit
```

或在 Gateway 已启动后单独运行：

```bash
python3 -m venv .run/streamlit-venv
.run/streamlit-venv/bin/python -m pip install -r streamlit-app/requirements.txt
GATEWAY_BASE_URL=http://localhost:9000 STREAMLIT_BROWSER_GATHER_USAGE_STATS=false \
  .run/streamlit-venv/bin/streamlit run streamlit-app/app.py \
  --server.headless true --server.port 8501
```

## DingTalk Entry Demo

前端已内置钉钉应用入口上下文骨架，支持本地模拟：

```text
http://localhost:5173/chat?channel=dingtalk&corpId=demo-corp&userId=zhangsan&userName=张三
```

进入页面后，前端先通过 `POST /agent/sessions` 将 `clientContext` 保存到 `mcp/mcp-gateway`，再由原生 EventSource 携带 `sessionId` 建立 SSE。Gateway 通过 MCP `_meta.clientContext` 透传上下文，钉钉授权码不会进入 SSE URL。

每次 Agent 回复会返回 `callChain`，前端会展示本轮调用过的 MCP 工具、服务名、耗时和状态；Gateway 同时会调用 `record_audit_log` 写入后端 Dubbo 审计服务。也可通过 Gateway 查询当前会话内存审计：

```bash
curl -s http://localhost:9000/agent/sessions/{sessionId}/audit
```

Gateway 默认使用确定性规则编排以保证本地演示稳定；配置 `AGENT_MODE=llm` 和 `GLM_API_KEY` 后，会把聚合后的完整工具清单交给智谱 GLM Function Calling，执行多轮工具调用。

或通过自然语言/MCP 查询后端审计日志：

```bash
curl -s http://localhost:9000/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"查询demo-session的调用审计日志"}'
```

真实钉钉接入时：

- 前端配置 `VITE_DINGTALK_CORP_ID`。
- 在钉钉容器中通过 JSAPI 获取 `authCode`。
- 后端可将 `authCode` 兑换为真实用户身份，再写入 `SessionContext.clientContext`。

## One-click Local Startup

本地一键启动完整演示环境：

```bash
scripts/start-all.sh
```

常用参数：

```bash
scripts/start-all.sh --force          # 先停止脚本记录的旧进程，再重新启动
scripts/start-all.sh --smoke          # 启动后执行基础 MCP 冒烟测试
scripts/start-all.sh --streamlit      # 额外启动 Streamlit 入口
scripts/start-all.sh --install-browser # 启动前安装 Playwright Chromium
scripts/start-all.sh --no-build --skip-npm-install # 跳过构建和 npm install，加快二次启动
```

启动成功后访问：

```text
http://localhost:5173
http://localhost:8501  # 仅在使用 --streamlit 时启动
http://localhost:5173/chat?channel=dingtalk&corpId=demo-corp&userId=zhangsan&userName=张三
```

日志和 PID 文件会写入：

```text
.run/logs
.run/pids
```

停止所有由脚本记录的服务：

```bash
scripts/stop-all.sh
```
