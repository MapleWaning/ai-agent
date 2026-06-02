import re
from typing import Optional

from langchain_core.messages import HumanMessage, SystemMessage

from app.models.schemas import RouteDecision, RouteRequest, RouteResponse, RouteType
from app.models.llm import create_routing_model
from app.models.route_prompts import ROUTING_SYSTEM_PROMPT

_ROUTE_RULES: list[tuple[re.Pattern[str], RouteType, str]] = [
    (
        re.compile(r"生成报告|分析报告|评估报告"),
        RouteType.REPORT,
        "输入包含报告相关关键词（生成报告/分析报告/评估报告）",
    ),
    (
        re.compile(r"知识库"),
        RouteType.RAG,
        "输入包含知识库相关关键词",
    ),
    (
        re.compile(r"附近|约会地点|地点|路线"),
        RouteType.MCP,
        "输入包含地点/路线相关关键词",
    ),
    (
        re.compile(r"搜索|网页|下载|生成\s*PDF", re.IGNORECASE),
        RouteType.TOOL,
        "输入包含工具/搜索相关关键词",
    ),
    (
        re.compile(r"帮我制定计划|分步骤完成|生成完整方案"),
        RouteType.WORKFLOW,
        "输入包含工作流/计划相关关键词",
    ),
    (
        re.compile(r"直接回答"),
        RouteType.NORMAL_CHAT,
        "输入为普通倾诉或咨询",
    ),
]


def _match_route_by_regex(text: str) -> Optional[tuple[RouteType, str]]:
    for pattern, route_type, reason in _ROUTE_RULES:
        if pattern.search(text):
            return route_type, reason
    return None


def _to_route_response(route_type: RouteType, reason: str) -> RouteResponse:
    return RouteResponse(
        routeType=route_type.value,
        enumName=route_type.name,
        reason=reason,
    )


async def route_agent_type(request: RouteRequest) -> RouteResponse:
    # matched = _match_route_by_regex(request.initPrompt)
    # if matched:
    #     route_type, reason = matched
    #     return _to_route_response(route_type, reason)

    model = create_routing_model().with_structured_output(RouteDecision, method="json_mode")
    decision = await model.ainvoke(
        [
            SystemMessage(content=ROUTING_SYSTEM_PROMPT),
            HumanMessage(content=request.initPrompt),
        ]
    )
    return _to_route_response(decision.routeType, decision.reason)
