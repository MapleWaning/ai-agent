# 后端接口文档

> 本文档基于 Java 后端源码整理，供 Vue 3 前端 Agent 生成项目使用。  
> 源码分析范围：`Controller`、`DTO`、`VO`、`Entity`、`Service`、认证拦截器、跨域配置、异常处理。  
> 生成时间：2026-06-08（最后更新：2026-06-08，第三轮）

---

## 1. 项目接口说明

### 架构说明

- **前端只调用 Java 后端**，不直接调用 Python FastAPI 服务。
- Java 后端在内部通过 `RestClient` / `HttpClient` 调用 Python 服务（默认 `http://localhost:8000`），前端无需感知。
- Python 负责 AI 能力（普通对话、RAG、MCP、工具调用、报告生成、Workflow 等）；Java 负责用户、会话、聊天历史、文件管理及接口暴露。

### 基础信息

| 项 | 值 |
| --- | --- |
| 服务端口 | `8123`（见 `application.yml`） |
| Context Path | `/api` |
| **接口基础路径** | `http://<host>:8123/api` |
| API 文档（Knife4j） | `http://<host>:8123/api/swagger-ui.html` |

### 登录要求

| 路径前缀 | 是否需要登录 |
| --- | --- |
| `/api/user/login`、`/api/user/register`、`/api/user/logout` | 否 |
| `/api/user/**`（除上述三个） | 是 |
| `/api/agent/**` | 是 |
| `/api/chatHistory/**` | 是 |
| `/api/chat/file/**` | 是 |

### 认证方式

- **Session + Cookie**，不使用 `Authorization: Bearer` Token。
- 登录成功后：
  - 服务端写入 **HttpSession**，键为 `user_login`，值为 `LoginUserVO`。
  - 同时写入 **Redis**（键：`user_login:{userId}`，TTL 默认 3600 秒）。
  - 响应 Set-Cookie：名称为 `user_login`，值为 `userId` 字符串，Path=`/`，MaxAge 与 Redis TTL 一致。
- 前端跨域请求必须设置 **`credentials: 'include'`**（或 Axios `withCredentials: true`），否则 Cookie 不会携带。

### 统一响应格式

大部分 JSON 接口使用 `BaseResponse<T>` 包装。以下接口**不使用** `BaseResponse`：

- `POST /api/agent/chat/stream`：SSE 流
- `POST /api/chat/file/list`：直接返回 `ChatFileVO[]`
- `POST /api/chat/file/download`：二进制流
- `DELETE /api/chat/file/delete`：直接返回 `boolean`

### 时间字段格式

- 文件列表中的 `lastModified`：`yyyy-MM-dd HH:mm:ss`（服务器本地时区）。
- `Chat` / `ChatVO` 的 `createTime`、`modifyTime` 类型为 `LocalDateTime`，JSON 序列化默认为 **ISO-8601** 字符串（如 `"2026-06-08T14:30:00"`），无时区后缀。
- `ChatHistory` 实体**无** `createTime` / `updateTime` 字段。

### 分页格式

聊天历史使用 **游标分页**（基于 `lastId`），返回 MyBatis-Plus `Page<T>` 结构，非传统 offset 分页。

---

## 2. 通用响应格式

### 成功响应

```json
{
  "code": 0,
  "data": {},
  "message": "ok"
}
```

### 字段说明

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `code` | `number` | 业务状态码，`0` 表示成功；非 `0` 表示失败 |
| `data` | `T` | 业务数据，失败时为 `null` |
| `message` | `string` | 提示信息，成功时为 `"ok"` |

### 失败响应

```json
{
  "code": 40000,
  "data": null,
  "message": "请求参数错误"
}
```

### 错误码一览（`ErrorCode` 枚举）

| code | message | 说明 |
| --- | --- | --- |
| `0` | `ok` | 成功 |
| `40000` | `请求参数错误` | 参数校验失败 |
| `40001` | `用户名已存在` | 注册时账号重复 |
| `40002` | `两次密码不一致` | 注册时确认密码不匹配 |
| `40100` | `密码错误` | 登录密码错误 |
| `40103` | `未登录` | 未登录或 Session 失效 |
| `40104` | `无权限` | 非管理员访问管理接口 |
| `40400` | `用户不存在` | 登录账号不存在 |
| `50000` | `操作失败` | 通用操作失败 |

### 异常处理

- 业务异常（`BusinessException`）由 `GlobalExceptionHandler` 捕获，返回 `BaseResponse`，HTTP 状态码仍为 **200**（拦截器触发的未登录除外）。
- 登录拦截器（`LoginAuthInterceptor`）拦截未登录请求时：HTTP 状态码 **401**，响应体为 `BaseResponse` JSON。

---

## 3. 认证与登录态

### 登录流程

1. 前端 `POST /api/user/login`，Body 传 `userAccount`、`userPassword`。
2. 服务端校验通过后写入 Session、Redis、Cookie。
3. 响应 `data` 为 `LoginUserVO`（`userId`、`userName`、`role`）。
4. 浏览器自动保存 Cookie `user_login`；后续请求同域/跨域（带 credentials）自动携带。

### 前端请求头

| 请求头 | 是否必须 | 说明 |
| --- | --- | --- |
| `Content-Type: application/json` | JSON 接口需要 | 常规 POST/PUT 请求 |
| `Authorization` | **不需要** | 项目未使用 Bearer Token |
| Cookie（`user_login`） | 需要（已登录接口） | 依赖浏览器自动携带 + `credentials: 'include'` |

### Session 校验逻辑

- 拦截器对受保护路径调用 `UserService.getLoginUser(request)`。
- 从 `HttpSession` 读取 `user_login` 属性；Session 不存在或无效则抛 `NOT_LOGIN_ERROR`（40103）。

### 获取当前用户

- 前端 `GET /api/user/current`，服务端从 Session 读取登录态。
- 响应 `data` 为 `LoginUserVO`（`userId`、`userName`、`role`）。
- 用于应用初始化时校验登录态、恢复用户信息。

### 退出登录

- 前端 `POST /api/user/logout`（**无需登录**，已加入拦截器排除列表）。
- 服务端清除 Session、Redis 登录态，并将 Cookie `user_login` 置空（MaxAge=0）。
- 即使未登录也可调用，幂等安全。

### 401 / 未登录时前端处理建议

1. 收到 HTTP **401** 或 `code === 40103` 时，清除本地用户状态。
2. 跳转登录页。
3. 确保 Axios / fetch 全局配置 `withCredentials: true` / `credentials: 'include'`。
4. 应用启动时可调用 `GET /api/user/current` 探测登录态；失败则跳转登录页。

### 管理员权限

- `/user/add`、`/user/delete/{id}`、`/user/update`、`/user/get/{id}`、`/user/list` 在 Controller 中调用 `UserService.checkAdmin()`。
- 当前用户 `role !== "admin"` 时返回 `code: 40104`，`message: "无权限"`（HTTP 200 + JSON `BaseResponse`）。

### 已知限制（TODO）

- **无 Token 刷新机制**：Session/Redis/Cookie TTL 默认 3600 秒，过期后需重新登录。

---

## 4. 用户模块接口

### 4.1 用户注册

**接口说明：** 注册新用户，仅落库，**不自动登录**。

**请求方法：** `POST`

**请求路径：** `/api/user/register`

**是否需要登录：** 否

**请求头：**

```
Content-Type: application/json
```

**请求参数：**

| 字段名 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| `userAccount` | `string` | 是 | 用户账号，落库为 `userName` |
| `userPassword` | `string` | 是 | 密码，长度必须 **大于 8** 个字符 |
| `checkPassword` | `string` | 是 | 确认密码，须与 `userPassword` 一致 |

**请求示例：**

```json
{
  "userAccount": "xiaoming",
  "userPassword": "123456789",
  "checkPassword": "123456789"
}
```

**响应参数：**

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `code` | `number` | `0` 成功 |
| `data` | `number` | 新注册用户的 `userId` |
| `message` | `string` | `"ok"` |

**响应示例：**

```json
{
  "code": 0,
  "data": 100,
  "message": "ok"
}
```

**前端对接注意事项：**

- 用于**注册页**。
- 注册成功后需引导用户跳转登录页手动登录。
- 密码当前为**明文存储**（源码中 MD5 加密已注释），TODO：生产环境加密策略需人工确认。

---

### 4.2 用户登录

**接口说明：** 用户登录，写入 Session、Redis、Cookie。

**请求方法：** `POST`

**请求路径：** `/api/user/login`

**是否需要登录：** 否

**请求头：**

```
Content-Type: application/json
```

**请求参数：**

| 字段名 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| `userAccount` | `string` | 是 | 用户账号 |
| `userPassword` | `string` | 是 | 用户密码 |

**请求示例：**

```json
{
  "userAccount": "xiaoming",
  "userPassword": "123456789"
}
```

**响应参数（`data` 为 `LoginUserVO`）：**

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `userId` | `number` | 用户 ID |
| `userName` | `string` | 用户名（与账号相同） |
| `role` | `string` | 角色，默认 `"user"`；管理员为 `"admin"` |

**响应示例：**

```json
{
  "code": 0,
  "data": {
    "userId": 1,
    "userName": "xiaoming",
    "role": "user"
  },
  "message": "ok"
}
```

**前端对接注意事项：**

- 用于**登录页**。
- 必须开启 `withCredentials: true`，以接收并后续携带 Cookie。
- 可将 `data` 缓存到 Pinia/Vuex 作为当前用户信息。
- 应用刷新后可用 `GET /api/user/current` 恢复完整用户信息。

---

### 4.3 获取当前登录用户

**接口说明：** 获取当前 Session 中的登录用户完整信息。

**请求方法：** `GET`

**请求路径：** `/api/user/current`

**是否需要登录：** 是

**请求头：**

```
Cookie: user_login=<userId>
```

**请求参数：** 无

**响应参数（`data` 为 `LoginUserVO`）：**

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `userId` | `number` | 用户 ID |
| `userName` | `string` | 用户名 |
| `role` | `string` | 角色 |

**响应示例：**

```json
{
  "code": 0,
  "data": {
    "userId": 1,
    "userName": "xiaoming",
    "role": "user"
  },
  "message": "ok"
}
```

**前端对接注意事项：**

- 用于**应用初始化**、**布局顶栏**展示用户名与角色。
- 未登录时拦截器返回 HTTP 401 + `code: 40103`。
- 与登录接口返回结构一致，可直接写入 Pinia/Vuex。

---

### 4.4 退出登录

**接口说明：** 清除服务端 Session、Redis 登录态及 Cookie。

**请求方法：** `POST`

**请求路径：** `/api/user/logout`

**是否需要登录：** 否（拦截器已排除；未登录时调用也安全）

**请求参数：** 无

**响应参数：**

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `data` | `boolean` | 固定为 `true` |

**响应示例：**

```json
{
  "code": 0,
  "data": true,
  "message": "ok"
}
```

**前端对接注意事项：**

- 用于**顶栏退出按钮**。
- 调用成功后清除本地用户状态并跳转登录页。
- 须携带 `credentials: 'include'`，以便服务端清除 Cookie。

---

### 4.5 新增用户

**接口说明：** 直接新增用户记录（管理用途）。

**请求方法：** `POST`

**请求路径：** `/api/user/add`

**是否需要登录：** 是（且需 `role=admin`）

**请求头：**

```
Content-Type: application/json
Cookie: user_login=<userId>
```

**请求参数（`User` 实体）：**

| 字段名 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| `userId` | `number` | 否 | 自增主键，新增时可不传 |
| `userName` | `string` | TODO | 用户名 |
| `password` | `string` | TODO | 密码 |
| `role` | `string` | 否 | 角色，默认 `"user"` |

**请求示例：**

```json
{
  "userName": "newuser",
  "password": "123456789",
  "role": "user"
}
```

**响应参数：**

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `data` | `boolean` | 是否保存成功 |

**响应示例：**

```json
{
  "code": 0,
  "data": true,
  "message": "ok"
}
```

**前端对接注意事项：**

- 管理后台用途；**需要 `role === "admin"`**，否则 `code: 40104`。
- 一般聊天前端**不需要**对接此接口。

---

### 4.6 删除用户

**接口说明：** 根据 ID 删除用户。

**请求方法：** `DELETE`

**请求路径：** `/api/user/delete/{id}`

**是否需要登录：** 是（且需 `role=admin`）

**路径参数：**

| 字段名 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| `id` | `number` | 是 | 用户 ID |

**响应参数：**

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `data` | `boolean` | 是否删除成功 |

**响应示例：**

```json
{
  "code": 0,
  "data": true,
  "message": "ok"
}
```

**前端对接注意事项：** 管理后台用途，需管理员角色；聊天前端一般不对接。

---

### 4.7 更新用户

**接口说明：** 更新用户信息。

**请求方法：** `PUT`

**请求路径：** `/api/user/update`

**是否需要登录：** 是（且需 `role=admin`）

**请求参数（`User` 实体）：**

| 字段名 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| `userId` | `number` | 是 | 要更新的用户 ID |
| `userName` | `string` | 否 | 用户名 |
| `password` | `string` | 否 | 密码 |
| `role` | `string` | 否 | 角色 |

**响应示例：**

```json
{
  "code": 0,
  "data": true,
  "message": "ok"
}
```

**前端对接注意事项：** 管理后台用途，需管理员角色。

---

### 4.8 根据 ID 查询用户

**接口说明：** 根据用户 ID 查询用户详情。

**请求方法：** `GET`

**请求路径：** `/api/user/get/{id}`

**是否需要登录：** 是（且需 `role=admin`）

**路径参数：**

| 字段名 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| `id` | `number` | 是 | 用户 ID |

**响应参数（`data` 为 `User`）：**

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `userId` | `number` | 用户 ID |
| `userName` | `string` | 用户名 |
| `password` | `string` | 密码（明文返回，注意安全） |
| `role` | `string` | 角色 |

**响应示例：**

```json
{
  "code": 0,
  "data": {
    "userId": 1,
    "userName": "xiaoming",
    "password": "123456789",
    "role": "user"
  },
  "message": "ok"
}
```

**前端对接注意事项：** 需管理员角色；响应包含密码字段，前端展示时需过滤。

---

### 4.9 查询用户列表

**接口说明：** 查询全部用户列表。

**请求方法：** `GET`

**请求路径：** `/api/user/list`

**是否需要登录：** 是（且需 `role=admin`）

**响应参数（`data` 为 `User[]`）：**

**响应示例：**

```json
{
  "code": 0,
  "data": [
    {
      "userId": 1,
      "userName": "xiaoming",
      "password": "123456789",
      "role": "user"
    }
  ],
  "message": "ok"
}
```

**前端对接注意事项：** 管理后台用途，需管理员角色，无分页。

---

## 5. 会话模块接口

### 5.1 创建会话

**接口说明：** 为当前登录用户创建一个新聊天会话，返回 `chatId`。

**请求方法：** `POST`

**请求路径：** `/api/agent/chat/create`

**是否需要登录：** 是

**请求头：**

```
Cookie: user_login=<userId>
```

**请求参数：** 无请求体。`userId` 从登录态 Session 自动获取，**不需要**前端传递。

**响应参数：**

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `data` | `number` | 新创建的 `chatId` |

**响应示例：**

```json
{
  "code": 0,
  "data": 101,
  "message": "ok"
}
```

**前端对接注意事项：**

- 用于**聊天页**「新建对话」。
- 创建时默认 `title` 为 `"新会话"`。
- `createTime` / `modifyTime` 由数据库自动维护。

---

### 5.2 查询当前用户会话列表

**接口说明：** 查询当前登录用户的全部会话，按 `modifyTime` 降序（最近修改在前）。

**请求方法：** `GET`

**请求路径：** `/api/agent/chat/list`

**是否需要登录：** 是

**请求头：**

```
Cookie: user_login=<userId>
```

**请求参数：** 无。`userId` 从登录态获取。

**响应参数（`data` 为 `ChatVO[]`）：**

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `chatId` | `number` | 会话 ID |
| `title` | `string` | 会话标题 |
| `createTime` | `string` | 创建时间（ISO-8601） |
| `modifyTime` | `string` | 最后修改时间（ISO-8601） |

**响应示例：**

```json
{
  "code": 0,
  "data": [
    {
      "chatId": 105,
      "title": "恋爱咨询",
      "createTime": "2026-06-08T10:00:00",
      "modifyTime": "2026-06-08T14:30:00"
    },
    {
      "chatId": 101,
      "title": "新会话",
      "createTime": "2026-06-07T09:00:00",
      "modifyTime": "2026-06-07T18:00:00"
    }
  ],
  "message": "ok"
}
```

**前端对接注意事项：**

- 用于**聊天页侧边栏**会话列表，一次请求即可展示标题与时间，无需 N+1 详情请求。
- 空列表时 `data` 为 `[]`。

---

### 5.3 查询单个会话详情

**接口说明：** 查询指定会话的元信息，并校验会话归属当前用户。

**请求方法：** `GET`

**请求路径：** `/api/agent/chat/{chatId}`

**是否需要登录：** 是

**路径参数：**

| 字段名 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| `chatId` | `string` | 是 | 会话 ID（路径参数为字符串，内部解析为整数） |

**响应参数（`data` 为 `ChatVO`）：**

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `chatId` | `number` | 会话 ID |
| `title` | `string` | 会话标题 |
| `createTime` | `string` | 创建时间（ISO-8601） |
| `modifyTime` | `string` | 最后修改时间（ISO-8601） |

**响应示例：**

```json
{
  "code": 0,
  "data": {
    "chatId": 101,
    "title": "新会话",
    "createTime": "2026-06-08T10:00:00",
    "modifyTime": "2026-06-08T14:30:00"
  },
  "message": "ok"
}
```

**失败场景：**

- 会话不存在或不属于当前用户：`code: 50000`，`message: "会话不存在"`。

**前端对接注意事项：**

- 用于进入某个会话时加载元信息，或侧边栏选中项详情刷新。
- 列表接口 [5.2](#52-查询当前用户会话列表) 已返回完整 `ChatVO`，通常无需逐个调用本接口。

---

### 5.4 更新会话标题

**接口说明：** 更新指定会话标题，校验归属当前用户。

**请求方法：** `PUT`

**请求路径：** `/api/agent/chat/{chatId}`

**是否需要登录：** 是

**路径参数：**

| 字段名 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| `chatId` | `string` | 是 | 会话 ID |

**请求头：**

```
Content-Type: application/json
Cookie: user_login=<userId>
```

**请求参数（`ChatUpdateRequest`）：**

| 字段名 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| `title` | `string` | 是 | 新标题，不能为空，最长 255 字符 |

**请求示例：**

```json
{
  "title": "恋爱咨询"
}
```

**响应参数（`data` 为更新后的 `ChatVO`）：**

**响应示例：**

```json
{
  "code": 0,
  "data": {
    "chatId": 101,
    "title": "恋爱咨询",
    "createTime": "2026-06-08T10:00:00",
    "modifyTime": "2026-06-08T15:00:00"
  },
  "message": "ok"
}
```

**失败场景：**

| 场景 | code | message |
| --- | --- | --- |
| 标题为空 | `40000` | `标题不能为空` |
| 标题超过 255 字符 | `40000` | `标题长度不能超过255个字符` |
| 会话不存在或不属于当前用户 | `50000` | `会话不存在` |

**前端对接注意事项：**

- 用于**聊天页侧边栏**重命名会话。
- `userId` 从登录态获取，不需要前端传递。

---

### 5.5 删除会话

**接口说明：** 删除指定会话，校验归属当前用户，并级联清理聊天历史与会话文件目录。

**请求方法：** `DELETE`

**请求路径：** `/api/agent/chat/{chatId}`

**是否需要登录：** 是

**路径参数：**

| 字段名 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| `chatId` | `string` | 是 | 会话 ID |

**请求参数：** 无请求体。

**响应参数：**

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `data` | `boolean` | 固定为 `true` |

**响应示例：**

```json
{
  "code": 0,
  "data": true,
  "message": "ok"
}
```

**前端对接注意事项：**

- 用于**聊天页侧边栏**删除对话。
- **级联删除**：`chat_history` 表中该会话记录 + `tmp/{userId}/{chatId}/` 目录下全部文件，最后删除 `chat` 表记录。
- 会话不存在或不属于当前用户时返回 `code: 50000`。
- 删除为不可逆操作，前端需二次确认。

---

## 6. 聊天模块接口

### 6.1 路由决策

**接口说明：** 根据用户初始输入，调用 Python `/ai/chat/route` 进行 AI 能力路由决策，返回推荐的 `routeType`。

**请求方法：** `POST`

**请求路径：** `/api/agent/chat/route`

**是否需要登录：** 是（拦截器保护，但接口内部不读取登录用户）

**请求头：**

```
Content-Type: application/json
Cookie: user_login=<userId>
```

**请求参数：**

| 字段名 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| `initPrompt` | `string` | 是 | 用户初始输入，用于路由决策 |

**请求示例：**

```json
{
  "initPrompt": "帮我搜索恋爱故事并生成 PDF 报告"
}
```

**响应参数（`data` 为 `RouteResponse`）：**

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `routeType` | `string` | 路由类型值，如 `"normal_chat"`、`"report"` 等 |
| `enumName` | `string` | 枚举名称，TODO：该字段含义需要人工确认 |
| `reason` | `string` | 路由决策原因说明 |

**响应示例：**

```json
{
  "code": 0,
  "data": {
    "routeType": "report",
    "enumName": "REPORT",
    "reason": "用户请求生成 PDF 报告"
  },
  "message": "ok"
}
```

**前端对接注意事项：**

- 用于**聊天页**发送消息前的路由预判（可选流程）。
- 标准流程参考集成测试：`create` → `route` → `stream`。
- 路由结果中的 `routeType` 需传入后续流式聊天请求的 `routeType` 字段。

---

### 6.2 流式聊天（SSE）

**接口说明：** 发送聊天消息并以 SSE 流式返回 AI 回复。服务端会预加载历史、转发 Python、落库 AI 回复。详见 [第 7 节](#7-sse-流式接口说明)。

**请求方法：** `POST`

**请求路径：** `/api/agent/chat/stream`

**是否需要登录：** 是

**请求头：**

```
Content-Type: application/json
Accept: text/event-stream
Cookie: user_login=<userId>
```

**请求参数（`ChatRequest`）：**

| 字段名 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| `message` | `string` | 是 | 用户发送的消息内容 |
| `chatId` | `string` | 是 | 会话 ID（字符串类型） |
| `routeType` | `string` | 否 | 路由类型，见 RouteType 枚举；不传时 TODO：Python 端默认行为需人工确认 |
| `userId` | `string` | 否 | **无需前端传递**；服务端从登录态覆盖设置 |

**请求示例：**

```json
{
  "message": "你好，我想咨询恋爱问题",
  "chatId": "101",
  "routeType": "normal_chat"
}
```

**响应：** 非 `BaseResponse`，直接返回 `text/event-stream` 流。见第 7 节。

**前端对接注意事项：**

- 用于**聊天页**核心对话。
- `userId` 由后端从 Session 注入，前端**不要**自行传 `userId`。
- `chatId` 来自创建会话接口返回值，前端自行维护。
- 发送消息后，用户消息由后端 `preload` 自动写入数据库，**无需前端单独调用保存接口**。

---

### 6.3 查询聊天历史（游标分页）

**接口说明：** 按会话 ID 游标分页查询对话历史，按 `id` 降序（最新在前）。

**请求方法：** `GET`

**请求路径：** `/api/chatHistory/chat/{chatId}`

**是否需要登录：** 是

**路径参数：**

| 字段名 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| `chatId` | `number` | 是 | 会话 ID，必须 > 0 |

**Query 参数：**

| 字段名 | 类型 | 是否必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `pageSize` | `number` | 否 | `10` | 每页条数，范围 1–50 |
| `lastId` | `number` | 否 | 无 | 游标 ID；首次加载不传；加载更早历史时传上一页最后一条的 `id` |

**请求示例：**

```
GET /api/chatHistory/chat/101?pageSize=10
GET /api/chatHistory/chat/101?pageSize=10&lastId=95
```

**响应参数（`data` 为 `Page<ChatHistory>`）：**

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `records` | `ChatHistory[]` | 当前页记录 |
| `total` | `number` | 符合条件的总记录数 |
| `size` | `number` | 每页大小（等于 `pageSize`） |
| `current` | `number` | 固定为 `1`（游标分页内部实现） |
| `pages` | `number` | 总页数 |

**`ChatHistory` 记录字段：**

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `number` | 历史记录主键 |
| `chatId` | `number` | 会话 ID |
| `userId` | `number` | 用户 ID |
| `content` | `string` | **LangChain 消息 JSON 字符串**，见下方结构说明 |

**`content` 字段结构（LangChain message_to_dict 格式）：**

人类消息示例：

```json
{
  "type": "human",
  "data": {
    "content": "你好",
    "additional_kwargs": {},
    "response_metadata": {},
    "type": "human",
    "name": null,
    "id": null
  }
}
```

AI 消息示例：

```json
{
  "type": "ai",
  "data": {
    "content": "你好，有什么可以帮你？",
    "additional_kwargs": {},
    "response_metadata": {},
    "type": "ai",
    "name": null,
    "id": null,
    "tool_calls": [],
    "invalid_tool_calls": [],
    "usage_metadata": null
  }
}
```

**前端解析建议：** 读取 `content` JSON 后，取 `data.content` 作为展示文本；`type` 区分用户/AI 消息。

**响应示例：**

```json
{
  "code": 0,
  "data": {
    "records": [
      {
        "id": 100,
        "chatId": 101,
        "userId": 1,
        "content": "{\"type\":\"ai\",\"data\":{\"content\":\"你好！\",\"type\":\"ai\",...}}"
      },
      {
        "id": 99,
        "chatId": 101,
        "userId": 1,
        "content": "{\"type\":\"human\",\"data\":{\"content\":\"你好\",\"type\":\"human\",...}}"
      }
    ],
    "total": 2,
    "size": 10,
    "current": 1,
    "pages": 1
  },
  "message": "ok"
}
```

**权限校验：**

- 服务端校验 `chatId` 对应会话属于当前登录用户；否则返回 `code: 50000`，`message: "会话不存在"`。
- 查询条件同时限定 `chatId` 与 `userId`，防止越权读取。

**前端对接注意事项：**

- 用于**聊天页**加载历史消息、上拉加载更多。
- 列表为 **id 降序**（最新在前）；聊天 UI 通常需反转为时间正序展示。
- 无 `createTime` 字段，排序依赖 `id`。

---

### RouteType 枚举

| RouteType | JSON 值 | 含义 | 前端展示文案 |
| --- | --- | --- | --- |
| `NORMAL_CHAT` | `normal_chat` | 普通对话 | 普通对话 |
| `REPORT` | `report` | 生成报告 | 报告生成 |
| `RAG` | `rag` | 知识库增强问答 | 知识库问答 |
| `MCP` | `mcp` | 地图服务 | 地图服务 |
| `TOOL` | `tool` | 工具调用 | 工具调用 |
| `WORKFLOW` | `workflow` | 复杂任务工作流 | 复杂任务 |

> 请求体中 `routeType` 传 JSON 值字符串（如 `"normal_chat"`），非 Java 枚举名。

---

## 7. SSE 流式接口说明

### 基本信息

| 项 | 值 |
| --- | --- |
| 请求方法 | `POST` |
| 请求路径 | `/api/agent/chat/stream` |
| Content-Type（请求） | `application/json` |
| Content-Type（响应） | `text/event-stream` |
| 是否需要登录 | 是（Cookie Session） |
| 是否使用 BaseResponse | **否**，直接流式输出 |

### 请求体

见 [6.2 流式聊天](#62-流式聊天sse)。

### 响应数据格式

Spring 将 `Flux<String>` 编码为标准 SSE。每个事件形如：

```
data: <payload>

```

- **内容块**：`data` 为 AI 回复的文本片段（Java 已从 Python 的 `data: ` 前缀中提取纯文本后重新封装）。
- **结束标识**：收到 `data: done`（大小写不敏感）表示流结束。

```
data: 你好
data: ，我是
data: AI助手
data: done
```

### 流结束判断

1. 解析 SSE 事件，当 `data` 字段值为 `done` 或 `[DONE]`（Java 内部统一转发为 `done`）时，判定流结束。
2. HTTP 连接关闭也表示结束。

### 错误处理

| 场景 | 表现 |
| --- | --- |
| 未登录 | HTTP 401 + JSON `BaseResponse`（`code: 40103`），非 SSE |
| 序列化失败 | 流式错误（`Flux.error`） |
| Python 返回非 200 | 流式错误 |
| 其他异常 | 流式中断 |

前端需同时处理：**非 200 的 JSON 错误响应** 和 **流中断**。

### 前端对接建议

> **必须使用 `fetch` + `ReadableStream`（或类似能力）解析 POST SSE，不要使用原生 `EventSource`**（`EventSource` 仅支持 GET）。

```typescript
// 伪代码示例
const response = await fetch('/api/agent/chat/stream', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream' },
  credentials: 'include',
  body: JSON.stringify({ message, chatId, routeType }),
})

if (!response.ok) {
  // 401 等，按 JSON 错误处理
  const err = await response.json()
  throw err
}

const reader = response.body!.getReader()
const decoder = new TextDecoder()
let buffer = ''

while (true) {
  const { done, value } = await reader.read()
  if (done) break
  buffer += decoder.decode(value, { stream: true })
  // 按 \n\n 拆分 SSE 事件，解析 data: 行
  // data === 'done' 时结束
}
```

### 标准聊天流程（参考集成测试）

```
1. POST /api/agent/chat/create        → 获取 chatId
2. POST /api/agent/chat/route         → 获取 routeType（可选）
3. POST /api/agent/chat/stream        → 流式对话
4. GET  /api/chatHistory/chat/{chatId} → 刷新历史（可选）
```

---

## 8. 文件管理模块接口

> 文件存储路径：`{项目根目录}/tmp/{userId}/{chatId}/`（服务端内部，**不暴露给前端**）。

### 8.1 查询会话文件列表

**接口说明：** 查看某个对话下 AI 生成的文件列表。

**请求方法：** `POST`

**请求路径：** `/api/chat/file/list`

**是否需要登录：** 是

**请求头：**

```
Content-Type: application/json
Cookie: user_login=<userId>
```

**请求参数（`FileRequest`）：**

| 字段名 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| `chatId` | `number` | 是 | 对话 ID |
| `fileName` | `string` | 否 | 列表接口不需要 |

**请求示例：**

```json
{
  "chatId": 101
}
```

**响应参数：**

> **注意：此接口直接返回 `ChatFileVO[]`，不使用 `BaseResponse` 包装。**

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `fileName` | `string` | 文件名 |
| `size` | `number` | 文件大小（字节） |
| `sizeText` | `string` | 格式化大小，如 `"1.5 KB"` |
| `lastModified` | `string` | 最后修改时间，`yyyy-MM-dd HH:mm:ss` |
| `downloadUrl` | `string` | 固定为 `"/api/chat/file/download"`，需配合 POST 下载 |

**响应示例：**

```json
[
  {
    "fileName": "report.pdf",
    "size": 102400,
    "sizeText": "100.0 KB",
    "lastModified": "2026-06-08 14:30:00",
    "downloadUrl": "/api/chat/file/download"
  }
]
```

**前端对接注意事项：**

- 用于**聊天页/文件页**展示会话产出文件。
- `userId` 从登录态获取，**不需要**前端传递。
- 目录不存在时返回空数组 `[]`。
- `downloadUrl` 仅为路径提示，实际下载需 POST 并传 `chatId` + `fileName`。

---

### 8.2 下载文件

**接口说明：** 下载指定会话下的文件，返回二进制流。

**请求方法：** `POST`

**请求路径：** `/api/chat/file/download`

**是否需要登录：** 是

**请求头：**

```
Content-Type: application/json
Cookie: user_login=<userId>
```

**请求参数（`FileRequest`）：**

| 字段名 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| `chatId` | `number` | 是 | 对话 ID |
| `fileName` | `string` | 是 | 要下载的文件名 |

**请求示例：**

```json
{
  "chatId": 101,
  "fileName": "report.pdf"
}
```

**响应：**

| 项 | 值 |
| --- | --- |
| 成功 Content-Type | `application/octet-stream` |
| Content-Disposition | `attachment; filename="<fileName>"` |
| Body | 文件二进制流 |

**失败时：** 返回 JSON `BaseResponse`（如文件不存在 `code: 50000`）。

**前端对接注意事项：**

- 使用 `fetch` + `blob()` 处理，**不能用普通 JSON Axios**。
- 必须 `credentials: 'include'`。
- 示例：

```typescript
const res = await fetch('/api/chat/file/download', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  credentials: 'include',
  body: JSON.stringify({ chatId, fileName }),
})
const blob = await res.blob()
// 触发浏览器下载
```

---

### 8.3 删除文件

**接口说明：** 删除指定会话下的文件。

**请求方法：** `DELETE`

**请求路径：** `/api/chat/file/delete`

**是否需要登录：** 是

**请求头：**

```
Content-Type: application/json
Cookie: user_login=<userId>
```

**请求参数（`FileRequest`）：**

| 字段名 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| `chatId` | `number` | 是 | 对话 ID |
| `fileName` | `string` | 是 | 要删除的文件名 |

**请求示例：**

```json
{
  "chatId": 101,
  "fileName": "report.pdf"
}
```

**响应参数：**

> **注意：此接口直接返回 `boolean`，不使用 `BaseResponse` 包装。**

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| （根值） | `boolean` | 固定为 `true` |

**响应示例：**

```json
true
```

**失败时：** 由 `GlobalExceptionHandler` 返回 JSON `BaseResponse`（如文件不存在 `code: 50000`）。

**前端对接注意事项：**

- 用于**文件页/聊天侧栏**删除文件。
- `userId` 从登录态获取，不需要前端传递。
- 响应用裸 `boolean`，不要按 `res.data.code` 解析。

---

## 9. 前端需要的 TypeScript 类型草案

```typescript
/** 统一响应（大部分 JSON 接口） */
export interface BaseResponse<T> {
  code: number
  data: T
  message: string
}

/** 错误码常量 */
export const ErrorCode = {
  SUCCESS: 0,
  PARAMS_ERROR: 40000,
  USER_ALREADY_EXIST: 40001,
  PASSWORD_NOT_MATCH: 40002,
  PASSWORD_ERROR: 40100,
  NOT_LOGIN_ERROR: 40103,
  NO_AUTH_ERROR: 40104,
  USER_NOT_FOUND: 40400,
  OPERATION_ERROR: 50000,
} as const

/** 用户实体 */
export interface User {
  userId?: number
  userName?: string
  password?: string
  role?: string
}

/** 登录请求 */
export interface LoginRequest {
  userAccount: string
  userPassword: string
}

/** 注册请求 */
export interface RegisterRequest {
  userAccount: string
  userPassword: string
  checkPassword: string
}

/** 登录响应 */
export interface LoginUserVO {
  userId: number
  userName: string
  role: string
}

/** 会话实体（后端落库结构） */
export interface Chat {
  chatId?: number
  userId?: number
  title?: string
  createTime?: string
  modifyTime?: string
}

/** 会话 VO（列表/详情/更新响应） */
export interface ChatVO {
  chatId: number
  title: string
  createTime: string
  modifyTime: string
}

/** 更新会话标题请求 */
export interface ChatUpdateRequest {
  title: string
}

/** LangChain 消息 content 解析后的结构 */
export interface LangChainMessage {
  type: 'human' | 'ai'
  data: {
    content: string
    type: 'human' | 'ai'
    additional_kwargs?: Record<string, unknown>
    response_metadata?: Record<string, unknown>
    name?: string | null
    id?: string | null
    tool_calls?: unknown[]
    invalid_tool_calls?: unknown[]
    usage_metadata?: unknown | null
  }
}

/** 聊天历史记录 */
export interface ChatHistory {
  id: number
  chatId: number
  userId: number
  /** LangChain message_to_dict JSON 字符串，需 JSON.parse */
  content: string
}

/** MyBatis-Plus 分页（游标分页） */
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/** 路由决策请求 */
export interface RouteRequest {
  initPrompt: string
}

/** 路由决策响应 */
export interface RouteResponse {
  routeType: string
  enumName: string
  reason: string
}

/** 路由类型 */
export type RouteType =
  | 'normal_chat'
  | 'report'
  | 'rag'
  | 'mcp'
  | 'tool'
  | 'workflow'

/** 流式聊天请求 */
export interface ChatRequest {
  message: string
  chatId: string
  routeType?: RouteType
  /** 无需前端传递，后端从 Session 覆盖 */
  userId?: string
}

/** 文件请求 */
export interface FileRequest {
  chatId: number
  fileName?: string
}

/** 会话文件 */
export interface ChatFileVO {
  fileName: string
  size: number
  sizeText: string
  lastModified: string
  downloadUrl: string
}
```

---

## 10. 前端 API 模块划分建议

```text
src/api/
  request.ts      # Axios/fetch 封装：baseURL、withCredentials、统一错误处理
  user.ts         # 用户注册、登录、当前用户、退出
  chat.ts         # 会话 CRUD、路由决策、流式聊天、聊天历史
  file.ts         # 文件列表、下载、删除
```

### 各文件职责

| 文件 | 包含接口 | 说明 |
| --- | --- | --- |
| `request.ts` | — | `baseURL = '/api'`；`withCredentials: true`；拦截 `code !== 0` 和 HTTP 401 |
| `user.ts` | `POST /user/register`、`POST /user/login`、`GET /user/current`、`POST /user/logout` | 登录页、注册页、应用初始化、退出 |
| `chat.ts` | `POST /agent/chat/create`、`GET /agent/chat/list`、`GET /agent/chat/{chatId}`、`PUT /agent/chat/{chatId}`、`DELETE /agent/chat/{chatId}`、`POST /agent/chat/route`、`POST /agent/chat/stream`、`GET /chatHistory/chat/{chatId}` | 核心聊天逻辑；`stream` 单独用 fetch 实现 |
| `file.ts` | `POST /chat/file/list`、`POST /chat/file/download`、`DELETE /chat/file/delete` | 列表/删除注意非 BaseResponse 响应；下载用 blob |

> 管理类用户 CRUD（`/user/add`、`/delete`、`/update`、`/get`、`/list`）可按需放入 `user.ts` 或 `admin.ts`。

---

## 11. 前端页面与接口对应关系

| 前端页面 | 使用接口 | 说明 |
| --- | --- | --- |
| 应用初始化 | `GET /api/user/current` | 校验登录态、恢复 `userId` / `userName` / `role` |
| 注册页 | `POST /api/user/register` | 用户注册，成功后跳转登录 |
| 登录页 | `POST /api/user/login` | 用户登录，保存用户信息与 Cookie |
| 布局顶栏 | `POST /api/user/logout` | 退出登录 |
| 聊天页侧边栏 | `GET /api/agent/chat/list` | 加载会话列表（含标题、时间） |
| 聊天页侧边栏 | `PUT /api/agent/chat/{chatId}` | 重命名会话标题 |
| 聊天页侧边栏 | `DELETE /api/agent/chat/{chatId}` | 删除会话（级联清理历史与文件） |
| 聊天页 | `GET /api/agent/chat/{chatId}` | 可选，进入会话时刷新元信息 |
| 聊天页 | `POST /api/agent/chat/create` | 新建对话，获取 chatId |
| 聊天页 | `POST /api/agent/chat/route` | 可选，发送前路由决策 |
| 聊天页 | `POST /api/agent/chat/stream` | 发送消息、流式接收 AI 回复 |
| 聊天页 | `GET /api/chatHistory/chat/{chatId}` | 加载/翻页历史消息 |
| 文件页 / 聊天侧栏 | `POST /api/chat/file/list` | 查看会话产出文件 |
| 文件页 / 聊天侧栏 | `POST /api/chat/file/download` | 下载文件（blob） |
| 文件页 / 聊天侧栏 | `DELETE /api/chat/file/delete` | 删除文件 |
| 管理后台（可选） | `GET/POST/PUT/DELETE /api/user/**` | 用户 CRUD，需 `role=admin` |

---

## 12. 前端生成注意事项

1. **前端只调用 Java 后端**（`/api` 前缀），禁止直接调用 Python `http://localhost:8000`。
2. **SSE 接口**（`POST /api/agent/chat/stream`）不能用普通 Axios JSON 请求，须用 `fetch` + `ReadableStream` 解析 `text/event-stream`。
3. **文件下载**须用 `fetch` + `blob()`，响应为 `application/octet-stream`，非 JSON。
4. **文件列表**（`POST /api/chat/file/list`）返回裸数组；**文件删除**（`DELETE /api/chat/file/delete`）返回裸 `boolean`；均**无** `BaseResponse` 包装。
5. **认证依赖 Cookie + Session**：所有需登录请求设置 `credentials: 'include'` / `withCredentials: true`；不使用 Bearer Token。
6. **`userId` 由后端从登录态注入**：创建会话、流式聊天、文件、会话 CRUD 接口均不需要前端传 `userId`。
7. **`chatId` 由前端维护**：创建会话后保存到状态/路由参数；流式聊天、会话 CRUD 传字符串路径参数；历史查询传数字路径参数。
8. **会话列表直接返回 `ChatVO[]`**：侧边栏可直接渲染 `chatId`、`title`、`modifyTime`，无需额外详情请求。
9. **删除会话会级联清理**历史记录和 `tmp/` 文件，前端删除操作须二次确认。
10. **历史消息 `content` 为 LangChain JSON 字符串**：展示前需 `JSON.parse(content).data.content`。
11. **历史查询已做归属校验**：传入非本人 `chatId` 将返回「会话不存在」。
12. **历史分页为游标模式**：用 `lastId` 加载更早消息，非 `pageNum` 翻页。
13. **应用启动时**调用 `GET /user/current` 探测登录态；失败跳转登录页。
14. **所有请求路径以本文档为准**，Context Path 为 `/api`。
15. 文档中标记 **TODO** 的字段/逻辑，前端生成时保留 TODO 注释，勿自行假设。
16. 跨域已配置 `allowCredentials(true)` + `allowedOriginPatterns("*")`，开发环境可用 Vite proxy 或确保 CORS 与 Cookie 策略一致。

---

## 13. 已实现接口能力清单

| 模块 | 接口 | 状态 |
| --- | --- | --- |
| 用户 | 注册、登录、当前用户、退出 | ✅ |
| 会话 | 创建、列表、详情、更新标题、删除（级联） | ✅ |
| 聊天 | 路由决策、SSE 流式、历史查询（含归属校验） | ✅ |
| 文件 | 列表、下载、删除 | ✅ |

---

## 附录：受保护路径与拦截器配置

```text
拦截路径：/user/**、/agent/**、/chatHistory/**、/chat/file/**
排除路径：/user/login、/user/register、/user/logout
```

## 附录：跨域配置

- 允许所有来源（`allowedOriginPatterns: "*"`）
- 允许携带 Cookie（`allowCredentials: true`）
- 允许方法：`GET`、`POST`、`PUT`、`DELETE`、`OPTIONS`
- 允许/暴露所有请求头
