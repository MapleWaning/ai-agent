from langchain_core.documents import Document

from app.rag.retrieval.retriever_factory import retrieve_documents
from app.util.tool_stream import emit_tool_end, emit_tool_error, emit_tool_start

_RAG_TOOL_NAME = "rag_search"
_RAG_TOOL_LABEL = "知识库检索"


def format_document_context(documents: list[Document]) -> str:
    if not documents:
        return "当前知识库没有检索到相关内容。"

    sections: list[str] = []
    for index, document in enumerate(documents, start=1):
        metadata = document.metadata
        title = metadata.get("title", "")
        category = metadata.get("category", "")
        score = metadata.get("score")

        header = f"【参考{index}】{category} | {title}"
        if score is not None:
            header += f"（相关度：{score:.2f}）"
        sections.append(f"{header}\n{document.page_content.strip()}")

    return "\n\n".join(sections)


def assemble_rag_message(user_message: str, query: str | None = None) -> str:
    tool_input = query if query is not None else user_message.strip()
    emit_tool_start(_RAG_TOOL_NAME, _RAG_TOOL_LABEL, tool_input)
    try:
        query_text = user_message.strip()
        documents = retrieve_documents(query_text)
        context = format_document_context(documents)
        result = f"知识库参考内容：\n{context}\n\n用户问题：\n{query_text}"
        emit_tool_end(_RAG_TOOL_NAME, _RAG_TOOL_LABEL, "知识库检索完成")
        return result
    except Exception as exc:
        emit_tool_error(_RAG_TOOL_NAME, _RAG_TOOL_LABEL, f"知识库检索失败: {exc}", tool_input)
        raise


class RAGService:
    def assemble_message(self, user_message: str, query: str | None = None) -> str:
        return assemble_rag_message(user_message, query=query)


def get_rag_service() -> RAGService:
    return RAGService()
