/** 统一响应格式（大部分 JSON 接口使用） */
export interface BaseResponse<T> {
  code: number
  data: T
  message: string
}

/** 错误码常量（与后端 ErrorCode 枚举对应） */
export const ErrorCode = {
  SUCCESS: 0,
  PARAMS_ERROR: 40000,
  USER_ALREADY_EXIST: 40001,
  PASSWORD_NOT_MATCH: 40002,
  PASSWORD_ERROR: 40100,
  NOT_LOGIN_ERROR: 40103,
  NO_AUTH_ERROR: 40104,
  USER_NOT_FOUND: 40400,
  OPERATION_ERROR: 50000,
} as const

/** MyBatis-Plus 分页结构（聊天历史为游标分页） */
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}
