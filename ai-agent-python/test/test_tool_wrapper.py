import asyncio

from langchain_core.tools import StructuredTool

from app.mcp.tool_wrapper import wrap_tool_with_stream_events


def test_wrap_structured_tool_with_coroutine() -> None:
    async def search(keyword: str) -> str:
        return f"result:{keyword}"

    tool = StructuredTool.from_function(
        coroutine=search,
        name="maps_search",
        description="search maps",
    )
    wrapped = wrap_tool_with_stream_events(tool)
    assert wrapped is not tool
    assert wrapped.name == "maps_search"
    result = asyncio.run(wrapped.ainvoke({"keyword": "北京"}))
    assert result == "result:北京"


def test_wrap_structured_tool_with_func() -> None:
    def search(keyword: str) -> str:
        return f"result:{keyword}"

    tool = StructuredTool.from_function(
        search,
        name="maps_search",
        description="search maps",
    )
    wrapped = wrap_tool_with_stream_events(tool)
    assert wrapped is not tool
    result = wrapped.invoke({"keyword": "上海"})
    assert result == "result:上海"
