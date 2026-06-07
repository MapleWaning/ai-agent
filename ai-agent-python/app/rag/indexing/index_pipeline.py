from pathlib import Path

from langchain_core.documents import Document

from app.rag.indexing.document_builder import build_documents
from app.rag.loaders.markdown_loader import load_markdown_documents
from app.rag.splitters.markdown_splitter import split_markdown_documents


def build_index_documents(
    directory: str | Path | None = None,
    *,
    pattern: str = "*.md",
) -> list[Document]:
    documents = load_markdown_documents(directory=directory, pattern=pattern)
    documents = split_markdown_documents(documents)
    return build_documents(documents)
