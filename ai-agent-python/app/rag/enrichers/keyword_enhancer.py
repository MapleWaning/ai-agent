from langchain_core.documents import Document
from langchain_core.messages import HumanMessage, SystemMessage

from app.models.llm import create_default_model
from app.prompt.rag_prompt import KEYWORD_ENHANCEMENT_PROMPT


def _extract_text(content: object) -> str:
    if isinstance(content, str):
        return content.strip()
    return str(content).strip()


def _append_keywords(content: str, keywords: str) -> str:
    normalized_keywords = keywords.strip().rstrip("。")
    if not normalized_keywords:
        return content
    return f"{content.rstrip()}\n\n关键词：{normalized_keywords}"


async def enhance_documents(document: Document) -> Document:
    content = document.page_content.strip()
    if not content:
        return document

    model = create_default_model()
    response = await model.ainvoke(
        [
            SystemMessage(content=KEYWORD_ENHANCEMENT_PROMPT),
            HumanMessage(content=content),
        ]
    )
    keywords = _extract_text(response.content)
    enhanced_content = _append_keywords(content, keywords)

    return Document(
        page_content=enhanced_content,
        metadata=dict(document.metadata),
        id=document.id,
    )
