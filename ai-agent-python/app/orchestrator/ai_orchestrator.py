from collections.abc import AsyncIterator, Callable

from langchain_core.messages import HumanMessage, SystemMessage

from app.executors.mcp_executor import execute_agent as execute_mcp
from app.executors.normal_report_executor import execute_agent as execute_normal_report
from app.executors.rag_executor import execute_agent as execute_rag
from app.executors.tool_executor import execute_agent as execute_tool
from app.executors.workflow_executor import execute_agent as execute_workflow
from app.memory.chat_memory_service import get_memory_service
from app.prompt.chat_prompts import PromptRegistry
from app.models.llm import create_reasoning_model,create_efficient_model
from app.models.schemas import ChatRequest, RouteType
from app.util.chat_context import ChatContext
from app.util.sse import format_sse

ExecutorFn = Callable[..., AsyncIterator[str]]

_EXECUTOR_MAP: dict[RouteType, ExecutorFn] = {
    RouteType.NORMAL_CHAT: execute_normal_report,
    RouteType.REPORT: execute_normal_report,
    RouteType.RAG: execute_rag,
    RouteType.MCP: execute_mcp,
    RouteType.TOOL: execute_tool,
    RouteType.WORKFLOW: execute_workflow,
}

_EFFICIENT_ROUTE_TYPES: set[RouteType] = {
    RouteType.NORMAL_CHAT,
    RouteType.REPORT,
    RouteType.RAG,
}

_memory_service = get_memory_service()


async def stream_chat(request: ChatRequest) -> AsyncIterator[str]:
    system_prompt = PromptRegistry().get_system_prompt(request.routeType)
    history_messages = _memory_service.get_recent_messages(
        user_id=request.userId,
        chat_id=request.chatId,
        route_type=request.routeType,
    )
    if request.routeType in _EFFICIENT_ROUTE_TYPES:
        model = create_efficient_model()
    else:
        model = create_reasoning_model()
    messages = [
        SystemMessage(content=system_prompt),
        *history_messages,
        HumanMessage(content=request.message),
    ]

    context = ChatContext(user_id=request.userId, chat_id=request.chatId)
    yield format_sse(request.routeType.value, event="route")

    executor = _EXECUTOR_MAP[request.routeType]
    async for chunk in executor(model, messages, context):
        yield chunk
