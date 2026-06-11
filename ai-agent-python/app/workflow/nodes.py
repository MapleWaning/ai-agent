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
        step_no = state.get("step_count", 0) + 1
        writer({
            "event": "workflow_step",
            "data": {
                "step": step_no,
                "title": "Agent 分析任务",
                "status": "running",
                "detail": "正在判断用户需求，并决定是否需要调用工具"
                }
        })
        response: AIMessageChunk | None = None
        async for chunk in llm_with_tools.astream(state["messages"]):
            writer(chunk)
            response = chunk if response is None else response + chunk

        if response is None:
            response = AIMessageChunk(content="")
        writer({
            "event": "workflow_step",
            "data": {
                "step": step_no,
                "title": "Agent 分析任务",
                "status": "finished",
                "detail": "Agent 已完成本轮决策"
                }
        })
        return {
            "messages": [response],
            "step_count": step_no,
        }

    return agent_node

