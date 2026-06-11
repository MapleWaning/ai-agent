<script setup lang="ts">
import { formatTime } from '@/utils/format'
import type { ChatFileVO } from '@/types/file'

defineProps<{
  file: ChatFileVO
}>()

const emit = defineEmits<{
  (e: 'download', fileName: string): void
  (e: 'delete', fileName: string): void
}>()
</script>

<template>
  <div class="file-item">
    <el-icon class="file-item__icon"><Document /></el-icon>
    <div class="file-item__info">
      <span class="file-item__name" :title="file.fileName">{{ file.fileName }}</span>
      <span class="file-item__meta">
        {{ file.sizeText }} · {{ formatTime(file.lastModified) }}
      </span>
    </div>
    <div class="file-item__actions">
      <el-icon class="file-item__btn" title="下载" @click="emit('download', file.fileName)">
        <Download />
      </el-icon>
      <el-icon class="file-item__btn" title="删除" @click="emit('delete', file.fileName)">
        <Delete />
      </el-icon>
    </div>
  </div>
</template>

<style scoped>
.file-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  transition: background 0.15s;
}
.file-item:hover {
  background: var(--color-surface-2);
}
.file-item__icon {
  flex: 0 0 auto;
  font-size: 18px;
  color: var(--color-primary);
}
.file-item__info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.file-item__name {
  font-size: 13px;
  color: var(--color-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.file-item__meta {
  font-size: 12px;
  color: var(--color-text-tertiary);
}
.file-item__actions {
  flex: 0 0 auto;
  display: flex;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.15s;
}
.file-item:hover .file-item__actions {
  opacity: 1;
}
.file-item__btn {
  color: var(--color-text-tertiary);
  cursor: pointer;
}
.file-item__btn:hover {
  color: var(--color-primary);
}
</style>
