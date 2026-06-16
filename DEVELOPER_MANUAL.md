# 财务通用 Agent 开发端手册

版本：V1.0  
日期：2026-06-06  
适用对象：后端开发、前端开发、MCP 接入开发、测试与联调人员

## 1. 文档定位

本文面向开发和联调人员，说明如何在本地启动财务通用 Agent、如何模拟钉钉入口登录、如何理解功能模块、如何通过 MCP 和 Gateway 调用后端能力，以及如何做冒烟测试和排障。

本项目当前是一个可运行的财务 Agent 原型，核心链路是：

```text
finance-web  ->  mcp-gateway  ->  MCP Server  ->  Finance Dubbo Provider
     |              |                 |                    |
   Vue 页面       Agent 编排        工具协议层           财务业务服务
```

> 注意 本地默认使用 H2 Demo 数据和确定性规则编排，目的是保证演示和联调稳定。接入企业真实环境时，需要替换 Dubbo 接口、用户身份、数据库、Redis 和模型密钥配置。

## 2. 仓库结构

| 目录 | 作用 | 开发关注点 |
|---|---|---|
| `finance-service` | Demo 财务 Dubbo Provider | 财务接口、DTO/VO、业务实现、H2/MySQL 数据访问 |
| `finance-service/finance-api` | Dubbo API 层 | 对外接口定义、入参 DTO、返回 VO |
| `finance-service/finance-core` | 业务实现层 | 应收、凭证、发票、报销、报表、分析、审计等实现 |
| `finance-service/finance-infra` | 数据与基础设施 | `demo-data.sql` 和仓储相关代码 |
| `finance-service/finance-starter` | 服务启动模块 | Spring Boot + Dubbo Provider 启动入口 |
| `mcp` | MCP 聚合目录 | 所有 MCP Server、Gateway、测试脚本统一放这里 |
| `mcp/finance-mcp` | Java 财务 Dubbo MCP | 45 个财务语义工具和 `dubbo_call` 泛化调用 |
| `mcp/zai-mcp-server` | 多模态 MCP | OCR、图像、图表、UI 分析，未配 Key 时本地降级 |
| `mcp/web-reader-server` | 网页/政策读取 MCP | 税务政策网页读取，支持远程 MCP、HTTP 抓取和本地降级 |
| `mcp/integrated-browser-server` | 浏览器自动化 MCP | Playwright 导航、点击、表单、截图、会话管理 |
| `mcp/mcp-gateway` | MCP 统一入口 | 聚合 76 个工具，提供 `/mcp`、`/agent/chat`、SSE 和审计 |
| `finance-web` | Web 前端 | Vue 3 页面、钉钉上下文、SSE 对话、发票上传和报表展示 |

## 3. 环境要求

本地开发建议准备：

- Java 8 或兼容运行环境。
- Maven。
- Node.js 与 npm。
- 可选：MySQL、Redis，用于生产 Profile 验证。
- 可选：Playwright Chromium，用于浏览器 MCP 的真实页面自动化。
- 可选：`GLM_API_KEY`，用于 Gateway 真实 LLM Function Calling。
- 可选：`Z_AI_API_KEY`，用于真实多模态 OCR 和 web-reader 能力。

浏览器 MCP 如未安装 Playwright Chromium，会自动尝试回退到本机 Chrome；也可以通过 `BROWSER_EXECUTABLE_PATH` 或 `BROWSER_CHANNEL` 指定浏览器。

## 4. 本地启动顺序

必须先启动 Dubbo Provider，再启动财务 MCP，再启动其他 MCP 和 Gateway，最后启动前端。

### 4.1 构建 Java 模块

在仓库根目录执行：

```bash
mvn -q -DskipTests package
```

### 4.2 启动 Finance Dubbo Provider

```bash
java -jar finance-service/finance-starter/target/finance-starter-0.0.1-SNAPSHOT.jar
```

默认导出 Dubbo 服务：

```text
dubbo://127.0.0.1:20880
```

默认数据源是 H2 内存库，启动时自动加载：

```text
finance-service/finance-infra/data/demo-data.sql
```

### 4.3 启动 Finance MCP

另开终端，在仓库根目录执行：

```bash
mvn -pl mcp/finance-mcp spring-boot:run
```

默认地址：

```text
http://localhost:8086/mcp
```

### 4.4 启动多模态 MCP

```bash
cd mcp/zai-mcp-server
npm install
npm run dev
```

默认地址：

```text
http://localhost:8088/mcp
```

### 4.5 启动网页/政策读取 MCP

```bash
cd mcp/web-reader-server
npm install
npm run dev
```

默认地址：

```text
http://localhost:8089/mcp
```

### 4.6 启动浏览器自动化 MCP

```bash
cd mcp/integrated-browser-server
npm install
npx playwright install chromium
npm run dev
```

默认地址：

```text
http://localhost:8090/mcp
```

### 4.7 启动 MCP Gateway

```bash
cd mcp/mcp-gateway
npm install
npm run dev
```

默认地址：

```text
http://localhost:9000/mcp
http://localhost:9000/agent/chat
```

默认使用确定性规则编排。启用真实 GLM Function Calling：

```bash
cd mcp/mcp-gateway
AGENT_MODE=llm GLM_API_KEY=your-key npm run dev
```

### 4.8 启动前端

```bash
cd finance-web
npm install
npm run dev
```

默认访问：

```text
http://localhost:5173
```

## 5. 登录与入口说明

当前项目没有传统用户名密码登录页。开发联调采用“钉钉入口上下文模拟”的方式，把用户身份通过 URL Query 或 API `clientContext` 传入 Gateway。

### 5.1 普通 Web 入口

```text
http://localhost:5173/chat
```

根路径会自动跳转到：

```text
/chat
```

### 5.2 本地模拟钉钉入口

```text
http://localhost:5173/chat?channel=dingtalk&corpId=demo-corp&userId=zhangsan&userName=张三
```

进入后，前端会先调用：

```text
POST /agent/sessions
```

把 `clientContext` 保存到 Gateway 会话，然后用原生 EventSource 建立 SSE 流式对话。这样可以避免把真实钉钉 `authCode` 暴露在 SSE URL 中。

### 5.3 真实钉钉接入预留点

真实钉钉环境需要补齐：

- 前端配置 `VITE_DINGTALK_CORP_ID`。
- 在钉钉容器中通过 JSAPI 获取免登 `authCode`。
- 后端把 `authCode` 兑换成真实用户身份。
- Gateway 将身份写入 `SessionContext.clientContext` 并透传给 MCP `_meta.clientContext`。
- 财务 Dubbo 服务根据用户身份做权限、组织和数据范围控制。

本地启动示例：

```bash
cd finance-web
VITE_DINGTALK_CORP_ID=your-corp-id npm run dev
```

## 6. 前端功能模块

前端位于 `finance-web`，技术栈为 Vue 3、TypeScript、Vite、Element Plus、Pinia、ECharts。

| 路由 | 页面 | 作用 |
|---|---|---|
| `/chat` | 对话页 | 输入自然语言指令，展示流式回复、调用链、历史会话、复制和重新生成 |
| `/cockpit` | 财务驾驶舱 | 展示应收、回款、预算、利润等关键指标和图表，支持刷新和图表追问 |
| `/invoice` | 发票页 | 上传发票图片，执行 OCR、验真、查重和凭证生成链路 |
| `/report` | 报表页 | 查看资产负债表、利润表、现金流量表及报表结果 |

前端开发常用命令：

```bash
cd finance-web
npm run dev
npm run build
npm run preview
```

前端代理配置在 `finance-web/vite.config.ts`：

```text
/agent  ->  http://localhost:9000
```

因此前端调用 `/agent/chat` 和 `/agent/chat/stream` 时，会代理到 MCP Gateway。

## 7. 后端财务功能模块

`finance-service` 当前覆盖以下财务域：

- 应收应付：应收看板、账龄分析、催收建议、催收计划、应付付款计划、往来对账。
- 科目与凭证：科目推荐、凭证创建、凭证查询、凭证自动审核、借贷平衡和三单匹配。
- 发票：发票录入、台账查询、标准查验、查重、增强验真、抵扣和归档检查。
- 报销：报销单创建、查询、智能审批、预算余额查询、审批状态回写。
- 报表：资产负债表、利润表、现金流量表、月度报表生成。
- 分析预算：财务比率、趋势分析、异常检测、差异归因、预算管控。
- 税务：税额计算、税务申报表草稿。
- 银行与资金：银行流水、银行对账、现金流预测、资金缺口和融资建议。
- 合规月结：合规风险评估、月末结账检查、关账清单和阻断项。
- 合同资产：合同收付款节点、固定资产折旧、资产盘点异常。
- 数据集成：ERP、银企直连、税务、OA、CRM、HR、采购连接器状态。
- 审计日志：记录 Agent、MCP、Dubbo 调用链、入参摘要、响应摘要、状态和耗时。

## 8. MCP 模块说明

| MCP 服务 | 默认端口 | 工具数 | 说明 |
|---|---:|---:|---|
| Finance MCP | 8086 | 45 | 财务语义工具与 Dubbo 泛化调用 |
| ZAI MCP | 8088 | 8 | OCR、图像、图表和 UI 分析 |
| Web Reader MCP | 8089 | 1 | 网页和政策内容读取 |
| Browser MCP | 8090 | 22 | Playwright 浏览器导航、表单、读取和会话管理 |
| MCP Gateway | 9000 | 76 | 统一工具入口和 Agent Chat 编排 |

Gateway 静态路由表：

```text
mcp/mcp-gateway/config/routes.yml
```

当某个下游 MCP 服务不可用时，Gateway 会降级处理，不阻塞其他 MCP 服务的工具聚合和调用。

## 9. MCP 与 Agent 调用方式

### 9.1 查询 Gateway 工具列表

```bash
curl -s http://localhost:9000/mcp \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}'
```

正常情况下，Gateway 聚合 76 个工具，且不可用服务数量为 0。

### 9.2 通过 Agent Chat 调自然语言

```bash
curl -s http://localhost:9000/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"生成2026-05资产负债表"}'
```

常用测试指令：

```text
查询2026-05应收账款汇总
查询截至2026-05-31的应收账龄分析
为客户C1001生成催收建议和催收措辞
为E1002创建一张销售部3600元差旅报销单并提交
审批销售部3200元差旅报销
上传识别一张发票并生成凭证
生成2026-05资产负债表
检测2026-05财务异常项
评估2026-05合规风控风险
查询demo-session的调用审计日志
```

### 9.3 携带钉钉上下文调用

```bash
curl -s http://localhost:9000/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{
    "message":"查一下本月应收",
    "clientContext":{
      "channel":"dingtalk",
      "corpId":"demo-corp",
      "userId":"zhangsan",
      "userName":"张三",
      "device":"mobile"
    }
  }'
```

### 9.4 直接调用 Finance MCP

```bash
curl -s http://localhost:8086/mcp \
  -H 'Content-Type: application/json' \
  -d @mcp/finance-mcp/examples/query-ar-dashboard.sample.json
```

### 9.5 查询调用链审计

查询 Gateway 当前会话内存审计：

```bash
curl -s http://localhost:9000/agent/sessions/{sessionId}/audit
```

查询后端持久化审计日志：

```bash
curl -s http://localhost:9000/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"查询demo-session的调用审计日志"}'
```

## 10. 新增一个财务工具的开发流程

当你拿到真实后端 Dubbo 接口后，建议按以下顺序接入。

1. 在 `finance-service/finance-api` 增加或确认 Dubbo 接口、DTO、VO。
2. 在 `finance-service/finance-core` 实现接口逻辑，必要时在 `finance-infra` 增加 SQL 或仓储。
3. 在 `finance-starter` 启动服务，确认 Dubbo Provider 可用。
4. 在 `mcp/finance-mcp` 注册工具定义，优先做语义化工具，必要时保留 `dubbo_call` 泛化兜底。
5. 如果只是简单字段映射，可参考 `query_ar_dashboard_configured` 的配置化工具模式。
6. 在 `mcp/finance-mcp/examples` 增加 JSON-RPC 样例。
7. 在 `mcp/mcp-gateway/config/routes.yml` 增加工具路由。
8. 用 `tools/list` 确认工具已被 Gateway 聚合。
9. 用 `/agent/chat` 增加自然语言触发规则或验证 LLM Function Calling。
10. 如需要前端入口，在 `finance-web/src/config/quickActions.ts` 或对应页面增加快捷动作。
11. 增加或更新冒烟测试脚本，确保写入、查询、审批类操作可重复验证。

> 注意 付款、报税、正式记账、审批通过等高风险动作必须在真实生产接入时增加权限、二次确认、组织级授权、审计和人工复核流程。本地 Demo 只证明链路可通。

## 11. 配置项说明

| 配置项 | 所属模块 | 默认值/说明 |
|---|---|---|
| `server.port` | Finance MCP | `8086` |
| `finance.dubbo.direct-url` | Finance MCP | `dubbo://127.0.0.1:20880` |
| `finance.mcp.auth.enabled` | Finance MCP | 默认 `false`，开启后由 Header Token 鉴权 |
| `FINANCE_MCP_TOKEN` | Gateway | 下游 Finance MCP 开启鉴权后使用 |
| `AGENT_MODE` | Gateway | 默认规则编排；设为 `llm` 启用 GLM Function Calling |
| `GLM_API_KEY` | Gateway | 智谱 GLM 调用密钥 |
| `Z_AI_API_KEY` | ZAI / Web Reader | 多模态和远程 web-reader 密钥 |
| `VITE_DINGTALK_CORP_ID` | 前端 | 真实钉钉容器联调时配置 |
| `SPRING_PROFILES_ACTIVE` | Finance Provider | `production` 时切换生产数据源配置 |
| `FINANCE_MYSQL_URL` | Finance Provider | 生产 MySQL JDBC 地址 |
| `FINANCE_MYSQL_USERNAME` | Finance Provider | 生产 MySQL 用户名 |
| `FINANCE_MYSQL_PASSWORD` | Finance Provider | 生产 MySQL 密码 |
| `FINANCE_REDIS_HOST` | Finance Provider | 生产 Redis 地址 |
| `FINANCE_REDIS_PORT` | Finance Provider | 生产 Redis 端口，默认 `6379` |
| `BROWSER_EXECUTABLE_PATH` | Browser MCP | 指定浏览器可执行文件 |
| `BROWSER_CHANNEL` | Browser MCP | 指定 Playwright 浏览器 Channel |

生产 Profile 示例：

```bash
SPRING_PROFILES_ACTIVE=production \
FINANCE_MYSQL_URL='jdbc:mysql://127.0.0.1:3306/finance_demo?serverTimezone=Asia/Shanghai' \
FINANCE_MYSQL_USERNAME=finance \
FINANCE_MYSQL_PASSWORD=finance \
FINANCE_REDIS_HOST=127.0.0.1 \
java -jar finance-service/finance-starter/target/finance-starter-0.0.1-SNAPSHOT.jar
```

## 12. 构建、测试与验收

### 12.1 Java 单测与打包

```bash
mvn -q test
mvn -q -DskipTests package
```

### 12.2 前端构建

```bash
cd finance-web
npm run build
```

### 12.3 MCP 冒烟测试

在所有服务都启动后，从仓库根目录执行：

```bash
bash mcp/scripts/smoke-test.sh
bash mcp/scripts/browser-smoke-test.sh
bash mcp/scripts/web-reader-smoke-test.sh
bash mcp/scripts/finance-persistence-smoke-test.sh
```

验证重点：

- Gateway 能聚合 76 个工具。
- 不可用服务数量为 0。
- 浏览器 MCP 的 22 个 Playwright 工具可真实交互。
- web-reader 能完成真实 HTTP 页面读取和 Markdown 转换。
- 凭证、发票、报销创建、查询和审批状态回写能通过 JDBC 持久化验证。
- 自然语言能编排 OCR 到凭证、三大报表、报销创建和银行流水查询。

## 13. 常见排障

### 13.1 前端页面无法访问

检查前端是否启动：

```bash
cd finance-web
npm run dev
```

访问：

```text
http://localhost:5173
```

如果 `/agent` 请求失败，检查 Gateway 是否启动在 9000 端口。

### 13.2 Agent 提示服务不可用

先查询 Gateway 工具列表：

```bash
curl -s http://localhost:9000/mcp \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}'
```

若 Finance 工具缺失，检查：

- Finance Dubbo Provider 是否启动在 `20880`。
- Finance MCP 是否启动在 `8086`。
- `mcp/mcp-gateway/config/routes.yml` 中 Finance MCP URL 是否正确。
- Finance MCP 到 Dubbo 的 `finance.dubbo.direct-url` 是否正确。

### 13.3 Finance MCP 调 Dubbo 失败

检查 Provider 启动日志和 Finance MCP 配置：

```text
finance.dubbo.direct-url: dubbo://127.0.0.1:20880
```

本地 Demo 使用直连地址和 `registry: N/A`。接入企业注册中心时，需要替换为真实注册中心、接口全限定名、Group、Version 和 DTO 签名。

### 13.4 发票 OCR 结果不准确

本地未配置 `Z_AI_API_KEY` 时，多模态 MCP 使用确定性降级结果。要验证真实 OCR，需要：

```bash
cd mcp/zai-mcp-server
Z_AI_API_KEY=your-key npm run dev
```

正式入账前必须由财务人员复核发票号码、金额、税额、购销方和科目。

### 13.5 浏览器 MCP 启动失败

先安装 Chromium：

```bash
cd mcp/integrated-browser-server
npx playwright install chromium
npm run dev
```

如果公司电脑限制下载，可指定本机 Chrome：

```bash
BROWSER_CHANNEL=chrome npm run dev
```

或：

```bash
BROWSER_EXECUTABLE_PATH=/path/to/chrome npm run dev
```

### 13.6 报表数据为什么会变化

报表由数据库中的凭证分录和业务数据实时聚合。创建新凭证、录入发票、审批报销或改写 Demo 数据后，同一期间的报表结果可能变化。

### 13.7 钉钉用户身份没有生效

本地只通过 Query 模拟身份。确认 URL 中包含：

```text
channel=dingtalk&corpId=demo-corp&userId=zhangsan&userName=张三
```

真实钉钉环境还需要服务端完成 `authCode` 换取用户身份，并把用户、组织、角色和权限写入会话上下文。

## 14. 当前外部联调边界

当前代码已提供接口入口和降级逻辑，但以下能力需要企业环境才能完成真实联调：

- 替换为企业真实 Dubbo 财务接口、DTO、注册中心和鉴权体系。
- 配置真实 `GLM_API_KEY` 验证 LLM Function Calling。
- 配置真实 `Z_AI_API_KEY` 验证 OCR、多模态和 web-reader。
- 接入钉钉服务端免登授权码换取用户身份。
- 使用生产 MySQL 和 Redis 验证 `production` Profile。
- 对付款、报税、审批、正式凭证入账增加组织级权限、审批流和人工复核。

开发验收建议以“服务可启动、工具可聚合、自然语言可触发、调用链可审计、写入后可查询”为最低标准。
