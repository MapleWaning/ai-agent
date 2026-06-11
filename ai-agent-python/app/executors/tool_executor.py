from collections.abc import AsyncIterator

from langchain_core.messages import BaseMessage
from langchain_openai import ChatOpenAI

from app.settings import settings
from app.tools.tool_registry import LOCAL_TOOLS
from app.util.agent_stream import stream_langchain_agent
from app.util.chat_context import ChatContext
from app.util.sse import format_sse
from app.util.stream_events import format_agent_stream_chunk


async def execute_agent(
    model: ChatOpenAI,
    messages: list[BaseMessage],
    context: ChatContext | None = None,
) -> AsyncIterator[str]:
    bound_model = model.bind(
        parallel_tool_calls=settings.REASONING_MODEL_PARALLEL_TOOL_CALLS,
    )
    async for chunk in stream_langchain_agent(
        bound_model,
        messages,
        tools=LOCAL_TOOLS,
        context=context,
    ):
        sse = format_agent_stream_chunk(chunk)
        if sse:
            yield sse

    yield format_sse("done", event="done")
