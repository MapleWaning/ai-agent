<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useUserStore } from '@/stores/userStore'
import { useChatStore } from '@/stores/chatStore'
import MainLayout from '@/layouts/MainLayout.vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import ChatSidebar from '@/components/chat/ChatSidebar.vue'
import ChatWindow from '@/components/chat/ChatWindow.vue'
import FilePanel from '@/components/file/FilePanel.vue'
import LoginDialog from '@/components/auth/LoginDialog.vue'
import RegisterDialog from '@/components/auth/RegisterDialog.vue'

const route = useRoute()
const userStore = useUserStore()
const chatStore = useChatStore()
const { isLoggedIn, initialized } = storeToRefs(userStore)

const authMode = ref<'login' | 'register'>('login')
// 完成登录态探测且未登录时展示登录弹窗，背景模糊
const showAuth = computed(() => initialized.value && !isLoggedIn.value)

async function syncRouteChat() {
  if (!isLoggedIn.value) return
  const param = route.params.chatId
  if (typeof param === 'string' && param) {
    const id = Number(param)
    if (!Number.isNaN(id)) {
      await chatStore.selectChat(id)
    }
  }
}

onMounted(async () => {
  await userStore.fetchCurrentUser()
  if (isLoggedIn.value) {
    await chatStore.loadChatList()
    await syncRouteChat()
  }
})

watch(isLoggedIn, async (loggedIn) => {
  if (loggedIn) {
    authMode.value = 'login'
    await chatStore.loadChatList()
    await syncRouteChat()
  }
})

watch(
  () => route.params.chatId,
  () => syncRouteChat(),
)
</script>

<template>
  <MainLayout :blurred="showAuth">
    <template #header>
      <AppHeader />
    </template>
    <template #sidebar>
      <ChatSidebar />
    </template>
    <template #main>
      <ChatWindow />
    </template>
    <template #aside>
      <FilePanel />
    </template>

    <template #overlay>
      <LoginDialog
        v-if="showAuth && authMode === 'login'"
        :visible="true"
        @switch="authMode = 'register'"
        @success="authMode = 'login'"
      />
      <RegisterDialog
        v-if="showAuth && authMode === 'register'"
        :visible="true"
        @switch="authMode = 'login'"
      />
    </template>
  </MainLayout>
</template>
