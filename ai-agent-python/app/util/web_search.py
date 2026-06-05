import json
from urllib.error import URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen

SEARCH_API_URL = "https://www.searchapi.io/api/v1/search"
SEARCH_ENGINE = "baidu"
MAX_RESULTS = 5
_SEARCH_TIMEOUT_SECONDS = 60
_USER_AGENT = "yu-ai-agent-web-search/1.0"


def search_web_content(query: str, api_key: str) -> str:
    normalized_query = query.strip()
    if not normalized_query:
        raise ValueError("Search query must not be empty")

    params = {
        "q": normalized_query,
        "api_key": api_key,
        "engine": SEARCH_ENGINE,
    }
    request_url = f"{SEARCH_API_URL}?{urlencode(params)}"
    request = Request(request_url, headers={"User-Agent": _USER_AGENT})
    try:
        with urlopen(request, timeout=_SEARCH_TIMEOUT_SECONDS) as response:
            payload = json.loads(response.read().decode("utf-8"))
    except URLError as exc:
        raise RuntimeError(f"Failed to call SearchAPI: {exc}") from exc

    organic_results = payload.get("organic_results") or []
    top_results = organic_results[:MAX_RESULTS]
    return ",".join(json.dumps(item, ensure_ascii=False) for item in top_results)
