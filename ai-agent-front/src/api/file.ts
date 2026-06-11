import { request, API_BASE_URL } from './request'
import type { ChatFileVO, FileRequest } from '@/types/file'

/**
 * 查询会话文件列表。
 * 注意：此接口直接返回 ChatFileVO[]，不使用 BaseResponse 包装，
 * 响应拦截器会原样透传数组。
 */
export function getFileList(chatId: number): Promise<ChatFileVO[]> {
  const payload: FileRequest = { chatId }
  return request<ChatFileVO[]>({
    url: '/chat/file/list',
    method: 'post',
    data: payload,
  })
}

/**
 * 删除会话文件。
 * 注意：此接口直接返回裸 boolean（true），不使用 BaseResponse 包装。
 * 失败时由后端返回 JSON BaseResponse（如 code:50000），由拦截器处理。
 */
export function deleteFile(chatId: number, fileName: string): Promise<boolean> {
  const payload: FileRequest = { chatId, fileName }
  return request<boolean>({
    url: '/chat/file/delete',
    method: 'delete',
    data: payload,
  })
}

/**
 * 下载会话文件。
 * 返回二进制流（application/octet-stream），必须用 fetch + blob 处理，
 * 不能走普通 JSON Axios。成功后触发浏览器下载。
 */
export async function downloadFile(
  chatId: number,
  fileName: string,
): Promise<void> {
  const payload: FileRequest = { chatId, fileName }
  const res = await fetch(`${API_BASE_URL}/chat/file/download`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify(payload),
  })

  // 失败时后端返回 JSON BaseResponse 而非二进制流
  const contentType = res.headers.get('Content-Type') || ''
  if (!res.ok || contentType.includes('application/json')) {
    let message = `下载失败：${res.status}`
    try {
      const err = await res.json()
      if (err && typeof err.message === 'string') message = err.message
    } catch {
      /* ignore */
    }
    throw new Error(message)
  }

  const blob = await res.blob()
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}
