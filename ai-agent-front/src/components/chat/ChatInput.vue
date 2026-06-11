<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  disabled?: boolean
}>()

const emit = defineEmits<{
  (e: 'send', text: string): void
}>()

const text = ref('')

function handleSend() {
  const value = text.value.trim()
  if (!value) return
  if (props.disabled) {
    ElMessage.warning('AI 正在回复中，请稍候')
    return
  }
  emit('send', value)
  text.value = ''
}

// Enter 发送，Shift + Enter 换行
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey && !e.isComposing) {
    e.preventDefault()
    handleSend()
  }
}
</script>

<template>
  <div class="chat-input">
    <el-input
      v-model="text"
      type="textarea"
      :autosize="{ minRows: 1, maxRows: 6 }"
      resize="none"
      placeholder="输入消息，Enter 发送，Shift + Enter 换行"
      @keydown="handleKeydown"
    />
    <el-button
      type="primary"
      class="chat-input__send"
      :loading="disabled"
      :disabled="!text.trim()"
      @click="handleSend"
    >
      发送
    </el-button>
  </div>
</template>

<style scoped>
.chat-input {
  display: flex;
  gap: 10px;
  align-items: flex-end;
  padding: 12px 16px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
}
.chat-input :deep(.el-textarea__inner) {
  box-shadow: none;
  border: none;
  padding: 6px 4px;
  font-size: 14px;
}
.chat-input :deep(.el-textarea__inner:focus) {
  box-shadow: none;
}
.chat-input__send {
  flex: 0 0 auto;
}
</style>
