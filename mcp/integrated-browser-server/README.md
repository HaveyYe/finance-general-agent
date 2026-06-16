# integrated-browser-server

基于 Playwright 的浏览器自动化 MCP 服务，对齐设计文档中的 `browser:8090` 服务。

提供 22 个状态化工具：

- 页面操作：`navigate`、`click`、`type`、`scroll`、`hover`、`press_key`
- 表单操作：`fill_form`、`upload_file`、`select_option`、`check`、`uncheck`
- 页面读取：`snapshot`、`screenshot`、`get_text`、`get_attribute`、`evaluate`
- 流程控制：`wait_for`、`back`、`forward`、`reload`
- 会话管理：`list_sessions`、`close_session`

```bash
npm install
npx playwright install chromium
npm run dev
```

默认地址：`http://localhost:8090/mcp`。

若未安装 Playwright Chromium，服务会自动回退到本机 Chrome。也可通过
`BROWSER_EXECUTABLE_PATH` 或 `BROWSER_CHANNEL` 指定浏览器。
