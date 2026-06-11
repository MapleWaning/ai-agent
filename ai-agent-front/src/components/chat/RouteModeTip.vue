<script setup lang="ts">
import { computed } from 'vue'
import type { RouteType } from '@/types/chat'

const props = defineProps<{
  routeType?: RouteType | null
  reason?: string | null
}>()

// 文案来自 docs/page-map.md 第 8.2 节 RouteType 映射
const LABELS: Record<RouteType, string> = {
  normal_chat: '已使用普通对话模式',
  report: '已使用报告生成模式',
  rag: '已使用 RAG 知识库问答模式',
  mcp: '已使用地图 MCP 模式',
  tool: '已使用工具调用模式',
  workflow: '已使用复杂工作流模式',
}

const label = computed(() => {
  if (!props.routeType) return ''
  return LABELS[props.routeType] ?? `已路由到：${props.routeType}`
})
</script>

<template>
  <div v-if="label" class="route-tip" :title="reason || ''">
    <el-icon class="route-tip__icon"><MagicStick /></el-icon>
    <span>{{ label }}</span>
  </div>
</template>

<style scoped>
.route-tip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  margin: 2px 0 8px;
  font-size: 12px;
  color: var(--color-text-secondary);
  background: var(--color-surface-2);
  border-radius: 999px;
}
.route-tip__icon {
  font-size: 13px;
  color: var(--color-primary);
}
</style>
