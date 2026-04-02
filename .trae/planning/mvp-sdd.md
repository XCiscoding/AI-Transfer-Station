# MVP推进SDD（Software Design Document）摘要

## 文档概述

**文档目的：** 为MVP三个核心模块提供一页式快速开发指导
**适用对象：** 后端/前端开发工程师、项目总经理
**覆盖范围：** M001（登录认证）、M002（渠道管理）、M003（APIKey管理）
**详细任务清单：** [mvp-tasks.md](file:///c:/Users/26404/.trae-cn/Project/AI调度中心——企业API Key管理系统/.trae/planning/mvp-tasks.md)

---

# M001: 系统基础模块 - 登录认证

## 模块概览

| 属性 | 说明 |
|------|------|
| **模块目标** | 实现基于JWT的用户认证系统，支持安全登录和会话管理 |
| **包含任务** | 5个（TASK-M001-002, 004, 005, 006, 007） |
| **预估工时** | 15小时 |
| **技术栈** | Spring Security + JWT + BCrypt + Vue3 + Element Plus |
| **前置依赖** | MySQL（schema.sql已执行）, Redis（已启动） |

## 核心数据流

```
用户输入账号密码
    ↓
前端表单验证 (Element Plus)
    ↓
POST /api/v1/auth/login
    ↓
后端验证 (AuthenticationManager)
    ↓
生成JWT Token (JwtTokenProvider)
    ↓
返回Token + 用户信息
    ↓
前端存储Token (localStorage/Pinia)
    ↓
后续请求携带Token (Authorization: Bearer xxx)
    ↓
后端过滤器验证 (JwtAuthenticationFilter)
    ↓
放行 / 拦截 (401)
```

## 数据库表结构

### users 表（核心字段）

```sql
-- 必须存在的初始管理员账号（手动插入或初始化脚本）
INSERT INTO users (username, password, email, status, created_at)
VALUES ('admin', '$2a$10$加密后的密码', 'admin@example.com', 1, NOW());
```

**关键字段说明：**
- `password`: BCrypt加密，长度255字符
- `status`: 0=禁用, 1=启用
- `is_locked`: 登录失败锁定机制
- `login_fail_count`: 连续失败次数

### 关联表
- **roles**: 角色表（需预置ADMIN角色）
- **user_roles**: 用户角色关联表
- **permissions**: 权限表（MVP阶段可简化）
- **role_permissions**: 角色权限关联表

## API接口设计

### 认证相关接口

| 方法 | 路径 | 描述 | 认证 |
|------|------|------|------|
| POST | `/api/v1/auth/login` | 用户登录 | 无需 |
| GET | `/api/v1/auth/me` | 获取当前用户信息 | 需Token |
| POST | `/api/v1/auth/logout` | 登出（可选） | 需Token |

### 请求/响应格式

**登录请求：**
```json
{
  "username": "admin",
  "password": "your_password"
}
```

**登录响应（成功）：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "userId": 1,
    "username": "admin",
    "email": "admin@example.com",
    "roles": ["ROLE_ADMIN"]
  }
}
```

**错误响应示例：**
```json
{
  "code": 401,
  "message": "用户名或密码错误",
  "data": null
}
```

## 关键组件架构

### 后端核心类

```
config/
└── SecurityConfig.java           # 安全配置（放行规则、过滤器链）

security/
├── JwtTokenProvider.java         # JWT工具类（生成/解析/验证）
└── JwtAuthenticationFilter.java   # 认证过滤器（每次请求验证Token）

service/
├── AuthService.java              # 认证业务逻辑
└── UserDetailsServiceImpl.java    # 用户详情加载（从DB）

controller/
└── AuthController.java           # 认证接口

dto/auth/
├── LoginRequest.java             # 登录请求DTO
├── LoginResponse.java            # 登录响应DTO
└── UserInfoResponse.java         # 用户信息DTO
```

### 前端核心文件

```
views/
└── login/
    └── LoginView.vue             # 登录页面

store/
└── modules/
    └── user.js                   # 用户状态管理（Pinia）

api/
└── auth.js                      # 认证API封装

utils/
├── request.js                   # Axios实例（拦截器）
└── auth.js                      # Token存储工具
```

## 安全要点

### 密码存储
- **算法**: BCrypt（强度10-12）
- **实现**: `BCryptPasswordEncoder`
- **禁止**: 明文存储、MD5、SHA-1等不安全算法

### JWT配置
- **算法**: HS256
- **Secret**: ≥256位（32字符以上）
- **过期时间**: 默认2小时（可配置化）
- **存储位置**: 前端localStorage

### 敏感数据处理
- 密码字段使用 `@JsonIgnore` 不序列化到JSON
- 日志中不得打印密码和完整Token
- 错误信息不泄露系统内部细节

## 开发检查清单

### 后端验收标准
- [ ] Spring Boot项目可启动，访问 `/actuator/health` 返回UP
- [ ] Swagger UI可访问 (`http://localhost:8080/swagger-ui.html`)
- [ ] 使用正确账号密码调用 `/auth/login` 返回200和Token
- [ ] 使用错误密码调用返回401
- [ ] 使用返回的Token调用 `/auth/me` 返回用户信息
- [ ] 不携带Token或无效Token调用受保护接口返回401
- [ ] 密码在数据库中以BCrypt哈希存储（非明文）

### 前端验收标准
- [ ] Vue3项目可启动，显示登录页面
- [ ] 输入正确的账号密码后跳转到主页面
- [ ] 输入错误的账号密码显示错误提示
- [ ] 刷新页面后保持登录状态（从localStorage读取Token）
- [ ] 页面美观，符合Element Plus设计规范
- [ ] 表单验证生效（非空校验等）

### 联调测试场景
1. **正常登录流程**: 前端输入 → 后端验证 → 返回Token → 存储Token → 跳转主页
2. **Token过期处理**: 等待2小时（或修改配置为短时间）→ 自动跳转登录页
3. **未登录拦截**: 直接访问主页URL → 重定向到登录页
4. **多标签同步**: 一个标签登出 → 其他标签自动检测并跳转

## 常见问题与解决方案

### Q1: CORS跨域问题
**方案A（推荐）**: Vite代理（已在vite.config.js配置）
**方案B**: 后端全局CORS配置（CorsConfig.java）

### Q2: JWT Secret不匹配
**症状**: Token生成成功但验证总是失败
**原因**: 前后端jwt.secret配置不一致
**解决**: 统一使用application.yml中的配置值

### Q3: 密码验证失败
**症状**: 明明密码正确但登录失败
**原因**: BCrypt每次生成的哈希不同（含salt），但验证时应能匹配
**排查**: 检查数据库password字段是否为BCrypt格式（$2a$开头）

### Q4: 前端路由守卫失效
**症状**: 未登录也能访问主页
**原因**: router.beforeEach未正确配置或Pinia store未初始化
**解决**: 检查router/index.js中的导航守卫逻辑

---

# M002: 渠道管理

## 模块概览

| 属性 | 说明 |
|------|------|
| **模块目标** | 实现AI厂商渠道的完整CRUD管理，支持多类型渠道接入和健康监测 |
| **包含任务** | 3个（TASK-M002-001, 002, 003） |
| **预估工时** | 10小时 |
| **技术栈** | Spring Data JPA + AES加密 + Element Plus表格 |
| **前置依赖** | M001-002（后端项目已搭建）, M001-003（前端项目已搭建） |

## 核心业务流程

```
管理员点击"新增渠道"
    ↓
弹出对话框（ChannelDialog.vue）
    ↓
填写表单（名称、编码、类型、Base URL、API Key）
    ↓
提交到后端 POST /api/v1/channels
    ↓
后端验证 → AES加密API Key → 保存到数据库
    ↓
返回成功 → 刷新列表
    ↓
列表展示（掩码显示API Key）
    ↓
可选操作：
  - 编辑（更新非敏感字段）
  - 测试连通性（异步请求厂商API）
  - 启用/禁用（状态切换）
  - 删除（逻辑删除）
```

## 数据库表结构

### channels 表（核心字段）

```sql
-- 示例数据
INSERT INTO channels (
    channel_name, channel_code, channel_type, base_url,
    api_key_encrypted, weight, priority, status, created_at
) VALUES (
    'OpenAI官方',
    'openai-official',
    'openai',
    'https://api.openai.com/v1',
    'AES加密后的sk-xxx',
    100,
    0,
    1,
    NOW()
);
```

**关键字段说明：**
- `channel_type`: 枚举值 openai/qwen/wenxin/doubao/claude/gemini/deepseek
- `api_key_encrypted`: AES-GCM加密后的API Key（TEXT类型）
- `status`: 0=禁用, 1=启用, 2=维护中
- `health_status`: 0=不健康, 1=健康（通过连通性测试更新）

### models 表（关联）
- 通过 `channel_id` 外键关联channels
- 一个渠道可有多个模型配置

## API接口设计

### 渠道管理接口

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | `/api/v1/channels` | 渠道列表（分页+筛选） | 已认证 |
| POST | `/api/v1/channels` | 创建渠道 | 已认证 |
| PUT | `/api/v1/channels/{id}` | 更新渠道 | 已认证 |
| DELETE | `/api/v1/channels/{id}` | 删除渠道（逻辑删除） | 已认证 |
| PUT | `/api/v1/channels/{id}/status` | 切换启用/禁用 | 已认证 |
| POST | `/api/v1/channels/{id}/test` | 测试连通性（异步） | 已认证 |

### 请求/响应示例

**创建渠道请求：**
```json
{
  "channelName": "OpenAI官方",
  "channelCode": "openai-official",
  "channelType": "openai",
  "baseUrl": "https://api.openai.com/v1",
  "apiKey": "sk-proj-real-api-key-here",
  "weight": 100,
  "priority": 0,
  "remark": "官方渠道"
}
```

**渠道列表VO（掩码显示）：**
```json
{
  "id": 1,
  "channelName": "OpenAI官方",
  "channelCode": "openai-official",
  "channelType": "openai",
  "baseUrl": "https://api.openai.com/v1",
  "apiKeyMask": "sk-p***...***ere",  // 掩码！
  "weight": 100,
  "status": 1,
  "healthStatus": 1,
  "createdAt": "2026-04-02T10:00:00"
}
```

## 关键技术实现

### AES加密工具类（AesEncryptUtil）

**用途**: 加密存储渠道API Key和真实Key

**关键点：**
- 算法: AES/GCM/NoPadding（带认证的加密模式）
- 密钥: 从 `aes.secret-key` 配置读取（必须32字符）
- IV: 每次随机生成12字节，拼接在密文前
- 实现: 加密时 IV+密文一起Base64编码；解密时先提取IV再解密

**代码片段参考:** 见 mvp-tasks.md 中 TASK-M003-001 的完整实现

### 连通性测试（异步）

**实现方式:**
1. 接收渠道ID
2. 从数据库查询渠道信息
3. 解密API Key
4. 使用RestTemplate发送HTTP GET请求到 `{baseUrl}/models`
5. 根据响应状态码更新 `health_status` 字段
6. 返回布尔值表示是否可达

**注意事项:**
- 使用 `@Async` 注解实现异步
- 设置合理的超时时间（5-10秒）
- 异常捕获要全面（网络超时、DNS解析失败、SSL证书问题等）

## 前端页面设计

### 页面布局

```
┌─────────────────────────────────────────────┐
│  渠道管理                                    │
├─────────────────────────────────────────────┤
│ [关键词____] [类型▼] [搜索] [重置] [+新增]   │
├─────────────────────────────────────────────┤
│ ID │ 名称     │ 类型   │ Base URL      │ ...│
│ 1  │ OpenAI   │ OpenAI │ https://...   │ ...│
│ 2  │ 通义千问 │ qwen   │ https://...   │ ...│
│ ─────────────────────────────────────────── │
│              [<] [1] [2] [3] [>] 共X条       │
└─────────────────────────────────────────────┘
```

### 关键交互

1. **新增/编辑**: 弹出el-dialog对话框，表单包含所有必填字段
2. **类型选择**: el-select下拉框，选项为预定义的7种渠道类型
3. **API Key输入**: input type="password" show-password
4. **状态切换**: el-switch组件，即时调用接口更新
5. **连通性测试**: 点击按钮 → 显示loading → 显示成功/失败提示
6. **删除确认**: el-popconfirm或el-messageBox.confirm

### 分页与搜索

- **分页**: el-pagination组件，支持每页10/20/50条切换
- **搜索**: 支持关键词模糊搜索（名称/编码）+ 类型精确筛选
- **实时搜索**: 可选防抖（debounce 300ms），避免频繁请求

## 开发检查清单

### 后端验收标准
- [ ] Channel实体映射到channels表，字段完整
- [ ] Model实体映射到models表，关联关系正确
- [ ] 创建渠道时API Key自动AES加密存储
- [ ] 渠道列表中API Key以掩码显示（前3***...***后3）
- [ ] 分页查询正常工作（page, size参数）
- [ ] 关键词搜索和类型筛选有效
- [ ] 更新渠道信息成功保存
- [ ] 删除渠道为逻辑删除（deleted=1）
- [ ] 启用/禁用切换即时生效
- [ ] 连通性测试可检测渠道是否可达
- [ ] 所有接口有Swagger注解

### 前端验收标准
- [ ] 渠道列表页面正常显示数据
- [ ] 新增渠道对话框可用，表单验证生效
- [ ] 编辑渠道可回显现有数据
- [ ] 删除操作有二次确认
- [ ] 分页组件功能正常
- [ ] 搜索和筛选功能可用
- [ ] 状态开关（switch）可点击并有反馈
- [ ] 连通性测试按钮点击后有loading和结果提示
- [ ] 页面无控制台报错
- [ ] 响应式布局适配良好

### 特殊场景测试
- [ ] 创建相同channelCode的渠道 → 应提示"编码已存在"
- [ ] Base URL格式校验（必须是合法URL）
- [ ] API Key为空时的提示
- [ ] 大量数据（100+条）的分页性能
- [ ] 并发创建渠道的数据一致性

---

# M003: APIKey管理

## 模块概览

| 属性 | 说明 |
|------|------|
| **模块目标** | 实现真实Key的安全管理和虚拟Key的生命周期管理 |
| **包含任务** | 5个（TASK-M003-001, 002, 003, 004, 005） |
| **预估工时** | 16小时 |
| **技术栈** | AES-256加密 + UUID生成 + Tab页面切换 |
| **前置依赖** | M001-002, M002-001（实体依赖） |

## 核心业务概念

### 真实Key（Real Key）
- **定义**: AI厂商提供的原始API Key（如OpenAI的sk-proj-xxx）
- **特点**: 高价值、需严格保护、不可明文暴露
- **存储**: AES-256加密后存入数据库
- **展示**: 仅显示掩码（sk-***...***abc）
- **绑定**: 必须关联到一个渠道（Channel）

### 虚拟Key（Virtual Key）
- **定义**: 平台生成的虚拟化Key（如sk-a1b2c3d4...）
- **用途**: 分发给开发者/应用使用，替代真实Key
- **优势**: 可控、可追踪、可限速、可随时吊销
- **存储**: 明文存储（本身就是虚拟的，非真实密钥）
- **格式**: sk-32位随机字符串
- **配置**: 可绑定用户、设置额度、限制频率、指定可用模型

### 两者的关系
```
真实Key (Real Key)          虚拟Key (Virtual Key)
┌─────────────┐            ┌─────────────┐
│ sk-proj-xxx │ ←──加密──→ │ AES密文存储  │
│ (厂商提供)   │            └─────────────┘
└─────────────┘                     ↑
                                     │ 绑定渠道
                              ┌──────┴──────┐
                              │   Channel    │
                              │ (渠道承载)    │
                              └─────────────┘

虚拟Key用于调用 → 系统鉴权 → 匹配真实Key → 转发到厂商
```

## 数据库表结构

### real_keys 表

```sql
INSERT INTO real_keys (
    key_name, key_value_encrypted, key_mask,
    channel_id, status, created_at
) VALUES (
    'OpenAI-Pro-Key',
    'AES加密后的真实Key',
    'sk-pr***...***jkl',
    1,  -- channel_id
    1,  // status=启用
    NOW()
);
```

**关键字段：**
- `key_value_encrypted`: TEXT类型，AES-GCM加密
- `key_mask`: 掩码值，如 `sk-pr***...***jkl`
- `channel_id`: 外键关联channels表
- `usage_count`: 使用次数统计（MVP阶段可能暂不更新）

### virtual_keys 表

```sql
INSERT INTO virtual_keys (
    key_name, key_value, user_id,
    quota_type, quota_limit, rate_limit_qpm,
    status, created_at
) VALUES (
    '测试用虚拟Key',
    'sk-a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6',
    1,  // user_id
    'token',
    1000000.00,  // 100万token额度
    60,  // 每分钟60次
    1,  // 启用
    NOW()
);
```

**关键字段：**
- `key_value`: 明文存储，格式 `sk-` + 32位随机字符
- `allowed_models`: JSON数组，空表示不限制模型
- `quota_type`: token/count/amount
- `rate_limit_qpm`: 每分钟请求限制
- `rate_limit_qpd`: 每日请求限制，0=不限制

## API接口设计

### 真实Key管理接口

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/v1/real-keys` | 真实Key列表（分页+按渠道筛选） |
| POST | `/api/v1/real-keys` | 录入真实Key（自动加密+生成掩码） |
| PUT | `/api/v1/real-keys/{id}/status` | 启用/禁用 |
| DELETE | `/api/v1/real-keys/{id}` | 删除（逻辑删除） |

### 虚拟Key管理接口

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/v1/virtual-keys` | 虚拟Key列表（分页+按用户筛选） |
| POST | `/api/v1/virtual-keys` | 生成新虚拟Key |
| PUT | `/api/v1/virtual-keys/{id}` | 更新配置（额度和限速） |
| PUT | `/api/v1/virtual-keys/{id}/refresh` | 刷新Key值（重新生成） |
| PUT | `/api/v1/virtual-keys/{id}/status` | 启用/禁用 |
| DELETE | `/api/v1/virtual-keys/{id}` | 删除（逻辑删除） |

### 请求/响应示例

**录入真实Key：**
```json
// Request
{
  "keyName": "生产环境主Key",
  "keyValue": "sk-proj-xxxxxxxxxxxxxxxxxxxx",  // 明文
  "channelId": 1,
  "expireTime": null,
  "remark": "重要：勿泄露"
}

// Response (201 Created)
{
  "code": 200,
  "data": {
    "id": 1,
    "keyName": "生产环境主Key",
    "keyMask": "sk-pr***...***xxx",  // 只返回掩码！
    "channelId": 1,
    "channelName": "OpenAI官方",
    "status": 1
  }
}
```

**生成虚拟Key：**
```json
// Request
{
  "keyName": "前端团队测试Key",
  "userId": 2,
  "quotaType": "token",
  "quotaLimit": 500000.00,
  "rateLimitQpm": 30,
  "expireTime": "2026-12-31T23:59:59"
}

// Response
{
  "code": 200,
  "data": {
    "id": 1,
    "keyName": "前端团队测试Key",
    "keyValue": "sk-a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6",  // 完整显示！
    "userId": 2,
    "userName": "zhangsan",
    "quotaType": "token",
    "quotaLimit": 500000.00,
    "quotaUsed": 0,
    "quotaRemaining": 500000.00,
    "rateLimitQpm": 30,
    "status": 1,
    "createdAt": "2026-04-02T14:00:00"
  }
}
```

## 前端页面设计

### 主页面布局（Tab切换）

```
┌────────────────────────────────────────────────────┐
│  API Key 管理                                       │
├────────────────────────────────────────────────────┤
│  [真实Key管理]  [虚拟Key管理]                        │
├────────────────────────────────────────────────────┤
│                                                    │
│  （根据选中的Tab显示不同的子页面内容）                 │
│                                                    │
│  Tab1: RealKeyList.vue                             │
│  Tab2: VirtualKeyList.vue                           │
│                                                    │
└────────────────────────────────────────────────────┘
```

### 真实Key列表页面

**列定义：**
| 列名 | 字段 | 特殊处理 |
|------|------|----------|
| ID | id | - |
| Key名称 | keyName | - |
| Key掩码 | keyMask | 显示为 `sk-***...***xxx` 格式 |
| 所属渠道 | channelName | 下拉筛选 |
| 状态 | status | el-switch 或 el-tag |
| 录入时间 | createdAt | 格式化显示 |
| 操作 | - | [编辑] [禁用] [删除] |

**特殊点：**
- 不需要"刷新"按钮（真实Key不可重新生成）
- 录入时必须选择所属渠道（下拉选择已有渠道）
- 列表中绝对不能出现真实Key明文

### 虚拟Key列表页面

**列定义：**
| 列名 | 字段 | 特殊处理 |
|------|------|----------|
| ID | id | - |
| Key名称 | keyName | - |
| Key值 | keyValue | **完整显示** + [复制] 按钮 |
| 所属用户 | userName | 下拉筛选 |
| 额度类型 | quotaType | el-tag (token/count/amount) |
| 已用/总额度 | quotaUsed/quotaLimit | 进度条展示 |
| 每分钟限制 | rateLimitQpm | - |
| 状态 | status | el-switch |
| 过期时间 | expireTime | 到期标红 |
| 操作 | - | [编辑] [刷新] [禁用] [删除] |

**特殊点：**
- Key值完整显示（因为是虚拟的，非真实密钥）
- 提供[复制]按钮方便用户复制Key
- 有[刷新Key]按钮（重新生成新的Key值）
- 额度使用进度条可视化
- 显示额度剩余百分比

### 对话框设计

**真实Key录入对话框：**
- Key名称（文本输入）
- Key值（密码输入框，show-password）
- 所属渠道（下拉选择，从API获取渠道列表）
- 过期时间（日期时间选择器，可选）
- 备注（多行文本）

**虚拟Key生成对话框：**
- Key名称（文本输入）
- 所属用户（下拉选择，从API获取用户列表）
- 额度类型（单选：token/count/amount）
- 额度上限（数字输入）
- 每分钟限制（数字输入，默认60）
- 每日限制（数字输入，默认0=不限）
- 过期时间（日期时间选择器，可选）
- 允许的模型（多选，可选，留空=不限制）
- 备注（多行文本）

## 关键技术实现

### 虚拟Key生成算法

```java
private String generateKeyValue() {
    String uuid = UUID.randomUUID().toString().replace("-", "");
    return "sk-" + uuid.substring(0, 32);
    // 结果示例: sk-a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6
}

// 唯一性保证
public VirtualKey createVirtualKey(VirtualKeyCreateRequest request) {
    String keyValue;
    do {
        keyValue = generateKeyValue();
    } while (virtualKeyRepository.existsByKeyValue(keyValue));
    // 循环直到生成唯一的Key值
    // ...
}
```

### 掩码生成算法

```java
private String generateMask(String originalKey) {
    if (originalKey.length() > 10) {
        return originalKey.substring(0, 3)
               + "***...***"
               + originalKey.substring(originalKey.length() - 3);
    }
    return "***";  // 太短的Key全部隐藏
}

// 示例:
// 输入: "sk-proj-abcdefghijklmnopqrstuvwxyz123456"
// 输出: "sk-p***...***456"
```

### 额度计算（预留接口）

虽然MVP阶段不一定实现完整的扣减逻辑，但建议预留：

```java
// 扣减额度（供未来调度网关调用）
@Transactional
public void consumeQuota(Long virtualKeyId, BigDecimal amount) {
    VirtualKey vk = virtualKeyRepository.findById(virtualKeyId)
            .orElseThrow(() -> new BusinessException("虚拟Key不存在"));

    if (vk.getQuotaRemaining().compareTo(amount) < 0) {
        throw new BusinessException("额度不足");
    }

    vk.setQuotaUsed(vk.getQuotaUsed().add(amount));
    vk.setQuotaRemaining(vk.getQuotaRemaining().subtract(amount));
    virtualKeyRepository.save(vk);

    // TODO: 异步记录流水到quota_transactions表
}
```

## 开发检查清单

### 后端验收标准 - 真实Key
- [ ] RealKey实体映射到real_keys表
- [ ] 录入真实Key时自动AES加密
- [ ] 自动生成掩码并保存
- [ ] 列表查询只返回掩码，不返回明文
- [ ] 按渠道筛选功能正常
- [ ] 启用/禁用切换正常
- [ ] 删除为逻辑删除
- [ ] 单元测试验证加密解密正确性

### 后端验收标准 - 虚拟Key
- [ ] VirtualKey实体映射到virtual_keys表
- [ ] 生成的Key符合 `sk-` + 32字符格式
- [ ] Key值唯一性保证（循环检测）
- [ ] 额度和限速配置正确保存
- [ ] 刷新Key可生成全新值（旧值失效）
- [ ] 列表查询支持分页和按用户筛选
- [ ] 所有CRUD接口可用

### 前端验收标准
- [ ] Tab切换流畅（真实Key ↔ 虚拟Key）
- [ ] 真实Key列表掩码显示正确
- [ ] 录入对话框可选择渠道
- [ ] 虚拟Key列表完整显示Key值
- [ ] 复制按钮可用（复制到剪贴板）
- [ ] 刷新Key后有明确提示和新值展示
- [ ] 额度进度条显示正确
- [ ] 所有表单验证生效
- [ ] 页面加载无白屏或卡顿

### 安全性专项检查
- [ ] 真实Key在任何情况下都不在前端明文展示（包括Network面板）
- [ ] 数据库中real_keys表的key_value_encrypted字段是密文
- [ ] 虚拟Key虽然明文存储，但要有明确的标识和使用范围
- [ ] API日志中不得打印完整Key值
- [ ] 导出功能（如有）需要对Key值脱敏

---

# 附录：MVP整体集成指南

## 模块间依赖关系

```
M001 (登录认证)
    ↓ 提供身份认证能力
M002 (渠道管理) ← 依赖M001的后端框架
    ↓ 提供渠道承载对象
M003 (APIKey管理) ← 依赖M001和M002的实体
    ↓
[MVP完成]
```

## 推荐的开发顺序

### 第一天：基础设施（并行）
- **上午**: TASK-M001-002 后端项目搭建（3h）
- **下午**: TASK-M001-003 前端项目搭建（3h）
- **产出**: 可运行的前后端骨架

### 第二天：认证基础
- TASK-M001-004 User/Role/Permission实体（2h）
- **产出**: 数据库实体层就绪

### 第三天：JWT认证
- TASK-M001-005 JWT认证+Security（4h）
- **产出**: 安全认证体系建立

### 第四天：登录功能（前后端联调）
- 上午: TASK-M001-006 登录接口（3h）
- 下午: TASK-M001-007 登录页面（3h）
- **产出**: 用户可登录系统 ✅

### 第五-六天：渠道管理
- Day5上午: TASK-M002-001 Channel/Model实体（2h）
- Day5下午: TASK-M002-002 渠道管理接口（4h）
- Day6: TASK-M002-003 渠道管理前端页面（4h）
- **产出**: 渠道CRUD完成 ✅

### 第七-九天：APIKey管理
- Day7上午: TASK-M003-001 RealKey实体+AES（2h）
- Day7下午: TASK-M003-003 VirtualKey实体（2h）
- Day8上午: TASK-M003-002 真实Key接口（3h）
- Day8下午: TASK-M003-004 虚拟Key接口（4h）
- Day9: TASK-M003-005 APIKey管理前端页面（5h）
- **产出**: MVP全部完成 ✅

## 最终交付物清单

### 代码层面
- [ ] 后端Spring Boot项目（可编译运行）
- [ ] 前端Vue3项目（可构建运行）
- [ ] 数据库Schema（已执行，含初始数据）
- [ ] 单元测试（核心逻辑覆盖率≥60%）
- [ ] Swagger API文档（所有接口可查看）

### 文档层面
- [x] MVP专属任务清单（mvp-tasks.md）
- [x] 工作流规范（spec.md已追加MVP章节）
- [x] 风险跟踪模板（promote.md已补全）
- [x] 当前调度信息（current-task.md已填写）
- [x] SDD摘要（本文档）

### 功能层面
- [ ] 用户可登录系统
- [ ] 管理员可管理渠道（增删改查、测试）
- [ ] 管理员可管理真实Key（录入、加密、掩码展示）
- [ ] 管理员可管理虚拟Key（生成、刷新、配额）
- [ ] 所有页面美观可用，无明显Bug

## 成功标准（MVP Definition of Done）

✅ **最小可行产品（MVP）达成标志：**

1. **可演示性**: 可以向利益相关者演示完整的业务流程
2. **可安装性**: 提供清晰的本地部署步骤（MySQL + Redis + 启动命令）
3. **可扩展性**: 代码架构清晰，易于后续添加M004-M010模块
4. **质量底线**: 无P0级Bug，核心流程可走通，安全性达标

---

**文档版本：** V1.0
**创建日期：** 2026-04-02
**创建者：** 项目策划师
**状态：** SDD摘要已完成，可供开发参考
