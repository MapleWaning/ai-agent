from typing import Any

from langgraph.config import get_stream_writer

TOOL_LABELS: dict[str, str] = {
    "search_web": "网页搜索",
    "read_file": "读取文件",
    "write_file": "写入文件",
    "generate_pdf": "生成 PDF",
    "download_resource": "下载资源",
    "scrape_web_page": "网页抓取",
    "picture_search": "图片搜索",
    "rag_search": "知识库检索",
}


def get_tool_label(name: str) -> str:
    return TOOL_LABELS.get(name, name)


def build_tool_start_event(
    name: str,
    label: str,
    tool_input: Any,
) -> dict[str, Any]:
    return {
        "event": "tool_start",
        "data": {
            "name": name,
            "label": label,
            "input": tool_input,
            "status": "running",
        },
    }


def build_tool_end_event(
    name: str,
    label: str,
    summary: str,
    status: str = "finished",
) -> dict[str, Any]:
    return {
        "event": "tool_end",
        "data": {
            "name": name,
            "label": label,
            "status": status,
            "summary": summary,
        },
    }


def build_tool_error_event(
    name: str,
    label: str,
    error: str,
    tool_input: Any = None,
) -> dict[str, Any]:
    data: dict[str, Any] = {
        "name": name,
        "label": label,
        "status": "failed",
        "error": error,
    }
    if tool_input is not None:
        data["input"] = tool_input
    return {
        "event": "tool_error",
        "data": data,
    }


def build_file_event(
    file_name: str,
    action: str = "created",
    summary: str = "",
) -> dict[str, Any]:
    return {
        "event": "file",
        "data": {
            "fileName": file_name,
            "action": action,
            "summary": summary or f"文件 {file_name} 已处理",
        },
    }


def _safe_emit(event: dict[str, Any]) -> None:
    try:
        writer = get_stream_writer()
        writer(event)
    except Exception:
        return


def emit_tool_start(name: str, label: str, tool_input: Any) -> None:
    _safe_emit(build_tool_start_event(name, label, tool_input))


def emit_tool_end(
    name: str,
    label: str,
    summary: str,
    status: str = "finished",
) -> None:
    _safe_emit(build_tool_end_event(name, label, summary, status=status))


def emit_tool_error(
    name: str,
    label: str,
    error: str,
    tool_input: Any = None,
) -> None:
    _safe_emit(build_tool_error_event(name, label, error, tool_input=tool_input))


def emit_file(
    file_name: str,
    action: str = "created",
    summary: str = "",
) -> None:
    _safe_emit(build_file_event(file_name, action, summary))
