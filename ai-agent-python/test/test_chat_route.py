import pytest
from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)

ROUTE_CASES = [
    ("请帮我生成一份恋爱分析报告", "report", "REPORT"),
    ("单身人士想谈恋爱要注意什么", "rag", "RAG"),
    ("我在杭州，有什么适合约会的地点", "mcp", "MCP"),
    ("搜索有关恋爱攻略的网页", "tool", "TOOL"),
    ("帮我制定一个七夕约会计划", "workflow", "WORKFLOW"),
    ("我的女朋友不回我消息，我该怎么办", "normal_chat", "NORMAL_CHAT"),
]


@pytest.mark.parametrize("prompt,expected_route_type,expected_enum_name", ROUTE_CASES)
def test_chat_route(prompt: str, expected_route_type: str, expected_enum_name: str) -> None:
    response = client.post("/ai/chat/route", json={"initPrompt": prompt})

    assert response.status_code == 200, response.text
    data = response.json()

    print(f"\n输入: {prompt}")
    print(f"路由类别 routeType: {data['routeType']}")
    print(f"枚举 enumName: {data['enumName']}")
    print(f"原因 reason: {data['reason']}")

    assert data["routeType"] == expected_route_type
    assert data["enumName"] == expected_enum_name
    assert data["reason"]
