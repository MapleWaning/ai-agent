import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// Java 后端地址（含 context-path /api 在路径上，不做 rewrite）
const BACKEND_TARGET = 'http://localhost:8123'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      // 开发环境通过代理保证与后端同源，Cookie/Session 正常携带
      '/api': {
        target: BACKEND_TARGET,
        changeOrigin: true,
      },
    },
  },
})
