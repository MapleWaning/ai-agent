from langchain.tools import tool

from app.settings import settings
from app.util.web_search import search_web_content


@tool
def search_web(query: str) -> str:
    """Search for information from Baidu Search Engine."""
    try:
        return search_web_content(query, settings.SEARCH_API_KEY)
    except Exception as exc:
        return f"Error searching Baidu: {exc}"
