from langchain.tools import ToolRuntime, tool

from app.util.chat_context import ChatContext
from app.util.resource_download import download_to_file, resolve_download_path
from app.util.tool_stream import emit_file, emit_tool_end, emit_tool_error, emit_tool_start
from app.util.workspace_path import ensure_parent_directory

_TOOL_NAME = "download_resource"
_TOOL_LABEL = "下载资源"


@tool
def download_resource(
    url: str,
    file_name: str,
    runtime: ToolRuntime[ChatContext],
) -> str:
    """Download a resource from a given URL."""
    emit_tool_start(_TOOL_NAME, _TOOL_LABEL, {"url": url, "file_name": file_name})
    try:
        file_path = ensure_parent_directory(resolve_download_path(runtime, file_name))
        download_to_file(url, file_path)
        emit_tool_end(_TOOL_NAME, _TOOL_LABEL, "下载资源完成")
        emit_file(file_name, action="downloaded", summary=f"资源 {file_name} 已下载")
        return f"Resource downloaded successfully to: {file_path}"
    except Exception as exc:
        emit_tool_error(
            _TOOL_NAME,
            _TOOL_LABEL,
            f"下载资源失败: {exc}",
            {"url": url, "file_name": file_name},
        )
        return f"Error downloading resource: {exc}"
