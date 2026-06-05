from app.models.schemas import RouteType
from app.prompt.base_prompt import BASE_PROMPT
from app.prompt.mcp_prompt import MCP_PROMPT
from app.prompt.normal_prompt import NORMAL_PROMPT
from app.prompt.rag_prompt import RAG_PROMPT
from app.prompt.report_prompt import REPORT_PROMPT
from app.prompt.tool_prompt import TOOL_PROMPT
from app.prompt.workflow_prompt import WORKFLOW_PROMPT


class PromptRegistry:
    def __init__(self):
        self.base_prompt = BASE_PROMPT
        self.route_prompts = {
            RouteType.NORMAL_CHAT.value: NORMAL_PROMPT,
            RouteType.RAG.value: RAG_PROMPT,
            RouteType.REPORT.value: REPORT_PROMPT,
            RouteType.MCP.value: MCP_PROMPT,
            RouteType.TOOL.value: TOOL_PROMPT,
            RouteType.WORKFLOW.value: WORKFLOW_PROMPT,
        }

    def get_system_prompt(self, route_type: RouteType) -> str:
        return self.base_prompt + "\n\n" + self.route_prompts[route_type.value]
