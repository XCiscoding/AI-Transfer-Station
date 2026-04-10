# AI调度中心 - 单机全栈部署指南

## 📋 部署架构

当前这台服务器的正式部署方案只有一个：**单机 all-in-one**。

- 前端：Nginx 容器，对外端口 `8083`
- 后端：Spring Boot 容器，供前端 Nginx 反代访问
- 数据库：复用服务器现有 MySQL（默认 `3306`）
- Redis：复用服务器现有 Redis（默认 `6379`）
- 自动部署：GitHub Actions 打包源码 → SCP 上传到服务器 → SSH 解压并执行 `docker compose -f deploy/docker-compose.all-in-one.yml up -d --build`

访问地址：

- 前端：`http://服务器IP:8083`

---

## 🚀 快速部署步骤

### 第一步：准备云服务器

#### 1.1 购买云服务器
- **配置**: 2核4G，3-5M带宽，50G SSD
- **系统**: Ubuntu 22.04 LTS（推荐）或 CentOS 7/8
- **厂商**: 阿里云、腾讯云、华为云均可

#### 1.2 安全组配置
只需开放以下端口：

| 端口 | 用途 | 访问来源 |
|------|------|----------|
| 22 | SSH管理 | 你的IP |
| 8083 | 前端访问 | 所有IP |

> MySQL(3306) 和 Redis(6379) 由服务器本机提供给容器访问，不需要公网开放。

#### 1.3 连接服务器
```bash
ssh root@你的服务器IP
```

---

### 第二步：安装 Docker

```bash
curl -fsSL https://get.docker.com | sh
systemctl start docker
systemctl enable docker

docker --version
docker compose version
```

---

### 第三步：准备服务器目录

```bash
mkdir -p /root/AI-center/releases
mkdir -p /root/AI-center/AI-Transfer-Station
```

说明：
- `/root/AI-center/releases`：GitHub Actions 上传的发布包和解压目录
- `/root/AI-center/AI-Transfer-Station`：当前线上运行版本

---

### 第四步：准备环境变量

```bash
cd /root/AI-center/AI-Transfer-Station/deploy
cp .env.example .env
nano .env
```

至少确认这些配置：

```bash
SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/ai_key_management?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=root
SPRING_DATA_REDIS_HOST=host.docker.internal
SPRING_DATA_REDIS_PORT=6379
```

如果服务器 MySQL / Redis 不是这个地址或账号，按实际情况改。

---

### 第五步：GitHub Actions 自动部署

当前正式链路：

1. 本地提交并 push 到 `main`
2. GitHub Actions 在 runner 上打包源码
3. 通过 SCP 上传到服务器 `/root/AI-center/releases`
4. 通过 SSH 解压发布包
5. 切换运行目录并执行：
   ```bash
   docker compose -f deploy/docker-compose.all-in-one.yml up -d --build
   ```
6. workflow 内自动验证：
   ```bash
   curl -I http://127.0.0.1:8083
   curl http://127.0.0.1:8083/api/health
   ```

这个链路**不再依赖服务器自己 `git pull`**，避免服务器访问 GitHub 不稳定导致部署失败。

---

## 🔧 常用运维命令

### 查看服务状态
```bash
cd /root/AI-center/AI-Transfer-Station
docker compose -f deploy/docker-compose.all-in-one.yml ps
```

### 查看日志
```bash
cd /root/AI-center/AI-Transfer-Station
docker compose -f deploy/docker-compose.all-in-one.yml logs -f
```

### 重启服务
```bash
cd /root/AI-center/AI-Transfer-Station
docker compose -f deploy/docker-compose.all-in-one.yml restart
```

### 重新构建并启动
```bash
cd /root/AI-center/AI-Transfer-Station
docker compose -f deploy/docker-compose.all-in-one.yml up -d --build
```

---

## ⚠️ 重要规则

这台服务器上不要再混用以下部署方式：

- 根目录 `docker-compose.yml`
- `deploy/docker-compose.prod.yml`

原因：
- 根 compose 不是完整前后端栈
- `deploy/docker-compose.prod.yml` 会占用 `3306:3306`、`6379:6379`，容易和服务器现有服务冲突
- 当前正式入口是 `http://服务器IP:8083`，应只使用 `deploy/docker-compose.all-in-one.yml`

---

## ✅ 验证清单

部署成功后检查：

```bash
docker compose -f deploy/docker-compose.all-in-one.yml ps
curl -I http://127.0.0.1:8083
curl http://127.0.0.1:8083/api/health
```

浏览器访问：

```text
http://你的服务器IP:8083
```

如果页面能打开，且 `/api/health` 正常返回，说明部署链路已恢复。
