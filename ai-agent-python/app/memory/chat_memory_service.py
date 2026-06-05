from typing import List, Union

from langchain_community.chat_message_histories import RedisChatMessageHistory
from langchain_core.messages import BaseMessage

from app.models.schemas import RouteType
from app.settings import settings


class MemoryService:
    """
    Python 侧只读 Redis 记忆服务。

    设计原则：
    1. Java 负责写入 Redis 和 MySQL，并控制历史条数（含去重当前消息等）。
    2. Python 不写入对话记忆，只按需读取 Java 已写入的 LangChain 格式历史。
    3. 不同 routeType 读取不同数量的历史消息。
    """

    MEMORY_LIMIT_BY_ROUTE = {
        RouteType.NORMAL_CHAT: 12,
        RouteType.RAG: 4,
        RouteType.REPORT: 50,
        RouteType.MCP: 4,
        RouteType.TOOL: 6,
        RouteType.WORKFLOW: 12,
    }

    def __init__(
        self,
        redis_url: str,
        key_prefix: str = "message_store:",
        ttl: int | None = None,
        default_limit: int = 8,
    ):
        self.redis_url = redis_url
        self.key_prefix = key_prefix
        self.ttl = ttl
        self.default_limit = default_limit

    def build_session_id(self, user_id: str, chat_id: str) -> str:
        """与 Java 约定 session_id 格式：{userId}_{chatId}"""
        return f"{user_id}_{chat_id}"

    def get_limit_by_route(self, route_type: Union[RouteType, str]) -> int:
        if isinstance(route_type, RouteType):
            return self.MEMORY_LIMIT_BY_ROUTE.get(route_type, self.default_limit)
        try:
            route = RouteType(route_type)
            return self.MEMORY_LIMIT_BY_ROUTE.get(route, self.default_limit)
        except ValueError:
            return self.default_limit

    def _create_history(self, user_id: str, chat_id: str) -> RedisChatMessageHistory:
        return RedisChatMessageHistory(
            session_id=self.build_session_id(user_id, chat_id),
            url=self.redis_url,
            key_prefix=self.key_prefix,
            ttl=self.ttl,
        )

    def get_recent_messages(
        self,
        user_id: str,
        chat_id: str,
        route_type: Union[RouteType, str],
    ) -> List[BaseMessage]:
        """
        从 Redis 读取最近 N 条历史（N 由 routeType 决定）。
        Java 已负责裁剪与去重，此处直接读取指定条数。
        """
        limit = self.get_limit_by_route(route_type)
        history = self._create_history(user_id, chat_id)
        messages = history.messages
        if limit <= 0:
            return []
        return messages[-limit:]


def get_memory_service() -> MemoryService:
    return MemoryService(
        redis_url=settings.REDIS_URL,
        key_prefix=settings.REDIS_KEY_PREFIX,
        ttl=settings.REDIS_TTL,
    )
