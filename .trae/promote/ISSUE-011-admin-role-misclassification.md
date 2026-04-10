# ISSUE-011 admin 团队管理员被识别为企业管理员

## 1. 错误基本信息

- 编号：ISSUE-011
- 日期：2026-04-10
- 账号：`admin`
- 场景：登录后进入团队管理相关页面 / 顶部身份展示
- 用户预期：`admin` 作为团队管理员进入团队管理员视角
- 实际现象：页面将 `admin` 识别为企业管理员
- 当前状态：排查中

## 2. 已知上下文

- 用户明确反馈：`admin` 是团队管理员账号，不应再次按企业管理员方向收口。
- 当前代码里，前端身份展示优先依据：
  - `userInfo.isSuperAdmin`
  - `userInfo.roles` 是否包含 `SUPER_ADMIN`
  - `userInfo.isTeamOwner`
- 当前项目规则已明确：后续处理 bug 时，必须先基于报错文档和真实报错信息制定排查方案，再改代码；每次报错都要记录。

## 3. 涉及文件

### 前端
- `frontend/src/components/MainLayout.vue`
- `frontend/src/views/Login.vue`
- `frontend/src/views/TeamManagement.vue`

### 后端
- `backend/src/main/java/com/aikey/service/AuthService.java`
- `backend/src/main/java/com/aikey/repository/UserRepository.java`
- `backend/src/main/java/com/aikey/repository/TeamRepository.java`

## 4. 涉及接口 / 数据源

- `POST /api/v1/auth/login`
- `GET /api/v1/auth/me`
- `GET /api/v1/teams`
- 数据库中的：
  - `users`
  - `roles`
  - `user_roles`
  - `teams.owner_id`

## 5. 初步怀疑点（按优先级）

### P1. 运行态数据本身把 `admin` 定义成了企业管理员
可能性：
- `admin` 在 `user_roles` 里实际仍关联了 `SUPER_ADMIN`
- 因此 `/auth/me` 返回 `isSuperAdmin=true` 或 `roles` 含 `SUPER_ADMIN`

### P2. `/auth/me` 判定与产品预期不一致
可能性：
- 后端当前用数据库角色直接判 `isSuperAdmin`
- 但产品上希望 `admin` 作为团队管理员使用
- 文档定义和运行态角色没有统一

### P3. 前端缓存残留旧身份
可能性：
- 登录后 `localStorage.userInfo` / `roles` 未被正确覆盖
- 页面继续读取了旧的企业管理员身份

### P4. 文档、数据库、代码三者漂移
可能性：
- 文档把 `admin` 当团队管理员
- 数据库里 `admin` 仍是企业管理员
- 代码按数据库真相渲染，导致用户感知冲突

## 6. 本次排查方案

### Step 0. 记录新增阻塞错误
新增运行态阻塞：后端无法启动，导致当前还不能继续做 `admin` 的接口真值核对。

- 错误时间：2026-04-10 08:52
- 错误命令：`mvn -f backend/pom.xml -DskipTests spring-boot:run`
- 原始报错：`Web server failed to start. Port 8080 was already in use.`
- 影响：无法继续执行 `/api/v1/auth/me` 与 `/api/v1/teams` 的本地运行态验证
- 性质：环境阻塞，不是当前 admin 身份问题的直接根因，但会阻断排查

### Step 1. 查运行态接口真相
核对 `admin` 登录后的：
- `/api/v1/auth/me` 原始返回
- `/api/v1/teams` 返回内容

重点确认：
- `isSuperAdmin`
- `isTeamOwner`
- `roles`
- 团队列表里 `ownerId / ownerName`

### Step 2. 查数据库真相
核对：
- `admin` 对应用户 ID
- `admin` 在 `user_roles` 实际关联的角色
- `teams.owner_id` 是否指向 `admin`

### Step 3. 查前端缓存链
核对：
- 登录后 `localStorage.userInfo`
- `localStorage.roles`
- 团队页刷新当前用户信息后是否覆盖成功

### Step 4. 判断根因归属
最终要判断是：
- 数据错
- 后端接口错
- 前端缓存/展示错
- 还是文档定义错

## 7. 后续修复原则

- 不再根据用户名猜权限
- 只根据运行态真实接口和数据库真相判断
- 先确定单一真相源，再改代码
- 修复完成后补充：触发条件、根因、最终修复方案、验证结果

## 8. 本次新增协作规则

- 每次报错都必须记录到 `.trae/promote/` 下
- 处理 bug 时，先写报错文档，再根据真实报错信息做排查方案
