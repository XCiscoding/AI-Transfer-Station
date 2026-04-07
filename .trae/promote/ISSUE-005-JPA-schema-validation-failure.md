# ISSUE-005: JPA Schema Validation 失败 - 实体类与数据库列类型不匹配

## 问题信息
- **问题ID**: ISSUE-005
- **标题**: Spring Boot 启动时 JPA Schema Validation 失败，实体类 Integer 字段与数据库 TINYINT 列不匹配
- **所属模块**: 应用层 / JPA实体类
- **严重程度**: 🔴 致命（导致应用完全无法启动）
- **报告日期**: 2026-04-04
- **状态**: ✅ 已修复（16个字段全部解决）
- **关联任务**: DEBUG-STARTUP-001 后续修复

---

## 问题描述

在修复启动脚本（ISSUE-004）后，Spring Boot 应用启动过程中出现 JPA/Hibernate Schema Validation 异常，导致应用无法启动。

### 错误信息
```
Schema-validation: wrong column type encountered in column [deleted] in table [channels]; 
found [tinyint (Types#TINYINT)], but expecting [integer (Types#INTEGER)]

（首次修复后暴露第二个同类错误：）
Schema-validation: wrong column type encountered in column [health_status] in table [channels]; 
found [tinyint (Types#TINYINT)], but expecting [integer (Types#INTEGER)]
```

### 触发条件
- `spring.jpa.hibernate.ddl-auto: validate` 模式下
- Hibernate 验证实体类定义与数据库 schema 是否一致时发现类型不匹配

---

## 根因分析

### 核心冲突

| 维度 | Java 实体类 | MySQL 数据库 (schema.sql) | Hibernate 映射 |
|------|-----------|------------------------|---------------|
| **Java 类型** | `Integer` (32位) | — | 默认映射为 `INTEGER` (4字节) |
| **DB 类型** | — | `TINYINT` (8位, 1字节) | — |
| **结果** | ❌ 类型不匹配 | | validate 模式抛出异常 |

### 影响范围（系统性问题）

**不是单个字段的问题**，而是整个项目的设计模式不一致：

schema.sql 中使用 `TINYINT` 的场景：
- 所有表的 `deleted` 列（逻辑删除标志）— 14张表
- 所有表的 `status` 列（启用/禁用状态）— 多张表
- 布尔语义字段：`is_locked`, `is_system`, `is_enabled`, `is_public`, `is_auto_mode`, `is_fallback`, `login_status`, `user_feedback`, `health_status`

Java 实体类中统一使用 `Integer` 类型声明这些字段，但未指定 `columnDefinition = "TINYINT"`。

### 为什么之前没发现？
启动脚本的运行时错误（ISSUE-004）掩盖了这个问题。脚本在 Spring Boot 启动前就退出了，所以 JPA Validation 从未执行过。

---

## 修复方案

### 方案选择

| 方案 | 描述 | 优点 | 缺点 |
|------|------|------|------|
| A: 改 ddl-auto 为 none/update | 跳过或自动修正验证 | 最快 | 可能隐藏真实 schema 问题 |
| B: 修改 schema.sql 全部改为 INT | 统一数据库端 | 一致性好 | 需要重建数据库 |
| **C: 给实体类加 columnDefinition (采用)** | 显式指定 SQL 类型 | 无需改DB、精确控制 | 需修改多个文件 |

### 实施内容

在所有对应 TINYINT 列的实体类字段上添加 `columnDefinition = "TINYINT"`：

#### 第一批修复（7个 deleted 字段）
| 文件 | 字段 | 行号 |
|------|------|------|
| [Channel.java](../backend/src/main/java/com/aikey/entity/Channel.java) | deleted | L83 |
| [User.java](../backend/src/main/java/com/aikey/entity/User.java) | deleted | L69 |
| [Role.java](../backend/src/main/java/com/aikey/entity/Role.java) | deleted | L51 |
| [Model.java](../backend/src/main/java/com/aikey/entity/Model.java) | deleted | L70 |
| [VirtualKey.java](../backend/src/main/java/com/aikey/entity/VirtualKey.java) | deleted | L87 |
| [RealKey.java](../backend/src/main/java/com/aikey/entity/RealKey.java) | deleted | L63 |
| [Permission.java](../backend/src/main/java/com/aikey/entity/Permission.java) | deleted | L57 |

#### 第二批修复（9个 status/isXxx 字段）
| 文件 | 字段 | 对应SQL列 |
|------|------|-----------|
| Channel.java | status, health_status | status TINYINT, health_status TINYINT |
| User.java | status, is_locked | status TINYINT, is_locked TINYINT |
| Role.java | is_system, status | is_system TINYINT, status TINYINT |
| Model.java | status | status TINYINT |
| RealKey.java | status | status TINYINT |
| VirtualKey.java | status | status TINYINT |

**总计：7个实体类 × 16处字段注解修改**

---

## 验证结果

### 修复前
```
[ERROR] BUILD FAILURE
[ERROR] Process terminated with exit code: 1
Service stopped.
```

### 修复后
```
Started AiKeyManagementApplication in 7.06 seconds (process running for 7.435)
Tomcat started on port 8080 (http) with context path ''
Initialized JPA EntityManagerFactory for persistence unit 'default'

GET http://localhost:8080/actuator/health → {"status":"UP"} (HTTP 200)
```

### 验收标准检查清单
- [x] deleted 字段 TINYINT 不匹配已修复（7个字段）
- [x] status/isLocked/isSystem/healthStatus 等 TINYINT 不匹配已修复（9个字段）
- [x] Spring Boot 成功启动，无 SchemaManagementException
- [x] JPA EntityManagerFactory 正常初始化
- [x] HikariPool 连接池成功连接 MySQL
- [x] Tomcat 在 8080 端口正常监听
- [x] Actuator /health 端点返回 UP
- [x] 系统稳定运行中（无异常退出）

---

## 修改文件清单

| 文件 | 修改数 | 说明 |
|------|--------|------|
| [Channel.java](../backend/src/main/java/com/aikey/entity/Channel.java) | 3处 | deleted + status + healthStatus |
| [User.java](../backend/src/main/java/com/aikey/entity/User.java) | 3处 | deleted + status + isLocked |
| [Role.java](../backend/src/main/java/com/aikey/entity/Role.java) | 3处 | deleted + isSystem + status |
| [Model.java](../backend/src/main/java/com/aikey/entity/Model.java) | 2处 | deleted + status |
| [VirtualKey.java](../backend/src/main/java/com/aikey/entity/VirtualKey.java) | 2处 | deleted + status |
| [RealKey.java](../backend/src/main/java/com/aikey/entity/RealKey.java) | 2处 | deleted + status |
| [Permission.java](../backend/src/main/java/com/aikey/entity/Permission.java) | 1处 | deleted |

---

## 经验总结

### 教训
1. **JPA 实体类设计规范缺失**: 项目缺少统一的字段类型映射规范。建议制定"布尔语义字段统一使用 `Boolean` 或带 `columnDefinition="TINYINT"` 的 `Integer`"的编码标准
2. **渐进式测试的重要性**: 如果只修复第一个 `deleted` 报错就停止，会遗漏另外 15 个同类问题。必须一次性系统性排查所有同模式问题
3. **ddl-auto:validate 是把双刃剑**: 它能捕获真实的 schema 偏差，但也要求开发时保持 entity 与 DDL 的严格同步

### 正面案例
1. **根因到修复的快速闭环**: 从截图分析 → 定位根因 → 批量修复 → 验证成功，全程约30分钟
2. **系统思维避免反复**: 识别出这是系统性问题而非个案后，一次性修复全部16个字段，避免了多次迭代

---

## 相关文档
- [ISSUE-004](ISSUE-004-startup-script-runtime-errors.md): 启动脚本运行时异常退出（前置依赖）
- [PROGRESS-DEBUG-STARTUP-001](../progress/PROGRESS-DEBUG-STARTUP-001-execution-log.md): 启动脚本修复执行日志

---
*本问题单由项目总经理于2026-04-04创建*
*最后更新: 2026-04-04*
*状态: ✅ 已修复（16/16个字段全部解决，系统正常运行）*
