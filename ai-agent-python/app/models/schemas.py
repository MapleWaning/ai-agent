from enum import Enum
from typing import Any, Optional

from pydantic import BaseModel, Field, field_validator



class RouteType(str, Enum):
    NORMAL_CHAT = "normal_chat"
    REPORT = "report"
    RAG = "rag"
    MCP = "mcp"
    TOOL = "tool"
    WORKFLOW = "workflow"

    @classmethod
    def normalize(cls, value: Any) -> Any:
        if isinstance(value, cls):
            return value
        if isinstance(value, str):
            value = value.strip()
            for code_gen_type in cls:
                if value == code_gen_type.value or value == code_gen_type.name:
                    return code_gen_type
        return value


class RouteRequest(BaseModel):
    initPrompt: str = Field(..., min_length=1)


class RouteDecision(BaseModel):
    routeType: RouteType = Field(..., description="Recommended agent type")
    reason: str = Field(..., description="Short reason for the selected type")

    @field_validator("routeType", mode="before")
    @classmethod
    def normalize_route_type(cls, value: Any) -> Any:
        return RouteType.normalize(value)


class RouteResponse(BaseModel):
    routeType: str
    enumName: str
    reason: str