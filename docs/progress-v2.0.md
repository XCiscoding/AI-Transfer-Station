# AI调度中心 v2.0 进度记录

更新日期：2026-04-21

## 已完成任务

### T1.1 智谱渠道种子与模型配置

- 已在 `backend/src/main/resources/db/data.sql` 增加智谱渠道 `zhipu`
- 已增加 `glm-4.7-flash`、`glm-4-flash-250414` 两条 `model_configs`
- 已增加智谱渠道下的两条 `models` 种子
- 前端渠道类型静态枚举已补齐 `zhipu`

### T1.2 智谱代理适配

- `ProxyForwardService` 已支持按 `channelType/baseUrl` 判断智谱路径
- 智谱渠道请求会直接命中 `{baseUrl}/chat/completions`
- `GatewayOrchestrationService` 已向代理层传递 `channelType`

### T1.3 渠道连通性测试前端接入

- `frontend/src/api/channel.js` 已新增 `testChannel(id)`
- `frontend/src/views/ChannelManagement.vue` 已接入真实测试接口
- 渠道列表已展示 `healthStatus` 与 `healthCheckTime`
- 测试按钮已支持 loading 态和真实结果提示

## 2026-04-21 补充修复

### 启动脚本与数据库初始化职责收口

- `start.ps1` 不再根据库状态切换 Spring SQL 初始化模式
- `backend/src/main/resources/application.yml` 已固定 `spring.sql.init.mode=never`
- 空库场景下，`start.ps1` 会直接提示先执行 `init-db.ps1`，不再隐式初始化数据库

### 渠道管理页 API Key 交互修复

- 编辑渠道时，API Key 改为“留空保持不变”，不再强制重复输入
- 编辑弹窗会显示当前已保存的掩码值，避免“已保存但看起来像没保存”的误判
- 已通过接口回归确认：渠道 API Key 创建与更新都会正确加密落库并回显最新掩码

## 本轮额外修复的运行态阻塞

以下问题不是计划文档中的显式任务，但都是 T1.1/T1.2/T1.3 联调时暴露出的主阻塞，已一起修复：

1. 默认团队未写入 `team_members`
   - 已在 `data.sql` 增加默认 owner 成员记录

2. 默认项目缺少 `owner_id`
   - 已修复 `projects` 种子插入语句

3. 模型分组存储格式与服务层不一致
   - `ModelGroupService` 现已兼容 legacy `model_code` 数组
   - `data.sql` 会在 models 落库后把 `china-llm` 分组回填为实际 `modelIds`

4. 默认团队/项目额度为 0，导致三层额度漏斗直接拒绝请求
   - 已为默认团队和默认项目补充非 0 初始额度

5. `quota_transactions` 初始化表结构落后于实体定义
   - 已在 `schema.sql` 为 `quota_transactions` 补齐 `target_type`、`target_id` 和复合索引

6. 网关成功扣费后，`lastUsedTime` 更新覆盖掉虚拟 Key 主表的最新额度
   - 已将整实体 `save` 改为只更新 `lastUsedTime`

## 运行验证结果

### 基础验证

- 后端健康检查：`/actuator/health` 返回 `UP`
- 后端编译：`mvn -q -DskipTests compile` 通过
- 前端构建：`npm run build` 通过

### 智谱链路验证

- `GET /api/v1/model-groups/3/channels` 已返回智谱渠道
- `POST /api/v1/channels/3/test` 返回 `true`
- 通过本地 mock 智谱上游完成 `/v1/chat/completions` 非流式调用
- 返回内容：`mock zhipu ok`

### 数据库落库验证

本轮最终验证后数据库状态如下：

- `virtual_keys`
  - `quota_used = 9.00`
  - `quota_remaining = 99991.00`
  - `last_used_time` 已写入

- `call_logs`
  - 最新记录 `request_model = glm-4.7-flash`
  - `selected_model = glm-4.7-flash`
  - `total_tokens = 9`
  - `status = 1`

- `quota_transactions`
  - 已写入 3 条消耗流水：`virtual_key` / `team` / `project`

## 当前可手测状态

- 后端运行地址：`http://127.0.0.1:8080`
- 前端开发地址：`http://127.0.0.1:5173`

---

## 2026-04-21 新增完成：T2.1 前序收尾 + SSE 流式转发

### TokenManagement.vue「接入信息」弹窗

- 令牌管理页新增"接入信息"按钮
- 点击后弹出对话框，展示：API 端点、Authorization 格式、调用示例（含 curl 命令）
- 前端不需要后端新增接口，直接从当前 Key 记录拼接生成

### nginx.conf SSE 支持

- `deploy/nginx.conf` 新增 `/v1/` location
- `proxy_buffering off`，读/发/代理超时均设为 120s
- 确保 SSE 流式响应不被 nginx 缓冲截断

### vite.config.js 前端代理

- `/v1` 路径已代理至 `http://localhost:8080`
- 前端开发环境直接可访问 `/v1/chat/completions`

### T2.1 后端 SSE 流式转发

**核心改动（3 个文件）：**

**GatewayController.java**
- 返回类型改为 `Object`
- 当 `request.stream == true` 时走 `processChatCompletionStream()`，返回 `SseEmitter`
- 非流式仍走原 `processChatCompletion()`，返回 `ResponseEntity<ChatCompletionResponse>`

**ProxyForwardService.java**
- 新增 `forwardChatCompletionStream(channel, realKey, request, emitter)` 方法
- 使用 Java 11 原生 `java.net.http.HttpClient`（zero new dependencies）
- `HttpResponse.BodyHandlers.ofLines()` 逐行读取上游 SSE
- 每读到 `data: ...` 行，通过 `SseEmitter.event().data()` 转发给调用方
- `AtomicReference<UsageInfo>` 从最后一块中提取 usage 数据
- 上游非 200 响应抛 `BusinessException(502/4xx)`

**GatewayOrchestrationService.java**
- **删除**原先 `if (stream==true) throw BusinessException(400, "Streaming is not yet supported")` 拦截
- 新增 `processChatCompletionStream()` 方法
  - 主线程做同步预检（模型权限、限流、额度）
  - `VirtualKeyAuthContext.get()` 在主线程取得，传入 async 避免 ThreadLocal 丢失
  - `CompletableFuture.runAsync()` 异步发起流式转发
  - 流结束后：调用 `handleSuccess()` 扣费 + 写日志（构造含 usage 的合成 response）
  - 流失败时：调用 `handleFailure()`，向 emitter 推送错误事件后 complete

**技术选型说明：**
- 选 `SseEmitter` 而非 `WebFlux`（Spring MVC 架构不变，无新依赖）
- 超时 5 分钟（长对话场景）
- `AtomicBoolean emitterCompleted` 防止客户端断开后继续调 emit

## 验证结果

### T2.1 验证

```
curl -N --max-time 25 -X POST http://localhost:8080/v1/chat/completions
  -H "Authorization: Bearer sk-e745a6b4ea3b469880485138f67e43bb"
  -d '{"model":"glm-4.7","messages":[...],"stream":true}'
```

- 返回 33KB SSE 数据流，逐行 `data: {...}` 格式正确
- 最后收到 `data: [DONE]`
- `call_logs` 有对应记录，`quota_transactions` 有三层扣减流水
- 后端 PID 42340 当前持续运行中

### CC Switch 验证情况（已搁置）

- CC Switch 选 `OpenAI Chat Completions API` 格式后仍返回 502
- 后端日志确认：CC Switch 的请求根本未到达 localhost:8080（日志里无对应请求时间戳）
- 结论：CC Switch 内部走云端中转，不支持直连 localhost；这是工具限制，不是后端问题
- **已放弃 CC Switch 验证，curl 已完整验证流式链路，后续开发不依赖 CC Switch**

---

## 当前可手测状态（更新后）

- 后端运行地址：`http://127.0.0.1:8080`（PID 42340）
- 前端开发地址：`http://127.0.0.1:5173`
- 测试虚拟 Key：`sk-e745a6b4ea3b469880485138f67e43bb`
- 测试模型名：`glm-4.7`（DB 中的 model_code）

---

## 下一步任务（按计划文档）

Phase 3 / Phase 4 / Phase 5 彼此无依赖，可任意顺序推进：

| 优先级 | 任务 | 说明 |
|-------|------|------|
| 高 | T5.1 + T5.2 | Overview 首页真实数据（后端统计 API + 前端接入），改动面小，收益立竿见影 |
| 高 | T3.1 日志导出 | 后端新增 export 端点 + 前端导出按钮，复用现有查询逻辑 |
| 中 | T4.1 告警规则 CRUD | 需新建 AlertRule/AlertHistory entity + CRUD + 前端页面 |
| 中 | T3.2 流水导出 | 与 T3.1 同模式 |
| 低 | T3.3 日志详情增强 | 纯前端弹窗字段补充 |
| 低 | T4.2 告警引擎 | 定时任务，依赖 T4.1 完成后 |
| 低 | T4.3 告警通知展示 | 铃铛 Badge，依赖 T4.2 完成后 |
| 收口 | T7.3 清理 /skills 路由 | 与 T4.1 一起改 router/MainLayout，减少重复改文件 |
- 当前联调中的智谱渠道已临时指向本地 mock：`http://127.0.0.1:18080/api/paas/v4`
- 默认测试账号：`admin / admin123`

## 备注

- 当前验证范围为 T1.1 / T1.2 / T1.3 的非流式闭环
- `stream=true` 仍未实现，仍属于后续 T2.1 范围

---

## 2026-04-21 工作收尾记录

### 本轮总体状态

- 所有 T1.x 代码改动和修复已完成并编译/构建通过
- Mock 联调链路已闭环验证（call_logs + quota_transactions 均有记录）
- **真实智谱 API Key 端到端请求尚未验证**（联调期间 base_url 指向本地 mock，未切换为真实上游）

### 本轮额外修复（非计划任务，联调暴露）

1. **spring.sql.init.mode=always 每次重启清库** → 已固定为 never
2. **start.ps1 越权参与数据库初始化** → 已收口，职责下移到 init-db.ps1
3. **默认额度 quota=0 三层漏斗直接拒绝** → 已补充非零种子额度
4. **team_members / projects 种子数据不完整** → 已补齐 owner 和 owner_id
5. **model_groups.models 格式不一致** → 已补兼容逻辑 + 修正种子
6. **quota_transactions 表结构落后实体** → 已补 target_type/target_id 列
7. **GatewayOrchestrationService stale save 覆盖额度** → 已改为定向更新 lastUsedTime
8. **ChannelManagement.vue 编辑态 API Key 清空误导用户** → 已增加掩码展示和留空保持逻辑

### 当前可用状态

| 条件 | 状态 |
|------|------|
| 后端健康 `/actuator/health` | ✅ UP |
| 前端可访问 http://localhost:5173 | ✅ |
| 网关非流式链路（Mock 渠道）| ✅ 已验证 |
| 网关非流式链路（真实智谱）| ⚠️ 未验证，渠道 base_url 当前仍为 mock 地址 |
| 流式链路 stream=true | ❌ 返回 400，T2.1 未实现 |

### 下一步必做（接手后第一件事）

1. 把智谱渠道 base_url 改回 `https://open.bigmodel.cn/api/paas/v4`，配置真实 API Key
2. 跑真实 Key 冒烟测试（见 promote-v2.0.md「真实 Key 验证步骤」）
3. 验证通过后再开始 T2.1 流式支持

详细踩坑记录和项目导读见：`docs/promote-v2.0.md`

---

## 2026-04-22 完成：T3.1 / T3.2 / T3.3 / T4.1 / T4.2 / T4.3

### T3.1 — 日志 CSV 导出

- `CallLogQueryService.java` 新增 `exportLogsCsv()` 方法，复用现有查询过滤条件
- `CallLogVO.java` 补充 3 个字段：`isAutoMode`、`selectedModel`、`selectionStrategy`
- `CallLogController.java` 新增 `GET /api/v1/logs/export`，返回带 UTF-8 BOM 的 CSV（兼容 Excel）
- `frontend/src/api/log.js` 新增 `exportLogs(params)`，`responseType: 'blob'`
- `frontend/src/views/RequestLog.vue` 新增「导出 CSV」按钮，`handleExport()` 触发浏览器下载

### T3.2 — 流水 CSV 导出

- `QuotaService.java` 新增 `exportTransactionsCsv()`，带 safe()/safeEscape() 防 CSV 注入
- `QuotaController.java` 新增 `GET /api/v1/quota/transactions/export`
- `frontend/src/api/quota.js` 新增 `exportQuotaTransactions(params)`
- `frontend/src/views/QuotaFlow.vue` 新增「导出 CSV」按钮

### T3.3 — 日志详情弹窗增强

- `RequestLog.vue` 详情弹窗新增「调度模式」「实际模型」「选择策略」3 个字段展示

### T4.1 — 告警规则 CRUD（全新功能模块）

- 后端新建 5 个文件：`AlertRule.java`（实体）、`AlertHistory.java`（实体）、`AlertRuleRepository.java`、`AlertHistoryRepository.java`、`AlertRuleService.java`、`AlertRuleController.java`
- `AlertRuleController` 提供 6 个端点：列表 / 创建 / 更新 / 切换启用 / 软删除 / 历史分页
- 前端新建 `frontend/src/api/alert.js` + `frontend/src/views/AlertManagement.vue`（双 Tab：规则管理 + 告警历史）
- `router/index.js` 将原占位 `/skills` 路由替换为 `/alerts` → `AlertManagement.vue`（同时完成 T7.3）
- `MainLayout.vue` 侧边栏新增「告警管理」菜单项

### T4.2 — 告警触发引擎（定时调度）

- `AiKeyManagementApplication.java` 新增 `@EnableScheduling`
- `CallLogRepository.java` 新增 4 个统计查询方法（按时间/渠道/状态 count）
- 新建 `AlertCheckService.java`，`@Scheduled(fixedRate=60000)` 每分钟轮询所有启用规则：
  - `quota_low`：虚拟 Key 剩余额度占比 < threshold%
  - `channel_down`：渠道 healthStatus=0 或 failCount ≥ failThreshold
  - `call_volume`：1 分钟内调用量 > maxCount
  - `error_rate`：5 分钟内错误率 > maxRate
  - 同一规则 1 小时内不重复触发（依赖 `existsByRuleIdAndCreatedAtAfter` 去重）

### T4.3 — 告警通知前端展示

- `AlertHistoryRepository.java` 新增 `countByCreatedAtAfter`、`findTop10ByOrderByCreatedAtDesc`
- `AlertRuleService.java` 新增 `getUnreadCount()`（24 小时内告警数）、`getRecentAlerts()`
- `AlertRuleController.java` 新增 `GET /api/v1/alert-rules/unread-count` 和 `/recent`
- `frontend/src/api/alert.js` 新增 `getAlertUnreadCount`、`getRecentAlerts`
- `MainLayout.vue` 顶栏铃铛：
  - Badge 绑定真实未读数，每 60 秒轮询刷新
  - 点击展开 Dropdown，展示最近 10 条告警（级别标签 + 标题 + 时间）
  - 点击告警跳转至 `/alerts` 告警管理页

### 编译状态

- 后端 `mvn -DskipTests compile`：**BUILD SUCCESS**
- 前端无新依赖，无需重新 build

---

## 当前可手测状态（2026-04-22）

| 功能点 | 测试入口 |
|--------|---------|
| 日志 CSV 导出 | 前端「请求日志」页 → 可带过滤条件点「导出 CSV」 |
| 流水 CSV 导出 | 前端「额度流水」页 → 点「导出 CSV」 |
| 日志详情 3 新字段 | 「请求日志」→ 点任意一行「详情」按钮 |
| 告警规则 CRUD | 前端侧栏「告警管理」，可创建/编辑/删/启停规则 |
| 告警历史查看 | 「告警管理」→ 切换到「告警历史」Tab |
| 告警触发引擎 | 服务启动后每 60 秒自动运行，满足条件写入历史表 |
| 顶栏铃铛通知 | Badge 展示 24h 内告警数；点击下拉显示最近 10 条 |
