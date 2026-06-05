from langchain.tools import tool

from app.settings import settings
from app.util.picture_search import search_picture_content


@tool
def picture_search(query: str) -> str:
    """search image from web"""
    try:
        return search_picture_content(query, settings.PEXELS_API_KEY)
    except Exception as exc:
        return f"Error search image: {exc}"
