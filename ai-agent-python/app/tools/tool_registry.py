from langchain_core.tools import BaseTool

from app.mcp.amap_mcp_client import get_amap_mcp_tools

from app.tools.file_operation_tool import read_file, write_file
from app.tools.pdf_generation_tool import generate_pdf
from app.tools.resource_download_tool import download_resource
from app.tools.web_scraping_tool import scrape_web_page
from app.tools.picture_search_tool import picture_search
from app.tools.web_search_tool import search_web
from app.tools.rag_search_tool import rag_search

_CACHED_TOOLS: list[BaseTool] | None = None

LOCAL_TOOLS: list[BaseTool] =[
    read_file,
    write_file,
    generate_pdf,
    download_resource,
    scrape_web_page,
    search_web,
    picture_search,
]

def check_duplicate_tool_names(tools: list[BaseTool]) -> None:
    names = [tool.name for tool in tools]
    duplicate_names = {name for name in names if names.count(name) > 1}

    if duplicate_names:
        raise ValueError(f"Duplicate tool names found: {duplicate_names}")


async def get_all_tools() -> list[BaseTool]:
    global _CACHED_TOOLS

    if _CACHED_TOOLS is not None:
        return _CACHED_TOOLS

    amap_tools = await get_amap_mcp_tools()

    tools = [
        *LOCAL_TOOLS,
        rag_search,
        *amap_tools,
    ]

    check_duplicate_tool_names(tools)

    _CACHED_TOOLS = tools
    return tools

