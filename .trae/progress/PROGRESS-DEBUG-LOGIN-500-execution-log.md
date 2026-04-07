# Debug执行日志 - 登录API返回500错误定位

## 任务信息
- **任务ID**: DEBUG-LOGIN-500
- **任务名称**: 后端登录API返回500错误根因定位
- **所属模块**: 认证模块 (Auth Module)
- **执行时间**: 2026-04-05
- **责任角色**: Debug工程师
- **问题现象**: POST http://localhost:8080/api/v1/auth/login 返回500错误

---

## 执行过程

### 阶段1: 问题复现 ✅
**操作内容**:
1. 使用Playwright发送登录请求复现问题
2. 请求参数: `{"username":"admin","password":"admin123"}`
3. 实际响应: `{"code":500,"message":"系统内部错误","data":null}`
4. **复现成功**: 确认500错误存在

### 阶段2: 代码审查 ✅
**操作内容**:
1. 审查 [AuthController.java](file:///c:/Users/26404/.trae-cn/Project/AI调度中心——企业API Key管理系统/backend/src/main/java/com/aikey/controller/AuthController.java) - 登录入口正常
2. 审查 [AuthService.java](file:///c:/Users/26404/.trae-cn/Project/AI调度中心——企业API Key管理系统/backend/src/main/java/com/aikey/service/AuthService.java) - 业务逻辑正常
3. 审查 [UserDetailsServiceImpl.java](file:///c:/Users/26404/.trae-cn/Project/AI调度中心——企业API Key管理系统/backend/src/main/java/com/aikey/service/UserDetailsServiceImpl.java) - 用户加载逻辑正常
4. 审查 [GlobalExceptionHandler.java](file:///c:/Users/26404/.trae-cn/Project/AI调度中心——企业API Key管理系统/backend/src/main/java/com/aikey/exception/GlobalExceptionHandler.java) - 异常处理正常

### 阶段3: 数据库配置审查 ✅
**操作内容**:
1. 检查 [application.yml](file:///c:/Users/26404/.trae-cn/Project/AI调度中心——企业API Key管理系统/backend/src/main/resources/application.yml) 数据库配置
2. 发现问题: `ddl-auto: validate` 模式不会自动执行schema.sql和data.sql
3. 检查 [schema.sql](file:///c:/Users/26404/.trae-cn/Project/AI调度中心——企业API Key管理系统/backend/src/main/resources/db/schema.sql) - 表结构定义完整
4. 检查 [data.sql](file:///c:/Users/26404/.trae-cn/Project/AI调度中心——企业API Key管理系统/backend/src/main/resources/db/data.sql) - 初始数据脚本完整

### 阶段4: 实体类与数据库映射审查 ✅
**操作内容**:
1. 检查 [User.java](file:///c:/Users/26404/.trae-cn/Project/AI调度中心——企业API Key管理系统/backend/src/main/java/com/aikey/entity/User.java) 实体类字段
2. 发现关键问题:
   - `login_fail_count` 字段标记为 `nullable = false` (第53行)
   - `deleted` 字段标记为 `nullable = false` (第68行)
   - `created_at` 字段标记为 `nullable = false` (第62行)
   - `updated_at` 字段标记为 `nullable = false` (第65行)
3. 检查data.sql中的INSERT语句 (第200-201行):
   ```sql
   INSERT INTO `users` (`username`, `password`, `email`, `real_name`, `status`, `is_locked`) VALUES
   ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'admin@example.com', '系统管理员', 1, 0);
   ```
4. **发现数据不一致**: INSERT语句缺少 `login_fail_count`, `deleted`, `created_at`, `updated_at` 字段

---

## 根因分析

### 主要根因: 数据库未初始化
**问题描述**:
1. application.yml中配置 `ddl-auto: validate`，该模式只验证数据库结构，不会自动创建表
2. 缺少 `spring.sql.init` 配置来执行schema.sql和data.sql
3. 数据库 `ai_key_management` 可能不存在或表结构未创建
4. 用户表中没有admin用户数据

### 次要问题: data.sql INSERT语句不完整
**问题描述**:
data.sql中的用户INSERT语句缺少以下非空字段:
- `login_fail_count` (INT NOT NULL DEFAULT 0)
- `deleted` (TINYINT NOT NULL DEFAULT 0)
- `created_at` (DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP)
- `updated_at` (DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP)

虽然数据库有默认值，但显式插入更安全。

---

## 修复方案

### 修复1: 添加SQL初始化配置 (application.yml)
**文件**: [application.yml](file:///c:/Users/26404/.trae-cn/Project/AI调度中心——企业API Key管理系统/backend/src/main/resources/application.yml)

在spring节点下添加:
```yaml
spring:
  # SQL初始化配置
  sql:
    init:
      mode: always
      schema-locations: classpath:db/schema.sql
      data-locations: classpath:db/data.sql
```

### 修复2: 修正data.sql中的INSERT语句
**文件**: [data.sql](file:///c:/Users/26404/.trae-cn/Project/AI调度中心——企业API Key管理系统/backend/src/main/resources/db/data.sql)

将第200-201行修改为:
```sql
INSERT INTO `users` (`username`, `password`, `email`, `real_name`, `status`, `is_locked`, `login_fail_count`, `deleted`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'admin@example.com', '系统管理员', 1, 0, 0, 0);
```

### 修复3: 手动初始化数据库 (临时方案)
如果无法修改配置，可以手动执行SQL脚本:
```bash
# 1. 创建数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS ai_key_management DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 2. 执行schema.sql
mysql -u root -p ai_key_management < backend/src/main/resources/db/schema.sql

# 3. 执行data.sql
mysql -u root -p ai_key_management < backend/src/main/resources/db/data.sql
```

---

## 影响范围

| 影响项 | 影响程度 | 说明 |
|--------|----------|------|
| 登录功能 | 🔴 致命 | 无法登录系统 |
| 用户认证 | 🔴 致命 | 所有需要认证的接口都无法使用 |
| 数据库结构 | 🟡 中等 | 需要确保DDL与实体类一致 |
| 其他模块 | 🟢 无 | 仅影响认证模块 |

---

## 回归验证建议

1. **验证数据库初始化**:
   ```sql
   SELECT * FROM users WHERE username = 'admin';
   ```

2. **验证登录API**:
   ```bash
   curl -X POST http://localhost:8080/api/v1/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"admin123"}'
   ```
   预期响应: `{"code":200,"message":"操作成功","data":{"token":"..."}}`

3. **验证其他认证接口**:
   - GET /api/v1/auth/me (需要Token)

---

## 风险与阻塞

| 风险项 | 状态 | 说明 |
|--------|------|------|
| 数据库连接 | ⚠️ 待确认 | 需要确认MySQL服务是否运行 |
| 数据库权限 | ⚠️ 待确认 | 需要确认root用户密码是否正确 |
| 数据一致性 | ⚠️ 待验证 | 修复后需要验证数据是否正确插入 |

---

## 建议下一步

1. **优先**: 修改application.yml添加SQL初始化配置
2. **其次**: 修正data.sql中的INSERT语句
3. **然后**: 重启后端服务，让Spring Boot自动执行SQL脚本
4. **最后**: 返回测试工程师进行登录功能回归验证

---

## 建议移交角色

项目总经理 - 协调修复实施并安排回归验证

---

*本日志由Debug工程师于2026-04-05创建*
