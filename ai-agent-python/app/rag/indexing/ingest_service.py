from pathlib import Path

from langchain_core.documents import Document
from langchain_postgres import PGVectorStore
from sqlalchemy import create_engine, text

from app.rag.indexing.index_pipeline import build_index_documents
from app.rag.vectorstores.pgvector_store import get_pgvector_store, iter_document_batches
from app.settings import settings
from app.rag.enrichers.keyword_enhancer import enhance_documents


def _get_indexed_count() -> int:
    engine = create_engine(settings.DATABASE_URL)
    query = text(
        f'SELECT COUNT(*) FROM "{settings.PGVECTOR_SCHEMA_NAME}"."{settings.PGVECTOR_TABLE_NAME}"'
    )
    with engine.connect() as conn:
        count = conn.execute(query).scalar()
    return int(count or 0)


def ingest_documents(
    documents: list[Document],
    *,
    store: PGVectorStore | None = None,
) -> list[str]:
    vector_store = store or get_pgvector_store()
    inserted_ids: list[str] = []

    for batch in iter_document_batches(documents):
        inserted_ids.extend(vector_store.add_documents(batch))

    return inserted_ids


async def ingest_directory(
    directory: str | Path | None = None,
    *,
    pattern: str = "*.md",
    store: PGVectorStore | None = None,
) -> list[str]:
    documents = build_index_documents(directory=directory, pattern=pattern)
    documents = [await enhance_documents(document) for document in documents]
    return ingest_documents(documents, store=store)


async def is_index_ready(
    directory: str | Path | None = None,
    *,
    pattern: str = "*.md",
    store: PGVectorStore | None = None,
) -> bool:
    expected_documents = build_index_documents(directory=directory, pattern=pattern)
    expected_count = len(expected_documents)
    if expected_count == 0:
        return True

    if _get_indexed_count() >= expected_count:
        return True

    await ingest_directory(directory=directory, pattern=pattern, store=store)
    return _get_indexed_count() >= expected_count
