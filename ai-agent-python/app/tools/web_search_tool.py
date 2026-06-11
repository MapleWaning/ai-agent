from langchain.tools import tool

from app.settings import settings
from app.util.tool_stream import emit_tool_end, emit_tool_error, emit_tool_start
from app.util.web_search import search_web_content

_TOOL_NAME = "search_web"
_TOOL_LABEL = "网页搜索"


@tool
def search_web(query: str) -> str:
    """Search for information from Baidu Search Engine."""
    emit_tool_start(_TOOL_NAME, _TOOL_LABEL, query)
    try:
        result = search_web_content(query, settings.SEARCH_API_KEY)
        emit_tool_end(_TOOL_NAME, _TOOL_LABEL, "网页搜索完成")
        return result
    except Exception as exc:
        emit_tool_error(_TOOL_NAME, _TOOL_LABEL, f"网页搜索失败: {exc}", query)
        return f"Error searching Baidu: {exc}"
