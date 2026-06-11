/** 会话实体（后端落库结构） */
export interface Chat {
  chatId?: number
  userId?: number
  title?: string
  createTime?: string
  modifyTime?: string
}

/** 会话 VO（列表 / 详情 / 更新响应） */
export interface ChatVO {
  chatId: number
  title: string
  createTime: string
  modifyTime: string
}

/** 更新会话标题请求 */
export interface ChatUpdateRequest {
  title: string
}

/** LangChain 消息 content 解析后的结构 */
export interface LangChainMessage {
  type: 'human' | 'ai'
  data: {
    content: string
    type: 'human' | 'ai'
    additional_kwargs?: Record<string, unknown>
    response_metadata?: Record<string, unknown>
    name?: string | null
    id?: string | null
    tool_calls?: unknown[]
    invalid_tool_calls?: unknown[]
    usage_metadata?: unknown | null
  }
}

/** 聊天历史记录（content 为 LangChain JSON 字符串，需要 JSON.parse） */
export interface ChatHistory {
  id: number
  chatId: number
  userId: number
  content: string
  /** 该轮对话路由类型：normal_chat / report / rag / mcp / tool / workflow */
  type?: string | null
}

/** 路由决策请求 */
export interface RouteRequest {
  initPrompt: string
}

/** 路由决策响应 */
export interface RouteResponse {
  routeType: string
  /** TODO: 该字段含义需要人工确认（api-contract.md 标注） */
  enumName: string
  reason: string
}

/** 路由类型（请求体中传 JSON 值字符串） */
export type RouteType =
  | 'normal_chat'
  | 'report'
  | 'rag'
  | 'mcp'
  | 'tool'
  | 'workflow'

/** 流式聊天请求（ChatRequest） */
export interface ChatRequest {
  message: string
  /** 会话 ID，字符串类型 */
  chatId: string
  /** TODO: 不传时 Python 端默认行为需人工确认（api-contract.md 标注） */
  routeType?: RouteType
  /** 无需前端传递，后端从 Session 覆盖 */
  userId?: string
}

/* ----------------------- 前端 UI 模型（非后端字段） ----------------------- */

export type MessageRole = 'user' | 'assistant'

/**
 * 后端 SSE / 落库的原始事件结构。
 * 落库格式见 ChatHistoryServiceImpl：additional_kwargs.events = [{ event, data }, ...]，
 * 取值范围：workflow_step / tool_start / tool_end / tool_error / file。
 */
export interface StreamEventRecord {
  event: string
  data: unknown
}

/** 活动（工具 / 工作流步骤 / 文件）的统一状态 */
export type ActivityStatus = 'running' | 'finished' | 'failed'

/** 工具 / MCP / RAG 调用活动（由 tool_start/tool_end/tool_error 合并而来） */
export interface ToolActivity {
  type: 'tool'
  /** 前端渲染用唯一 key */
  key: string
  /** 工具标识，如 search_web / rag_search */
  name: string
  /** 工具中文名，如「网页搜索」「知识库检索」 */
  label: string
  status: ActivityStatus
  /** 工具入参（tool_start / tool_error 携带） */
  input?: unknown
  /** 完成摘要（tool_end 携带） */
  summary?: string
  /** 错误信息（tool_error 携带） */
  error?: string
}

/** 工作流单步骤（由 workflow_step 的 running/finished 合并而来） */
export interface WorkflowStepActivity {
  type: 'workflow_step'
  key: string
  step: number
  title: string
  status: 'running' | 'finished'
  detail?: string
}

/** 生成 / 处理文件活动（由 file 事件而来） */
export interface FileActivity {
  type: 'file'
  key: string
  fileName: string
  action: string
  summary: string
}

/** 统一的对话活动（思路链 / 功能触发提示） */
export type ChatActivity = ToolActivity | WorkflowStepActivity | FileActivity

/** 前端聊天消息模型 */
export interface UIMessage {
  /** 前端本地唯一 id */
  id: string
  role: MessageRole
  content: string
  /** 本轮路由类型（仅本次发送的 AI 消息有；历史消息无此信息） */
  routeType?: RouteType | null
  /** 本轮路由原因（来自 /agent/chat/route） */
  routeReason?: string | null
  /**
   * 思路链 / 功能触发活动时间线（工具、MCP、RAG、文件、工作流步骤）。
   * 流式时实时追加，历史回放时由 additional_kwargs.events 重建。
   */
  activities?: ChatActivity[]
  /** 流式过程中的临时状态文案（如「正在启动复杂工作流...」），不落库 */
  statusText?: string | null
  /** 是否正在流式输出中 */
  streaming?: boolean
  /** 是否发生错误（流中断等） */
  error?: boolean
}
