# ISSUE-006: 启动脚本端口8080预检缺失导致启动失败

## 问题基本信息
- **问题ID**: ISSUE-006
- **问题标题**: start.ps1缺少端口预检逻辑，导致重复启动时失败
- **所属模块**: 基础设施 / 启动脚本
- **严重程度**: 🔴 致命（阻塞系统启动）
- **发现时间**: 2026-04-04 20:30
- **发现者**: 用户反馈 + 项目总经理诊断
- **当前状态**: ✅ **已解决-测试通过**（2026-04-04 by Debug工程师 + 测试工程师验证）

---

## 问题描述

用户执行 `start.ps1` 脚本启动系统时，出现以下错误链：

### 错误现象（截图证据）
```
Web server failed to start. Port 8080 was already in use.

[INFO] BUILD FAILURE
[ERROR] Failed to execute goal org.springframework.boot:spring-boot-maven-plugin:3.2.5:run
(default-cli) on project ai-key-management: Process terminated with exit code: 1
```

### 复现步骤
1. 首次运行 `start.ps1` 成功启动系统（或手动启动Java进程）
2. 不关闭已运行的进程
3. 再次运行 `start.ps1`
4. 观察到BUILD FAILURE错误

---

## 根因分析

### error_stage: Executor（执行阶段）
### reason: 脚本设计缺陷 - 缺少端口预检机制

**详细分析：**

| 检测项 | 结果 | 证据 |
|--------|------|------|
| 端口8080占用情况 | ✅ PID **46816** 占用 | `netstat -ano \| findstr :8080` |
| 占用进程信息 | java.exe (74,472 K) | `tasklist /FI "PID eq 46816"` |
| Docker容器状态 | aikey-mysql ✅, aikey-redis ✅ | `docker ps -a` |
| Java进程总数 | 3个java.exe运行 | `tasklist \| findstr java` |

**代码缺陷定位：**
- **文件**: [start.ps1:337-370](../../start.ps1#L337-L370)
- **问题**: 第337行直接进入"Starting Spring Boot application"阶段，**未检查端口8080是否被占用**
- **后果**: Maven执行 `spring-boot:run` 时，Spring Boot尝试绑定8080端口失败 → 进程退出码1 → BUILD FAILURE

**缺失的逻辑：**
```powershell
# ❌ 当前代码：直接启动，无预检
Write-Step -Step 6 -Total 6 -Message "Starting Spring Boot application..."
& mvn spring-boot:run

# ✅ 应该有的逻辑：
Write-Step -Step 6 -Total 6 -Message "Checking port 8080..."
if (Test-Port -Port 8080) {
    # 提示用户并处理冲突
}
```

---

## 影响范围

| 影响项 | 说明 | 严重程度 |
|--------|------|----------|
| 系统启动 | 完全无法启动 | 🔴 致命 |
| 用户体验 | 错误信息不友好，难以理解根因 | 🟠 严重 |
| 开发效率 | 每次都需要手动查找并终止进程 | 🟡 中等 |

---

## 修复方案

### 方案A：自动检测+智能处理（推荐）
在 [start.ps1](../../start.ps1) 第337行前添加端口预检逻辑：

```powershell
Write-Step -Step 6 -Total 6 -Message "Checking port 8080 availability..."

if (Test-Port -Port 8080) {
    $pidOnPort = (netstat -ano | findstr ":8080.*LISTENING") -replace '.*\s+(\d+)$','$1'
    Write-Host "`n  [WARN] Port 8080 is already in use by PID $pidOnPort" -ForegroundColor $Yellow
    Write-Host "  This may be a previous instance of the application." -ForegroundColor $Yellow

    $choice = Read-Host "  Do you want to terminate it? (Y/N)"
    if ($choice -eq 'Y' -or $choice -eq 'y') {
        Stop-Process -Id $pidOnPort -Force -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 2
        Write-Host "  Process terminated." -ForegroundColor $Green
    } else {
        Write-Host "  Please manually free port 8080 and try again." -ForegroundColor $Red
        Read-Host "`nPress Enter to exit"
        exit 1
    }
}
```

### 方案B：仅提示+退出（保守）
仅检测并提示用户手动处理，不自动终止进程。

**推荐**: 方案A（更用户友好）

---

## 验证标准

- [ ] 再次运行 `start.ps1` 时能检测到端口占用
- [ ] 提示信息清晰易懂
- [ ] 用户选择Y后能成功终止旧进程
- [ ] 终止后系统能正常启动
- [ ] 不影响首次启动（端口未被占用时）

---

## 关联问题

- [ISSUE-004](./ISSUE-004-startup-script-runtime-errors.md): 上一次启动脚本错误（已修复）
- [ISSUE-005](./ISSUE-005-JPA-schema-validation-failure.md): JPA Schema验证失败（已修复）

---

## 经验教训

1. **防御性编程**: 任何涉及网络端口的操作都必须先进行可用性检查
2. **用户体验**: 错误信息必须清晰指出问题和解决方案，而不是抛出技术性异常
3. **幂等性考虑**: 启动脚本应该支持多次运行而不出错
4. **自动化测试**: 缺少对边界场景（如端口占用）的测试覆盖

---

## 修复责任人
- **角色**: Debug工程师
- **调度人**: 项目总经理
- **预计修复时间**: 立即

---

*本问题单由项目总经理于2026-04-04创建*
*基于用户截图和系统诊断结果*
