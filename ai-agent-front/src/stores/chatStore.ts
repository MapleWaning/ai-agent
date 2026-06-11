import { defineStore } from 'pinia'
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createChat as createChatApi,
  deleteChat as deleteChatApi,
  getChatHistory,
  getChatList,
  routeChat,
  updateChatTitle,
} from '@/api/chat'
import {
  deleteFile as deleteFileApi,
  downloadFile as downloadFileApi,
  getFileList,
} from '@/api/file'
import { streamChat, SSEError } from '@/utils/sse'
import { applyStreamEvent, buildActivitiesFromEvents } from '@/utils/activity'
import type {
  ChatHistory,
  ChatVO,
  LangChainMessage,
  RouteType,
  StreamEventRecord,
  UIMessage,
} from '@/types/chat'
import type { ChatFileVO } from '@/types/file'

let messageSeq = 0
function nextId(prefix: string): string {
  messageSeq += 1
  return `${prefix}-${Date.now()}-${messageSeq}`
}

/**
 * 规范化历史消息文本。
 *
 * 后端早期版本把每个 LLM 分片以 JSON 字符串（带包裹引号、转义换行）下发并直接拼接落库，
 * 因此历史记录里的 data.content 可能是形如 `"小红""你好\n\n世界""done"` 的拼接串。
 * 这里将其还原为正常文本（去引号、还原换行、去掉结尾的 done），
 * 使刷新后历史展示与实时流式渲染保持一致。
 *
 * 对已经是干净文本的内容（新版本后端）保持原样返回。
 */
function normalizeHistoryContent(text: string): string {
  if (!text) return ''
  const trimmed = text.trim()
  // 仅当整体是「多个相邻 JSON 字符串片段直接拼接」时才做还原，避免误伤正常文本
  const isConcatenatedFragments = /^(?:"(?:[^"\\]|\\.)*")+$/.test(trimmed)
  if (!isConcatenatedFragments) return text

  const matches = trimmed.match(/"(?:[^"\\]|\\.)*"/g)
  if (!matches) return text

  return matches
    .map((fragment) => {
      try {
        const parsed = JSON.parse(fragment)
        return typeof parsed === 'string' ? parsed : fragment
      } catch {
        return fragment
      }
    })
    .filter((part) => part.trim().toLowerCase() !== 'done')
    .join('')
}

const ROUTE_TYPES: RouteType[] = [
  'normal_chat',
  'report',
  'rag',
  'mcp',
  'tool',
  'workflow',
]

/** 校验并归一化路由类型，非法值返回 null */
function normalizeRouteType(value?: string | null): RouteType | null {
  if (value && (ROUTE_TYPES as string[]).includes(value)) {
    return value as RouteType
  }
  return null
}

interface ParsedHistoryRecord {
  role: 'user' | 'assistant'
  content: string
  routeType: RouteType | null
  /** AI 消息落库的功能触发 / 思路链事件（additional_kwargs.events） */
  events: StreamEventRecord[]
  /** AI 消息落库的路由原因（若有） */
  routeReason: string | null
}

/** 从 AI 消息的 additional_kwargs 中安全提取事件列表 */
function extractEvents(kwargs: Record<string, unknown> | undefined): StreamEventRecord[] {
  const raw = kwargs?.events
  if (!Array.isArray(raw)) return []
  return raw.filter(
    (item): item is StreamEventRecord =>
      !!item && typeof item === 'object' && typeof (item as StreamEventRecord).event === 'string',
  )
}

/** 解析单条聊天历史记录（content 为 LangChain JSON 字符串，type 为路由类型） */
function parseHistoryRecord(record: ChatHistory): ParsedHistoryRecord | null {
  try {
    const parsed = JSON.parse(record.content) as LangChainMessage
    const role = parsed.type === 'ai' ? 'assistant' : 'user'
    const kwargs = parsed.data?.additional_kwargs as Record<string, unknown> | undefined
    const reason = kwargs?.routeReason
    return {
      role,
      content: normalizeHistoryContent(parsed.data?.content ?? ''),
      routeType: normalizeRouteType(record.type),
      events: role === 'assistant' ? extractEvents(kwargs) : [],
      routeReason: typeof reason === 'string' ? reason : null,
    }
  } catch {
    return null
  }
}

export const useChatStore = defineStore('chat', () => {
  const chatList = ref<ChatVO[]>([])
  const currentChatId = ref<number | null>(null)
  const messages = ref<UIMessage[]>([])
  const files = ref<ChatFileVO[]>([])

  const listLoading = ref(false)
  const historyLoading = ref(false)
  const filesLoading = ref(false)
  const sending = ref(false)

  let abortController: AbortController | null = null

  /** 加载会话列表 */
  async function loadChatList(): Promise<void> {
    listLoading.value = true
    try {
      chatList.value = await getChatList()
    } finally {
      listLoading.value = false
    }
  }

  /** 进入「新建会话」状态（不立即调用后端，首条消息发送时再创建） */
  function startNewChat(): void {
    abortStreaming()
    currentChatId.value = null
    messages.value = []
    files.value = []
  }

  /** 选择并加载某个会话 */
  async function selectChat(chatId: number): Promise<void> {
    if (currentChatId.value === chatId) return
    abortStreaming()
    currentChatId.value = chatId
    messages.value = []
    files.value = []
    await Promise.all([loadHistory(chatId), loadFiles(chatId)])
  }

  /** 加载历史消息（首屏取较大页，UI 反转为时间正序） */
  async function loadHistory(chatId: number): Promise<void> {
    historyLoading.value = true
    try {
      const page = await getChatHistory(chatId, 50)
      const records = [...page.records].reverse() // id 降序 -> 时间正序
      const list: UIMessage[] = []
      // 同一轮对话的 human / ai 记录都带有相同的 type，
      // 用 lastType 兜底，确保 AI 消息能拿到本轮路由类型。
      let lastType: RouteType | null = null
      for (const record of records) {
        const parsed = parseHistoryRecord(record)
        if (!parsed) continue
        if (parsed.routeType) lastType = parsed.routeType
        const isAssistant = parsed.role === 'assistant'
        const activities = isAssistant
          ? buildActivitiesFromEvents(parsed.events)
          : undefined
        list.push({
          id: nextId('history'),
          role: parsed.role,
          content: parsed.content,
          // 仅 AI 消息展示路由类型提示
          routeType: isAssistant ? parsed.routeType ?? lastType : null,
          routeReason: isAssistant ? parsed.routeReason : null,
          // 历史回放：由落库事件重建功能触发 / 思路链时间线
          activities: activities && activities.length > 0 ? activities : undefined,
        })
      }
      messages.value = list
    } finally {
      historyLoading.value = false
    }
  }

  /** 加载文件列表 */
  async function loadFiles(chatId: number): Promise<void> {
    filesLoading.value = true
    try {
      files.value = await getFileList(chatId)
    } catch {
      files.value = []
    } finally {
      filesLoading.value = false
    }
  }

  /** 修改会话标题 */
  async function renameChat(chatId: number, title: string): Promise<void> {
    const updated = await updateChatTitle(chatId, { title })
    const target = chatList.value.find((c) => c.chatId === chatId)
    if (target) target.title = updated.title
    ElMessage.success('标题已更新')
  }

  /** 删除会话 */
  async function removeChat(chatId: number): Promise<void> {
    await deleteChatApi(chatId)
    chatList.value = chatList.value.filter((c) => c.chatId !== chatId)
    if (currentChatId.value === chatId) {
      startNewChat()
    }
    ElMessage.success('会话已删除')
  }

  /** 发送消息（含首次自动创建会话 + SSE 流式接收） */
  async function sendMessage(text: string): Promise<void> {
    const content = text.trim()
    if (!content || sending.value) return

    sending.value = true
    let createdNew = false

    try {
      // 1. 无 chatId 时先创建会话
      if (currentChatId.value === null) {
        const newId = await createChatApi()
        currentChatId.value = newId
        createdNew = true
      }
      const chatId = currentChatId.value as number

      // 2. 追加用户消息
      messages.value.push({
        id: nextId('user'),
        role: 'user',
        content,
      })

      // 3. 路由决策（可选）：失败不阻断发送
      let routeType: RouteType | undefined
      let routeReason: string | null = null
      try {
        const route = await routeChat({ initPrompt: content })
        routeType = route.routeType as RouteType
        routeReason = route.reason
      } catch {
        routeType = undefined
      }

      // 4. 追加 AI 占位消息（流式填充）
      const aiMessage: UIMessage = {
        id: nextId('ai'),
        role: 'assistant',
        content: '',
        routeType: routeType ?? null,
        routeReason,
        activities: [],
        statusText: null,
        streaming: true,
      }
      messages.value.push(aiMessage)
      const target = messages.value[messages.value.length - 1]

      // 打字机渲染队列：网络分片可能成批/突发到达，
      // 这里把已收到内容放入 pending，再由定时器逐步显示，保证平滑的打字机效果。
      let pending = ''
      let streamEnded = false
      let typingTimer: number | null = null
      const stopTyping = () => {
        if (typingTimer !== null) {
          clearInterval(typingTimer)
          typingTimer = null
        }
      }
      const ensureTyping = () => {
        if (typingTimer !== null) return
        typingTimer = window.setInterval(() => {
          if (pending.length > 0) {
            // 积压越多每帧显示越多，避免长文本播放过慢
            const step = Math.max(1, Math.ceil(pending.length / 50))
            target.content += pending.slice(0, step)
            pending = pending.slice(step)
          } else if (streamEnded) {
            target.streaming = false
            stopTyping()
          }
        }, 20)
      }

      // 5. SSE 流式请求
      abortController = new AbortController()
      await streamChat(
        {
          message: content,
          chatId: String(chatId),
          routeType,
        },
        {
          onMessage: (chunk) => {
            pending += chunk
            // 正文到达后清除临时状态文案
            target.statusText = null
            ensureTyping()
          },
          onEvent: (event, data) => {
            if (event === 'status') {
              target.statusText =
                typeof data === 'string' ? data : String(data ?? '')
              return
            }
            if (event === 'route') {
              // 路由原因兜底（routeChat 未取到时由流补充）
              const d = data as Record<string, unknown> | null
              if (!target.routeReason && d && typeof d.reason === 'string') {
                target.routeReason = d.reason
              }
              return
            }
            // 工具 / MCP / RAG / 文件 / 工作流步骤：写入活动时间线
            if (!target.activities) target.activities = []
            applyStreamEvent(target.activities, event, data)
          },
          onDone: () => {
            streamEnded = true
            target.statusText = null
            ensureTyping()
          },
          onError: (error) => {
            // 出错时立即冲刷剩余内容并停止打字机
            target.content += pending
            pending = ''
            streamEnded = true
            target.statusText = null
            stopTyping()
            target.streaming = false
            target.error = true
            if (error instanceof SSEError && error.status === 401) {
              ElMessage.error('登录已过期，请重新登录')
            } else {
              const msg =
                error instanceof Error ? error.message : 'AI 回复异常中断'
              ElMessage.error(msg)
            }
          },
        },
        abortController.signal,
      )

      // 6. 刷新会话列表与文件列表
      await loadChatList()
      await loadFiles(chatId)
    } catch (error) {
      const msg = error instanceof Error ? error.message : '发送失败'
      ElMessage.error(msg)
      // 创建会话成功但后续失败时，仍保留已创建会话；创建会话本身失败则回到新建态
      if (createdNew && messages.value.length === 0) {
        currentChatId.value = null
      }
    } finally {
      sending.value = false
      abortController = null
    }
  }

  /** 中止当前流式请求 */
  function abortStreaming(): void {
    if (abortController) {
      abortController.abort()
      abortController = null
    }
  }

  /** 下载文件 */
  async function downloadFile(fileName: string): Promise<void> {
    if (currentChatId.value === null) return
    try {
      await downloadFileApi(currentChatId.value, fileName)
      ElMessage.success('已开始下载')
    } catch (error) {
      const msg = error instanceof Error ? error.message : '下载失败'
      ElMessage.error(msg)
    }
  }

  /** 删除文件（二次确认由组件处理） */
  async function removeFile(fileName: string): Promise<void> {
    if (currentChatId.value === null) return
    const chatId = currentChatId.value
    await deleteFileApi(chatId, fileName)
    ElMessage.success('文件已删除')
    await loadFiles(chatId)
  }

  /** 重置（退出登录时） */
  function reset(): void {
    abortStreaming()
    chatList.value = []
    currentChatId.value = null
    messages.value = []
    files.value = []
  }

  return {
    chatList,
    currentChatId,
    messages,
    files,
    listLoading,
    historyLoading,
    filesLoading,
    sending,
    loadChatList,
    startNewChat,
    selectChat,
    loadHistory,
    loadFiles,
    renameChat,
    removeChat,
    sendMessage,
    abortStreaming,
    downloadFile,
    removeFile,
    reset,
  }
})
