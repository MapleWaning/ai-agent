from langchain_core.tools import BaseTool
from langchain_mcp_adapters.client import MultiServerMCPClient
from langchain_mcp_adapters.sessions import StreamableHttpConnection

from app.settings import settings

AMAP_MCP_SERVER_NAME = "amap-maps"
AMAP_MCP_BASE_URL = "https://mcp.amap.com/mcp"


def build_amap_mcp_connection(api_key: str | None = None) -> StreamableHttpConnection:
    key = (api_key or settings.AMAP_API_KEY).strip()
    if not key:
        raise ValueError("AMAP_API_KEY is required")
    return {
        "transport": "streamable_http",
        "url": f"{AMAP_MCP_BASE_URL}?key={key}",
    }


def build_amap_mcp_connections(api_key: str | None = None) -> dict[str, StreamableHttpConnection]:
    return {
        AMAP_MCP_SERVER_NAME: build_amap_mcp_connection(api_key),
    }


def create_amap_mcp_client(api_key: str | None = None) -> MultiServerMCPClient:
    return MultiServerMCPClient(connections=build_amap_mcp_connections(api_key))


async def get_amap_mcp_tools(api_key: str | None = None) -> list[BaseTool]:
    from app.mcp.tool_wrapper import wrap_tool_with_stream_events

    client = create_amap_mcp_client(api_key)
    tools = await client.get_tools()
    return [wrap_tool_with_stream_events(tool) for tool in tools]
