# AI调度中心 v2.0 报错日志

> 本文件只记录运行态 bug：现象、根因、修复代码、教训。
> 项目总结见 SUMMARY.md，任务进度见 progress-v2.0.md。

更新日期：2026-04-21

---

## 踩坑记录

### 坑1：spring.sql.init.mode=always 导致每次重启清库

**现象**：重启后端，虚拟 Key / 真实 Key / 渠道等手动创建的数据全消失。

**根因**：`application.yml` 原配置 `spring.sql.init.mode=always`，而 `schema.sql` 开头有大量 `DROP TABLE IF EXISTS`，每次启动都清库重建。

**错误路径**：
```
Spring Boot 启动
 → spring.sql.init.mode=always
   → 执行 schema.sql
     → DROP TABLE IF EXISTS channels / virtual_keys / real_keys ...
       → 所有运行态数据清空
```

**修复**：
- `application.yml`: `spring.sql.init.mode: never`（固定关闭，不依赖环境变量）
- 数据库初始化只走 `.\init-db.ps1`（显式触发）
- `start.ps1` 不再参与任何初始化，只做就绪检查

**教训**：Spring SQL init 和带 DROP 的 schema.sql 组合是定时炸弹。生产项目首选 Flyway 版本管理；如果用 spring.sql.init，schema.sql 不能有 DROP TABLE。

---

### 坑2：种子数据 quota=0，三层额度漏斗直接拒绝所有请求

**现象**：网关鉴权通过，但请求直接被拒绝，日志显示 `QUOTA_EXCEEDED`。

**根因**：`data.sql` 默认团队和默认项目的 `quota_total` / `quota_remaining` 为 0，三层预检第一轮就失败。

**修复**：在 data.sql 补充非零初始额度：
```sql
UPDATE teams SET quota_total=100000, quota_remaining=100000 WHERE id=1;
UPDATE projects SET quota_total=100000, quota_remaining=100000 WHERE id=1;
```

**教训**：种子数据必须是「可用状态」，不能只建结构。每次新增业务约束字段，都要回检 data.sql 里对应字段是否为合理默认值，而不是 0 或 NULL。

---

### 坑3：model_groups.models 存储格式不一致

**现象**：模型分组接口返回空，或绑定分组后调度层找不到模型。

**根因**：历史数据中 `model_groups.models` 存的是 `modelCode` 字符串数组（如 `["glm-4.7-flash"]`），但服务层 `ModelGroupService` 期望的是 `modelId` 整数数组。

**修复**：
- `ModelGroupService` 增加 legacy 兼容分支，能识别字符串格式并自动查 ID
- `data.sql` 中新增分组的 models 字段改用真实 `modelIds`：先插 models 行再用子查询回填 ID

**教训**：JSON 列里存了数组，一定要在种子数据注释里标注格式约定（存 ID 还是 Code）。格式变更后要同步检查所有读取方。

---

### 坑4：quota_transactions 表结构落后于实体定义

**现象**：网关扣费成功，但 call_logs 有记录，quota_transactions 没有对应流水，偶现 500。

**根因**：`schema.sql` 中 `quota_transactions` 表缺少 `target_type` 和 `target_id` 列，但 Entity 里已经有。JPA 启动不报错（`ddl-auto=none`），直到运行态写入时才 500。

**修复**：在 `schema.sql`（及 `patch_v2.sql`）补齐两列和复合索引：
```sql
ALTER TABLE quota_transactions
  ADD COLUMN target_type VARCHAR(32) AFTER id,
  ADD COLUMN target_id BIGINT AFTER target_type;
```

**教训**：`ddl-auto=none` 下，schema 和 Entity 不一致只在运行时暴露。开发时每次给 Entity 加字段，必须同步更新 schema.sql；或用 Flyway 做增量迁移（推荐，到 Phase 2 后迁移进去）。

---

### 坑5：GatewayOrchestrationService 用整实体 save 覆盖额度

**现象**：调用成功，call_logs 有记录，但 virtual_keys.quota_used / quota_remaining 回到了旧值（或本次扣减没有持久化）。

**根因**：`GatewayOrchestrationService` 在 `deductQuotaWithFunnel` 之后，又调用了 `virtualKeyRepository.save(virtualKey)`，传入的是调用前加载的 stale 实体对象，覆盖了扣费后的最新值。

**修复**：将整实体 save 改为定向更新只写 `lastUsedTime`：
```java
virtualKeyRepository.updateLastUsedTime(virtualKey.getId(), LocalDateTime.now());
```

**教训**：数据库原子操作（如 `UPDATE ... SET quota_remaining = quota_remaining - ?` WHERE id=?）之后，绝不能再 save 调用前加载的旧实体。高并发场景下这是经典丢更新 bug，低并发下也会覆盖本轮扣减。

---

### 坑6：默认 team_members / projects 种子数据不完整

**现象**：创建虚拟 Key 时返回 403，或项目接口返回 500。

**根因1**：`team_members` 没有插入 admin 对应的记录，团队长为空，`isTeamOwner` 判断失败。

**根因2**：`projects` 表种子插入缺了 `owner_id`，不满足 NOT NULL 约束。

**修复**：在 data.sql 补齐：
```sql
INSERT INTO team_members (team_id, user_id, role, join_time) VALUES (1, 1, 'owner', NOW());
INSERT INTO projects (..., owner_id) VALUES (..., 1);
```

**教训**：每张表的种子必须过一遍 NOT NULL 和外键约束，不能光靠开发时"感觉没问题"。建议写完 data.sql 就做一次 `init-db.ps1` 全量跑，用后端启动能不能通来兜底验证。

---

### 坑7：ProxyForwardService URL 硬编码，智谱路径不兼容

**现象**：转发请求给智谱时返回 404，上游日志显示请求路径是 `/v1/chat/completions` 而不是 `/chat/completions`。

**根因**：原 `ProxyForwardService.buildUrl()` 对所有渠道都拼 `/v1/chat/completions`（OpenAI 风格），而智谱 base_url 已包含 `/api/paas/v4`，正确路径应拼为 `/api/paas/v4/chat/completions`。

**修复**：在 `buildUrl()` 增加渠道类型判断：
```java
boolean isZhipu = "zhipu".equalsIgnoreCase(channelType)
    || normalizedBase.contains("/api/paas/v4");
return isZhipu
    ? (normalizedBase.endsWith("/") ? normalizedBase.substring(0, normalizedBase.length()-1) : normalizedBase) + path
    : normalizedBase + "/v1" + path;
```

**教训**：接入新厂商前，先看它的 API 路径规范是否与 OpenAI 兼容（路径+鉴权方式两项都检查）。不兼容的部分做渠道类型 switch，不要对所有渠道走同一 URL 模板。

---

### 坑8：ChannelManagement.vue 编辑态 API Key 清空，用户误判为"没保存"

**现象**：渠道管理页面编辑渠道后重新打开弹窗，API Key 输入框为空，用户认为没保存成功。

**根因**：编辑渠道时前端把 apiKey 清空（安全做法），但没有展示「当前已保存掩码」，且表单校验规则依然要求必填 → 用户看到空框 + 被提示必填，自然认为 Key 没有持久化。

**实际后端状态**：API Key 从未丢失，`channels.api_key_encrypted` 一直有值。

**修复**：
- 编辑弹窗展示 `当前已保存：sk-***...***890，留空表示保持不变`
- API Key 改为编辑态可留空，留空时不提交该字段
- 新增时仍保持必填校验

**教训**：安全实践（编辑不回显敏感字段）必须配合交互提示，否则用户无法区分「空框=没保存」vs「空框=有值但不展示」。每个「有意为之的空」都要有视觉说明。

---

### 坑9：Mock 联调通了 ≠ 真实 Key 通了

**现象**：Mock 验证网关链路走通，但切换真实 Zhipu Key 后未做验证即宣称"通了"。

**根因**：联调时 data.sql 里智谱渠道的 base_url 临时改成了本地 Mock 地址（`http://127.0.0.1:18080/api/paas/v4`），并没有改回生产地址；此时的"调通"只是调通了 Mock，没有经过真实上游。

**当前剩余工作**：参见上方「真实 Key 验证步骤」，需要把渠道 base_url 改回并配置真实 API Key 后完整跑一遍。

**教训**：Mock 联调结束后，必须有一个检查节点——确认渠道 base_url 已还原，并用真实 Key 跑一遍完整链路。不能跳过这步就关闭任务。建议把「真实 Key 冒烟测试」列为独立验收条件。

---

### 坑10：stream=true 的 400 拦截隐藏后导致 CC Switch 报 502 而非 400

**现象**：CC Switch 连接我们网关时显示 502，没有更多错误信息。

**根因**：`GatewayOrchestrationService` 原来有一段拦截：
```java
if (Boolean.TRUE.equals(request.getStream())) {
    throw new BusinessException(400, "Streaming is not yet supported.");
}
```
CC Switch 等客户端发 stream=true 时，后端返回 400 JSON；但某些客户端（或 CC Switch 内部中转层）会把 4xx 包装成 502 呈现给用户，导致诊断时误以为是代理/网络问题。

**修复**：实装 T2.1 SSE 流式转发后，该拦截已删除。stream=true 现在走真实流式路径。

**教训**：「功能未实现时抛 400」的占位代码，日后容易被工具层错误包装成 502，让排查方向跑偏。遇到 502 时，先在后端日志里确认请求是否真正到达，再判断是网络/代理问题还是后端主动抛错。

---

### 坑11：PowerShell 内联 JSON 单引号会被 curl 吃掉导致 JSON 解析失败

**现象**：在 PowerShell 里用 `curl.exe -d '{"model":...}'` 测试时后端返回：
```
com.fasterxml.jackson.core.JsonParseException: Unexpected character 'm'
```

**根因**：PowerShell 的单引号字符串不做变量替换，但 `curl.exe`（Windows 原生）处理参数时会把外层单引号去掉，导致 JSON 里的双引号被进一步处理，最终 Body 变成 `{model:...}` 这类不合法格式。

**修复**：写 JSON 到临时文件，用 `--data-binary @file` 传入：
```powershell
'{"model":"glm-4.7",...}' | Out-File -Encoding utf8 -FilePath "$env:TEMP\req.json" -NoNewline
curl.exe -X POST ... --data-binary "@$env:TEMP\req.json"
```

**教训**：在 PowerShell 里测 HTTP，凡是包含 JSON 的请求，一律用临时文件传 Body，不要直接内联 `-d '{...}'`。

---

### 坑12：DB 中 model_code 是 `glm-4.7`，不是 `glm-4-flash` 或 `glm-4.7-flash`

**现象**：curl 测试时返回 `{"error": "Assigned channel does not support model: glm-4-flash"}`。

**根因**：DB `models` 表里实际插入的 model_code 为 `glm-4.7`（种子数据），但测试时用了 `glm-4-flash` 或 `glm-4.7-flash`，名字对不上。

**修复**：curl 测试与 CC Switch 配置均改用 `glm-4.7`。

**教训**：每次测试前先查一遍 DB 里的 model_code 实际值：
```sql
SELECT model_code FROM models WHERE deleted=0;
```
不要凭印象猜模型名，尤其是厂商模型名经常有多个变体。
