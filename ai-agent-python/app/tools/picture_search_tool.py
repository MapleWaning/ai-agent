from langchain.tools import tool

from app.settings import settings
from app.util.picture_search import search_picture_content
from app.util.tool_stream import emit_tool_end, emit_tool_error, emit_tool_start

_TOOL_NAME = "picture_search"
_TOOL_LABEL = "图片搜索"


@tool
def picture_search(query: str) -> str:
    """search image from web"""
    emit_tool_start(_TOOL_NAME, _TOOL_LABEL, query)
    try:
        result = search_picture_content(query, settings.PEXELS_API_KEY)
        emit_tool_end(_TOOL_NAME, _TOOL_LABEL, "图片搜索完成")
        return result
    except Exception as exc:
        emit_tool_error(_TOOL_NAME, _TOOL_LABEL, f"图片搜索失败: {exc}", query)
        return f"Error search image: {exc}"
