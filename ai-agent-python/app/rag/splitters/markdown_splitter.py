from langchain_core.documents import Document

from app.rag.splitters.token_splitter import split_document


def split_markdown_document(document: Document) -> list[Document]:
    """对 loader 产出的 Document 做 token 二次切分（过大 chunk 才拆分）。"""
    return split_document(document)


def split_markdown_documents(documents: list[Document]) -> list[Document]:
    results: list[Document] = []
    for document in documents:
        results.extend(split_markdown_document(document))
    return results
