# AI调度中心 v2.0 迭代计划

> 基于 v1.0 代码全量评估 + PRD 对照产出
> 生成时间：2026-04-19
> 决策依据：用户 5 项澄清回复

---

## 0. 决策摘要

| # | 问题 | 用户选择 | 落地影响 |
|---|------|---------|---------|
| 1 | "个人"含义 | B：团队成员身份，个人维度 = 团队内用户视角 | 不新增独立个人工作区，保持现有 user→team→project 组织模型 |
| 2 | "个人限额"绑定 | B：虚拟 Key 自带额度即个人限额 | 保持现有 3 层漏斗（VirtualKey→Team→Project），不新增 user-level 额度池 |
| 3 | 首批厂商 | 智谱免费模型；需要具体方案 | 智谱免费模型为首个实接渠道，提供轻量适配接入方案 |
| 4 | 模型市场路线 | 实施简单 / 维护低 / 可行性强 | 本地 DB + 管理员 CRUD，不做 API 自动拉取 |
| 5 | 日志监控期望 | D（全功能但不带图表） | 列表+高级筛选+详情+导出+告警规则+通知推送，跳过可视化图表 |

---

## 1. 现有资产盘点（v1.0 已实现）

### 1.1 后端管线（已通）

```
VirtualKeyAuthFilter (Bearer sk-xxx)
  → GatewayOrchestrationService (编排)
    → RateLimitService (QPM/QPD, Redis)
    → QuotaService.checkQuotaWithFunnel (三层预检)
    → DispatchService.dispatch (4策略: RoundRobin/Weighted/LowestLatency/LowestCost)
    → ProxyForwardService.forwardChatCompletion (AES解密→RestTemplate转发)
    → QuotaService.deductQuotaWithFunnel (三层原子扣减)
    → CallLogService.recordAsync (@Async异步写日志)
```

- 重试逻辑：MAX_RETRY=3，4xx(除429)不重试，5xx/429/502/504 重试并排除失败渠道
- 调度：自动调度(channelId=null) vs 指定渠道(channelId!=null)
- 加密：AES-256 加密 RealKey，掩码展示

### 1.2 前端页面（11 页，3 占位）

| 路由 | 页面 | 状态 |
|------|------|------|
| / | Overview (控制台首页) | ✅ 已实现，数据硬编码 |
| /tokens | TokenManagement (令牌管理) | ✅ 已实现，绑团队上下文 |
| /channels | ChannelManagement (渠道管理) | ✅ 已实现 |
| /teams | TeamManagement (团队管理) | ✅ 已实现 |
| /projects | ProjectManagement (项目管理) | ✅ 已实现 |
| /analytics | Analytics (数据看板) | ✅ 已实现 |
| /logs | RequestLog (请求日志) | ✅ 已实现 |
| /quota-flow | QuotaFlow (额度流水) | ✅ 已实现 |
| /models | Placeholder | ❌ 占位 |
| /skills | Placeholder | ❌ 占位 |
| /profile | Placeholder | ❌ 占位 |

### 1.3 数据库（18 张表）

users, roles, permissions, user_roles, role_permissions,
channels, models, model_groups, model_configs,
real_keys, virtual_keys,
quotas, billing_rules, quota_transactions,
call_logs, login_logs,
teams, team_members, projects,
role_model_mappings, model_selection_logs,
system_configs, alert_rules, alert_histories

### 1.4 关键缺口

| 缺口 | 说明 |
|------|------|
| 流式不支持 | GatewayOrchestrationService 遇到 stream=true 直接抛 400 |
| 代理格式单一 | ProxyForwardService 硬编码 OpenAI 格式 (/v1/chat/completions + Bearer) |
| Overview 静态 | 4 张概览卡片数据是前端硬编码，未接后端 API |
| 导出功能缺失 | 日志/流水页面无导出按钮 |
| 告警仅有表结构 | alert_rules / alert_histories 表存在，但后端无 Service/Controller |
| 模型市场占位 | /models 路由指向 Placeholder（弱保留，不纳入本轮主线） |
| 个人中心占位 | /profile 路由指向 Placeholder（备选） |
| 渠道连通性测试 | ChannelService.testConnection 存在但前端未暴露 |
| 登录日志 | login_logs 表存在，LoginLog Entity 未确认实装（备选） |

---

## 2. v2.0 迭代范围

### 原则

- 不做架构重构，在现有 Spring Boot + Vue 3 + MySQL 技术栈上增量迭代
- 每个任务可独立完成、独立验证、独立合并
- 优先打通"能用"闭环：智谱免费模型实际跑通 → 流式支持 → 日志可查可导 → 告警可收

### 排除项

- 不做可视化图表（折线图/饼图/柱状图）
- 不做 SSO/LDAP 集成
- 不做套餐/充值/支付
- 不做 Webhook 回调
- 不做 K8s 部署
- 不做自定义角色
- 不做 skills 页面（无对应业务逻辑）

### 本轮主线

- Phase 1 ～ Phase 5 全部保留
- T7.3（删除 /skills 占位路由）保留在主线，作为低风险收口项
- 本轮主线验收只看智谱接入、流式支持、日志增强、告警系统、首页真实数据、skills 清理

### 弱保留 / 备选

- T6.1 模型市场页面：弱保留，只保留方案设计，不纳入当前排期
- T7.1 个人中心页面：备选，有余力再做
- T7.2 登录日志实装：备选，有余力再做

---

## 3. 任务清单（按执行顺序）

### Phase 1：智谱免费模型接入闭环（打通核心链路）

#### T1.1 — 新增智谱渠道种子数据与免费模型配置

状态：已完成（2026-04-20）

**目标**：data.sql 新增智谱渠道 + 免费文本模型配置，使系统启动即可见

**改动范围**：
- `backend/src/main/resources/db/data.sql`

**具体内容**：
```
channels 表新增：
  channel_name: 智谱AI
  channel_code: zhipu
  channel_type: zhipu
  base_url: https://open.bigmodel.cn/api/paas/v4
  weight: 100
  status: 1

models 表新增（关联 zhipu 渠道）：
  - glm-4.7-flash (chat, input_price=0.000000, output_price=0.000000, quota_weight=1.0)
  - glm-4-flash-250414 (chat, input_price=0.000000, output_price=0.000000, quota_weight=1.0)

model_configs 表新增：
  - glm-4.7-flash (provider=zhipu, context_window=200000, is_streaming=1, is_function_calling=0)
  - glm-4-flash-250414 (provider=zhipu, context_window=128000, is_streaming=1, is_function_calling=0)

补充说明：
  - 若渠道类型下拉框为静态枚举，同步补充 zhipu 选项
  - 首期只接文本免费模型，不接视觉/视频免费模型，控制实施面
```

**验证**：启动后在渠道管理页面可见智谱AI，模型管理可见两个免费模型

---

#### T1.2 — 智谱 API 轻量适配

状态：已完成（2026-04-20）

**目标**：在不重构网关的前提下，让 ProxyForwardService 正常转发智谱 Chat Completions

**分析**：
- 智谱通用 API 端点：`https://open.bigmodel.cn/api/paas/v4`
- Chat 路径：`/chat/completions`
- 认证方式：`Authorization: Bearer sk-xxx`
- 请求体与 OpenAI ChatCompletion 高度相似，可复用现有 DTO
- 官方示例支持 `stream: true`
- 当前 ProxyForwardService 的 `buildUrl()` 逻辑硬编码 `/v1/chat/completions`，不能直接命中智谱端点

**结论**：不是零代码接入，但只需做一层轻量适配，不需要重写网关。

**最小改动方案**：
1. ProxyForwardService 新增按渠道类型或 baseUrl 特征判断的 URL 构建分支
2. 当 `channel_type=zhipu` 或 `base_url` 包含 `/api/paas/v4` 时，目标地址拼为：`{baseUrl}/chat/completions`
3. Bearer 鉴权、请求序列化、错误处理、重试机制全部复用现有实现
4. 若后续再接更多非 OpenAI 路径厂商，再抽象 provider adapter；当前不提前过度设计

**实际验证**：
1. 在渠道管理页面创建智谱AI渠道，填入真实 base_url 和 API Key
2. 创建虚拟 Key，绑定智谱AI渠道
3. curl 测试：
```bash
curl -X POST http://localhost:8080/v1/chat/completions \
  -H "Authorization: Bearer sk-你的虚拟Key" \
  -H "Content-Type: application/json" \
  -d '{"model":"glm-4.7-flash","messages":[{"role":"user","content":"你好"}]}'
```
4. 预期：收到智谱的回复 JSON，call_logs 表有记录，额度有扣减

---

#### T1.3 — 前端渠道管理增加"连通性测试"按钮

状态：已完成（2026-04-20）

**目标**：ChannelService.testConnection 已存在，前端暴露操作入口

**改动范围**：
- `frontend/src/views/ChannelManagement.vue`
- `frontend/src/api/channel.js`

**具体内容**：
- channel.js 新增 `testChannel(id)` 方法，POST `/api/v1/channels/{id}/test`
- ChannelManagement.vue 渠道列表操作列新增"测试连通性"按钮
- 点击后调用 API，展示 ElMessage 成功/失败
- 渠道详情中展示 health_status 和 health_check_time

**验证**：创建智谱AI渠道后，点击测试，health_status 变为健康

---

### Phase 2：流式支持（真实使用的基础）

#### T2.1 — 后端支持 SSE 流式转发

**目标**：让 stream=true 的请求能正常透传并流式返回给调用方

**改动范围**：
- `GatewayOrchestrationService.java`：去掉 stream=true 的 400 拦截
- `ProxyForwardService.java`：新增 `forwardChatCompletionStream` 方法
- `GatewayController.java`（或现有入口）：新增流式端点
- `pom.xml`：确认 spring-boot-starter-webflux 或 SseEmitter 依赖

**实现方案（推荐 SseEmitter，不引入 WebFlux）**：
```
1. Controller 新增：
   @PostMapping(value="/v1/chat/completions", produces=MediaType.TEXT_EVENT_STREAM_VALUE)
   当 request.stream==true 时走流式分支

2. ProxyForwardService.forwardChatCompletionStream:
   - 使用 RestTemplate + ResponseExtractor 逐行读取上游 SSE
   - 每读到一行 data: {...} 就通过 SseEmitter.send() 推给调用方
   - 读到 data: [DONE] 时 complete()

3. GatewayOrchestrationService:
   - 流式场景：额度在流结束后一次性扣减（从最终的 usage 字段提取 token 数）
   - 日志记录：流结束后异步写入
```

**技术要点**：
- SseEmitter 超时设置：5 分钟（长对话场景）
- 错误处理：上游中断时 SseEmitter.completeWithError()
- Token 统计：优先读取流式结束块中的 usage；若智谱流式响应未返回 usage，则记录为待补全并在非流式模式下校准

**验证**：
```bash
curl -N -X POST http://localhost:8080/v1/chat/completions \
  -H "Authorization: Bearer sk-虚拟Key" \
  -H "Content-Type: application/json" \
  -d '{"model":"glm-4.7-flash","messages":[{"role":"user","content":"写一首诗"}],"stream":true}'
```
预期：逐行收到 SSE 事件，最后收到 `data: [DONE]`

---

### Phase 3：日志增强（可查、可导、可追踪）

#### T3.1 — 日志导出功能

**目标**：调用日志支持 CSV 导出

**改动范围**：
- `CallLogController.java`：新增 GET `/api/v1/logs/export` 端点
- `CallLogQueryService.java`：新增 `exportLogs` 方法
- `frontend/src/views/RequestLog.vue`：新增"导出"按钮
- `frontend/src/api/log.js`：新增 `exportLogs` 方法

**具体内容**：
- 后端：复用现有 Specification 查询逻辑，输出为 CSV（Content-Type: text/csv, Content-Disposition: attachment）
- 导出字段：时间, traceId, 用户, 模型, 渠道, promptTokens, completionTokens, totalTokens, cost, responseTime, status, errorCode
- 单次导出上限：10000 条（超出提示缩小时间范围）
- 前端：筛选条件栏右侧新增"导出 CSV"按钮，使用 Blob 下载

**验证**：设置筛选条件后点击导出，浏览器下载 CSV 文件，内容与列表一致

---

#### T3.2 — 额度流水导出功能

**目标**：额度流水支持 CSV 导出

**改动范围**：
- `QuotaController.java`：新增 GET `/api/v1/quota/transactions/export`
- `frontend/src/views/QuotaFlow.vue`：新增"导出"按钮
- `frontend/src/api/quota.js`：新增 `exportTransactions` 方法

**实现逻辑**：与 T3.1 同模式，复用现有查询 + CSV 输出

---

#### T3.3 — 日志详情增强

**目标**：日志详情弹窗展示完整调用链路信息

**改动范围**：
- `frontend/src/views/RequestLog.vue`：增强详情弹窗

**新增展示字段**：
- 调用链路：是否自动调度 (is_auto_mode) → 选中模型 (selected_model) → 选择策略 (selection_strategy)
- 错误信息：错误码 + 错误消息（已有字段，确认展示）
- 请求元信息：客户端 IP + User-Agent（已有字段，确认展示）

---

### Phase 4：告警系统（从表结构到可用）

#### T4.1 — 告警规则 CRUD

**目标**：实现 alert_rules 表的后端 CRUD + 前端管理页面

**改动范围**：
- 新增 `AlertRule.java`（Entity）
- 新增 `AlertRuleRepository.java`
- 新增 `AlertRuleService.java`
- 新增 `AlertRuleController.java`：`/api/v1/alert-rules`
- 新增 `frontend/src/api/alert.js`
- 新增 `frontend/src/views/AlertManagement.vue`
- 修改 `frontend/src/router/index.js`：新增路由 `/alerts`
- 修改 `frontend/src/components/MainLayout.vue`：侧边栏新增"告警管理"菜单项

**告警规则类型**（首批支持）：
| 类型 | 触发条件示例 | condition_config JSON |
|------|-------------|----------------------|
| quota | 额度剩余 < 阈值% | `{"metric":"quota_remaining_pct","operator":"<","value":20}` |
| channel | 渠道连续失败 >= N 次 | `{"metric":"consecutive_failures","operator":">=","value":5}` |
| frequency | 某 Key 每分钟调用 > N | `{"metric":"qpm","operator":">","value":100}` |
| error | 全局错误率 > 阈值% | `{"metric":"error_rate_pct","operator":">","value":10}` |

**action_config JSON**：
```json
{
  "actions": ["log"],
  "notification_channels": ["system"]
}
```
首批只做系统内通知（写 alert_histories 表），不集成外部通道。

**前端页面**：
- 告警规则列表（表格：名称 / 类型 / 目标 / 状态 / 最后触发时间 / 操作）
- 新建/编辑规则弹窗（表单：名称 / 类型 / 条件配置 / 启用状态）
- 告警历史列表（表格：时间 / 级别 / 标题 / 内容 / 通知状态）

**验证**：创建一条"额度低于 20% 告警"规则，在告警管理页面可见

---

#### T4.2 — 告警触发引擎

**目标**：定时检测告警规则条件，触发时写入 alert_histories

**改动范围**：
- 新增 `AlertCheckService.java`
- 配置 `@Scheduled` 定时任务

**实现方案**：
```
@Scheduled(fixedRate = 60000)  // 每分钟检测一次
checkAlertRules():
  1. 查询所有 is_enabled=1 的规则
  2. 对每条规则：
     - quota 类型：查 quotas / virtual_keys 表，计算剩余百分比
     - channel 类型：查 channels 表的 fail_count / health_status
     - frequency 类型：查 call_logs 最近1分钟记录数
     - error 类型：查 call_logs 最近5分钟 status=0 占比
  3. 条件满足 → 写入 alert_histories（防重复：同规则同目标1小时内不重复触发）
  4. 更新 rule.last_triggered_time 和 trigger_count
```

**验证**：创建一条阈值很低的规则，手动制造触发条件，alert_histories 表有新记录

---

#### T4.3 — 告警通知展示

**目标**：在前端顶栏的通知铃铛中展示未读告警

**改动范围**：
- `AlertRuleController.java`：新增 GET `/api/v1/alerts/unread-count` 和 `/api/v1/alerts/recent`
- `frontend/src/components/MainLayout.vue`：铃铛 Badge 绑定真实未读数，点击展开最近告警列表
- `frontend/src/api/alert.js`：新增查询方法

**实现**：
- 前端每 60 秒轮询 unread-count 接口
- 点击铃铛展示 Dropdown，显示最近 10 条告警
- 每条：级别标签(info/warning/error/critical) + 标题 + 时间
- 点击某条告警跳转到告警历史详情

---

### Phase 5：Overview 首页接入真实数据

#### T5.1 — 后端统计 API

**目标**：为首页概览卡片提供真实数据

**改动范围**：
- 新增 `DashboardController.java`：GET `/api/v1/dashboard/overview`
- 新增 `DashboardService.java`

**返回数据**：
```json
{
  "todayCalls": 128,
  "todayTokens": 24560,
  "todayCost": 12.80,
  "avgResponseTime": 156,
  "todayCallsTrend": 12.5,
  "todayTokensTrend": 8.3,
  "todayCostTrend": -5.2,
  "avgResponseTimeTrend": 2.1
}
```

**实现**：
- todayCalls: `SELECT COUNT(*) FROM call_logs WHERE DATE(created_at) = CURDATE()`
- todayTokens: `SELECT COALESCE(SUM(total_tokens), 0) FROM call_logs WHERE DATE(created_at) = CURDATE()`
- todayCost: `SELECT COALESCE(SUM(cost), 0) FROM call_logs WHERE DATE(created_at) = CURDATE()`
- avgResponseTime: `SELECT COALESCE(AVG(response_time), 0) FROM call_logs WHERE DATE(created_at) = CURDATE() AND status = 1`
- trend: 对比昨天同期数据，计算变化百分比

---

#### T5.2 — 前端 Overview 接入真实数据

**改动范围**：
- `frontend/src/views/Overview.vue`：去掉硬编码数据，onMounted 调用 API
- `frontend/src/api/dashboard.js`：新增

---

### Phase 6：主线收尾（沿用原任务号，避免历史记录断链）

#### T7.3 — 删除 /skills 占位路由

**目标**：清理明确不会落地的占位页面，避免导航与实际能力不一致

**改动范围**：
- `frontend/src/router/index.js`：移除 /skills 路由
- `frontend/src/components/MainLayout.vue`：移除侧边栏对应菜单项（如有）

**说明**：
- T7.3 保留在主线，原因是对应业务逻辑已经明确为“不做”，属于低风险收口项
- 为避免与已有讨论、进度记录和代码备注断链，本轮不重新编号，继续沿用 T7.3

---

### 附录 A：弱保留 / 备选任务（不纳入当前主线）

#### T6.1 — 模型市场页面（弱保留）

状态：弱保留，不进入当前主线排期

**目标**：用已有的 ModelMarket.vue（或新建）替换 /models 的 Placeholder

**处理原则**：
- 当前只保留方案，不纳入本轮执行和验收
- 若后续模型运营或展示诉求明确，再恢复为正式任务
- 恢复时继续走“本地 DB + 管理员 CRUD”路线，不引入外部 API 拉取

**改动范围**：
- `frontend/src/router/index.js`：/models 路由指向 ModelMarket
- 确认 ModelMarket.vue 已接入 `/api/v1/models` 后端 API

**页面功能**：
- 模型列表表格：模型名称 / 编码 / 类型 / 所属渠道 / 价格 / 状态
- 筛选：按渠道、类型、状态筛选
- 管理员操作：新增 / 编辑 / 上下线
- 模型详情弹窗：完整配置信息

**数据来源**：
- models 表 + model_configs 表
- 管理员通过页面手动维护
- 不从外部 API 拉取

**恢复后验证**：/models 页面展示智谱免费模型和已有的种子模型

---

#### T7.1 — 个人中心页面（备选）

状态：备选，有余力再做

**目标**：替换 /profile 的 Placeholder

**改动范围**：
- 新增 `frontend/src/views/Profile.vue`
- `frontend/src/router/index.js`：/profile 路由指向 Profile
- 后端如需新增 `/api/v1/users/me` 端点（获取当前用户信息）

**页面内容**：
- 基本信息卡片：用户名、邮箱、手机、真实姓名、头像
- 安全设置：修改密码表单（旧密码 + 新密码 + 确认密码）
- 我的团队：当前所在团队列表
- 我的虚拟 Key 摘要：名下 Key 数量、总额度、已用额度

---

#### T7.2 — 登录日志实装（备选）

状态：备选，有余力再做

**目标**：login_logs 表已存在，确保登录行为写入并可查

**改动范围**：
- 确认/新增 `LoginLog.java`（Entity）
- 确认/新增 `LoginLogRepository.java`
- 在 `AuthController` 的登录方法中，成功/失败均写 login_logs
- 新增 GET `/api/v1/login-logs` 查询端点（管理员可查所有，普通用户仅查自己）
- 个人中心页面增加"最近登录记录"子区块

---

## 4. 任务依赖关系

```
Phase 1 (智谱免费模型接入)
  T1.1 种子数据 ──→ T1.2 兼容确认 ──→ T1.3 连通性测试按钮
                                            │
Phase 2 (流式支持)                           │
  T2.1 SSE流式转发 ←────────────────────────┘
 
Phase 3 (日志增强)          Phase 4 (告警系统)          Phase 5 (首页真实数据)
  T3.1 日志导出              T4.1 告警CRUD              T5.1 后端统计API ──→ T5.2 前端接入
  T3.2 流水导出              T4.2 告警引擎 ──→ T4.3 通知展示
  T3.3 详情增强

Phase 6 (主线收尾)
  T7.3 清理占位路由  ←──── 建议与 T4.1 一并处理 router / menu
```

- Phase 1 → Phase 2：流式支持需要先有可用渠道
- Phase 3 / Phase 4 / Phase 5 彼此无依赖，可并行
- T7.3 保留在主线，但不属于关键路径；建议和 T4.1 一起改 `router/index.js`、`MainLayout.vue`，减少重复修改
- T6.1 为弱保留，T7.1 / T7.2 为备选，不计入本轮排期与关键路径

---

## 5. 智谱免费模型接入完整方案

### 5.1 为什么智谱不是零代码接入，但仍然适合首批接入

当前 ProxyForwardService 的转发逻辑：
1. 读取 channel.base_url → 拼接 `/v1/chat/completions`
2. 解密 RealKey → 设为 `Authorization: Bearer {key}`
3. 原封不动转发 ChatCompletionRequest JSON
4. 原封不动返回 ChatCompletionResponse JSON

智谱的 API 规格：
- 通用端点: `https://open.bigmodel.cn/api/paas/v4`
- 路径: `/chat/completions`
- 认证: `Authorization: Bearer sk-xxx`
- 请求体: 与现有 ChatCompletionRequest 高度兼容
- 响应体: 可沿用现有 ChatCompletionResponse 解析
- 官方调用示例支持 `stream: true`

结论：**需要一处 URL 拼接适配，但改动范围极小**。相比需要专用 SDK 或复杂签名的厂商，智谱仍然符合“实施逻辑简单、维护成本低、可行性强”的路线。

### 5.2 接入步骤

```
步骤1：获取 API Key
  → 注册 https://bigmodel.cn
  → 创建 API Key，记录 sk-xxx

步骤2：系统中创建渠道
  → 渠道管理 → 新增
  → 名称: 智谱AI
  → 编码: zhipu
  → 类型: zhipu
  → Base URL: https://open.bigmodel.cn/api/paas/v4
  → 权重: 100

步骤3：录入真实 Key
  → 渠道管理 → 智谱AI → 管理 Key
  → 新增，粘贴 sk-xxx
  → 系统自动 AES-256 加密存储

步骤4：创建模型记录
  → 模型管理 → 新增
  → 模型编码: glm-4.7-flash，绑定智谱AI渠道
  → 模型编码: glm-4-flash-250414，绑定智谱AI渠道

步骤5：创建虚拟 Key 并测试
  → 令牌管理 → 新增虚拟 Key
  → 允许模型: glm-4.7-flash, glm-4-flash-250414
  → 渠道: 自动调度（或指定智谱AI）
  → 额度: 按需设置

步骤6：验证调用
  → curl 测试（见 T1.2）
  → 检查 call_logs 有记录
  → 检查 quota_transactions 有扣减
```

### 5.3 智谱免费文本模型参考

| 模型 | 上下文窗口 | 输出上限 | 定位 | 特点 |
|------|-----------|---------|------|------|
| glm-4.7-flash | 200K | 128K | 首选免费通用文本模型 | 最新基座的普惠版本，适合首批对话接入 |
| glm-4-flash-250414 | 128K | 16K | 备选免费文本模型 | 免费、响应快，可做低成本回退 |

quota_weight 建议：
- glm-4.7-flash: 1.0（免费不等于不限额，先保持默认消耗规则）
- glm-4-flash-250414: 1.0（作为免费备选模型，同样保持默认规则）

---

## 6. 技术决策记录

### 6.1 流式方案：SseEmitter vs WebFlux

| 维度 | SseEmitter | WebFlux |
|------|-----------|---------|
| 改动量 | 小，现有 Spring MVC 架构不变 | 大，需引入 reactor 全家桶 |
| 学习成本 | 低 | 高 |
| 线程模型 | 阻塞线程（需配置线程池） | 非阻塞 |
| 适用场景 | 中等并发（<1000 并发流） | 高并发 |

**选择：SseEmitter**
理由：符合"实施简单、维护成本低"原则。当前是 Spring MVC 架构，引入 WebFlux 需要大量改造。配合 TaskExecutor 线程池可支撑中等规模并发。

### 6.2 告警通知方案：轮询 vs WebSocket

| 维度 | 前端轮询 | WebSocket |
|------|---------|-----------|
| 实现复杂度 | 极低 | 中 |
| 实时性 | 60 秒延迟 | 实时 |
| 服务端改动 | 1 个 GET 接口 | 需新增 WebSocket 配置 |

**选择：前端 60 秒轮询**
理由：告警不需要秒级实时性，60 秒延迟完全可接受。实现零成本。

### 6.3 导出方案：后端生成 CSV vs 前端生成

| 维度 | 后端 CSV | 前端 xlsx |
|------|---------|----------|
| 数据量 | 支持大量（流式输出） | 受限于浏览器内存 |
| 依赖 | 无（纯字符串拼接） | 需引入 xlsx.js |
| 格式 | CSV | Excel |

**选择：后端生成 CSV**
理由：无额外依赖，CSV 可被 Excel 直接打开，后端流式输出支持大数据量。

### 6.4 弱保留：模型市场方案

**选择：本地 DB + 管理员 CRUD**

当前状态：
- 保留方案，不纳入本轮主线
- 若后续恢复 T6.1，优先按本方案直接落地

理由（用户明确要求）：
- 不调外部 API → 无网络依赖，无 API 变动风险
- 管理员手动维护 → 数据完全可控
- 种子数据预置常用模型 → 开箱即用
- 未来如需自动同步，可在不改表结构的前提下增加定时任务

---

## 7. 文件变更清单

### 新增文件（主线）

| 文件路径 | 对应任务 |
|---------|---------|
| backend/.../controller/DashboardController.java | T5.1 |
| backend/.../service/DashboardService.java | T5.1 |
| backend/.../entity/AlertRule.java | T4.1 |
| backend/.../repository/AlertRuleRepository.java | T4.1 |
| backend/.../entity/AlertHistory.java | T4.1 |
| backend/.../repository/AlertHistoryRepository.java | T4.1 |
| backend/.../service/AlertRuleService.java | T4.1 |
| backend/.../service/AlertCheckService.java | T4.2 |
| backend/.../controller/AlertRuleController.java | T4.1 |
| frontend/src/api/dashboard.js | T5.2 |
| frontend/src/api/alert.js | T4.1 |
| frontend/src/views/AlertManagement.vue | T4.1 |

### 新增文件（备选）

| 文件路径 | 对应任务 |
|---------|---------|
| backend/.../entity/LoginLog.java | T7.2 |
| backend/.../repository/LoginLogRepository.java | T7.2 |
| frontend/src/views/Profile.vue | T7.1 |

### 修改文件（主线）

| 文件路径 | 对应任务 | 改动点 |
|---------|---------|--------|
| backend/.../resources/db/data.sql | T1.1 | 新增智谱免费模型种子数据 |
| backend/.../service/GatewayOrchestrationService.java | T2.1 | 去掉 stream=true 拦截，新增流式分支 |
| backend/.../service/ProxyForwardService.java | T1.2, T2.1 | 新增智谱路径适配 + 新增流式分支 |
| backend/.../controller/GatewayController.java | T2.1 | 新增 SSE 流式端点 |
| backend/.../controller/CallLogController.java | T3.1 | 新增 export 端点 |
| backend/.../service/CallLogQueryService.java | T3.1 | 新增 exportLogs 方法 |
| backend/.../controller/QuotaController.java | T3.2 | 新增 transactions export 端点 |
| frontend/src/views/ChannelManagement.vue | T1.1, T1.3 | 补 zhipu 类型选项 + 新增连通性测试按钮 |
| frontend/src/api/channel.js | T1.3 | 新增 testChannel 方法 |
| frontend/src/views/RequestLog.vue | T3.1, T3.3 | 新增导出按钮 + 增强详情弹窗 |
| frontend/src/api/log.js | T3.1 | 新增 exportLogs 方法 |
| frontend/src/views/QuotaFlow.vue | T3.2 | 新增导出按钮 |
| frontend/src/api/quota.js | T3.2 | 新增 exportTransactions 方法 |
| frontend/src/views/Overview.vue | T5.2 | 去掉硬编码，接入 API |
| frontend/src/router/index.js | T4.1, T7.3 | 新增告警路由、清理 skills 占位 |
| frontend/src/components/MainLayout.vue | T4.1, T7.3 | 新增告警菜单、清理 skills 菜单 |

### 修改文件（弱保留 / 备选恢复时）

| 文件路径 | 对应任务 | 改动点 |
|---------|---------|--------|
| backend/.../controller/AuthController.java | T7.2 | 登录时写 login_logs |
| frontend/src/router/index.js | T6.1, T7.1 | /models 和 /profile 路由改为真实页面 |
| frontend/src/views/ModelMarket.vue | T6.1 | 如现状不足则补齐模型市场真实管理交互 |

---

## 8. 不改什么

以下模块 v2.0 明确不动：

| 模块 | 理由 |
|------|------|
| 3 层额度漏斗 (VirtualKey→Team→Project) | 用户确认现有设计满足需求 |
| RBAC 角色体系 (SUPER_ADMIN/ADMIN/USER) | 不做自定义角色 (PRD 标注 P1) |
| 调度策略 (4种) | 已足够，不新增 |
| AES-256 加密方案 | 已满足安全要求 |
| RestTemplate 代理 | 不换 WebClient/OkHttp |
| MySQL + JPA 技术栈 | 不引入新存储 |
| Docker Compose 部署方案 | 不做 K8s |
| 前端 UI 设计体系 (玻璃态) | 保持一致 |

---

## 9. 验收检查表

### Phase 1 验收

- [ ] 智谱AI 渠道在渠道管理页面可见
- [ ] glm-4.7-flash 和 glm-4-flash-250414 在模型列表可见
- [ ] 填入真实 API Key 后，连通性测试通过
- [ ] curl 非流式调用成功返回结果
- [ ] call_logs 表有对应记录
- [ ] quota_transactions 表有扣减记录

### Phase 2 验收

- [ ] curl stream=true 调用逐行返回 SSE 数据
- [ ] 流式结束后 call_logs 有记录且 token 数正确
- [ ] 流式结束后额度正确扣减
- [ ] 上游超时/断开时客户端收到错误事件

### Phase 3 验收

- [ ] 日志列表点击"导出 CSV"下载文件，内容正确
- [ ] 额度流水点击"导出 CSV"下载文件，内容正确
- [ ] 日志详情弹窗展示调度链路信息
- [ ] 导出受 10000 条上限保护

### Phase 4 验收

- [ ] 告警规则 CRUD 正常（创建/编辑/删除/启停）
- [ ] 定时任务每分钟执行（查看日志确认）
- [ ] 触发条件满足时 alert_histories 有记录
- [ ] 同规则同目标 1 小时内不重复触发
- [ ] 顶栏铃铛显示未读告警数
- [ ] 点击铃铛展示最近告警列表

### Phase 5 验收

- [ ] Overview 首页 4 张卡片展示真实数据
- [ ] 趋势百分比与昨天对比正确
- [ ] 无调用数据时显示 0 而非硬编码值

### Phase 6 验收

- [ ] /skills 路由和菜单已移除
- [ ] 访问 /skills 时不会再进入占位页

### 弱保留项恢复验收（T6.1）

- [ ] /models 路由显示模型市场页面（非 Placeholder）
- [ ] 模型列表展示智谱免费模型和已有种子模型
- [ ] 管理员可新增/编辑/上下线模型

### 备选项恢复验收（T7.1 / T7.2）

- [ ] /profile 显示个人中心页面（非 Placeholder）
- [ ] 修改密码功能正常
- [ ] 登录成功/失败写入 login_logs
