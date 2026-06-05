import json
from urllib.error import URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen

PEXELS_SEARCH_API_URL = "https://api.pexels.com/v1/search"
_SEARCH_TIMEOUT_SECONDS = 60
_USER_AGENT = "yu-ai-agent-picture-search/1.0"


def search_medium_images(query: str, api_key: str) -> list[str]:
    normalized_query = query.strip()
    if not normalized_query:
        raise ValueError("Search query must not be empty")

    params = {"query": normalized_query}
    request_url = f"{PEXELS_SEARCH_API_URL}?{urlencode(params)}"
    request = Request(
        request_url,
        headers={
            "Authorization": api_key,
            "User-Agent": _USER_AGENT,
        },
    )
    try:
        with urlopen(request, timeout=_SEARCH_TIMEOUT_SECONDS) as response:
            payload = json.loads(response.read().decode("utf-8"))
    except URLError as exc:
        raise RuntimeError(f"Failed to call Pexels API: {exc}") from exc

    image_urls: list[str] = []
    for photo in payload.get("photos") or []:
        src = photo.get("src") or {}
        medium_url = src.get("medium")
        if medium_url:
            image_urls.append(medium_url)
    return image_urls


def search_picture_content(query: str, api_key: str) -> str:
    return ",".join(search_medium_images(query, api_key))
