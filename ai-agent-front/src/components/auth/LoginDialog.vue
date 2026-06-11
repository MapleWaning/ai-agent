<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/stores/userStore'
import type { LoginRequest } from '@/types/user'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  (e: 'switch'): void
  (e: 'success'): void
}>()

const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive<LoginRequest>({
  userAccount: '',
  userPassword: '',
})

const rules: FormRules<LoginRequest> = {
  userAccount: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  userPassword: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.login({ ...form })
    ElMessage.success('登录成功')
    emit('success')
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="props.visible"
    title="登录"
    width="380px"
    align-center
    :show-close="false"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      @submit.prevent="handleSubmit"
    >
      <el-form-item label="用户名" prop="userAccount">
        <el-input v-model="form.userAccount" placeholder="请输入用户名" />
      </el-form-item>
      <el-form-item label="密码" prop="userPassword">
        <el-input
          v-model="form.userPassword"
          type="password"
          show-password
          placeholder="请输入密码"
          @keyup.enter="handleSubmit"
        />
      </el-form-item>
      <el-button
        type="primary"
        class="auth-submit"
        :loading="loading"
        @click="handleSubmit"
      >
        登录
      </el-button>
    </el-form>

    <div class="auth-switch">
      没有账号？
      <el-link type="primary" :underline="false" @click="emit('switch')">去注册</el-link>
    </div>
  </el-dialog>
</template>

<style scoped>
.auth-submit {
  width: 100%;
  margin-top: 4px;
}
.auth-switch {
  margin-top: 16px;
  text-align: center;
  font-size: 13px;
  color: var(--color-text-secondary);
}
</style>
