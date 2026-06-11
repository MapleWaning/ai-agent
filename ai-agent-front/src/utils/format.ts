/**
 * 格式化时间展示。
 * 兼容 ISO-8601（如 2026-06-08T14:30:00）与 yyyy-MM-dd HH:mm:ss。
 */
export function formatTime(value?: string | null): string {
  if (!value) return ''
  // 后端 ISO-8601 无时区后缀，直接交给 Date 解析即可
  const date = new Date(value.replace(' ', 'T'))
  if (Number.isNaN(date.getTime())) return value
  const pad = (n: number) => String(n).padStart(2, '0')
  const y = date.getFullYear()
  const m = pad(date.getMonth() + 1)
  const d = pad(date.getDate())
  const hh = pad(date.getHours())
  const mm = pad(date.getMinutes())
  return `${y}-${m}-${d} ${hh}:${mm}`
}

/** 相对简短时间（今天显示 HH:mm，否则 MM-dd） */
export function formatShortTime(value?: string | null): string {
  if (!value) return ''
  const date = new Date(value.replace(' ', 'T'))
  if (Number.isNaN(date.getTime())) return value
  const pad = (n: number) => String(n).padStart(2, '0')
  const now = new Date()
  const sameDay =
    date.getFullYear() === now.getFullYear() &&
    date.getMonth() === now.getMonth() &&
    date.getDate() === now.getDate()
  if (sameDay) return `${pad(date.getHours())}:${pad(date.getMinutes())}`
  return `${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}
