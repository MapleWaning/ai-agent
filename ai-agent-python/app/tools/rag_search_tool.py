from langchain.tools import ToolRuntime, tool
from app.util.chat_context import ChatContext
from app.rag.rag_service import get_rag_service
from app.rag.enrichers.query_rewriter import rewrite_user_message

@tool
async def rag_search(
    query: str,
    runtime: ToolRuntime[ChatContext],
) -> str:
    """Search the RAG knowledge base for relevant information."""
    rewritten_message = await rewrite_user_message(query)
    return get_rag_service().assemble_message(rewritten_message)