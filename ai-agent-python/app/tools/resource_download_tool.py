from langchain.tools import ToolRuntime, tool

from app.util.chat_context import ChatContext
from app.util.resource_download import download_to_file, resolve_download_path
from app.util.workspace_path import ensure_parent_directory


@tool
def download_resource(
    url: str,
    file_name: str,
    runtime: ToolRuntime[ChatContext],
) -> str:
    """Download a resource from a given URL."""
    try:
        file_path = ensure_parent_directory(resolve_download_path(runtime, file_name))
        download_to_file(url, file_path)
        return f"Resource downloaded successfully to: {file_path}"
    except Exception as exc:
        return f"Error downloading resource: {exc}"
