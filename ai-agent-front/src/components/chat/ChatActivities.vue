<script setup lang="ts">
import { computed } from 'vue'
import type { ActivityStatus, ChatActivity } from '@/types/chat'

const props = defineProps<{
  activities: ChatActivity[]
  /** workflow 模式下作为「思路链」展示，带标题与连接线 */
  workflow?: boolean
}>()

interface ActivityView {
  key: string
  kind: 'tool' | 'workflow_step' | 'file'
  status: ActivityStatus
  label: string
  detail: string
}

/** 把工具入参压缩成简短可读的文本 */
function formatInput(input: unknown): string {
  if (input === undefined || input === null) return ''
  if (typeof input === 'string') return input
  try {
    const text = JSON.stringify(input)
    return text.length > 120 ? `${text.slice(0, 120)}…` : text
  } catch {
    return String(input)
  }
}

const items = computed<ActivityView[]>(() =>
  props.activities.map((a) => {
    if (a.type === 'workflow_step') {
      return {
        key: a.key,
        kind: 'workflow_step',
        status: a.status,
        label: `步骤 ${a.step} · ${a.title}`,
        detail: a.detail ?? '',
      }
    }
    if (a.type === 'file') {
      return {
        key: a.key,
        kind: 'file',
        status: 'finished',
        label: `生成文件：${a.fileName}`,
        detail: a.summary ?? '',
      }
    }
    // tool / mcp / rag
    const verb =
      a.status === 'running'
        ? '正在调用'
        : a.status === 'failed'
          ? '调用失败'
          : '已调用'
    const detail =
      a.status === 'failed'
        ? a.error ?? '执行失败'
        : a.summary ?? formatInput(a.input)
    return {
      key: a.key,
      kind: 'tool',
      status: a.status,
      label: `${verb}：${a.label}`,
      detail,
    }
  }),
)

const statusLabel: Record<ActivityStatus, string> = {
  running: '进行中',
  finished: '完成',
  failed: '失败',
}
</script>

<template>
  <div class="activities" :class="{ 'activities--workflow': workflow }">
    <div v-if="workflow" class="activities__title">
      <el-icon><MagicStick /></el-icon>
      <span>思路链</span>
    </div>

    <ul class="activities__list">
      <li
        v-for="item in items"
        :key="item.key"
        class="activity"
        :class="`activity--${item.status}`"
      >
        <span class="activity__icon">
          <el-icon v-if="item.status === 'running'" class="is-loading"><Loading /></el-icon>
          <el-icon v-else-if="item.status === 'failed'"><CircleCloseFilled /></el-icon>
          <el-icon v-else-if="item.kind === 'file'"><Document /></el-icon>
          <el-icon v-else><CircleCheckFilled /></el-icon>
        </span>

        <div class="activity__body">
          <div class="activity__head">
            <span class="activity__label">{{ item.label }}</span>
            <span class="activity__status">{{ statusLabel[item.status] }}</span>
          </div>
          <div v-if="item.detail" class="activity__detail">{{ item.detail }}</div>
        </div>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.activities {
  margin-bottom: 10px;
  padding: 8px 12px;
  background: var(--color-surface-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm, 8px);
}
.activities--workflow {
  background: var(--color-primary-soft, var(--color-surface-2));
}
.activities__title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
  margin-bottom: 8px;
}
.activities__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.activity {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}
.activity__icon {
  flex: 0 0 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 1px;
  font-size: 15px;
  color: var(--color-text-tertiary);
}
.activity--running .activity__icon {
  color: var(--color-primary);
}
.activity--finished .activity__icon {
  color: #2faa6b;
}
.activity--failed .activity__icon {
  color: #e04c4c;
}
.activity__body {
  min-width: 0;
  flex: 1;
}
.activity__head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.activity__label {
  font-size: 13px;
  color: var(--color-text);
  font-weight: 500;
}
.activity__status {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 999px;
  color: var(--color-text-secondary);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
}
.activity--running .activity__status {
  color: var(--color-primary);
}
.activity--failed .activity__status {
  color: #e04c4c;
}
.activity__detail {
  margin-top: 2px;
  font-size: 12px;
  color: var(--color-text-secondary);
  word-break: break-word;
  white-space: pre-wrap;
}
</style>
