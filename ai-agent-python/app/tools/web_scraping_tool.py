from langchain.tools import tool

from app.util.web_scraping import scrape_web_page_content


@tool
def scrape_web_page(url: str) -> str:
    """Scrape the content of a web page."""
    try:
        return scrape_web_page_content(url)
    except Exception as exc:
        return f"Error scraping web page: {exc}"
