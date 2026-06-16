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
LOCAL_MODE_NOTICE = "当前未连接 MCP Gateway，已切换到 Streamlit 本地 Demo 模式。"


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
        return False, f"未连接：{exc.__class__.__name__}"


def local_agent_response(prompt: str) -> Dict[str, Any]:
    normalized = prompt.strip()
    if "应收" in normalized or "账龄" in normalized:
        rows = [
            {"客户": "杭州云帆科技", "账龄": "0-30天", "应收金额": 328000, "风险": "低"},
            {"客户": "上海星河制造", "账龄": "31-60天", "应收金额": 186500, "风险": "中"},
            {"客户": "宁波启明贸易", "账龄": "90天以上", "应收金额": 74200, "风险": "高"},
        ]
        return {
            "text": "本地 Demo 结果：2026-05 应收账龄整体可控，但 90 天以上款项需要优先跟进。",
            "type": "table",
            "content": rows,
            "callChain": [],
        }
    if "资产负债" in normalized or "报表" in normalized:
        return {
            "text": "本地 Demo 结果：2026-05 资产负债表试算平衡，资产总额 12,860,000 元，负债合计 7,420,000 元，所有者权益 5,440,000 元。",
            "type": "card",
            "content": {
                "period": "2026-05",
                "assets": 12860000,
                "liabilities": 7420000,
                "equity": 5440000,
                "balanced": True,
            },
            "callChain": [],
        }
    if "指标" in normalized or "分析" in normalized:
        return {
            "text": "本地 Demo 结果：流动比率保持在 1.8 左右，回款率较上月改善，费用率略有上升。",
            "type": "chart",
            "content": {
                "series": [
                    {"month": "2026-03", "流动比率": 1.62, "回款率": 0.81},
                    {"month": "2026-04", "流动比率": 1.74, "回款率": 0.84},
                    {"month": "2026-05", "流动比率": 1.81, "回款率": 0.88},
                ]
            },
            "callChain": [],
        }
    if "报销" in normalized:
        rows = [
            {"单号": "EXP-202605-001", "申请人": "张三", "金额": 1280, "状态": "PENDING"},
            {"单号": "EXP-202605-002", "申请人": "李四", "金额": 560, "状态": "NEED_REVIEW"},
        ]
        return {
            "text": "本地 Demo 结果：当前有 2 笔报销待处理，其中 1 笔需要复核。",
            "type": "table",
            "content": rows,
            "callChain": [],
        }
    if "凭证" in normalized:
        rows = [
            {"凭证号": "V-202605-001", "日期": "2026-05-31", "摘要": "销售回款入账", "状态": "已审核"},
            {"凭证号": "V-202605-002", "日期": "2026-05-31", "摘要": "费用报销入账", "状态": "待审核"},
        ]
        return {
            "text": "本地 Demo 结果：最近凭证台账如下，待审核凭证建议先校验借贷平衡和附件完整性。",
            "type": "table",
            "content": rows,
            "callChain": [],
        }
    return {
        "text": f"{LOCAL_MODE_NOTICE} 你刚才的问题是：{normalized}。如需真实工具调用和知识库检索，请先启动 `scripts/start-all.sh --streamlit`。",
        "type": "text",
        "content": None,
        "callChain": [],
    }


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
        gateway_ok, _ = check_gateway()
        if not gateway_ok:
            response = local_agent_response(prompt)
            st.markdown(response["text"])
            assistant_message = {
                "role": "assistant",
                "content": response["text"],
                "type": response["type"],
                "data": response.get("content"),
                "call_chain": response.get("callChain"),
            }
            render_structured(assistant_message)
            st.session_state.messages.append(assistant_message)
            return

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
            st.warning(f"{detail}，使用本地 Demo 模式")

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
