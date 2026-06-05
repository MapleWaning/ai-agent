from collections.abc import AsyncIterator

from langchain_core.messages import BaseMessage
from langchain_openai import ChatOpenAI

from app.util.chat_context import ChatContext


async def execute_agent(
    model: ChatOpenAI,
    messages: list[BaseMessage],
    context: ChatContext | None = None,
) -> AsyncIterator[str]:
    print(f"调用了 RAG executor, model={model.model_name}, messages={len(messages)}")
    yield "[RAG] executor placeholder\n"
