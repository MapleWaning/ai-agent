from langchain_core.documents import Document

from app.rag.retrieval.retriever_factory import retrieve_documents


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


def assemble_rag_message(user_message: str) -> str:
    query = user_message.strip()
    documents = retrieve_documents(query)
    context = format_document_context(documents)
    return f"知识库参考内容：\n{context}\n\n用户问题：\n{query}"


class RAGService:
    def assemble_message(self, user_message: str) -> str:
        return assemble_rag_message(user_message)


def get_rag_service() -> RAGService:
    return RAGService()
