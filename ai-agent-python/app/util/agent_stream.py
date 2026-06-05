from collections.abc import AsyncIterator, Sequence

from langchain.agents import create_agent
from langchain_core.messages import AIMessageChunk, BaseMessage, SystemMessage
from langchain_core.tools import BaseTool
from langchain_openai import ChatOpenAI

from app.util.chat_context import ChatContext


def extract_chunk_text(chunk: AIMessageChunk) -> str:
    content = chunk.content
    if isinstance(content, str):
        return content
    if isinstance(content, list):
        parts: list[str] = []
        for item in content:
            if isinstance(item, str):
                parts.append(item)
            elif isinstance(item, dict) and item.get("type") == "text":
                parts.append(str(item.get("text", "")))
        return "".join(parts)
    return ""


def _split_messages(messages: list[BaseMessage]) -> tuple[str, list[BaseMessage]]:
    if not messages or not isinstance(messages[0], SystemMessage):
        raise ValueError("messages must start with SystemMessage")
    system_prompt = messages[0].content
    if not isinstance(system_prompt, str):
        system_prompt = str(system_prompt)
    return system_prompt, messages[1:]


async def stream_langchain_agent(
    model: ChatOpenAI,
    messages: list[BaseMessage],
    tools: Sequence[BaseTool],
    context: ChatContext | None = None,
) -> AsyncIterator[str]:
    system_prompt, agent_messages = _split_messages(messages)
    agent = create_agent(
        model,
        tools=list(tools),
        system_prompt=system_prompt,
        context_schema=ChatContext,
    )

    stream_kwargs: dict = {
        "stream_mode": "messages",
    }
    if context is not None:
        stream_kwargs["context"] = context

    async for message_chunk, metadata in agent.astream(
        {"messages": agent_messages},
        **stream_kwargs,
    ):
        if metadata.get("langgraph_node") != "model":
            continue
        if not isinstance(message_chunk, AIMessageChunk):
            continue
        text = extract_chunk_text(message_chunk)
        if text:
            yield text
