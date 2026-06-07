from langchain_core.messages import HumanMessage, SystemMessage

from app.models.llm import create_default_model
from app.prompt.rag_prompt import REWRITE_PROMPT


def _extract_text(content: object) -> str:
    if isinstance(content, str):
        return content.strip()
    return str(content).strip()


async def rewrite_user_message(user_message: str) -> str:
    query = user_message.strip()
    if not query:
        return query

    model = create_default_model()
    response = await model.ainvoke(
        [
            SystemMessage(content=REWRITE_PROMPT),
            HumanMessage(content=f"用户原始输入：{query}\n\n请输出改写后的检索问题："),
        ]
    )
    rewritten = _extract_text(response.content)
    result = rewritten or query
    print(f"[RAG Query Rewrite] {result}", flush=True)
    return result
