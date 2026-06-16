# finance-web

财务数智人 Web 前端，按系统设计文档实现：

- Vue 3 + TypeScript + Vite
- Element Plus 操作界面
- ECharts 财务图表
- Pinia 会话状态
- 通过 `/agent/chat` 和原生 EventSource SSE 调用 `mcp/mcp-gateway`
- 历史会话本地持久化，支持新建、切换、删除、复制和重新生成
- 图表点击追问、双击下钻，驾驶舱支持定时刷新
- 发票上传自动执行 OCR → 凭证生成链路
- 钉钉上下文先通过 POST 建立会话，敏感授权码不进入 SSE URL

## Run

先启动 `finance-service`、`mcp/finance-mcp` 和 `mcp/mcp-gateway`，再启动前端：

```bash
npm install
npm run dev
```

默认访问：

```text
http://localhost:5173
```

## DingTalk Demo

本地模拟钉钉入口：

```text
http://localhost:5173/chat?channel=dingtalk&corpId=demo-corp&userId=zhangsan&userName=张三
```

如需在真实钉钉容器中获取免登授权码，配置：

```bash
VITE_DINGTALK_CORP_ID=your-corp-id npm run dev
```

前端会把钉钉上下文作为 `clientContext` 传给 `/agent/chat`。
