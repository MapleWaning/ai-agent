from dataclasses import dataclass


@dataclass
class ChatContext:
    """Agent 调用时注入的运行时上下文，对应 ChatRequest 中的 userId / chatId。"""

    user_id: str
    chat_id: str
