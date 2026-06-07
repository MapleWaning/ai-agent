import uuid

from langchain_core.documents import Document

from app.rag.loaders.markdown_loader import count_tokens

DOCUMENT_ID_NAMESPACE = uuid.UUID("f47ac10b-58cc-4372-a567-0e02b2c3d479")


def resolve_document_id(metadata: dict) -> str:
    chunk_id = metadata.get("chunk_id")
    if chunk_id:
        return str(uuid.uuid5(DOCUMENT_ID_NAMESPACE, str(chunk_id)))
    return str(uuid.uuid4())


def build_document(document: Document) -> Document:
    metadata = dict(document.metadata)
    metadata["token_count"] = count_tokens(document.page_content)
    document_id = document.id or resolve_document_id(metadata)
    return Document(
        page_content=document.page_content,
        metadata=metadata,
        id=document_id,
    )


def build_documents(documents: list[Document]) -> list[Document]:
    return [build_document(document) for document in documents]
