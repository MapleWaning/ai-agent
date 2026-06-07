from dataclasses import dataclass

from app.settings import settings

CATEGORY_KEYWORDS: dict[str, list[str]] = {
    "单身篇": ["单身", "脱单", "相亲", "找对象", "潜在伴侣", "线上交友"],
    "恋爱篇": ["恋爱", "交往", "浪漫", "争吵", "喜欢", "情侣", "男友", "女友", "约会"],
    "已婚篇": ["已婚", "结婚", "婚后", "夫妻", "婆媳", "婚姻", "家庭责任"],
}


@dataclass(frozen=True)
class RetrievalFilter:
    top_k: int
    score_threshold: float | None
    metadata_filter: dict | None = None


def infer_category(query: str) -> str | None:
    normalized = query.strip()
    if not normalized:
        return None

    matched: list[tuple[str, int]] = []
    for category, keywords in CATEGORY_KEYWORDS.items():
        hits = sum(1 for keyword in keywords if keyword in normalized)
        if hits:
            matched.append((category, hits))

    if not matched:
        return None

    matched.sort(key=lambda item: item[1], reverse=True)
    return matched[0][0]


def build_metadata_filter(
    *,
    category: str | None = None,
    chunk_type: str | None = "qa",
    source: str | None = None,
    extra: dict | None = None,
) -> dict | None:
    conditions: list[dict] = []

    if category:
        conditions.append({"category": category})
    if chunk_type:
        conditions.append({"chunk_type": chunk_type})
    if source:
        conditions.append({"source": source})
    if extra:
        conditions.append(extra)

    if not conditions:
        return None
    if len(conditions) == 1:
        return conditions[0]
    return {"$and": conditions}


def build_retrieval_filter(
    query: str | None = None,
    *,
    category: str | None = None,
    chunk_type: str | None = "qa",
    source: str | None = None,
    top_k: int | None = None,
    score_threshold: float | None = None,
    extra_metadata: dict | None = None,
    auto_category: bool = True,
) -> RetrievalFilter:
    resolved_category = category
    if resolved_category is None and auto_category and query:
        resolved_category = infer_category(query)

    return RetrievalFilter(
        top_k=top_k or settings.RAG_RETRIEVAL_TOP_K,
        score_threshold=(
            settings.RAG_SCORE_THRESHOLD
            if score_threshold is None
            else score_threshold
        ),
        metadata_filter=build_metadata_filter(
            category=resolved_category,
            chunk_type=chunk_type,
            source=source,
            extra=extra_metadata,
        ),
    )
