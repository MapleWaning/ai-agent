<script setup lang="ts">
import { computed } from 'vue'
import { renderMarkdown } from '@/utils/markdown'
import RouteModeTip from './RouteModeTip.vue'
import ChatActivities from './ChatActivities.vue'
import type { UIMessage } from '@/types/chat'

const props = defineProps<{
  message: UIMessage
}>()

const isUser = computed(() => props.message.role === 'user')
const isWorkflow = computed(() => props.message.routeType === 'workflow')
const renderedContent = computed(() => renderMarkdown(props.message.content))
const hasActivities = computed(() => (props.message.activities?.length ?? 0) > 0)
// 仅在没有任何可展示内容（正文 / 活动 / 状态文案）时显示「正在思考中」
const showThinking = computed(
  () =>
    props.message.streaming &&
    !props.message.content &&
    !hasActivities.value &&
    !props.message.statusText,
)
const showResultLabel = computed(
  () => isWorkflow.value && hasActivities.value && !!props.message.content,
)
</script>

<template>
  <div class="msg" :class="isUser ? 'msg--user' : 'msg--ai'">
    <div class="msg__avatar">
      <el-icon v-if="isUser"><User /></el-icon>
      <el-icon v-else><Comment /></el-icon>
    </div>

    <div class="msg__body">
      <!-- 路由模式提示（仅 AI 消息且本轮有路由信息时展示） -->
      <RouteModeTip
        v-if="!isUser && message.routeType"
        :route-type="message.routeType"
        :reason="message.routeReason"
      />

      <div class="msg__bubble" :class="{ 'msg__bubble--error': message.error }">
        <!-- 用户消息：纯文本 -->
        <template v-if="isUser">
          <span class="msg__text">{{ message.content }}</span>
        </template>

        <!-- AI 消息 -->
        <template v-else>
          <!-- 功能触发 / 思路链时间线（工具、MCP、RAG、文件、工作流步骤） -->
          <ChatActivities
            v-if="hasActivities"
            :activities="message.activities!"
            :workflow="isWorkflow"
          />

          <!-- 临时状态文案（流式中、正文尚未产生时） -->
          <div
            v-if="message.streaming && message.statusText && !message.content"
            class="msg__status"
          >
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>{{ message.statusText }}</span>
          </div>

          <span v-else-if="showThinking" class="msg__thinking">正在思考中…</span>

          <!-- 最终结果 -->
          <div v-if="showResultLabel" class="msg__result-label">最终结果</div>
          <div
            v-if="message.content"
            class="markdown-body"
            v-html="renderedContent"
          ></div>

          <span
            v-if="message.streaming && message.content"
            class="msg__cursor"
            >▍</span
          >
        </template>

        <div v-if="message.error" class="msg__error-tip">回复异常结束</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.msg {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  align-items: flex-start;
}
.msg--user {
  flex-direction: row-reverse;
}
.msg__avatar {
  flex: 0 0 32px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-surface-2);
  color: var(--color-text-secondary);
}
.msg--user .msg__avatar {
  background: var(--color-primary-soft);
  color: var(--color-primary);
}
.msg__body {
  max-width: 76%;
  display: flex;
  flex-direction: column;
}
.msg--user .msg__body {
  align-items: flex-end;
}
.msg__bubble {
  padding: 10px 14px;
  border-radius: var(--radius);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
}
.msg--user .msg__bubble {
  background: var(--color-primary);
  color: #fff;
  border-color: var(--color-primary);
}
.msg__bubble--error {
  border-color: #f5c2c2;
}
.msg__text {
  white-space: pre-wrap;
  word-break: break-word;
}
.msg__thinking {
  color: var(--color-text-tertiary);
}
.msg__status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--color-text-secondary);
}
.msg__result-label {
  font-size: 12px;
  color: var(--color-text-tertiary);
  margin-bottom: 4px;
}
.msg__cursor {
  color: var(--color-primary);
  animation: blink 1s step-start infinite;
}
.msg__error-tip {
  margin-top: 6px;
  font-size: 12px;
  color: #e04c4c;
}
@keyframes blink {
  50% {
    opacity: 0;
  }
}
</style>
