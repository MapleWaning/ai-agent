<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useChatStore } from '@/stores/chatStore'
import ChatMessage from './ChatMessage.vue'
import ChatInput from './ChatInput.vue'

const chatStore = useChatStore()
const { messages, currentChatId, historyLoading, sending } =
  storeToRefs(chatStore)

const scrollRef = ref<HTMLElement | null>(null)

function scrollToBottom() {
  nextTick(() => {
    const el = scrollRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

// 消息变化（新增 / 正文流式追加 / 活动时间线更新 / 状态文案）时自动滚动到底部
watch(
  () => {
    const last = messages.value[messages.value.length - 1]
    return [
      messages.value.length,
      last?.content,
      last?.activities?.length,
      last?.statusText,
    ]
  },
  scrollToBottom,
  { deep: true },
)
watch(currentChatId, scrollToBottom)

function handleSend(text: string) {
  chatStore.sendMessage(text)
}
</script>

<template>
  <section class="chat-window">
    <div ref="scrollRef" class="chat-window__scroll">
      <!-- 欢迎 / 空状态 -->
      <div v-if="messages.length === 0 && !historyLoading" class="chat-window__welcome">
        <el-icon class="chat-window__welcome-icon"><ChatDotRound /></el-icon>
        <h2>欢迎使用 AI 智能体</h2>
        <p>请输入你的问题，开始一次新的对话。</p>
      </div>

      <div v-else class="chat-window__list">
        <ChatMessage v-for="msg in messages" :key="msg.id" :message="msg" />
      </div>
    </div>

    <div class="chat-window__footer">
      <ChatInput :disabled="sending" @send="handleSend" />
    </div>
  </section>
</template>

<style scoped>
.chat-window {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 0;
}
.chat-window__scroll {
  flex: 1;
  overflow-y: auto;
  padding: 24px max(24px, 8%) 8px;
}
.chat-window__list {
  max-width: 860px;
  margin: 0 auto;
}
.chat-window__welcome {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--color-text-tertiary);
  text-align: center;
}
.chat-window__welcome-icon {
  font-size: 44px;
  color: var(--color-primary);
  margin-bottom: 16px;
}
.chat-window__welcome h2 {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text);
}
.chat-window__welcome p {
  margin: 0;
  font-size: 14px;
}
.chat-window__footer {
  padding: 12px max(24px, 8%) 20px;
}
.chat-window__footer > * {
  max-width: 860px;
  margin: 0 auto;
}
</style>
