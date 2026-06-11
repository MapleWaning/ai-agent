<script setup lang="ts">
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useChatStore } from '@/stores/chatStore'
import { formatShortTime } from '@/utils/format'
import type { ChatVO } from '@/types/chat'

const router = useRouter()
const chatStore = useChatStore()
const { chatList, currentChatId, listLoading } = storeToRefs(chatStore)

function handleNewChat() {
  chatStore.startNewChat()
  router.push('/chat')
}

function handleSelect(chat: ChatVO) {
  if (chat.chatId === currentChatId.value) return
  router.push(`/chat/${chat.chatId}`)
}

async function handleRename(chat: ChatVO) {
  try {
    const { value } = await ElMessageBox.prompt('请输入新的会话标题', '修改标题', {
      inputValue: chat.title,
      inputPlaceholder: '会话标题',
      // 标题不能为空，长度 1~50（后端硬上限 255）
      inputValidator: (val: string) => {
        const t = (val || '').trim()
        if (!t) return '标题不能为空'
        if (t.length > 50) return '标题长度不能超过 50 个字符'
        return true
      },
      confirmButtonText: '保存',
      cancelButtonText: '取消',
    })
    const title = value.trim()
    if (title && title !== chat.title) {
      await chatStore.renameChat(chat.chatId, title)
    }
  } catch {
    /* 用户取消 */
  }
}

async function handleDelete(chat: ChatVO) {
  try {
    await ElMessageBox.confirm(
      `确定删除会话「${chat.title}」吗？该操作将级联清理历史与文件，不可恢复。`,
      '删除会话',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
    )
    await chatStore.removeChat(chat.chatId)
    if (currentChatId.value === null) router.push('/chat')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}
</script>

<template>
  <aside class="sidebar">
    <div class="sidebar__top">
      <el-button type="primary" class="sidebar__new" @click="handleNewChat">
        <el-icon class="sidebar__new-icon"><Plus /></el-icon>
        新建会话
      </el-button>
    </div>

    <div v-loading="listLoading" class="sidebar__list">
      <p v-if="!listLoading && chatList.length === 0" class="sidebar__empty">
        暂无会话
      </p>

      <div
        v-for="chat in chatList"
        :key="chat.chatId"
        class="chat-item"
        :class="{ 'chat-item--active': chat.chatId === currentChatId }"
        @click="handleSelect(chat)"
      >
        <div class="chat-item__main">
          <span class="chat-item__title">{{ chat.title }}</span>
          <span class="chat-item__time">{{ formatShortTime(chat.modifyTime) }}</span>
        </div>
        <div class="chat-item__actions" @click.stop>
          <el-icon class="chat-item__btn" title="修改标题" @click="handleRename(chat)">
            <Edit />
          </el-icon>
          <el-icon class="chat-item__btn" title="删除会话" @click="handleDelete(chat)">
            <Delete />
          </el-icon>
        </div>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--color-surface);
  border-right: 1px solid var(--color-border);
}
.sidebar__top {
  padding: 16px 14px;
}
.sidebar__new {
  width: 100%;
}
.sidebar__new-icon {
  margin-right: 4px;
}
.sidebar__list {
  flex: 1;
  overflow-y: auto;
  padding: 0 10px 16px;
}
.sidebar__empty {
  text-align: center;
  color: var(--color-text-tertiary);
  font-size: 13px;
  margin-top: 24px;
}
.chat-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background 0.15s;
}
.chat-item:hover {
  background: var(--color-surface-2);
}
.chat-item--active {
  background: var(--color-primary-soft);
}
.chat-item__main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.chat-item__title {
  font-size: 14px;
  color: var(--color-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.chat-item--active .chat-item__title {
  color: var(--color-primary);
}
.chat-item__time {
  font-size: 12px;
  color: var(--color-text-tertiary);
}
.chat-item__actions {
  flex: 0 0 auto;
  display: flex;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.15s;
}
.chat-item:hover .chat-item__actions,
.chat-item--active .chat-item__actions {
  opacity: 1;
}
.chat-item__btn {
  color: var(--color-text-tertiary);
  cursor: pointer;
}
.chat-item__btn:hover {
  color: var(--color-primary);
}
</style>
