# Finance General Agent Streamlit

Streamlit 版入口复用现有 MCP Gateway，不重新实现财务业务逻辑。

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
