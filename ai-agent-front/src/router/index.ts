import { createRouter, createWebHistory } from 'vue-router'
import MainView from '@/views/MainView.vue'

// 登录 / 注册以弹窗形式展示，不设独立全屏路由。
// 三个路径都渲染同一主界面，未登录时由 MainView 内部弹出登录窗。
const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: MainView,
    },
    {
      path: '/chat',
      name: 'chat',
      component: MainView,
    },
    {
      path: '/chat/:chatId',
      name: 'chat-detail',
      component: MainView,
    },
    // 兼容历史链接：/login、/register 重定向回主界面（以弹窗呈现）
    { path: '/login', redirect: '/' },
    { path: '/register', redirect: '/' },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

export default router
