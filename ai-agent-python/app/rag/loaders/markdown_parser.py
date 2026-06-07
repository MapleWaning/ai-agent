import re
from dataclasses import dataclass

HEADING_PATTERN = re.compile(r"^(#{1,6})\s+(.+?)\s*$")


@dataclass(frozen=True)
class MarkdownBlock:
    title: str
    section_path: str
    chunk_type: str
    content: str
    heading_level: int


def _normalize_heading_title(raw_title: str) -> str:
    return raw_title.strip()


def _build_section_path(category: str, headings: list[str]) -> str:
    parts = [category, *headings]
    return " > ".join(part for part in parts if part)


def parse_markdown_blocks(text: str, category: str) -> list[MarkdownBlock]:
    """
    解析 Markdown 标题与问答结构，生成结构化 block。

    当前文档约定：
    - `#` 为文档标题
    - `####` 为问答标题（问题）
    - 标题下方段落为回答内容
    """
    blocks: list[MarkdownBlock] = []
    heading_stack: list[tuple[int, str]] = []

    current_title: str | None = None
    current_level: int | None = None
    current_lines: list[str] = []

    def flush_current_block() -> None:
        nonlocal current_title, current_level, current_lines
        if current_title is None:
            current_lines = []
            return

        body = "\n".join(line.rstrip() for line in current_lines).strip()
        if not body:
            current_lines = []
            return

        section_titles = [title for _, title in heading_stack if title != category]
        if current_title not in section_titles:
            section_titles.append(current_title)

        chunk_type = "qa" if current_level == 4 else "section"
        content = f"#### {current_title}\n\n{body}" if chunk_type == "qa" else body

        blocks.append(
            MarkdownBlock(
                title=current_title,
                section_path=_build_section_path(category, section_titles),
                chunk_type=chunk_type,
                content=content,
                heading_level=current_level or 1,
            )
        )
        current_lines = []

    for raw_line in text.splitlines():
        line = raw_line.rstrip()
        match = HEADING_PATTERN.match(line)
        if not match:
            if current_title is not None:
                current_lines.append(line)
            continue

        level = len(match.group(1))
        title = _normalize_heading_title(match.group(2))

        if level == 1:
            heading_stack = [(1, category)]
            flush_current_block()
            current_title = None
            current_level = None
            continue

        flush_current_block()

        while heading_stack and heading_stack[-1][0] >= level:
            heading_stack.pop()
        heading_stack.append((level, title))

        current_title = title
        current_level = level

    flush_current_block()
    return blocks
