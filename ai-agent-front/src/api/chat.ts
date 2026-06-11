import { request } from './request'
import type { PageResult } from '@/types/common'
import type {
  ChatHistory,
  ChatUpdateRequest,
  ChatVO,
  RouteRequest,
  RouteResponse,
} from '@/types/chat'

/** 创建会话，返回新 chatId（userId 由后端从登录态注入） */
export function createChat(): Promise<number> {
  return request<number>({
    url: '/agent/chat/create',
    method: 'post',
  })
}

/** 查询当前用户会话列表（按 modifyTime 降序） */
export function getChatList(): Promise<ChatVO[]> {
  return request<ChatVO[]>({
    url: '/agent/chat/list',
    method: 'get',
  })
}

/** 查询单个会话详情 */
export function getChatDetail(chatId: number | string): Promise<ChatVO> {
  return request<ChatVO>({
    url: `/agent/chat/${chatId}`,
    method: 'get',
  })
}

/** 更新会话标题，返回更新后的 ChatVO */
export function updateChatTitle(
  chatId: number | string,
  payload: ChatUpdateRequest,
): Promise<ChatVO> {
  return request<ChatVO>({
    url: `/agent/chat/${chatId}`,
    method: 'put',
    data: payload,
  })
}

/** 删除会话（级联清理历史与文件） */
export function deleteChat(chatId: number | string): Promise<boolean> {
  return request<boolean>({
    url: `/agent/chat/${chatId}`,
    method: 'delete',
  })
}

/** 路由决策（可选流程：create -> route -> stream） */
export function routeChat(payload: RouteRequest): Promise<RouteResponse> {
  return request<RouteResponse>({
    url: '/agent/chat/route',
    method: 'post',
    data: payload,
  })
}

/** 查询聊天历史（游标分页，按 id 降序） */
export function getChatHistory(
  chatId: number,
  pageSize = 10,
  lastId?: number,
): Promise<PageResult<ChatHistory>> {
  return request<PageResult<ChatHistory>>({
    url: `/chatHistory/chat/${chatId}`,
    method: 'get',
    params: { pageSize, lastId },
  })
}

/* 流式聊天 POST /agent/chat/stream 见 @/utils/sse.ts，不能用普通 Axios JSON 请求。 */
