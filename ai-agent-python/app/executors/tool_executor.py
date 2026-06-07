from collections.abc import AsyncIterator

from langchain_core.messages import BaseMessage
from langchain_openai import ChatOpenAI

from app.settings import settings
from app.tools.tool_registry import LOCAL_TOOLS
from app.util.agent_stream import stream_langchain_agent
from app.util.chat_context import ChatContext
from app.util.sse import format_sse


async def execute_agent(
    model: ChatOpenAI,
    messages: list[BaseMessage],
    context: ChatContext | None = None,
) -> AsyncIterator[str]:
    bound_model = model.bind(
        parallel_tool_calls=settings.REASONING_MODEL_PARALLEL_TOOL_CALLS,
    )
    async for text in stream_langchain_agent(
        bound_model,
        messages,
        tools=LOCAL_TOOLS,
        context=context,
    ):
        if text:
            yield format_sse(text)

    yield format_sse("done")
