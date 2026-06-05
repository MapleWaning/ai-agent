from urllib.error import URLError
from urllib.parse import urlparse
from urllib.request import Request, urlopen

from bs4 import BeautifulSoup

_SCRAPE_TIMEOUT_SECONDS = 60
_USER_AGENT = "yu-ai-agent-web-scraper/1.0"


def _validate_scrape_url(url: str) -> str:
    normalized = url.strip()
    parsed = urlparse(normalized)
    if parsed.scheme not in {"http", "https"} or not parsed.netloc:
        raise ValueError(f"Invalid URL: {url}")
    return normalized


def scrape_web_page_content(url: str) -> str:
    validated_url = _validate_scrape_url(url)
    request = Request(validated_url, headers={"User-Agent": _USER_AGENT})
    try:
        with urlopen(request, timeout=_SCRAPE_TIMEOUT_SECONDS) as response:
            charset = response.headers.get_content_charset() or "utf-8"
            html = response.read().decode(charset, errors="replace")
    except URLError as exc:
        raise RuntimeError(f"Failed to fetch web page: {exc}") from exc

    document = BeautifulSoup(html, "lxml")
    return document.prettify()
