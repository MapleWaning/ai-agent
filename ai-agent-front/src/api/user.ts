import { request } from './request'
import type { LoginRequest, LoginUserVO, RegisterRequest } from '@/types/user'

/** 用户注册（仅落库，不自动登录），返回新用户 userId */
export function register(payload: RegisterRequest): Promise<number> {
  return request<number>({
    url: '/user/register',
    method: 'post',
    data: payload,
  })
}

/** 用户登录，写入 Session / Redis / Cookie */
export function login(payload: LoginRequest): Promise<LoginUserVO> {
  return request<LoginUserVO>({
    url: '/user/login',
    method: 'post',
    data: payload,
  })
}

/** 获取当前登录用户（应用初始化探测登录态） */
export function getCurrentUser(): Promise<LoginUserVO> {
  return request<LoginUserVO>({
    url: '/user/current',
    method: 'get',
  })
}

/** 退出登录（无需登录，幂等） */
export function logout(): Promise<boolean> {
  return request<boolean>({
    url: '/user/logout',
    method: 'post',
  })
}
