from collections.abc import AsyncIterator

from langchain_core.messages import BaseMessage
from langchain_openai import ChatOpenAI

from app.mcp.amap_mcp_client import get_amap_mcp_tools
from app.settings import settings
from app.util.agent_stream import stream_langchain_agent
from app.util.chat_context import ChatContext
from app.util.sse import format_sse


async def execute_agent(
    model: ChatOpenAI,
    messages: list[BaseMessage],
    context: ChatContext | None = None,
) -> AsyncIterator[str]:
    mcp_tools = await get_amap_mcp_tools()
    bound_model = model.bind(
        parallel_tool_calls=settings.REASONING_MODEL_PARALLEL_TOOL_CALLS,
    )
    async for text in stream_langchain_agent(
        bound_model,
        messages,
        tools=mcp_tools,
        context=context,
    ):
        if text:
            yield format_sse(text)

    yield format_sse("done")
