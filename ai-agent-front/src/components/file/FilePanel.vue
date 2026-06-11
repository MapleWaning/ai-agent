<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useChatStore } from '@/stores/chatStore'
import FileItem from './FileItem.vue'

const chatStore = useChatStore()
const { files, currentChatId, filesLoading } = storeToRefs(chatStore)

function handleDownload(fileName: string) {
  chatStore.downloadFile(fileName)
}

async function handleDelete(fileName: string) {
  try {
    await ElMessageBox.confirm(`确定删除文件「${fileName}」吗？`, '删除文件', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    await chatStore.removeFile(fileName)
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}
</script>

<template>
  <aside class="file-panel">
    <div class="file-panel__header">
      <span>会话文件</span>
    </div>

    <div v-loading="filesLoading" class="file-panel__body">
      <!-- 未选择会话：空状态 -->
      <div v-if="currentChatId === null" class="file-panel__empty">
        选择会话后查看生成文件
      </div>

      <!-- 已选会话但无文件 -->
      <div v-else-if="!filesLoading && files.length === 0" class="file-panel__empty">
        暂无会话文件
      </div>

      <div v-else class="file-panel__list">
        <FileItem
          v-for="file in files"
          :key="file.fileName"
          :file="file"
          @download="handleDownload"
          @delete="handleDelete"
        />
      </div>
    </div>
  </aside>
</template>

<style scoped>
.file-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--color-surface);
  border-left: 1px solid var(--color-border);
}
.file-panel__header {
  padding: 18px 16px 12px;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
}
.file-panel__body {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px 16px;
}
.file-panel__empty {
  text-align: center;
  color: var(--color-text-tertiary);
  font-size: 13px;
  margin-top: 32px;
  padding: 0 16px;
}
</style>
