import json
from typing import Any


def format_sse(data: Any, event: str = "message") -> str:
    """
    将数据格式化为标准 SSE（Server-Sent Events）文本块。

    为保证大模型输出的全部字符（包括 ``\\n``、``\\r`` 等）在传输过程中
    不被 SSE 的“按行拆分”语义丢弃，payload 统一以 JSON 字符串的形式
    写入单行 ``data:``，由消费端反序列化还原。

    格式示例::
        data: "hello\\nworld"

        event: message
        data: "hello"

    """
    lines: list[str] = []
    lines.append(f"event: {event}")

    # text = "" if data is None else str(data)
    # JSON 序列化将换行/回车等控制字符转义为单行可见序列，
    # 避免 SSE 多行 data 语义把这些字符当作分隔符丢弃。
    payload = json.dumps(data, ensure_ascii=False)
    lines.append(f"data: {payload}")

    lines.append("")
    return "\n".join(lines) + "\n"
