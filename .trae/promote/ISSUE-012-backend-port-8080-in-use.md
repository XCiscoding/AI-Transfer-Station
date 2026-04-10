# ISSUE-012 后端启动被 8080 端口占用阻塞

## 1. 错误基本信息

- 编号：ISSUE-012
- 日期：2026-04-10
- 场景：为核对 `admin` 身份真相启动后端运行态
- 命令：`mvn -f backend/pom.xml -DskipTests spring-boot:run`
- 当前状态：已记录，待处理

## 2. 原始报错

```text
APPLICATION FAILED TO START

Description:
Web server failed to start. Port 8080 was already in use.
```

## 3. 影响范围

- 后端本地运行态无法启动
- 当前无法继续执行：
  - `/api/v1/auth/me`
  - `/api/v1/teams`
- 间接阻塞 `admin` 被误识别为企业管理员问题的运行态验证

## 4. 初步怀疑点

1. 本地已有旧后端进程占用 8080
2. 其他服务占用 8080
3. 启动脚本 / 手工运行 / IDE 运行存在残留进程
4. 端口联动状态与文档不一致

## 5. 处理方案

### Step 1. 查 8080 被谁占用
确认占用进程 PID、进程名、启动来源。

### Step 2. 判断是否为本项目残留进程
如果是旧的 Spring Boot / Java 进程，进一步确认是否可直接复用，还是应停止后重启。

### Step 3. 再继续 admin 身份排查
只有端口恢复后，才能继续验证：
- `/api/v1/auth/me`
- `/api/v1/teams`
- 数据库角色 / owner 关系

## 6. 当前排查结果

已确认：
- 8080 占用进程 PID：`13952`
- 进程名：`java`
- 可执行文件：`C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot\bin\java.exe`
- 命令行包含：`backend\target\classes` 与 `com.aikey.AiKeyManagementApplication`

结论：
- 当前占用 8080 的不是外部陌生服务，而是**本项目已经有一个 Spring Boot 后端实例在运行**。
- 因此这次 `spring-boot:run` 失败的直接原因不是后端起不来，而是**重复启动**导致端口冲突。

对当前主问题的意义：
- 这说明本地其实已经存在可用运行态，后续应优先复用这个 8080 上的后端去核对 `admin` 的 `/auth/me` 与 `/teams`，而不是先继续重复拉起第二个实例。

## 7. 与现有问题单的关系

- 与 [ISSUE-011-admin-role-misclassification.md](ISSUE-011-admin-role-misclassification.md) 相关，但不是同一个问题
- ISSUE-011 是身份误判问题
- ISSUE-012 是排查 ISSUE-011 过程中新增暴露的环境阻塞问题

## 8. 记录规则

本问题按“每次报错都要记录”要求新增。后续若查明占用来源、解决方式、验证结果，应继续补充到本文件。
