from __future__ import annotations

import json
import os
from typing import Any, Dict, Generator, Iterable, Optional, Tuple

import pandas as pd
import requests
import streamlit as st


DEFAULT_GATEWAY_BASE_URL = os.getenv("GATEWAY_BASE_URL", "http://localhost:9000")
DEFAULT_USER_ID = os.getenv("STREAMLIT_USER_ID", "streamlit-user")
DEFAULT_USER_NAME = os.getenv("STREAMLIT_USER_NAME", "Streamlit 用户")
WELCOME = (
    "你好，我是财务数智人。你可以直接询问财务报表、发票、凭证、报销、预算、"
    "应收应付、知识库制度等问题。"
)


st.set_page_config(page_title="财务数智人", layout="wide")


def init_state() -> None:
    st.session_state.setdefault("gateway_base_url", DEFAULT_GATEWAY_BASE_URL)
    st.session_state.setdefault("gateway_session_id", "")
    st.session_state.setdefault(
        "messages",
        [
            {
                "role": "assistant",
                "content": WELCOME,
                "type": "text",
                "data": None,
                "call_chain": None,
            }
        ],
    )


def client_context() -> Dict[str, Any]:
    return {
        "channel": "streamlit",
        "userId": st.session_state.get("user_id") or DEFAULT_USER_ID,
        "userName": st.session_state.get("user_name") or DEFAULT_USER_NAME,
        "device": "desktop",
    }


def gateway_url(path: str) -> str:
    return f"{st.session_state.gateway_base_url.rstrip('/')}{path}"


def check_gateway() -> Tuple[bool, str]:
    try:
        response = requests.get(gateway_url("/health"), timeout=3)
        if response.ok:
            return True, "已连接"
        return False, f"HTTP {response.status_code}"
    except requests.RequestException as exc:
        return False, str(exc)


def prepare_session() -> str:
    response = requests.post(
        gateway_url("/agent/sessions"),
        json={
            "sessionId": st.session_state.gateway_session_id or None,
            "clientContext": client_context(),
        },
        timeout=10,
    )
    response.raise_for_status()
    session_id = response.json()["sessionId"]
    st.session_state.gateway_session_id = session_id
    return session_id


def iter_sse(response: requests.Response) -> Generator[Tuple[str, Dict[str, Any]], None, None]:
    event = "message"
    data_lines: list[str] = []

    for raw_line in response.iter_lines(decode_unicode=True):
        if raw_line is None:
            continue
        line = raw_line.strip()
        if not line:
            if data_lines:
                payload = "\n".join(data_lines)
                try:
                    yield event, json.loads(payload)
                except json.JSONDecodeError:
                    yield event, {"raw": payload}
            event = "message"
            data_lines = []
            continue
        if line.startswith("event:"):
            event = line[6:].strip()
        elif line.startswith("data:"):
            data_lines.append(line[5:].strip())


def stream_agent(message: str) -> Iterable[Tuple[str, Dict[str, Any]]]:
    session_id = prepare_session()
    with requests.get(
        gateway_url("/agent/chat/stream"),
        params={"message": message, "sessionId": session_id},
        stream=True,
        timeout=(10, 180),
    ) as response:
        response.raise_for_status()
        yield from iter_sse(response)


def render_structured(message: Dict[str, Any]) -> None:
    message_type = message.get("type")
    data = message.get("data")
    if not data or message_type == "text":
        return

    if message_type == "table":
        rows = data if isinstance(data, list) else data.get("records") if isinstance(data, dict) else None
        if isinstance(rows, list):
            st.dataframe(pd.DataFrame(rows), use_container_width=True)
            return

    if message_type == "chart" and isinstance(data, dict):
        series = data.get("series")
        if isinstance(series, list):
            chart_data = pd.DataFrame(series)
            if not chart_data.empty:
                st.line_chart(chart_data)
                return

    with st.expander("结构化结果", expanded=False):
        st.json(data)


def render_call_chain(call_chain: Optional[list[dict[str, Any]]]) -> None:
    if not call_chain:
        return
    with st.expander("调用链", expanded=False):
        st.dataframe(pd.DataFrame(call_chain), use_container_width=True)


def append_message(
    role: str,
    content: str,
    message_type: str = "text",
    data: Any = None,
    call_chain: Optional[list[dict[str, Any]]] = None,
) -> None:
    st.session_state.messages.append(
        {
            "role": role,
            "content": content,
            "type": message_type,
            "data": data,
            "call_chain": call_chain,
        }
    )


def send_prompt(prompt: str) -> None:
    append_message("user", prompt)
    with st.chat_message("user"):
        st.markdown(prompt)

    with st.chat_message("assistant"):
        status = st.empty()
        final_response: Dict[str, Any] = {}
        structured: Dict[str, Any] = {}

        def token_generator() -> Generator[str, None, None]:
            for event, payload in stream_agent(prompt):
                if event == "status":
                    status.caption(str(payload.get("message") or "处理中..."))
                elif event == "token":
                    yield str(payload.get("token") or "")
                elif event in {"table", "chart", "card", "file"}:
                    structured["type"] = event
                    structured["data"] = payload.get("content")
                elif event == "done":
                    final_response.update(payload)
                    st.session_state.gateway_session_id = str(payload.get("sessionId") or "")
                elif event == "stream_error":
                    raise RuntimeError(str(payload.get("message") or "SSE stream failed"))

        try:
            streamed_text = st.write_stream(token_generator)
            status.empty()
            if isinstance(streamed_text, list):
                streamed_text = "".join(str(item) for item in streamed_text)
            assistant_text = str(final_response.get("text") or streamed_text or "")
            message_type = str(final_response.get("type") or structured.get("type") or "text")
            data = final_response.get("content", structured.get("data"))
            call_chain = final_response.get("callChain")
            assistant_message = {
                "role": "assistant",
                "content": assistant_text,
                "type": message_type,
                "data": data,
                "call_chain": call_chain,
            }
            render_structured(assistant_message)
            render_call_chain(call_chain if isinstance(call_chain, list) else None)
            st.session_state.messages.append(assistant_message)
        except Exception as exc:
            status.empty()
            error_text = f"请求失败：{exc}"
            st.error(error_text)
            append_message("assistant", error_text)


def sidebar() -> None:
    with st.sidebar:
        st.header("连接")
        st.text_input("Gateway URL", key="gateway_base_url")
        st.text_input("用户 ID", value=DEFAULT_USER_ID, key="user_id")
        st.text_input("用户名", value=DEFAULT_USER_NAME, key="user_name")

        ok, detail = check_gateway()
        if ok:
            st.success(detail)
        else:
            st.error(detail)

        if st.button("新建会话", use_container_width=True):
            st.session_state.gateway_session_id = ""
            st.session_state.messages = [
                {
                    "role": "assistant",
                    "content": WELCOME,
                    "type": "text",
                    "data": None,
                    "call_chain": None,
                }
            ]
            st.rerun()

        st.divider()
        st.caption("快捷问题")
        quick_prompts = [
            "查询 2026-05 应收账龄看板",
            "生成 2026-05 资产负债表",
            "分析 2026-05 财务指标",
            "查询待审批报销单",
            "查询最近的凭证台账",
        ]
        for prompt in quick_prompts:
            if st.button(prompt, use_container_width=True):
                st.session_state.pending_prompt = prompt
                st.rerun()


def main() -> None:
    init_state()
    sidebar()

    st.title("财务数智人")

    for message in st.session_state.messages:
        with st.chat_message(message["role"]):
            st.markdown(message["content"])
            render_structured(message)
            render_call_chain(message.get("call_chain"))

    prompt = st.chat_input("请输入财务、报销、发票、凭证或知识库问题")
    pending_prompt = st.session_state.pop("pending_prompt", None)
    if pending_prompt:
        prompt = pending_prompt

    if prompt:
        send_prompt(prompt)


if __name__ == "__main__":
    main()
