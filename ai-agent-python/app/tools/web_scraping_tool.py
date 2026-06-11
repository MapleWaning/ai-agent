from langchain.tools import tool

from app.util.tool_stream import emit_tool_end, emit_tool_error, emit_tool_start
from app.util.web_scraping import scrape_web_page_content

_TOOL_NAME = "scrape_web_page"
_TOOL_LABEL = "网页抓取"


@tool
def scrape_web_page(url: str) -> str:
    """Scrape the content of a web page."""
    emit_tool_start(_TOOL_NAME, _TOOL_LABEL, url)
    try:
        result = scrape_web_page_content(url)
        emit_tool_end(_TOOL_NAME, _TOOL_LABEL, "网页抓取完成")
        return result
    except Exception as exc:
        emit_tool_error(_TOOL_NAME, _TOOL_LABEL, f"网页抓取失败: {exc}", url)
        return f"Error scraping web page: {exc}"
