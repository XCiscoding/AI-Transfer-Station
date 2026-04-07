# 项目经验总结：AI调度中心——企业API Key管理系统

## 文档信息

| 字段 | 内容 |
|------|------|
| **文档类型** | 经验总结与最佳实践 |
| **创建日期** | 2026-04-05 |
| **最后更新** | 2026-04-05 |
| **适用范围** | 开发团队、运维团队、后续迭代 |

---

## 目录

1. [启动脚本开发经验](#1-启动脚本开发经验)
2. [前端基础设施经验](#2-前端基础设施经验)
3. [后端服务经验](#3-后端服务经验)
4. [问题排查方法论](#4-问题排查方法论)
5. [预防措施清单](#5-预防措施清单)
6. [工具与命令参考](#6-工具与命令参考)

---

## 1. 启动脚本开发经验

### 1.1 PowerShell 脚本注意事项

#### ⚠️ 关键发现：命令解析差异

**问题现象**:
```powershell
# 在 PowerShell 中
Start-Process -FilePath "npm" -ArgumentList "run", "dev"
# 报错: %1 is not a valid Win32 application
```

**根因**:
- `Get-Command npm` 返回 `npm.ps1`（PowerShell 脚本）
- `Start-Process` 要求 `.exe/.cmd` 格式
- PowerShell 优先解析 `.ps1` 文件

**解决方案**:
```powershell
# ✅ 正确做法：显式指定 .cmd 扩展名
Start-Process -FilePath "npm.cmd" -ArgumentList "run", "dev"

# ✅ 替代方案：使用完整路径
Start-Process -FilePath "C:\Program Files\nodejs\npm.cmd" -ArgumentList "run", "dev"

# ✅ 替代方案：使用 cmd /c 包装
Start-Process -FilePath "cmd" -ArgumentList "/c", "npm", "run", "dev"
```

#### 📋 最佳实践

1. **显式指定文件扩展名**
   ```powershell
   # ❌ 不推荐
   Start-Process -FilePath "node"
   Start-Process -FilePath "npm"
   
   # ✅ 推荐
   Start-Process -FilePath "node.exe"
   Start-Process -FilePath "npm.cmd"
   ```

2. **添加错误处理**
   ```powershell
   try {
       $process = Start-Process -FilePath "npm.cmd" -ArgumentList "run", "dev" -PassThru -Wait
       if ($process.ExitCode -ne 0) {
           Write-Error "进程退出码: $($process.ExitCode)"
       }
   } catch {
       Write-Error "启动失败: $_"
   }
   ```

3. **环境验证前置**
   ```powershell
   # 验证命令存在性
   if (-not (Get-Command "npm.cmd" -ErrorAction SilentlyContinue)) {
       Write-Error "npm.cmd 未找到，请检查 Node.js 安装"
       exit 1
   }
   ```

4. **多环境测试**
   - ✅ CMD (Windows Command Prompt)
   - ✅ PowerShell 5.1+
   - ✅ PowerShell 7+
   - ✅ Git Bash

---

## 2. 前端基础设施经验

### 2.1 Vite 开发服务器配置

#### 代理配置要点

```javascript
// vite.config.js
export default defineConfig({
  server: {
    port: 5173,
    proxy: {
      // ✅ 正确配置：带路径重写
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
        ws: true,  // 支持 WebSocket
        // rewrite: (path) => path.replace(/^\/api/, '')  // 如需重写
      },
      
      // ✅ Swagger UI 代理
      '/swagger-ui': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      
      // ✅ OpenAPI 文档代理
      '/v3/api-docs': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

#### 常见问题

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| 接口 404 | 代理未配置或路径错误 | 检查 proxy 配置和路径匹配 |
| CORS 错误 | 后端未配置跨域 | 配置后端 CORS 或确保代理生效 |
| HMR 不工作 | WebSocket 未代理 | 添加 `ws: true` 配置 |

### 2.2 API 层设计

#### Axios 封装最佳实践

```javascript
// utils/request.js
import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api/v1',  // ✅ 使用相对路径，配合 Vite 代理
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code && res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      if (res.code === 401) {
        localStorage.removeItem('token')
        window.location.href = '/login'
      }
      return Promise.reject(new Error(res.message))
    }
    return res
  },
  error => {
    const { response } = error
    if (response) {
      switch (response.status) {
        case 401:
          ElMessage.error('登录已过期，请重新登录')
          localStorage.removeItem('token')
          window.location.href = '/login'
          break
        case 403:
          ElMessage.error('没有权限执行此操作')
          break
        case 500:
          ElMessage.error('服务器内部错误')
          break
        default:
          ElMessage.error(response.data?.message || '请求失败')
      }
    } else {
      ElMessage.error('网络连接失败')
    }
    return Promise.reject(error)
  }
)

export default request
```

#### API 模块化组织

```javascript
// api/auth.js - 认证相关
export function login(data) { ... }
export function getUserInfo() { ... }
export function logout() { ... }

// api/channel.js - 渠道管理
export function getChannelList(params) { ... }
export function createChannel(data) { ... }

// 统一导出
export * from './auth'
export * from './channel'
```

---

## 3. 后端服务经验

### 3.1 Spring Boot 配置

#### Redis 弹性配置

```yaml
# application.yml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password:
      database: 0
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
      timeout: 5000ms
      connect-timeout: 3000ms
  
  # 可选依赖降级
  autoconfigure:
    exclude: 
      # 如果 Redis 不可用，可以排除自动配置
      # - org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
```

#### CORS 跨域配置

```java
@Configuration
public class SecurityConfig {
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

### 3.2 健康检查端点

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always
```

访问：`GET /actuator/health`

---

## 4. 问题排查方法论

### 4.1 系统性排查流程

```
问题报告
    ↓
收集上下文（日志、截图、环境信息）
    ↓
复现问题（确认可稳定复现）
    ↓
分层定位（前端/后端/网络/环境）
    ↓
收集证据（命令输出、抓包、调试信息）
    ↓
根因分析（基于证据，禁止猜测）
    ↓
设计修复方案（多方案对比）
    ↓
实施最小修复
    ↓
多维度验证（语法/功能/集成/回归）
    ↓
文档化（Issue记录、经验总结）
```

### 4.2 常用排查命令

#### 网络连通性
```powershell
# 测试端口连通性
Test-NetConnection -ComputerName localhost -Port 5173
Test-NetConnection -ComputerName localhost -Port 8080

# 查看端口占用
Get-Process -Id (Get-NetTCPConnection -LocalPort 5173).OwningProcess
```

#### 进程管理
```powershell
# 查找 Node.js 进程
Get-Process -Name "node" -ErrorAction SilentlyContinue

# 结束进程
Stop-Process -Name "node" -Force
```

#### 环境验证
```powershell
# Node.js 环境
node --version
npm --version
Get-Command npm

# Java 环境
java -version
mvn -version

# Docker 环境
docker ps
docker logs <container_id>
```

#### 日志查看
```powershell
# Vite 日志
Get-Content "$env:TEMP\vite-stdout.log" -Tail 50
Get-Content "$env:TEMP\vite-stderr.log" -Tail 50

# Spring Boot 日志
tail -f backend/logs/application.log
```

### 4.3 分层排查清单

#### 前端层
- [ ] 浏览器控制台是否有 JS 错误？
- [ ] 网络面板请求状态码是什么？
- [ ] Vite 代理配置是否正确？
- [ ] API 请求路径是否正确？

#### 后端层
- [ ] 服务是否已启动？端口是否正确？
- [ ] 健康检查端点是否返回 200？
- [ ] 日志中是否有异常堆栈？
- [ ] 数据库连接是否正常？

#### 网络层
- [ ] 端口是否被占用？
- [ ] 防火墙是否放行？
- [ ] CORS 配置是否正确？
- [ ] 代理是否生效？

#### 环境层
- [ ] Node.js/Java 版本是否符合要求？
- [ ] 环境变量是否正确设置？
- [ ] 依赖包是否完整安装？
- [ ] 配置文件是否正确加载？

---

## 5. 预防措施清单

### 5.1 开发阶段

- [ ] **代码审查**: 所有脚本代码需经过审查，特别是系统调用部分
- [ ] **多环境测试**: 在 CMD、PowerShell、Git Bash 下测试脚本
- [ ] **错误处理**: 添加 try-catch 和友好的错误提示
- [ ] **日志记录**: 关键操作记录日志，便于排查
- [ ] **配置验证**: 启动时验证环境配置是否完整

### 5.2 测试阶段

- [ ] **单元测试**: 核心逻辑单元测试覆盖
- [ ] **集成测试**: 前后端联调测试
- [ ] **环境测试**: 在不同操作系统和环境下测试
- [ ] **异常测试**: 模拟各种异常情况（端口占用、服务未启动等）

### 5.3 部署阶段

- [ ] **健康检查**: 部署后执行健康检查
- [ ] **监控告警**: 配置服务监控和异常告警
- [ ] **回滚方案**: 准备快速回滚方案
- [ ] **文档更新**: 更新部署文档和运维手册

---

## 6. 工具与命令参考

### 6.1 快速诊断脚本

```powershell
# diagnose.ps1 - 系统诊断脚本
Write-Host "=== AI调度中心系统诊断 ===" -ForegroundColor Cyan

# 检查 Node.js
Write-Host "`n[1/6] 检查 Node.js 环境..." -ForegroundColor Yellow
if (Get-Command node -ErrorAction SilentlyContinue) {
    Write-Host "  ✓ Node.js: $(node --version)" -ForegroundColor Green
    Write-Host "  ✓ npm: $(npm --version)" -ForegroundColor Green
} else {
    Write-Host "  ✗ Node.js 未安装" -ForegroundColor Red
}

# 检查 Java
Write-Host "`n[2/6] 检查 Java 环境..." -ForegroundColor Yellow
if (Get-Command java -ErrorAction SilentlyContinue) {
    Write-Host "  ✓ Java: $(java -version 2>&1 | Select-String 'version' | ForEach-Object { $_.ToString().Split('"')[1] })" -ForegroundColor Green
} else {
    Write-Host "  ✗ Java 未安装" -ForegroundColor Red
}

# 检查端口
Write-Host "`n[3/6] 检查端口占用..." -ForegroundColor Yellow
$ports = @(5173, 8080, 3306, 6379)
foreach ($port in $ports) {
    $connection = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
    if ($connection) {
        $process = Get-Process -Id $connection.OwningProcess -ErrorAction SilentlyContinue
        Write-Host "  ⚠ 端口 $port 被占用 ($($process.ProcessName))" -ForegroundColor Yellow
    } else {
        Write-Host "  ✓ 端口 $port 可用" -ForegroundColor Green
    }
}

# 检查服务
Write-Host "`n[4/6] 检查服务状态..." -ForegroundColor Yellow
$services = @('MySQL', 'Redis')
foreach ($service in $services) {
    $svc = Get-Service -Name $service -ErrorAction SilentlyContinue
    if ($svc -and $svc.Status -eq 'Running') {
        Write-Host "  ✓ $service 运行中" -ForegroundColor Green
    } else {
        Write-Host "  ✗ $service 未运行" -ForegroundColor Red
    }
}

# 检查 Docker
Write-Host "`n[5/6] 检查 Docker 容器..." -ForegroundColor Yellow
if (Get-Command docker -ErrorAction SilentlyContinue) {
    $containers = docker ps --format "{{.Names}}" 2>$null
    if ($containers) {
        Write-Host "  运行中的容器:" -ForegroundColor Green
        $containers | ForEach-Object { Write-Host "    - $_" -ForegroundColor Gray }
    } else {
        Write-Host "  ⚠ 无运行中的容器" -ForegroundColor Yellow
    }
} else {
    Write-Host "  ✗ Docker 未安装" -ForegroundColor Red
}

# 检查项目文件
Write-Host "`n[6/6] 检查项目文件..." -ForegroundColor Yellow
$files = @('start.ps1', 'frontend/package.json', 'backend/pom.xml')
foreach ($file in $files) {
    if (Test-Path $file) {
        Write-Host "  ✓ $file" -ForegroundColor Green
    } else {
        Write-Host "  ✗ $file 缺失" -ForegroundColor Red
    }
}

Write-Host "`n=== 诊断完成 ===" -ForegroundColor Cyan
```

### 6.2 快速修复命令

```powershell
# 清理并重新安装前端依赖
Set-Location frontend
Remove-Item -Recurse -Force node_modules
Remove-Item -Force package-lock.json
npm install

# 清理 Maven 并重新构建
Set-Location backend
mvn clean install -DskipTests

# 重启所有服务
Stop-Process -Name "node" -Force -ErrorAction SilentlyContinue
Stop-Process -Name "java" -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 2
.\start.ps1
```

---

## 附录：已解决问题索引

| Issue ID | 问题描述 | 修复日期 | 状态 |
|----------|----------|----------|------|
| ISSUE-001 | Pageable import missing | 2026-03-30 | ✅ 已修复 |
| ISSUE-002 | JpaSpecificationExecutor missing | 2026-03-30 | ✅ 已修复 |
| ISSUE-003 | Docker Hub network failure | 2026-03-30 | ✅ 已修复 |
| ISSUE-004 | Startup script runtime errors | 2026-04-01 | ✅ 已修复 |
| ISSUE-005 | JPA schema validation failure | 2026-04-01 | ✅ 已修复 |
| ISSUE-006 | Port 8080 preflight missing | 2026-04-02 | ✅ 已修复 |
| ISSUE-007 | CORS 403 forbidden | 2026-04-02 | ✅ 已修复 |
| ISSUE-008 | Vite startup Win32 error | 2026-04-05 | ✅ 已修复 |

---

*本文档由项目总经理于 2026-04-05 创建，持续更新中*
