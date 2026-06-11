from collections.abc import AsyncIterator

from langchain_core.messages import BaseMessage, HumanMessage
from langchain_openai import ChatOpenAI

from app.settings import settings
from app.tools.tool_registry import get_all_tools
from app.util.chat_context import ChatContext
from app.util.sse import format_sse
from app.util.stream_events import format_agent_stream_chunk
from app.workflow.graph import build_workflow_graph


def _extract_user_input(messages: list[BaseMessage]) -> str:
    for message in reversed(messages):
        if isinstance(message, HumanMessage):
            content = message.content
            return content if isinstance(content, str) else str(content)
    return ""

async def execute_agent(
    model: ChatOpenAI,
    messages: list[BaseMessage],
    context: ChatContext | None = None,
) -> AsyncIterator[str]:
    tools = await get_all_tools()
    llm_with_tools = model.bind(
        parallel_tool_calls=settings.REASONING_MODEL_PARALLEL_TOOL_CALLS,
    ).bind_tools(tools)
    graph = build_workflow_graph(llm_with_tools, tools)

    initial_state = {
        "messages": messages,
        "original_user_input": _extract_user_input(messages),
        "step_count": 0,
    }

    stream_kwargs: dict = {"stream_mode": "custom"}
    if context is not None:
        stream_kwargs["context"] = context

    yield format_sse("正在启动复杂工作流...", event="status")

    async for chunk in graph.astream(
        initial_state,
        **stream_kwargs,
    ):
        sse = format_agent_stream_chunk(chunk)
        if sse:
            yield sse

    yield format_sse("done", event="done")
