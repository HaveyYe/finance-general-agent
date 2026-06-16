# mcp-gateway

MCP Gateway 是统一 MCP 入口，按系统设计文档提供：

- `POST /mcp`
- `initialize`
- `tools/list`
- `tools/call`
- `POST /agent/chat`：轻量 Agent 编排入口，接收自然语言并调用 MCP 工具
- `POST /agent/sessions`：建立或更新 Agent 会话上下文，供 SSE 安全复用
- `GET /agent/chat/stream`：原生 EventSource SSE 流式对话入口
- `GET /agent/sessions/:sessionId/audit`：查询 Gateway 当前内存中的会话 MCP 工具调用审计记录
- 静态路由表：`config/routes.yml`
- 后端 MCP Server 不可用时降级，不影响其他服务
- 支持财务、OCR、多模态、网页政策读取和浏览器自动化等 4 个 MCP 服务聚合
- 可选智谱 GLM Function Calling 多轮工具编排；默认规则路由保证本地演示稳定

## Run

```bash
npm install
npm run dev
```

默认地址：

```text
http://localhost:9000/mcp
```

启用 GLM 自主工具编排：

```bash
AGENT_MODE=llm GLM_API_KEY=your-key npm run dev
```

Gateway 会把聚合后的完整 MCP 工具清单传给 GLM，并最多执行 6 轮 `tool_call`。未配置密钥或 GLM 调用失败时自动回退到规则路由。

如果下游 `finance-mcp` 开启 token 鉴权，在 `config/routes.yml` 中配置环境变量名：

```yaml
services:
  finance:
    url: http://localhost:8086/mcp
    authTokenEnv: FINANCE_MCP_TOKEN
```

启动 Gateway 时传入：

```bash
FINANCE_MCP_TOKEN=demo-token npm run dev
```

## Test

```bash
curl -s http://localhost:9000/mcp \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}'
```

调用财务工具：

```bash
curl -s http://localhost:9000/mcp \
  -H 'Content-Type: application/json' \
  -d @../finance-mcp/examples/query-ar-dashboard.sample.json
```

调用 Agent Chat：

```bash
curl -s http://localhost:9000/agent/chat \
  -H 'Content-Type: application/json' \
  -d @examples/agent-chat-ar.sample.json
```

调用智能催收和付款计划：

```bash
curl -s http://localhost:9000/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"生成C类逾期35天客户应收催收计划和催收函"}'

curl -s http://localhost:9000/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"优化上海数科服务有限公司42000元应付付款计划，考虑提前付款折扣"}'

curl -s http://localhost:9000/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"对浙江智造设备有限公司做2026-05应收往来对账，生成余额确认函和差异明细"}'
```

调用设计文档标准应收应付服务：

```bash
curl -s http://localhost:9000/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"查询截至2026-05-31的应收账龄分析"}'

curl -s http://localhost:9000/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"为客户C1001生成催收建议和催收措辞"}'

curl -s http://localhost:9000/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"查询C1001在2026-05的往来对账结果"}'
```

调用设计文档标准费用报销流程：

```bash
curl -s http://localhost:9000/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"为E1002创建一张销售部3600元差旅报销单并提交"}'

curl -s http://localhost:9000/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"查询销售部2026-05费用预算余额"}'

curl -s http://localhost:9000/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"审批销售部3200元差旅报销"}'
```

调用细粒度财务分析：

```bash
curl -s http://localhost:9000/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"计算2026-05财务比率和健康度"}'

curl -s http://localhost:9000/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"分析2026-01到2026-05营业收入趋势"}'

curl -s http://localhost:9000/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"检测2026-05财务异常项"}'
```

携带钉钉用户上下文：

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

Gateway 会把上下文保存到 `SessionContext`，后续工具调用通过 `_meta.clientContext` 透传，并在工具调用成功后通过 `record_audit_log` 写入后端 Dubbo 审计服务。
前端在建立 SSE 前先调用 `POST /agent/sessions`，避免将钉钉 `authCode` 放入 EventSource URL。

查询调用链审计：

```bash
curl -s http://localhost:9000/agent/sessions/{sessionId}/audit
```

审计记录包含工具名、后端服务、调用耗时、状态、脱敏后的入参和错误信息。该接口返回 Gateway 内存调用链，适合展示本轮会话；后端持久演示日志可通过自然语言查询：

```bash
curl -s http://localhost:9000/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"查询demo-session的调用审计日志"}'
```

调用税务政策查询：

```bash
curl -s http://localhost:9000/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"增值税税率政策是什么"}'
```

调用发票验真、查重和抵扣检查：

```bash
curl -s http://localhost:9000/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"对发票INV-202605-001做标准查验"}'

curl -s http://localhost:9000/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"录入一张杭州差旅服务有限公司3600元发票，发票号INV-202606-009，并自动验真"}'

curl -s http://localhost:9000/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"查重发票INV-202605-001，检查是否重复报销或重复入账"}'

curl -s http://localhost:9000/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"查验发票 INV-202605-003 真伪、查重和抵扣状态"}'
```

调用三大报表专用接口：

```bash
curl -s http://localhost:9000/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"生成2026-05资产负债表"}'

curl -s http://localhost:9000/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"生成2026-05利润表"}'

curl -s http://localhost:9000/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"生成2026-05现金流量表"}'
```

调用凭证自动审核与异常标记：

```bash
curl -s http://localhost:9000/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"给上海数科服务有限公司42000元软件服务费发票推荐会计科目和凭证预览"}'

curl -s http://localhost:9000/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"查询2026-05凭证台账"}'

curl -s http://localhost:9000/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"查看凭证V-20260531-0001明细"}'

curl -s http://localhost:9000/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"审核一张采购异常凭证，检查三单匹配、借贷平衡和岗位分离"}'
```

调用费用报销智能审批：

```bash
curl -s http://localhost:9000/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"审批一张8600元采购异常报销单，检查预算、发票和三单匹配"}'
```

调用预算管控评估：

```bash
curl -s http://localhost:9000/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"2026-05销售费用为什么超了，帮我做差异归因和行动计划"}'

curl -s http://localhost:9000/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"评估销售部98000元差旅预算占用，检查预算执行率和超支预警"}'
```

调用月末结账检查：

```bash
curl -s http://localhost:9000/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"执行2026-05月结检查，生成关账清单和阻断项"}'
```

调用税额计算/申报表生成：

```bash
curl -s http://localhost:9000/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"帮我计算128.65万元增值税税额"}'

curl -s http://localhost:9000/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"生成2026年5月增值税申报表"}'
```

调用合规风控评估：

```bash
curl -s http://localhost:9000/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"评估2026-05合规风控风险"}'
```

调用资金预测：

```bash
curl -s http://localhost:9000/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"预测未来3个月现金流和资金缺口"}'
```

调用合同与资产看板：

```bash
curl -s http://localhost:9000/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"查询合同收付款节点和固定资产折旧"}'
```

调用数据集成状态：

```bash
curl -s http://localhost:9000/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"查询ERP银企直连税务OA数据集成状态"}'
```
