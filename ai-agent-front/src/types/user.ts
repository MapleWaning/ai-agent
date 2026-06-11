/** 用户实体（管理接口返回，聊天前端基本不用） */
export interface User {
  userId?: number
  userName?: string
  password?: string
  role?: string
}

/** 登录请求体 */
export interface LoginRequest {
  userAccount: string
  userPassword: string
}

/** 注册请求体 */
export interface RegisterRequest {
  userAccount: string
  userPassword: string
  checkPassword: string
}

/** 登录 / 当前用户响应（LoginUserVO） */
export interface LoginUserVO {
  userId: number
  userName: string
  role: string
}
