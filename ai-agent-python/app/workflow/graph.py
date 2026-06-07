
from langchain_core.tools import BaseTool
from langchain_openai import ChatOpenAI
from langgraph.graph import END, START, StateGraph
from langgraph.prebuilt import ToolNode, tools_condition

from app.workflow.nodes import create_agent_node
from app.workflow.state import WorkflowState


MAX_STEPS = 8


def should_continue(state: WorkflowState) -> str:
    """
    控制 agent 节点之后的流向。

    1. 如果达到最大步数，结束工作流，防止工具调用死循环。
    2. 如果模型返回了 tool_calls，进入 tools 节点。
    3. 如果模型没有 tool_calls，说明模型已经输出最终答案，结束工作流。
    """

    if state.get("step_count", 0) >= MAX_STEPS:
        return END

    return tools_condition(state)


def build_workflow_graph(
    llm_with_tools: ChatOpenAI,
    tools: list[BaseTool],
):
    """
    构建 Workflow Agent 的 LangGraph 图。

    注意：
    - llm_with_tools 由外部创建并完成 bind_tools
    - tools 由外部 tool_registry 获取
    - graph.py 只负责编排节点和边
    """

    graph_builder = StateGraph(WorkflowState)

    graph_builder.add_node(
        "agent",
        create_agent_node(llm_with_tools),
    )

    graph_builder.add_node(
        "tools",
        ToolNode(tools),
    )

    graph_builder.add_edge(START, "agent")

    graph_builder.add_conditional_edges(
        "agent",
        should_continue,
        {
            "tools": "tools",
            END: END,
        },
    )

    graph_builder.add_edge("tools", "agent")

    return graph_builder.compile()