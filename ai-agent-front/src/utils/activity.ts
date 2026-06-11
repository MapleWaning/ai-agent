import type {
  ChatActivity,
  StreamEventRecord,
  ToolActivity,
  WorkflowStepActivity,
} from '@/types/chat'

let activitySeq = 0
function nextKey(prefix: string): string {
  activitySeq += 1
  return `${prefix}-${activitySeq}`
}

function asRecord(data: unknown): Record<string, unknown> {
  return data && typeof data === 'object' ? (data as Record<string, unknown>) : {}
}

function toText(value: unknown): string | undefined {
  if (value === undefined || value === null) return undefined
  if (typeof value === 'string') return value
  return String(value)
}

/** 找到同名、仍在执行中的工具活动，用于把 tool_end/tool_error 合并到对应的 tool_start */
function findLastRunningTool(
  activities: ChatActivity[],
  name: string,
): ToolActivity | undefined {
  for (let i = activities.length - 1; i >= 0; i--) {
    const a = activities[i]
    if (a.type === 'tool' && a.name === name && a.status === 'running') {
      return a
    }
  }
  return undefined
}

/**
 * 将单个后端事件应用到活动时间线（原地修改 activities）。
 *
 * 事件来源：
 * - 实时流：sse.ts 的 onEvent 回调
 * - 历史回放：additional_kwargs.events
 *
 * 设计要点：tool_start 与后续的 tool_end/tool_error 会合并为同一条活动，
 * workflow_step 的 running/finished 也按 step 合并，避免时间线出现重复项。
 */
export function applyStreamEvent(
  activities: ChatActivity[],
  event: string,
  data: unknown,
): void {
  const d = asRecord(data)

  switch (event) {
    case 'workflow_step': {
      const step = Number(d.step ?? 0)
      const status = d.status === 'finished' ? 'finished' : 'running'
      const existing = activities.find(
        (a): a is WorkflowStepActivity =>
          a.type === 'workflow_step' && a.step === step,
      )
      if (existing) {
        existing.status = status
        const title = toText(d.title)
        if (title) existing.title = title
        const detail = toText(d.detail)
        if (detail !== undefined) existing.detail = detail
      } else {
        activities.push({
          type: 'workflow_step',
          key: nextKey('step'),
          step,
          title: toText(d.title) ?? `步骤 ${step}`,
          status,
          detail: toText(d.detail),
        })
      }
      break
    }

    case 'tool_start': {
      const name = toText(d.name) ?? ''
      activities.push({
        type: 'tool',
        key: nextKey('tool'),
        name,
        label: toText(d.label) ?? name ?? '工具',
        status: 'running',
        input: d.input,
      })
      break
    }

    case 'tool_end': {
      const name = toText(d.name) ?? ''
      const status = d.status === 'failed' ? 'failed' : 'finished'
      const item = findLastRunningTool(activities, name)
      if (item) {
        item.status = status
        const summary = toText(d.summary)
        if (summary !== undefined) item.summary = summary
      } else {
        activities.push({
          type: 'tool',
          key: nextKey('tool'),
          name,
          label: toText(d.label) ?? name ?? '工具',
          status,
          summary: toText(d.summary),
        })
      }
      break
    }

    case 'tool_error': {
      const name = toText(d.name) ?? ''
      const item = findLastRunningTool(activities, name)
      if (item) {
        item.status = 'failed'
        const error = toText(d.error)
        if (error !== undefined) item.error = error
      } else {
        activities.push({
          type: 'tool',
          key: nextKey('tool'),
          name,
          label: toText(d.label) ?? name ?? '工具',
          status: 'failed',
          error: toText(d.error),
          input: d.input,
        })
      }
      break
    }

    case 'file': {
      const fileName = toText(d.fileName) ?? ''
      activities.push({
        type: 'file',
        key: nextKey('file'),
        fileName,
        action: toText(d.action) ?? 'created',
        summary: toText(d.summary) ?? `文件 ${fileName} 已处理`,
      })
      break
    }

    default:
      // route / status 等非时间线事件由调用方单独处理，这里忽略
      break
  }
}

/** 由历史记录中的事件列表重建活动时间线 */
export function buildActivitiesFromEvents(
  events: StreamEventRecord[] | undefined | null,
): ChatActivity[] {
  const activities: ChatActivity[] = []
  if (!Array.isArray(events)) return activities
  for (const record of events) {
    if (record && typeof record === 'object' && typeof record.event === 'string') {
      applyStreamEvent(activities, record.event, record.data)
    }
  }
  return activities
}
