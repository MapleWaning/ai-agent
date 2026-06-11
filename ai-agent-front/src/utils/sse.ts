import { API_BASE_URL } from '@/api/request'

/** SSE 流结束标识（不区分大小写） */
const DONE_FLAGS = ['done', '[done]']

/** SSE 请求错误 */
export class SSEError extends Error {
  status: number
  constructor(message: string, status: number) {
    super(message)
    this.name = 'SSEError'
    this.status = status
  }
}

export interface StreamChatBody {
  message: string
  chatId: string
  /** 路由类型，JSON 值字符串；不传时由后端 / Python 决定（TODO） */
  routeType?: string
  /** 严禁前端传 userId，后端从 Session 注入 */
}

export interface StreamCallbacks {
  /** 收到一段正文内容片段（event: message） */
  onMessage: (chunk: string) => void
  /**
   * 收到结构化事件（route / status / workflow_step / tool_start / tool_end /
   * tool_error / file 等非正文事件）。data 已尽量反序列化为对象或字符串。
   */
  onEvent?: (event: string, data: unknown) => void
  /** 流正常结束（收到 done 或连接关闭） */
  onDone?: () => void
  /** 出错（HTTP 非 2xx、流中断、event: error 等） */
  onError?: (error: unknown) => void
}

/** 单个已解析的 SSE 事件 */
interface ParsedSseEvent {
  event: string
  data: string
}

/**
 * 从单个 SSE 事件块中解析出事件名与 data 内容。
 * 一个事件可能包含多行 data:，按 SSE 规范用换行拼接；event: 行给出事件名。
 */
function parseSseEvent(rawEvent: string): ParsedSseEvent | null {
  const lines = rawEvent.split('\n')
  let event = 'message'
  const dataLines: string[] = []
  for (const line of lines) {
    if (line.startsWith('event:')) {
      event = line.slice(6).trim()
    } else if (line.startsWith('data:')) {
      // 仅去掉 "data:" 前缀，不能再 strip 前导空格。
      // 后端 Spring SseEmitter 写出的是 "data:<value>"（无 SSE 约定的装饰性空格，
      // Python 的装饰性空格已在 Java 网关 readPythonSseAndForward 中剥离）。
      // 若此处再去掉一个前导空格，会吃掉分片本身的有效空格，
      // 导致 "### 标题" / "1. xxx" / "- xxx" 等 Markdown 标记后的空格丢失，
      // 标题/列表偶发无法渲染（刷新后从库里读取完整内容则正常）。
      dataLines.push(line.slice(5))
    }
  }
  if (dataLines.length === 0) return null
  return { event, data: dataLines.join('\n') }
}

/**
 * 解码正文分片（event: message）。
 * Java 网关已把 Python 的 JSON 字符串还原为纯文本下发，
 * 仅当片段被引号包裹且为合法 JSON 字符串时才二次还原，避免误伤普通文本。
 */
function decodeMessageChunk(raw: string): string {
  if (raw.length >= 2 && raw.startsWith('"') && raw.endsWith('"')) {
    try {
      const parsed = JSON.parse(raw)
      if (typeof parsed === 'string') return parsed
    } catch {
      /* 非合法 JSON 字符串，按原文处理 */
    }
  }
  return raw
}

/**
 * 解码结构化事件 data。
 * Java 网关把工具 / 工作流等事件以 JSON 形式转发，这里尝试反序列化为对象；
 * 纯文本（如 status 文案）解析失败时原样返回字符串。
 */
function decodeEventData(raw: string): unknown {
  try {
    return JSON.parse(raw)
  } catch {
    return raw
  }
}

function isDone(event: string, data: string): boolean {
  if (event === 'done') return true
  const text = decodeMessageChunk(data).trim().toLowerCase()
  return DONE_FLAGS.includes(text)
}

/**
 * 发起流式聊天请求（POST + SSE）。
 *
 * 使用 fetch + ReadableStream 解析 text/event-stream，
 * 不使用原生 EventSource（仅支持 GET），也不使用普通 Axios JSON 请求。
 *
 * 事件分发：
 * - event: message → onMessage（拼入正文）
 * - event: done    → 结束
 * - event: error   → onError（并结束）
 * - 其余事件        → onEvent（工具 / MCP / RAG / 工作流步骤 / 文件等）
 */
export async function streamChat(
  body: StreamChatBody,
  callbacks: StreamCallbacks,
  signal?: AbortSignal,
): Promise<void> {
  const { onMessage, onEvent, onDone, onError } = callbacks

  const handleEvent = (parsed: ParsedSseEvent): boolean => {
    const { event, data } = parsed
    if (isDone(event, data)) return false

    if (event === 'message') {
      onMessage(decodeMessageChunk(data))
      return true
    }

    if (event === 'error') {
      const decoded = decodeEventData(data)
      const message =
        typeof decoded === 'string' && decoded
          ? decoded
          : 'AI 服务返回错误'
      onError?.(new SSEError(message, 0))
      return false
    }

    onEvent?.(event, decodeEventData(data))
    return true
  }

  try {
    const response = await fetch(`${API_BASE_URL}/agent/chat/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
      },
      credentials: 'include',
      body: JSON.stringify(body),
      signal,
    })

    // 非 200：未登录（401）等会返回 JSON BaseResponse，按错误处理
    if (!response.ok) {
      let message = `请求失败：${response.status}`
      try {
        const err = await response.json()
        if (err && typeof err.message === 'string') message = err.message
      } catch {
        /* ignore */
      }
      throw new SSEError(message, response.status)
    }

    if (!response.body) {
      throw new SSEError('响应没有可读流', response.status)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let finished = false

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      buffer = buffer.replace(/\r\n/g, '\n')

      let sepIndex: number
      while ((sepIndex = buffer.indexOf('\n\n')) !== -1) {
        const rawEvent = buffer.slice(0, sepIndex)
        buffer = buffer.slice(sepIndex + 2)

        const parsed = parseSseEvent(rawEvent)
        if (parsed === null) continue
        if (!handleEvent(parsed)) {
          finished = true
          break
        }
      }
      if (finished) break
    }

    // 处理结束时缓冲区残留的最后一个事件
    if (!finished && buffer.trim().length > 0) {
      const parsed = parseSseEvent(buffer)
      if (parsed !== null) handleEvent(parsed)
    }

    onDone?.()
  } catch (error) {
    // 主动中止（切换会话等）不视为错误
    if (error instanceof DOMException && error.name === 'AbortError') {
      onDone?.()
      return
    }
    onError?.(error)
  }
}
