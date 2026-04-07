# MVP 测试常见问题排查手册

## 文档信息
- **文档版本**: V1.0
- **创建日期**: 2026-04-06
- **适用对象**: MVP 功能测试人员、QA工程师
- **关联文档**: MVP-测试通过标准-CHECKLIST.md

---

## 目录
1. [环境启动问题](#1-环境启动问题)
2. [数据库连接问题](#2-数据库连接问题)
3. [API 调用问题](#3-api-调用问题)
4. [前端显示问题](#4-前端显示问题)
5. [业务逻辑问题](#5-业务逻辑问题)
6. [性能问题](#6-性能问题)

---

## 1. 环境启动问题

### 问题 1.1: 后端服务启动失败

**现象**
```
Error: Application run failed
Caused by: org.springframework.beans.factory.BeanCreationException
```

**可能原因**
- MySQL 容器未启动
- 端口 8080 被占用
- 配置文件错误

**排查步骤**
1. 检查 MySQL 容器状态
   ```bash
   docker ps | findstr aikey-mysql
   ```
2. 检查端口占用
   ```bash
   netstat -ano | findstr 8080
   ```
3. 查看详细错误日志
   ```bash
   docker logs aikey-mysql
   ```

**解决方案**
- 启动 MySQL 容器：`docker start aikey-mysql`
- 释放 8080 端口或修改 `application.yml` 中的端口配置
- 检查 `application.yml` 中的数据库连接配置

---

### 问题 1.2: 前端编译失败

**现象**
```
[vite]: Rollup failed to resolve import "element-plus"
```

**可能原因**
- node_modules 缺失
- 依赖版本不兼容

**排查步骤**
1. 检查 node_modules 是否存在
2. 检查 package.json 依赖版本

**解决方案**
```bash
cd frontend
npm install
npm run dev
```

---

### 问题 1.3: 前端无法连接后端

**现象**
浏览器控制台显示：
```
Failed to load resource: net::ERR_CONNECTION_REFUSED
```

**可能原因**
- 后端未启动
- 跨域配置问题
- 代理配置错误

**排查步骤**
1. 确认后端服务是否运行：`curl http://localhost:8080/actuator/health`
2. 检查 `vite.config.js` 中的代理配置
3. 检查后端 CORS 配置

**解决方案**
- 启动后端服务
- 确认 `vite.config.js` 中代理指向正确端口
- 检查 `WebConfig.java` 中的 CORS 配置

---

## 2. 数据库连接问题

### 问题 2.1: 数据库连接超时

**现象**
```
com.mysql.cj.jdbc.exceptions.CommunicationsException: Communications link failure
```

**可能原因**
- MySQL 容器未运行
- 网络配置问题
- 防火墙阻止

**排查步骤**
1. 检查容器状态：`docker ps`
2. 测试连接：`docker exec -it aikey-mysql mysql -uroot -p -e "SELECT 1"`

**解决方案**
- 重启容器：`docker restart aikey-mysql`
- 检查 Docker 网络配置
- 确认防火墙允许 3306 端口

---

### 问题 2.2: 表不存在错误

**现象**
```
Table 'ai_key_management.channels' doesn't exist
```

**可能原因**
- 数据库未初始化
- JPA 自动创建表被禁用

**排查步骤**
1. 检查数据库表是否存在
   ```sql
   SHOW TABLES;
   ```
2. 检查 `application.yml` 中 JPA 配置

**解决方案**
- 执行初始化脚本：`docker exec -i aikey-mysql mysql -uroot -p ai_key_management < test-data-mvp.sql`
- 设置 `spring.jpa.hibernate.ddl-auto=update`（开发环境）

---

### 问题 2.3: 测试数据缺失

**现象**
页面显示空白列表，无测试数据

**排查步骤**
1. 检查数据是否存在
   ```sql
   SELECT COUNT(*) FROM channels;
   SELECT COUNT(*) FROM real_keys;
   SELECT COUNT(*) FROM virtual_keys;
   ```

**解决方案**
- 重新执行测试数据脚本
- 检查脚本执行日志

---

## 3. API 调用问题

### 问题 3.1: 500 内部服务器错误

**现象**
```json
{
  "timestamp": "2026-04-06T10:30:00",
  "status": 500,
  "error": "Internal Server Error"
}
```

**可能原因**
- 空指针异常
- 数据库连接问题
- 业务逻辑错误

**排查步骤**
1. 查看后端日志：`docker logs <backend-container>`
2. 检查请求参数是否正确
3. 检查数据库连接状态

**解决方案**
- 根据日志定位具体错误
- 修复代码中的空指针问题
- 重启后端服务

**历史案例**
- **ISSUE**: API 路径不匹配（`/realkeys` vs `/real-keys`）
- **修复**: 统一使用 kebab-case 命名（`/real-keys`）

---

### 问题 3.2: 404 Not Found

**现象**
```json
{
  "timestamp": "2026-04-06T10:30:00",
  "status": 404,
  "error": "Not Found",
  "path": "/api/v1/realkeys"
}
```

**可能原因**
- API 路径错误
- Controller 映射错误

**排查步骤**
1. 检查 Controller 中的 `@RequestMapping`
2. 检查前端请求的 URL
3. 确认应用已重启

**解决方案**
- 对齐前后端 API 路径
- 重启后端服务使配置生效

---

### 问题 3.3: 401 Unauthorized

**现象**
```json
{
  "message": "未登录或token已过期"
}
```

**可能原因**
- Token 过期
- 未携带 Authorization 头
- Token 格式错误

**排查步骤**
1. 检查 LocalStorage 中是否有 token
2. 检查请求头中是否包含 `Authorization: Bearer <token>`
3. 检查 Token 是否过期

**解决方案**
- 重新登录获取新 token
- 检查前端请求拦截器配置
- 检查后端 JWT 验证逻辑

---

### 问题 3.4: 403 Forbidden

**现象**
```json
{
  "message": "权限不足"
}
```

**可能原因**
- 用户角色权限不足
- 接口需要特定角色

**排查步骤**
1. 检查用户角色
2. 检查接口的权限注解

**解决方案**
- 使用管理员账号测试
- 检查 `@PreAuthorize` 注解配置

---

## 4. 前端显示问题

### 问题 4.1: 页面空白/白屏

**现象**
页面加载后显示空白，控制台无错误

**可能原因**
- 路由配置错误
- 组件渲染异常
- 数据加载失败但未处理

**排查步骤**
1. 检查浏览器控制台是否有错误
2. 检查 Vue DevTools 组件树
3. 检查网络请求是否成功

**解决方案**
- 检查路由配置是否正确
- 添加错误边界处理
- 检查数据加载逻辑

---

### 问题 4.2: 侧边栏不显示或样式异常

**现象**
- 侧边栏缺失
- 侧边栏样式错乱
- 菜单项不显示

**可能原因**
- MainLayout 未正确应用
- CSS 样式冲突
- 路由未配置 meta 信息

**排查步骤**
1. 检查 router/index.js 中路由的 meta 配置
2. 检查 MainLayout.vue 是否正确引入
3. 检查 Element Plus 样式是否正确加载

**解决方案**
- 确保所有路由都有正确的 meta.title
- 检查 Element Plus 主题配置
- 清除浏览器缓存

---

### 问题 4.3: 数据表格不显示数据

**现象**
表格组件显示 "暂无数据"

**可能原因**
- API 请求失败
- 数据格式不匹配
- 分页参数错误

**排查步骤**
1. 检查网络请求是否成功
2. 检查返回数据格式
3. 检查表格的 prop 绑定

**解决方案**
- 修复 API 调用
- 调整数据映射逻辑
- 检查分页组件配置

---

### 问题 4.4: 面包屑导航不显示

**现象**
顶部面包屑区域空白或显示错误

**可能原因**
- 组件未正确引入
- 路由 meta 信息缺失

**排查步骤**
1. 检查 MainLayout.vue 中面包屑组件
2. 检查路由配置中的 meta.title

**解决方案**
- 使用 Element Plus 标准组件 `<el-breadcrumb>`
- 确保所有路由都有 meta.title

**历史案例**
- **ISSUE**: 使用了未注册的 `<breadcrumb />` 组件
- **修复**: 改为 `<el-breadcrumb>` 并添加正确导入

---

## 5. 业务逻辑问题

### 问题 5.1: 渠道优先级不生效

**现象**
虚拟 Key 调用的渠道与设置的优先级不符

**排查步骤**
1. 检查渠道优先级配置
2. 检查虚拟 Key 的渠道绑定
3. 查看调度日志

**解决方案**
- 确认优先级数值越小优先级越高
- 检查调度算法实现
- 查看 RealKeyService 中的选择逻辑

---

### 问题 5.2: 轮询策略不生效

**现象**
同一个渠道的 Key 总是使用同一个，没有轮询

**排查步骤**
1. 检查渠道轮询配置
2. 检查 Key 状态是否正常
3. 查看轮询计数器

**解决方案**
- 确认渠道 poll_strategy 设置为 'round_robin'
- 检查 Key 是否处于 active 状态
- 重启服务清除轮询计数器缓存

---

### 问题 5.3: 故障转移不生效

**现象**
Key 失效后没有自动切换到备用 Key

**排查步骤**
1. 检查 Key 状态是否正确更新
2. 检查故障检测机制
3. 查看调度日志

**解决方案**
- 确认 Key 状态正确标记为 invalid 或 depleted
- 检查调度算法中的过滤逻辑
- 查看是否有异常处理阻塞

---

### 问题 5.4: 用量配额计算错误

**现象**
虚拟 Key 用量显示与实际不符

**排查步骤**
1. 检查 quota_type 配置
2. 检查用量统计逻辑
3. 对比数据库记录

**解决方案**
- 确认 quota_type 与统计维度一致（token/count/amount）
- 检查统计 SQL 是否正确
- 核对数据库中的 usage 字段

---

### 问题 5.5: 已禁用渠道仍被调用

**现象**
status='disabled' 的渠道仍参与调度

**排查步骤**
1. 检查渠道状态是否正确
2. 检查调度过滤逻辑
3. 查看调度日志

**解决方案**
- 确认调度算法中过滤了 disabled 渠道
- 检查 RealKeyService.selectChannel() 方法
- 验证数据库中的 status 字段值

---

## 6. 性能问题

### 问题 6.1: 页面加载缓慢

**现象**
首次加载时间超过 3 秒

**排查步骤**
1. 检查网络请求耗时
2. 检查资源文件大小
3. 检查数据库查询性能

**解决方案**
- 启用 Gzip 压缩
- 使用 CDN 加速静态资源
- 优化数据库查询（添加索引）

---

### 问题 6.2: API 响应慢

**现象**
API 响应时间超过 500ms

**排查步骤**
1. 检查数据库查询时间
2. 检查是否有 N+1 查询问题
3. 检查网络延迟

**解决方案**
- 添加数据库索引
- 使用 JOIN 替代多次查询
- 启用查询缓存

---

### 问题 6.3: 内存占用过高

**现象**
后端服务内存持续增长

**排查步骤**
1. 检查内存泄漏
2. 检查缓存策略
3. 检查大对象持有

**解决方案**
- 优化缓存过期策略
- 检查是否有未关闭的资源
- 调整 JVM 内存参数

---

## 附录 A: 快速诊断命令

### 检查服务状态
```bash
# 检查容器状态
docker ps

# 检查后端日志
docker logs aikey-backend 2>&1 | tail -50

# 检查 MySQL 日志
docker logs aikey-mysql 2>&1 | tail -50
```

### 检查数据库
```bash
# 进入 MySQL 容器
docker exec -it aikey-mysql mysql -uroot -p

# 常用查询
USE ai_key_management;
SELECT COUNT(*) FROM channels;
SELECT COUNT(*) FROM real_keys;
SELECT COUNT(*) FROM virtual_keys;
SHOW PROCESSLIST;
```

### 检查 API
```bash
# 测试健康检查
curl http://localhost:8080/actuator/health

# 测试登录
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 测试渠道列表
curl http://localhost:8080/api/v1/channels \
  -H "Authorization: Bearer <token>"
```

---

## 附录 B: 联系与支持

### 问题升级流程
1. 按照本手册排查无法解决
2. 收集错误日志和复现步骤
3. 创建 Issue 到项目管理系统
4. 联系开发团队

### 需要收集的信息
- 错误截图
- 浏览器控制台日志
- 后端错误日志
- 复现步骤
- 环境信息（浏览器版本、操作系统）

---

---

## 附录 C: 2026-04-06 Bug修复记录

### 问题1: 模型广场和令牌管理页面无法使用（显示"页面开发中"）

**根因**: 路由配置中 `/models` 和 `/tokens` 都指向 `Placeholder.vue`，没有绑定真实组件。

**修复内容**:
- `frontend/src/router/index.js`: `/tokens` 改为加载 `TokenManagement.vue`，`/models` 改为加载 `ModelMarket.vue`（新建），`/channels` 新增路由加载 `ChannelManagement.vue`
- 添加路由守卫：未登录自动跳转 `/login`，已登录访问 `/login` 跳转首页
- 添加 `/login` 路由
- 用嵌套路由将无独立 layout 的页面包裹在 `MainLayout` 下

### 问题2: 退出登录无法跳转（路由缺失 /login）

**根因**: 路由表中没有 `/login` 路由，`router.push('/login')` 跳转失败。

**修复内容**: 在路由表中添加 Login 路由，并配置 `requiresAuth: false`。

### 问题3: 模型广场后端 API 缺失

**根因**: 项目有 `Model` 实体和 `ModelRepository`，但没有 `ModelController` 和 `ModelService`。

**新增文件**:
- `backend/.../controller/ModelController.java`: GET /api/v1/models（分页列表）、POST（创建）、PUT /{id}/status（切换状态）、DELETE /{id}
- `backend/.../service/ModelService.java`: 模型 CRUD 业务逻辑
- `backend/.../dto/model/ModelVO.java`: 模型视图对象
- `backend/.../dto/model/ModelCreateRequest.java`: 创建请求 DTO
- `frontend/src/api/model.js`: 前端模型 API 封装
- `frontend/src/views/ModelMarket.vue`: 模型广场完整视图

### 问题4: ModelRepository 缺少 JpaSpecificationExecutor

**根因**: `ModelRepository` 只继承 `JpaRepository`，无法使用 `Specification` 分页查询。

**修复**: 添加 `JpaSpecificationExecutor<Model>` 继承。

### 重启说明

后端新增了 `ModelController`，**需要重启 Spring Boot 后端**才能生效：
```bash
# 在 backend 目录执行
mvn spring-boot:run
```

*最后更新: 2026-04-06*

---

## 附录 D: 2026-04-06 退出登录后无法登录（404）修复记录

### 问题现象
退出登录后点击登录，浏览器控制台报 404 错误，请求发到 `http://localhost:5173/api/v1/auth/login` 但返回 404。

### 根因分析

**根因 1：vite.config.js 缺少代理配置（最严重）**

`request.js` 的 `baseURL` 是 `/api/v1`，前端向 `localhost:5173/api/v1/...` 发请求。但 `vite.config.js` 的 `server` 块没有 `proxy` 配置，Vite dev server 不知道要把 `/api` 转发到后端 8080，直接返回 404。

**根因 2：嵌套路由结构错误（次要）**

路由中 `children` 的 path 以 `/` 开头（绝对路径），Vue Router 会绕过父路由 `MainLayout` 直接匹配，导致 `MainLayout` 的 `<router-view>` 从不渲染 children，侧边栏和顶栏不显示。

**根因 3：Overview.vue 包含完整 layout（次要）**

`Overview.vue` 自带侧边栏和顶栏，放入 `MainLayout` 后会双重侧边栏，且顶栏的退出按钮没有绑定任何函数。

### 修复内容

**修复 1：`frontend/vite.config.js`**
```js
// 新增 proxy 配置
server: {
  port: 5173,
  open: true,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

**修复 2：`frontend/src/router/index.js`**
- 将所有需要 layout 的页面（包括 Overview）统一放在 `MainLayout` 的 children 下
- children 的 path 改为相对路径（去掉开头的 `/`），确保父路由 `MainLayout` 被渲染
- `/` 路径的子路由用 `path: ''` 作为默认子路由

**修复 3：`frontend/src/views/Overview.vue`**
- 去掉 Overview 自带的侧边栏、顶栏 HTML 和对应 CSS
- 只保留内容区（欢迎卡片、统计卡片、快捷操作），交由 MainLayout 统一管理 layout
- 快捷操作中的「接入新渠道」链接修正为 `/channels`（渠道管理页）

### 验证结果（2026-04-06）
- `http://localhost:5173/api/v1/auth/login` → 200，代理转发正常
- `http://localhost:5173/api/v1/models` → 200，ModelController 加载正常
- 所有 API（channels/real-keys/virtual-keys/models）通过前端代理均返回 200

*最后更新: 2026-04-06*
