import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
import { registerUnauthorizedHandler } from './api/request'
import { useUserStore } from './stores/userStore'
import { useChatStore } from './stores/chatStore'
import './styles/index.css'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
app.use(ElementPlus)

// 全局注册 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 注册未登录 / 登录过期处理：清空状态，主界面会自动弹出登录窗
const userStore = useUserStore()
const chatStore = useChatStore()
registerUnauthorizedHandler(() => {
  userStore.clear()
  chatStore.reset()
})

app.mount('#app')
