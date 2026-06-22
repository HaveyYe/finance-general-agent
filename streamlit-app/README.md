# Finance General Agent Streamlit

Streamlit 版入口是原生 Streamlit 页面，只调用真实 MCP Gateway、财务工具和知识库，不提供本地 Demo 降级。

如果部署到 Streamlit Cloud，`localhost:9000` 指的是 Streamlit 云端容器，不是你的电脑。必须把 `GATEWAY_BASE_URL` 配成公网可访问的 Gateway 地址，例如 `https://your-gateway.example.com`。

## Run

先启动完整后端和 Gateway：

```bash
scripts/start-all.sh --streamlit
```

然后访问：

```text
http://localhost:8501
```

也可以手动启动：

```bash
python3 -m venv .run/streamlit-venv
.run/streamlit-venv/bin/python -m pip install -r streamlit-app/requirements.txt
GATEWAY_BASE_URL=http://localhost:9000 STREAMLIT_BROWSER_GATHER_USAGE_STATS=false \
  .run/streamlit-venv/bin/streamlit run streamlit-app/app.py \
  --server.headless true --server.port 8501
```

## Config

- `GATEWAY_BASE_URL`: MCP Gateway 地址，默认 `http://localhost:9000`。
- `STREAMLIT_USER_ID`: 默认用户 ID，默认 `streamlit-user`。
- `STREAMLIT_USER_NAME`: 默认用户名，默认 `Streamlit 用户`。

Streamlit Cloud 可在 App settings 的 Secrets 中配置：

```toml
GATEWAY_BASE_URL = "https://your-gateway.example.com"
```
