# finance-contract-mcp-server

面向 `zcy-finance-contract` 的 MCP(JSON-RPC) 服务。服务通过 Dubbo 泛化调用合同系统 Facade，给上层 Agent 暴露受控业务工具，而不是让 Agent 直接拼 Dubbo 接口名和 Java 参数类型。

## 设计边界

- MCP 只负责工具定义、业务身份注入、权限前置校验、Dubbo 调用、统一返回和审计日志。
- 合同、单据、发票、回款、产品、项目库等业务逻辑仍由 `zcy-finance-contract` Dubbo Provider 执行。
- 默认不暴露通用 `dubbo_call`，避免 Agent 绕过工具白名单调用任意 Dubbo 接口。
- 写操作默认关闭；管理员工具需要 `write-enabled=true`、管理员白名单和二次确认口令。

## 启动

```bash
mvn -pl mcp/finance-mcp spring-boot:run
```

默认端口：`8086`。如果端口被占用：

```bash
MCP_SERVER_PORT=18088 mvn -pl mcp/finance-mcp spring-boot:run
```

关键环境变量：

```bash
export DUBBO_REGISTRY_ADDRESS=zookeeper://127.0.0.1:2181
export FINANCE_DUBBO_VERSION=1.0.0-digital
export FINANCE_DUBBO_REFERENCE_ENV=beijing
```

## 业务身份头

普通合同移动端工具会把 `X-Finance-Token` 注入到对应 PO 的 `token` 字段。

```text
X-Finance-Token: 合同系统登录 token
X-Operator-Id: 当前操作人 ID
X-Operator-Name: 当前操作人名称
X-Trace-Id: 可选调用链 ID
```

`receipt.page` 会根据 `X-Operator-Id` / `X-Operator-Name` 构造 `digitUser`，不接受前端传入任意 `DigitUser` 覆盖当前登录用户。

## 工具列表

第一阶段默认暴露 18 个工具：

- `contract.loginByDingTalk`
- `contract.currentUser`
- `contract.page`
- `contract.detail`
- `contract.statusList`
- `contract.workflowKey`
- `contract.attachments`
- `contract.receiptAttachments`
- `contract.invoices`
- `contract.payment`
- `receipt.page`
- `common.dictionary`
- `common.departmentTree`
- `common.bizProductInfo`
- `product.page`
- `product.detail`
- `project.page`
- `project.digitPage`

`contract.invoices` 和 `contract.payment` 调用前会先执行 `contract.detail`，确认当前 token 能访问该合同后才会调用未显式带用户信息的 `FinanceContractFacade` 方法。

## MCP 调用

### tools/list

```bash
curl -s http://localhost:8088/mcp \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}'
```

### contract.detail

```bash
curl -s http://localhost:8088/mcp \
  -H 'Content-Type: application/json' \
  -H 'X-Finance-Token: your-token' \
  -H 'X-Operator-Id: 12345' \
  -H 'X-Operator-Name: 张三' \
  -d '{
    "jsonrpc": "2.0",
    "id": 2,
    "method": "tools/call",
    "params": {
      "name": "contract.detail",
      "arguments": {
        "contractNo": "HT202606090001"
      }
    }
  }'
```

工具返回统一包裹在 MCP `structuredContent.result` 中：

```json
{
  "success": true,
  "tool": "contract.detail",
  "traceId": "xxx",
  "data": {},
  "message": "查询成功",
  "raw": {}
}
```

失败时：

```json
{
  "success": false,
  "tool": "contract.detail",
  "traceId": "xxx",
  "errorCode": "NO_PERMISSION",
  "message": "当前用户无权限访问该合同",
  "detail": {}
}
```

## 安全配置

```yaml
finance:
  mcp:
    expose-generic-dubbo-call: false
    safety:
      write-enabled: false
      write-confirm-token: ${MCP_WRITE_CONFIRM_TOKEN:}
      admin-user-ids: []
```

只有显式开启 `expose-generic-dubbo-call` 后，`dubbo_call` 才会出现在 `tools/list` 中，并且仍要求调用者在管理员白名单内。
