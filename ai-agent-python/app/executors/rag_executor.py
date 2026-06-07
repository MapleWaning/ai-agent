from collections.abc import AsyncIterator

from langchain_core.messages import BaseMessage, HumanMessage
from langchain_openai import ChatOpenAI

from app.rag.rag_service import get_rag_service
from app.rag.enrichers.query_rewriter import rewrite_user_message
from app.util.agent_stream import extract_chunk_text
from app.util.chat_context import ChatContext
from app.util.sse import format_sse


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
    rag_message = get_rag_service().assemble_message(rewritten_message)
    rag_messages = _replace_last_human_message(messages, rag_message)

    async for chunk in model.astream(rag_messages):
        text = extract_chunk_text(chunk)
        if text:
            yield format_sse(text)

    yield format_sse("done")
