from functools import lru_cache
from typing import Iterator

from asyncpg.exceptions import DuplicateTableError
from langchain_core.documents import Document
from langchain_postgres import PGEngine, PGVectorStore
from langchain_postgres.v2.indexes import (
    BaseIndex,
    DistanceStrategy,
    HNSWIndex,
    IVFFlatIndex,
)
from sqlalchemy.exc import ProgrammingError

from app.models.llm import create_vector_model
from app.settings import settings

_DISTANCE_STRATEGY_MAP: dict[str, DistanceStrategy] = {
    "COSINE_DISTANCE": DistanceStrategy.COSINE_DISTANCE,
    "COSINE": DistanceStrategy.COSINE_DISTANCE,
    "EUCLIDEAN": DistanceStrategy.EUCLIDEAN,
    "L2": DistanceStrategy.EUCLIDEAN,
    "INNER_PRODUCT": DistanceStrategy.INNER_PRODUCT,
}


def _to_async_database_url(url: str) -> str:
    """PGEngine uses asyncpg; settings DATABASE_URL uses psycopg driver."""
    if url.startswith("postgresql+psycopg://"):
        return url.replace("postgresql+psycopg://", "postgresql+asyncpg://", 1)
    if url.startswith("postgresql://"):
        return url.replace("postgresql://", "postgresql+asyncpg://", 1)
    return url


def _resolve_distance_strategy() -> DistanceStrategy:
    key = settings.PGVECTOR_DISTANCE_TYPE.strip().upper()
    try:
        return _DISTANCE_STRATEGY_MAP[key]
    except KeyError as exc:
        supported = ", ".join(sorted(_DISTANCE_STRATEGY_MAP))
        raise ValueError(
            f"Unsupported PGVECTOR_DISTANCE_TYPE={settings.PGVECTOR_DISTANCE_TYPE!r}. "
            f"Supported values: {supported}"
        ) from exc


def _build_vector_index(distance_strategy: DistanceStrategy) -> BaseIndex | None:
    index_type = settings.PGVECTOR_INDEX_TYPE.strip().upper()
    if index_type == "HNSW":
        return HNSWIndex(distance_strategy=distance_strategy)
    if index_type == "IVFFLAT":
        return IVFFlatIndex(distance_strategy=distance_strategy)
    return None


@lru_cache(maxsize=1)
def create_pg_engine() -> PGEngine:
    return PGEngine.from_connection_string(_to_async_database_url(settings.DATABASE_URL))


def _init_vectorstore_table(engine: PGEngine) -> None:
    try:
        engine.init_vectorstore_table(
            table_name=settings.PGVECTOR_TABLE_NAME,
            vector_size=settings.PGVECTOR_DIMENSIONS,
            schema_name=settings.PGVECTOR_SCHEMA_NAME,
        )
    except ProgrammingError as exc:
        if isinstance(getattr(exc, "orig", None), DuplicateTableError):
            return
        if "already exists" in str(exc).lower():
            return
        raise


def _ensure_vector_index(
    store: PGVectorStore,
    index: BaseIndex | None,
) -> None:
    if index is None or store.is_valid_index():
        return
    store.apply_vector_index(index)


def create_pgvector_store(*, init_schema: bool | None = None) -> PGVectorStore:
    engine = create_pg_engine()
    should_init_schema = (
        settings.PGVECTOR_INIT_SCHEMA if init_schema is None else init_schema
    )
    if should_init_schema:
        _init_vectorstore_table(engine)

    distance_strategy = _resolve_distance_strategy()
    store = PGVectorStore.create_sync(
        engine=engine,
        embedding_service=create_vector_model(),
        table_name=settings.PGVECTOR_TABLE_NAME,
        schema_name=settings.PGVECTOR_SCHEMA_NAME,
        distance_strategy=distance_strategy,
    )

    if should_init_schema:
        _ensure_vector_index(store, _build_vector_index(distance_strategy))

    return store


@lru_cache(maxsize=1)
def get_pgvector_store() -> PGVectorStore:
    return create_pgvector_store()


def iter_document_batches(
    documents: list[Document],
    batch_size: int | None = None,
) -> Iterator[list[Document]]:
    size = batch_size or settings.PGVECTOR_MAX_DOCUMENT_BATCH_SIZE
    for start in range(0, len(documents), size):
        yield documents[start : start + size]
