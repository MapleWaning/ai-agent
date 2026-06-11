from typing import Any

from langchain_core.messages import AIMessageChunk

from app.util.agent_stream import extract_chunk_text
from app.util.sse import format_sse


def is_event_dict(chunk: Any) -> bool:
    return isinstance(chunk, dict) and "event" in chunk


def format_agent_stream_chunk(chunk: Any) -> str | None:
    if isinstance(chunk, AIMessageChunk):
        text = extract_chunk_text(chunk)
        if text:
            return format_sse(text, event="message")
        return None

    if is_event_dict(chunk):
        event = chunk.get("event", "status")
        data = chunk.get("data", "")
        return format_sse(data, event=event)

    if isinstance(chunk, str):
        return format_sse(chunk, event="status")

    return None
