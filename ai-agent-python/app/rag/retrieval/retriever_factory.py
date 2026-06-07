from langchain_core.documents import Document
from langchain_postgres import PGVectorStore

from app.rag.retrieval.doc_filter import (
    RetrievedChunk,
    filter_retrieval_results,
    to_documents,
)
from app.rag.retrieval.filters import RetrievalFilter, build_retrieval_filter
from app.rag.vectorstores.pgvector_store import get_pgvector_store


def retrieve_chunks(
    query: str,
    retrieval_filter: RetrievalFilter | None = None,
    *,
    store: PGVectorStore | None = None,
) -> list[RetrievedChunk]:
    filt = retrieval_filter or build_retrieval_filter(query)
    vector_store = store or get_pgvector_store()

    search_kwargs: dict = {"k": filt.top_k}
    if filt.metadata_filter:
        search_kwargs["filter"] = filt.metadata_filter
    if filt.score_threshold is not None:
        search_kwargs["score_threshold"] = filt.score_threshold

    results = vector_store.similarity_search_with_relevance_scores(
        query,
        **search_kwargs,
    )
    return [
        RetrievedChunk(document=document, score=score)
        for document, score in results
    ]


def retrieve_documents(
    query: str,
    retrieval_filter: RetrievalFilter | None = None,
    *,
    store: PGVectorStore | None = None,
) -> list[Document]:
    filt = retrieval_filter or build_retrieval_filter(query)
    chunks = retrieve_chunks(query, filt, store=store)
    filtered = filter_retrieval_results(
        chunks,
        score_threshold=filt.score_threshold,
    )
    return to_documents(filtered)
