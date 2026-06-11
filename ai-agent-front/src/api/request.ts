import axios, { type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { ErrorCode } from '@/types/common'

/** 接口基础路径（Context Path 为 /api）。默认走 Vite 代理。 */
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

/** 业务错误 */
export class ApiError extends Error {
  code: number
  constructor(message: string, code: number) {
    super(message)
    this.name = 'ApiError'
    this.code = code
  }
}

/**
 * 未登录 / 登录过期处理回调。
 * 由 main.ts 注册，避免 request 直接依赖 store 造成循环引用。
 */
let unauthorizedHandler: (() => void) | null = null
export function registerUnauthorizedHandler(handler: () => void) {
  unauthorizedHandler = handler
}
function handleUnauthorized() {
  unauthorizedHandler?.()
}

const http = axios.create({
  baseURL: API_BASE_URL,
  // 认证依赖 Cookie + Session，必须携带凭证
  withCredentials: true,
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
})

http.interceptors.response.use(
  (response) => {
    const res = response.data
    // 仅当响应体是带 code 字段的 BaseResponse 才统一拆包；
    // 文件列表（裸数组）、删除文件（裸 boolean）等接口会原样返回。
    if (res && typeof res === 'object' && 'code' in res) {
      const body = res as { code: number; data: unknown; message?: string }
      if (body.code === ErrorCode.SUCCESS) {
        return body.data
      }
      if (body.code === ErrorCode.NOT_LOGIN_ERROR) {
        handleUnauthorized()
      }
      const message = body.message || '请求失败'
      ElMessage.error(message)
      return Promise.reject(new ApiError(message, body.code))
    }
    return res
  },
  (error) => {
    const status = error?.response?.status
    if (status === 401) {
      handleUnauthorized()
    }
    const message =
      error?.response?.data?.message || error?.message || '网络请求异常'
    ElMessage.error(message)
    return Promise.reject(error)
  },
)

/**
 * 统一请求方法。返回值已是 BaseResponse.data（成功）或原始数据（非 BaseResponse 接口）。
 */
export function request<T>(config: AxiosRequestConfig): Promise<T> {
  return http.request<unknown, T>(config)
}

export default http
