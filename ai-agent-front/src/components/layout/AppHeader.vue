<script setup lang="ts">
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/userStore'
import { useChatStore } from '@/stores/chatStore'

const router = useRouter()
const userStore = useUserStore()
const chatStore = useChatStore()
const { isLoggedIn, userName } = storeToRefs(userStore)

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定退出登录吗？', '退出登录', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
    })
    await userStore.logout()
    chatStore.reset()
    router.push('/')
    ElMessage.success('已退出登录')
  } catch {
    /* 用户取消 */
  }
}
</script>

<template>
  <header class="app-header">
    <div class="app-header__brand">
      <el-icon class="app-header__logo"><MagicStick /></el-icon>
      <span class="app-header__title">恋爱咨询智能体</span>
    </div>

    <div v-if="isLoggedIn" class="app-header__user">
      <el-icon class="app-header__avatar"><UserFilled /></el-icon>
      <span class="app-header__name">{{ userName }}</span>
      <el-button text type="primary" @click="handleLogout">退出登录</el-button>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  height: var(--header-height);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
}
.app-header__brand {
  display: flex;
  align-items: center;
  gap: 8px;
}
.app-header__logo {
  font-size: 20px;
  color: var(--color-primary);
}
.app-header__title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
}
.app-header__user {
  display: flex;
  align-items: center;
  gap: 10px;
}
.app-header__avatar {
  font-size: 20px;
  color: var(--color-text-secondary);
}
.app-header__name {
  font-size: 14px;
  color: var(--color-text);
}
</style>
