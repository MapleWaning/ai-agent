from functools import lru_cache

from langchain_core.documents import Document
from langchain_text_splitters import TokenTextSplitter

from app.rag.loaders.markdown_loader import count_tokens
from app.settings import settings


@lru_cache(maxsize=1)
def create_token_text_splitter() -> TokenTextSplitter:
    return TokenTextSplitter(
        chunk_size=settings.TOKEN_TEXT_SPLITTER_CHUNK_SIZE,
        chunk_overlap=settings.TOKEN_TEXT_SPLITTER_CHUNK_OVERLAP,
        encoding_name="cl100k_base",
    )


def is_oversized(text: str) -> bool:
    return count_tokens(text) > settings.TOKEN_TEXT_SPLITTER_CHUNK_SIZE


def split_text(text: str) -> list[str]:
    return create_token_text_splitter().split_text(text)


def _build_sub_chunk_metadata(
    parent_metadata: dict,
    *,
    content: str,
    sub_chunk_index: int,
) -> dict:
    metadata = dict(parent_metadata)
    parent_chunk_id = str(metadata.get("chunk_id", "chunk"))
    metadata["parent_chunk_id"] = parent_chunk_id
    metadata["sub_chunk_index"] = sub_chunk_index
    metadata["chunk_id"] = f"{parent_chunk_id}_p{sub_chunk_index:02d}"
    metadata["token_count"] = count_tokens(content)
    return metadata


def split_document(document: Document) -> list[Document]:
    if not is_oversized(document.page_content):
        metadata = dict(document.metadata)
        metadata["token_count"] = count_tokens(document.page_content)
        return [Document(page_content=document.page_content, metadata=metadata)]

    chunks = split_text(document.page_content)
    return [
        Document(
            page_content=chunk,
            metadata=_build_sub_chunk_metadata(
                document.metadata,
                content=chunk,
                sub_chunk_index=index,
            ),
        )
        for index, chunk in enumerate(chunks, start=1)
    ]


def split_documents(documents: list[Document]) -> list[Document]:
    results: list[Document] = []
    for document in documents:
        results.extend(split_document(document))
    return results
