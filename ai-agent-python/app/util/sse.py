def format_sse(data: str, event: str | None = None) -> str:
    """
    将数据格式化为标准 SSE（Server-Sent Events）文本块。

    格式示例::
        data: hello

        event: message
        data: hello

    """
    lines: list[str] = []
    if event:
        lines.append(f"event: {event}")

    text = "" if data is None else str(data)
    payload_lines = text.split("\n") if text else [""]
    for line in payload_lines:
        lines.append(f"data: {line}")

    lines.append("")
    return "\n".join(lines) + "\n"
