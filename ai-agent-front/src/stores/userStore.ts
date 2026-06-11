import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import {
  getCurrentUser,
  login as loginApi,
  logout as logoutApi,
  register as registerApi,
} from '@/api/user'
import type { LoginRequest, LoginUserVO, RegisterRequest } from '@/types/user'

export const useUserStore = defineStore('user', () => {
  const user = ref<LoginUserVO | null>(null)
  /** 是否已完成初始登录态探测 */
  const initialized = ref(false)

  const isLoggedIn = computed(() => !!user.value)
  const userName = computed(() => user.value?.userName ?? '')

  /** 应用初始化：探测当前登录态 */
  async function fetchCurrentUser(): Promise<void> {
    try {
      user.value = await getCurrentUser()
    } catch {
      user.value = null
    } finally {
      initialized.value = true
    }
  }

  /** 登录 */
  async function login(payload: LoginRequest): Promise<void> {
    user.value = await loginApi(payload)
  }

  /** 注册（不自动登录） */
  async function register(payload: RegisterRequest): Promise<number> {
    return registerApi(payload)
  }

  /** 退出登录 */
  async function logout(): Promise<void> {
    try {
      await logoutApi()
    } finally {
      user.value = null
    }
  }

  /** 仅清除本地登录态（如收到 401 时） */
  function clear(): void {
    user.value = null
  }

  return {
    user,
    initialized,
    isLoggedIn,
    userName,
    fetchCurrentUser,
    login,
    register,
    logout,
    clear,
  }
})
