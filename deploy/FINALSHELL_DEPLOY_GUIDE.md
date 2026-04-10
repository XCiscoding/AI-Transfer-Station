# AI调度中心 - FinalShell 部署指南

## 📋 准备工作

### 1. 购买云服务器
- **配置**: 2核4G，3-5M带宽
- **系统**: Ubuntu 22.04 LTS（推荐）
- **厂商**: 阿里云、腾讯云、华为云均可

### 2. 配置安全组
在云服务控制台配置安全组，开放端口：

| 端口 | 用途 | 访问来源 |
|------|------|----------|
| 22 | SSH/FinalShell连接 | 你的IP |
| 8083 | Web访问 | 所有IP |

> 当前正式前端入口是 `http://服务器IP:8083`。不要再按 80 端口部署。

---

## 🚀 FinalShell 部署步骤

### 步骤1: 连接服务器

1. 打开 FinalShell
2. 点击 "连接" → "SSH连接"
3. 填写信息：
   - 名称: AI调度中心
   - 主机: 你的服务器公网IP
   - 端口: 22
   - 用户名: root
   - 密码: 你的root密码
4. 点击确定并连接

---

### 步骤2: 准备运行目录

```bash
mkdir -p /root/AI-center/releases
mkdir -p /root/AI-center/AI-Transfer-Station
```

说明：
- `/root/AI-center/releases`：GitHub Actions 上传的发布包和临时解压目录
- `/root/AI-center/AI-Transfer-Station`：当前线上运行目录

---

### 步骤3: 准备环境变量

```bash
cd /root/AI-center/AI-Transfer-Station/deploy
cp .env.example .env
nano .env
```

建议至少确认这些值：

```bash
SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/ai_key_management?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=root
SPRING_DATA_REDIS_HOST=host.docker.internal
SPRING_DATA_REDIS_PORT=6379
```

如果服务器上的 MySQL / Redis 账号或地址不同，按实际情况改。

---

### 步骤4: 自动部署链路说明

当前正式发布链路不是服务器自己 `git pull`，而是：

1. 本地 `git push origin main`
2. GitHub Actions 在 runner 上打包源码
3. 通过 SCP 上传发布包到服务器 `/root/AI-center/releases`
4. SSH 到服务器解压发布包
5. 使用：
   ```bash
   docker compose -f deploy/docker-compose.all-in-one.yml up -d --build
   ```
6. 自动验证：
   ```bash
   curl -I http://127.0.0.1:8083
   curl http://127.0.0.1:8083/api/health
   ```

这样可以绕开服务器访问 GitHub 不稳定的问题。

---

## 🔧 常用运维操作

### 查看服务状态
```bash
cd /root/AI-center/AI-Transfer-Station
docker compose -f deploy/docker-compose.all-in-one.yml ps
```

### 查看日志
```bash
docker compose -f deploy/docker-compose.all-in-one.yml logs -f
```

### 重启服务
```bash
cd /root/AI-center/AI-Transfer-Station
docker compose -f deploy/docker-compose.all-in-one.yml restart
```

### 重新构建
```bash
cd /root/AI-center/AI-Transfer-Station
docker compose -f deploy/docker-compose.all-in-one.yml up -d --build
```

---

## 🐛 常见问题

### Q1: GitHub Actions 报 Deploy to Server 失败
优先检查：

```bash
cd /root/AI-center/AI-Transfer-Station
docker compose -f deploy/docker-compose.all-in-one.yml ps
docker compose -f deploy/docker-compose.all-in-one.yml logs -f
```

再检查：

```bash
curl -I http://127.0.0.1:8083
curl http://127.0.0.1:8083/api/health
```

### Q2: 页面打不开
检查清单：
1. 安全组是否开放 8083
2. 容器是否正常运行：`docker ps`
3. 前端容器是否绑定 `0.0.0.0:8083->80/tcp`
4. 查看 Nginx 日志：`docker logs aikey-frontend`

### Q3: 端口冲突
不要在这台机器上混跑：
- 根目录 `docker-compose.yml`
- `deploy/docker-compose.prod.yml`

正式部署只用：
- `deploy/docker-compose.all-in-one.yml`

---

## ✅ 最终访问地址

```text
http://你的服务器IP:8083
```
