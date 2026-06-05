import pytest
from fastapi.testclient import TestClient
from langchain_core.messages import AIMessageChunk
from langchain_openai import ChatOpenAI

from app.main import app
from app.orchestrator import ai_orchestrator

client = TestClient(app)


@pytest.fixture(autouse=True)
def mock_chat_memory(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setattr(
        ai_orchestrator._memory_service,
        "get_recent_messages",
        lambda user_id, chat_id, route_type: [],
    )


@pytest.fixture(autouse=True)
def mock_model_astream(monkeypatch: pytest.MonkeyPatch) -> None:
    async def fake_astream(self, messages, config=None, **kwargs):
        yield AIMessageChunk(content="你好")
        yield AIMessageChunk(content="，世界")

    monkeypatch.setattr(ChatOpenAI, "astream", fake_astream)


@pytest.fixture(autouse=True)
def mock_mcp_tools(monkeypatch: pytest.MonkeyPatch) -> None:
    async def fake_get_amap_mcp_tools(api_key=None):
        return []

    monkeypatch.setattr(
        "app.executors.mcp_executor.get_amap_mcp_tools",
        fake_get_amap_mcp_tools,
    )


@pytest.fixture(autouse=True)
def mock_stream_langchain_agent(monkeypatch: pytest.MonkeyPatch) -> None:
    async def fake_stream_langchain_agent(model, messages, tools, context=None):
        yield "你好"
        yield "，世界"

    monkeypatch.setattr(
        "app.executors.tool_executor.stream_langchain_agent",
        fake_stream_langchain_agent,
    )
    monkeypatch.setattr(
        "app.executors.mcp_executor.stream_langchain_agent",
        fake_stream_langchain_agent,
    )


STREAM_CASES = [
    ("normal_chat", "NORMAL_CHAT", "data: 你好"),
    ("report", "REPORT", "data: 你好"),
    ("rag", "RAG", "[RAG] executor placeholder"),
    ("mcp", "MCP", "data: 你好"),
    ("tool", "TOOL", "data: 你好"),
    ("workflow", "WORKFLOW", "[WORKFLOW] executor placeholder"),
]


@pytest.mark.parametrize("route_type,route_enum_name,expected_marker", STREAM_CASES)
def test_chat_stream_dispatches_to_executor(
    route_type: str,
    route_enum_name: str,
    expected_marker: str,
) -> None:
    response = client.post(
        "/ai/chat/stream",
        json={
            "message": f"测试 {route_enum_name} 流式分发",
            "userId": "test-user-001",
            "chatId": "test-chat-stream-001",
            "routeType": route_type,
        },
    )

    assert response.status_code == 200, response.text
    body = response.text

    print(f"\nrouteType: {route_type} ({route_enum_name})")
    print(f"响应内容: {body.strip()}")

    assert expected_marker in body
    if route_type in {"normal_chat", "report", "mcp", "tool"}:
        assert "data: ，世界" in body
        assert "data: done" in body
