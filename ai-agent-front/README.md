# AI 智能体前端（ai-agent-front）

基于 **Vue 3 + Vite + TypeScript + Pinia + Vue Router + Element Plus** 的 AI Agent 聊天系统前端。
前端只与 Java 后端（`/api`）通信，不直接调用 Python AI 服务。

## 技术栈

- Vue 3（组合式 API）+ TypeScript
- Vite 6
- Vue Router 4
- Pinia
- Axios（普通 HTTP）
- 原生 `fetch + ReadableStream`（SSE 流式聊天）
- Element Plus + Element Plus Icons
- markdown-it（AI 消息 Markdown 渲染）

## 目录结构

```text
src/
  api/        request.ts(Axios 封装) user.ts chat.ts file.ts
  components/
    auth/     LoginDialog.vue RegisterDialog.vue
    layout/   AppHeader.vue
    chat/     ChatSidebar.vue ChatWindow.vue ChatMessage.vue
              ChatInput.vue RouteModeTip.vue ChatActivities.vue
    file/     FilePanel.vue FileItem.vue
  layouts/    MainLayout.vue
  router/     index.ts
  stores/     userStore.ts chatStore.ts
  types/      common.ts user.ts chat.ts file.ts
  utils/      sse.ts activity.ts markdown.ts format.ts
  views/      MainView.vue
  styles/     index.css
  App.vue  main.ts
```

## 一、安装依赖

```bash
npm install
```

## 二、启动项目

```bash
npm run dev
```

默认运行在 `http://localhost:5173`。

其他命令：

```bash
npm run build        # 类型检查 + 生产构建
npm run type-check   # 仅做 TypeScript 类型检查
npm run preview      # 预览生产构建
```

## 三、配置 Java 后端接口地址

后端基础路径为 `http://<host>:8123/api`（Context Path 为 `/api`）。

### 开发环境（推荐：Vite 代理）

`vite.config.ts` 已配置代理，将 `/api` 转发到后端 `http://localhost:8123`，
保证与前端同源，Cookie / Session 正常携带：

```ts
server: {
  proxy: {
    '/api': { target: 'http://localhost:8123', changeOrigin: true },
  },
}
```

若后端地址不同，修改 `vite.config.ts` 中的 `BACKEND_TARGET`。

### 自定义基础路径

复制 `.env.example` 为 `.env`，设置 `VITE_API_BASE_URL`：

```bash
# 开发默认走代理
VITE_API_BASE_URL=/api
# 生产可填完整地址
# VITE_API_BASE_URL=https://your-domain.com/api
```

> 认证使用 **Session + Cookie**（非 Bearer Token），所有请求均带 `withCredentials/credentials: 'include'`。

## 四、对接的后端接口

| 模块 | 接口 |
|---|---|
| 用户 | `POST /user/register`、`POST /user/login`、`GET /user/current`、`POST /user/logout` |
| 会话 | `POST /agent/chat/create`、`GET /agent/chat/list`、`PUT/DELETE /agent/chat/{chatId}`、`POST /agent/chat/route` |
| 聊天 | `POST /agent/chat/stream`（SSE）、`GET /chatHistory/chat/{chatId}` |
| 文件 | `POST /chat/file/list`、`POST /chat/file/download`（blob）、`DELETE /chat/file/delete` |

特殊处理：

- **SSE 流式聊天**：`utils/sse.ts` 用 `fetch + ReadableStream`，按 `\n\n` 解析 `data:` 行，`done` 结束。
- **文件下载**：`api/file.ts` 用 `fetch + blob()` 触发浏览器下载。
- **裸响应接口**：文件列表返回数组、删除文件返回 `boolean`，不走 BaseResponse 拆包。

## 五、待人工确认的 TODO

代码中以 `TODO` 注释标记，均来自 `api-contract.md` 的不确定点：

1. `RouteResponse.enumName` 字段含义未明确（仅展示 `routeType` + `reason`）。
2. `ChatRequest.routeType` 不传时 Python 端默认行为未明确。
3. **功能触发 / 思路链展示**：SSE 除正文 `message` 外，还会下发
   `workflow_step` / `tool_start` / `tool_end` / `tool_error` / `file` 等结构化事件。
   `utils/sse.ts` 按事件名分发，`utils/activity.ts` 将其归并为活动时间线，
   `ChatActivities.vue` 渲染为「功能触发提示 / 思路链」；历史回放时由落库的
   `additional_kwargs.events` 重建（见 `ChatHistoryServiceImpl`）。
4. 注册接口仅落库、不自动登录（已按「注册成功后切回登录弹窗」实现）。
5. 后端密码当前明文存储（MD5 已注释），生产加密策略需后端确认。
6. 会话标题前端限制 1~50 字符（后端硬上限 255）。

## 六、核心交互

- 一进入即主界面；未登录时主界面模糊 + 居中登录/注册弹窗，可互相切换。
- 三栏布局：左侧会话列表 / 中间聊天框 / 右侧当前会话文件列表。
- 首次对话无 `chatId` 时自动 `create → route → stream`，完成后刷新会话与文件列表。
- 每轮展示路由模式提示；`workflow` 模式额外展示执行过程与最终结果。
- 输入框 Enter 发送、Shift+Enter 换行，回复中禁止重复发送，自动滚动到底部。
- 文件支持下载（blob）与删除（二次确认）。
```
