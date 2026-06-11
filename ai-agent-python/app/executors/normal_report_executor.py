from collections.abc import AsyncIterator

from langchain_core.messages import BaseMessage
from langchain_openai import ChatOpenAI

from app.util.agent_stream import extract_chunk_text
from app.util.chat_context import ChatContext
from app.util.sse import format_sse


async def execute_agent(
    model: ChatOpenAI,
    messages: list[BaseMessage],
    context: ChatContext | None = None,
) -> AsyncIterator[str]:
    async for chunk in model.astream(messages):
        text = extract_chunk_text(chunk)
        if text:
            yield format_sse(text, event="message")

    yield format_sse("done", event="done")
