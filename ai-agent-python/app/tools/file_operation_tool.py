from pathlib import Path

from langchain.tools import ToolRuntime, tool

from app.settings import settings
from app.util.chat_context import ChatContext
from app.util.workspace_path import ensure_parent_directory


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
    try:
        file_path = _resolve_file_path(runtime, file_name)
        if not file_path.is_file():
            return f"Error reading file: file not found: {file_name}"
        return file_path.read_text(encoding="utf-8")
    except Exception as exc:
        return f"Error reading file: {exc}"


@tool
def write_file(
    file_name: str,
    content: str,
    runtime: ToolRuntime[ChatContext],
) -> str:
    """Write content to a file in the current chat workspace."""
    try:
        file_path = ensure_parent_directory(_resolve_file_path(runtime, file_name))
        file_path.write_text(content, encoding="utf-8")
        return f"File written successfully to: {file_path}"
    except Exception as exc:
        return f"Error writing to file: {exc}"
