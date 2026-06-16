# zai-mcp-server

多模态 MCP 服务，提供 8 个智谱/视觉工具。配置 `Z_AI_API_KEY` 后调用真实视觉模型；未配置密钥或远程调用失败时使用确定性本地结果，保证联调链路可测试：

- `extract_text_from_screenshot`: 发票/单据 OCR 结构化识别
- `analyze_data_visualization`: 结构化数据转 ECharts 配置
- `analyze_image`: 通用图片分析
- `analyze_video`: 通用视频分析
- `diagnose_error_screenshot`: 错误截图诊断
- `ui_diff_check`: UI 截图差异检查
- `understand_technical_diagram`: 技术图表理解
- `ui_to_artifact`: UI 转组件规格

## Run

```bash
npm install
npm run dev
```

默认地址：

```text
http://localhost:8088/mcp
```

## Test

```bash
curl -s http://localhost:8088/mcp \
  -H 'Content-Type: application/json' \
  -d @examples/extract-invoice.sample.json
```
