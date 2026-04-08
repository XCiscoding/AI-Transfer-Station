# 项目当前状态总结

> 本文件由 AI 自动生成，每次会话结束时会更新。
> 更新机制：每次覆盖前保留 20% 核心上下文（项目定位、架构、当前任务），覆盖 80% 详细内容。

---

## 项目基本信息

- **项目名称**：AI调度中心 — 企业API Key管理系统
- **技术栈**：Spring Boot (Java) + Vue3 (前端) + MySQL + Redis
- **前后端分离**：后端 `:8080`，前端 `:5173`
- **启动命令**：后端 `cd backend && mvn spring-boot:run`，前端 `cd frontend && npm run dev`

---

## 当前问题（核心任务）

**症状**：点击「令牌管理」「团队管理」「数据看板」后退出登录（跳转到 `/login`）

**根因已确认**：
1. `frontend/src/api/auth.js` 第13行：`/auth/userinfo` 接口路径错误，后端只有 `/auth/me`
2. `frontend/src/views/Analytics.vue` 第323行：调用 `/api/v1/users`，但后端无此接口 → 401/403 → 触发退出
3. `frontend/src/utils/request.js` 第48-56行：403 处理过激，直接清 token + 跳转登录

---

## 修复方案（8个改动，待执行）

| 优先级 | 文件 | 改动 |
|--------|------|------|
| P0 | `frontend/src/api/auth.js` | `/auth/userinfo` → `/auth/me` |
| P0 | `frontend/src/views/Analytics.vue` | 移除 `/api/v1/users` 调用，改为不发请求 |
| P0 | `frontend/src/utils/request.js` | 403 只提示「权限不足」，不清 token 不跳转 |
| P1 | `frontend/src/views/Login.vue` | 登录成功时把 `roles` 写入 localStorage |
| P1 | `frontend/src/router/index.js` | 路由守卫读取 roles 备用（暂不拦截） |
| P2 | `backend/src/main/java/com/aikey/service/TeamService.java` | `toVO()` owner 为 null 时用 fallback |
| P2 | `backend/src/main/java/com/aikey/service/VirtualKeyService.java` | `convertToVO()` user 为 null 时用 fallback |
| P3 | 新建 `backend/src/main/java/com/aikey/controller/UserController.java` | 提供 `GET /api/v1/users` 接口 |

---

## 测试验证清单

- [ ] 登录后 localStorage 有 `token` 和 `roles`
- [ ] 点击令牌管理正常显示
- [ ] 点击团队管理正常显示
- [ ] 点击数据看板正常显示（不再退出）
- [ ] 无后端时点击模块提示「网络错误」而非跳转登录

---

## 用户信息（跨会话记住）

- 姓名：陈子明，大一学生，广东技术师范大学机械电子工程
- 考研目标：华南理工大学
- 性格：内向但渴望链接，有名校情结，敢想敢做
- 痛点：没有作品集，压力大，晚上失眠
- 审美：喜欢优雅高级感，衬线字体，深色系配金色点缀
- 讨厌：AI 味表达，讨好式回复，过度客套

---

## 架构设计（后期方向）

### 三角色体系（尚未实现）

```
管理员 (SUPER_ADMIN)
  └─ 用户管理、全局渠道、全局额度、全局看板、系统设置

团队管理员 (TEAM_ADMIN)
  └─ 令牌管理(团队内)、团队成员、项目管理、额度查看(团队)、团队看板

普通用户 (USER)
  └─ 我的令牌、额度查看、调用日志、个人中心
```

### 当前已实现
- JWT 无状态认证（Spring Security）
- 角色表 `roles`、用户角色关联 `user_roles`（数据库层已有）
- `User` 实体有 `roles` 关联（EAGER 加载）
- AuthService 登录时通过原生 SQL 查询 roles

### 待实现
- 前端路由按角色拦截（当前只有 requiresAuth: true）
- 后端接口按角色权限控制
- 不同角色的不同界面/菜单

---

*最后更新：2026-04-08*
