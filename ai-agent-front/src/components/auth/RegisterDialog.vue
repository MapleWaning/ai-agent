<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/stores/userStore'
import type { RegisterRequest } from '@/types/user'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  (e: 'switch'): void
}>()

const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive<RegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})

const validateCheckPassword = (
  _rule: unknown,
  value: string,
  callback: (error?: Error) => void,
) => {
  if (value !== form.userPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules: FormRules<RegisterRequest> = {
  userAccount: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  // 后端要求密码长度必须大于 8 个字符
  userPassword: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 9, message: '密码长度必须大于 8 个字符', trigger: 'blur' },
  ],
  checkPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateCheckPassword, trigger: 'blur' },
  ],
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.register({ ...form })
    // 后端注册仅落库、不自动登录，注册成功后切回登录弹窗
    ElMessage.success('注册成功，请登录')
    emit('switch')
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
    title="注册"
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
          placeholder="密码长度需大于 8 位"
        />
      </el-form-item>
      <el-form-item label="确认密码" prop="checkPassword">
        <el-input
          v-model="form.checkPassword"
          type="password"
          show-password
          placeholder="请再次输入密码"
          @keyup.enter="handleSubmit"
        />
      </el-form-item>
      <el-button
        type="primary"
        class="auth-submit"
        :loading="loading"
        @click="handleSubmit"
      >
        注册
      </el-button>
    </el-form>

    <div class="auth-switch">
      已有账号？
      <el-link type="primary" :underline="false" @click="emit('switch')">去登录</el-link>
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
