from functools import lru_cache
from pathlib import Path

from langchain_core.documents import Document

from app.rag.loaders.markdown_parser import MarkdownBlock, parse_markdown_blocks
from app.settings import settings

CATEGORY_PREFIX_MAP: dict[str, str] = {
    "恋爱篇": "love_app",
    "单身篇": "love_single",
    "已婚篇": "love_married",
}


def extract_category(file_name: str) -> str:
    """从文件名提取 category，例如 `恋爱常见问题和回答 - 恋爱篇.md` -> `恋爱篇`。"""
    stem = Path(file_name).stem
    if " - " in stem:
        return stem.rsplit(" - ", 1)[-1].strip()
    if "-" in stem:
        return stem.rsplit("-", 1)[-1].strip()
    return stem


def resolve_chunk_prefix(category: str) -> str:
    return CATEGORY_PREFIX_MAP.get(category, "love_doc")


def build_chunk_id(prefix: str, chunk_index: int) -> str:
    return f"{prefix}_{chunk_index:04d}"


@lru_cache(maxsize=1)
def _get_token_encoder():
    try:
        import tiktoken
    except ImportError:
        return None
    return tiktoken.get_encoding("cl100k_base")


def count_tokens(text: str) -> int:
    encoder = _get_token_encoder()
    if encoder is not None:
        return len(encoder.encode(text))
    return max(1, len(text) // 2)


def _read_markdown_text(file_path: Path) -> str:
    return file_path.read_text(encoding="utf-8")


def _block_to_document(
    block: MarkdownBlock,
    *,
    source: str,
    category: str,
    chunk_id: str,
    chunk_index: int,
) -> Document:
    token_count = count_tokens(block.content)
    metadata = {
        "source": source,
        "category": category,
        "title": block.title,
        "section_path": block.section_path,
        "chunk_id": chunk_id,
        "chunk_index": chunk_index,
        "chunk_type": block.chunk_type,
        "token_count": token_count,
    }
    return Document(page_content=block.content, metadata=metadata)


def load_markdown_file(
    file_path: str | Path,
    *,
    start_index: int = 1,
) -> list[Document]:
    path = Path(file_path).resolve()
    text = _read_markdown_text(path)
    source = path.name
    category = extract_category(source)
    prefix = resolve_chunk_prefix(category)

    blocks = parse_markdown_blocks(text, category)
    documents: list[Document] = []
    for offset, block in enumerate(blocks):
        chunk_index = start_index + offset
        documents.append(
            _block_to_document(
                block,
                source=source,
                category=category,
                chunk_id=build_chunk_id(prefix, chunk_index),
                chunk_index=chunk_index,
            )
        )
    return documents


def load_markdown_documents(
    directory: str | Path | None = None,
    *,
    pattern: str = "*.md",
) -> list[Document]:
    doc_dir = Path(directory or settings.DEFAULT_DOCUMENT_DIR).resolve()
    documents: list[Document] = []
    chunk_counter: dict[str, int] = {}

    for file_path in sorted(doc_dir.glob(pattern)):
        if not file_path.is_file():
            continue

        category = extract_category(file_path.name)
        prefix = resolve_chunk_prefix(category)
        start_index = chunk_counter.get(prefix, 1)

        file_documents = load_markdown_file(file_path, start_index=start_index)
        documents.extend(file_documents)

        if file_documents:
            chunk_counter[prefix] = start_index + len(file_documents)

    return documents
