from dataclasses import dataclass

from langchain_core.documents import Document

REQUIRED_METADATA_KEYS = ("chunk_id", "title", "category", "chunk_type")


@dataclass(frozen=True)
class RetrievedChunk:
    document: Document
    score: float


def normalize_document(document: Document, score: float) -> Document:
    metadata = dict(document.metadata)
    metadata["score"] = score
    return Document(
        page_content=document.page_content.strip(),
        metadata=metadata,
        id=document.id,
    )


def is_valid_chunk(document: Document) -> bool:
    if not document.page_content.strip():
        return False

    metadata = document.metadata
    if metadata.get("chunk_type") not in {None, "qa", "section"}:
        return False

    return all(metadata.get(key) for key in REQUIRED_METADATA_KEYS)


def deduplicate_chunks(chunks: list[RetrievedChunk]) -> list[RetrievedChunk]:
    seen: set[str] = set()
    unique_chunks: list[RetrievedChunk] = []

    for chunk in chunks:
        chunk_id = str(
            chunk.document.metadata.get("chunk_id") or chunk.document.id or ""
        )
        if not chunk_id or chunk_id in seen:
            continue
        seen.add(chunk_id)
        unique_chunks.append(chunk)

    return unique_chunks


def filter_retrieval_results(
    chunks: list[RetrievedChunk],
    *,
    score_threshold: float | None = None,
) -> list[RetrievedChunk]:
    filtered: list[RetrievedChunk] = []

    for chunk in chunks:
        if score_threshold is not None and chunk.score < score_threshold:
            continue

        document = normalize_document(chunk.document, chunk.score)
        if not is_valid_chunk(document):
            continue

        filtered.append(RetrievedChunk(document=document, score=chunk.score))

    return deduplicate_chunks(filtered)


def to_documents(chunks: list[RetrievedChunk]) -> list[Document]:
    return [chunk.document for chunk in chunks]
