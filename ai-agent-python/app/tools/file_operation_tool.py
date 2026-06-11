from pathlib import Path

from langchain.tools import ToolRuntime, tool

from app.settings import settings
from app.util.chat_context import ChatContext
from app.util.tool_stream import emit_file, emit_tool_end, emit_tool_error, emit_tool_start
from app.util.workspace_path import ensure_parent_directory

_READ_TOOL_NAME = "read_file"
_READ_TOOL_LABEL = "读取文件"
_WRITE_TOOL_NAME = "write_file"
_WRITE_TOOL_LABEL = "写入文件"



def _resolve_file_path(runtime: ToolRuntime[ChatContext], file_name: str) -> Path:
    user_id = runtime.context.user_id
    chat_id = runtime.context.chat_id
    base_dir = Path(settings.FILE_SAVE_DIR) / user_id / chat_id
    file_path = (base_dir / file_name).resolve()
    base_resolved = base_dir.resolve()
    try:
        file_path.relative_to(base_resolved)
    except ValueError as exc:
        raise ValueError(f"Invalid file path: {file_name}") from exc
    return file_path


@tool
def read_file(file_name: str, runtime: ToolRuntime[ChatContext]) -> str:
    """Read content from a file in the current chat workspace."""
    emit_tool_start(_READ_TOOL_NAME, _READ_TOOL_LABEL, file_name)
    try:
        file_path = _resolve_file_path(runtime, file_name)
        if not file_path.is_file():
            emit_tool_error(_READ_TOOL_NAME, _READ_TOOL_LABEL, f"文件不存在: {file_name}", file_name)
            return f"Error reading file: file not found: {file_name}"
        result = file_path.read_text(encoding="utf-8")
        emit_tool_end(_READ_TOOL_NAME, _READ_TOOL_LABEL, "读取文件完成")
        return result
    except Exception as exc:
        emit_tool_error(_READ_TOOL_NAME, _READ_TOOL_LABEL, f"读取文件失败: {exc}", file_name)
        return f"Error reading file: {exc}"


@tool
def write_file(
    file_name: str,
    content: str,
    runtime: ToolRuntime[ChatContext],
) -> str:
    """Write content to a file in the current chat workspace."""
    emit_tool_start(_WRITE_TOOL_NAME, _WRITE_TOOL_LABEL, file_name)
    try:
        file_path = ensure_parent_directory(_resolve_file_path(runtime, file_name))
        file_path.write_text(content, encoding="utf-8")
        emit_tool_end(_WRITE_TOOL_NAME, _WRITE_TOOL_LABEL, "写入文件完成")
        emit_file(file_name, action="created", summary=f"文件 {file_name} 已写入")
        return f"File written successfully to: {file_path}"
    except Exception as exc:
        emit_tool_error(_WRITE_TOOL_NAME, _WRITE_TOOL_LABEL, f"写入文件失败: {exc}", file_name)
        return f"Error writing to file: {exc}"
