from pathlib import Path
from urllib.error import URLError
from urllib.parse import urlparse
from urllib.request import Request, urlopen

from langchain.tools import ToolRuntime

from app.settings import settings
from app.util.chat_context import ChatContext

_DOWNLOAD_TIMEOUT_SECONDS = 60
_USER_AGENT = "yu-ai-agent-resource-downloader/1.0"


def resolve_download_path(runtime: ToolRuntime[ChatContext], file_name: str) -> Path:
    user_id = runtime.context.user_id
    chat_id = runtime.context.chat_id
    base_dir = Path(settings.FILE_SAVE_DIR) / user_id / chat_id / "download"
    file_path = (base_dir / file_name).resolve()
    base_resolved = base_dir.resolve()
    try:
        file_path.relative_to(base_resolved)
    except ValueError as exc:
        raise ValueError(f"Invalid file path: {file_name}") from exc
    return file_path


def _validate_download_url(url: str) -> str:
    normalized = url.strip()
    parsed = urlparse(normalized)
    if parsed.scheme not in {"http", "https"} or not parsed.netloc:
        raise ValueError(f"Invalid download URL: {url}")
    return normalized


def download_to_file(url: str, file_path: Path) -> None:
    validated_url = _validate_download_url(url)
    request = Request(validated_url, headers={"User-Agent": _USER_AGENT})
    try:
        with urlopen(request, timeout=_DOWNLOAD_TIMEOUT_SECONDS) as response, file_path.open("wb") as output:
            while True:
                chunk = response.read(8192)
                if not chunk:
                    break
                output.write(chunk)
    except URLError as exc:
        raise RuntimeError(f"Failed to download resource: {exc}") from exc
