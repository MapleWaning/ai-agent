from collections.abc import Callable
from typing import Any

from langchain_core.tools import BaseTool, StructuredTool

from app.util.tool_stream import emit_tool_end, emit_tool_error, emit_tool_start, get_tool_label


def _extract_tool_input(args: tuple[Any, ...], kwargs: dict[str, Any]) -> Any:
    if kwargs:
        return kwargs if len(kwargs) > 1 else next(iter(kwargs.values()), kwargs)
    if len(args) == 1:
        return args[0]
    if args:
        return args
    return None


def _wrap_callable(
    tool: BaseTool,
    label: str,
    original: Callable[..., Any],
    *,
    is_async: bool,
) -> Callable[..., Any]:
    if is_async:
        async def wrapped(*args: Any, **kwargs: Any) -> Any:
            tool_input = _extract_tool_input(args, kwargs)
            emit_tool_start(tool.name, label, tool_input)
            try:
                result = await original(*args, **kwargs)
                emit_tool_end(tool.name, label, f"{label}完成")
                return result
            except Exception as exc:
                emit_tool_error(tool.name, label, f"{label}失败: {exc}", tool_input)
                raise

        return wrapped

    def wrapped(*args: Any, **kwargs: Any) -> Any:
        tool_input = _extract_tool_input(args, kwargs)
        emit_tool_start(tool.name, label, tool_input)
        try:
            result = original(*args, **kwargs)
            emit_tool_end(tool.name, label, f"{label}完成")
            return result
        except Exception as exc:
            emit_tool_error(tool.name, label, f"{label}失败: {exc}", tool_input)
            raise

    return wrapped


def _wrap_structured_tool(tool: StructuredTool) -> StructuredTool:
    label = get_tool_label(tool.name)

    if tool.coroutine is not None:
        return tool.model_copy(
            update={
                "coroutine": _wrap_callable(
                    tool,
                    label,
                    tool.coroutine,
                    is_async=True,
                ),
            },
        )

    if tool.func is not None:
        return tool.model_copy(
            update={
                "func": _wrap_callable(
                    tool,
                    label,
                    tool.func,
                    is_async=False,
                ),
            },
        )

    return _wrap_via_ainvoke(tool, label)


def _wrap_via_ainvoke(tool: BaseTool, label: str) -> StructuredTool:
    original_ainvoke = tool.ainvoke

    async def runner(*args: Any, **kwargs: Any) -> Any:
        if kwargs:
            tool_input: Any = kwargs
            payload: Any = kwargs
        elif len(args) == 1:
            tool_input = args[0]
            payload = args[0]
        else:
            tool_input = args
            payload = args[0] if len(args) == 1 else args

        emit_tool_start(tool.name, label, tool_input)
        try:
            if kwargs:
                result = await original_ainvoke(payload)
            elif len(args) == 1:
                result = await original_ainvoke(payload)
            else:
                result = await original_ainvoke(*args, **kwargs)
            emit_tool_end(tool.name, label, f"{label}完成")
            return result
        except Exception as exc:
            emit_tool_error(tool.name, label, f"{label}失败: {exc}", tool_input)
            raise

    return StructuredTool.from_function(
        coroutine=runner,
        name=tool.name,
        description=tool.description,
        args_schema=tool.args_schema,
    )


def wrap_tool_with_stream_events(tool: BaseTool) -> BaseTool:
    label = get_tool_label(tool.name)

    if isinstance(tool, StructuredTool):
        return _wrap_structured_tool(tool)

    return _wrap_via_ainvoke(tool, label)
