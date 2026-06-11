from collections.abc import AsyncIterator

from langchain_core.messages import BaseMessage, HumanMessage
from langchain_openai import ChatOpenAI

from app.rag.enrichers.query_rewriter import rewrite_user_message
from app.rag.rag_service import get_rag_service
from app.util.chat_context import ChatContext
from app.util.sse import format_sse
from app.util.stream_events import format_agent_stream_chunk
from app.util.tool_stream import (
    build_tool_end_event,
    build_tool_error_event,
    build_tool_start_event,
)


def _extract_user_message(messages: list[BaseMessage]) -> str:
    for message in reversed(messages):
        if isinstance(message, HumanMessage):
            content = message.content
            if isinstance(content, str):
                return content.strip()
            return str(content).strip()
    return ""


def _replace_last_human_message(
    messages: list[BaseMessage],
    content: str,
) -> list[BaseMessage]:
    updated_messages = list(messages)
    for index in range(len(updated_messages) - 1, -1, -1):
        if isinstance(updated_messages[index], HumanMessage):
            updated_messages[index] = HumanMessage(content=content)
            break
    return updated_messages


async def execute_agent(
    model: ChatOpenAI,
    messages: list[BaseMessage],
    context: ChatContext | None = None,
) -> AsyncIterator[str]:
    user_message = _extract_user_message(messages)
    rewritten_message = await rewrite_user_message(user_message)

    start_event = build_tool_start_event("rag_search", "知识库检索", user_message)
    yield format_sse(start_event["data"], event=start_event["event"])

    try:
        rag_message = get_rag_service().assemble_message(
            rewritten_message,
            query=user_message,
        )
    except Exception as exc:
        error_event = build_tool_error_event(
            "rag_search",
            "知识库检索",
            f"知识库检索失败: {exc}",
            user_message,
        )
        yield format_sse(error_event["data"], event=error_event["event"])
        raise

    end_event = build_tool_end_event("rag_search", "知识库检索", "知识库检索完成")
    yield format_sse(end_event["data"], event=end_event["event"])

    rag_messages = _replace_last_human_message(messages, rag_message)

    async for chunk in model.astream(rag_messages):
        sse = format_agent_stream_chunk(chunk)
        if sse:
            yield sse

    yield format_sse("done", event="done")
