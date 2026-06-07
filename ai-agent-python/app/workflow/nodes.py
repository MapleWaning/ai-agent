from collections.abc import Awaitable, Callable
from typing import Any

from langchain_core.messages import AIMessageChunk
from langchain_openai import ChatOpenAI
from langgraph.config import get_stream_writer

from app.workflow.state import WorkflowState


def create_agent_node(
    llm_with_tools: ChatOpenAI,
) -> Callable[[WorkflowState], Awaitable[dict[str, Any]]]:
    async def agent_node(state: WorkflowState) -> dict[str, Any]:
        writer = get_stream_writer()
        response: AIMessageChunk | None = None
        async for chunk in llm_with_tools.astream(state["messages"]):
            writer(chunk)
            response = chunk if response is None else response + chunk

        if response is None:
            response = AIMessageChunk(content="")

        return {
            "messages": [response],
            "step_count": state.get("step_count", 0) + 1,
        }

    return agent_node

