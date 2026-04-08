# AI调度中心 - 前后端分离部署指南

## 📋 部署架构

```
┌─────────────────────────────────────────────────────────────┐
│  🌐 Netlify (前端托管)                                       │
│  └── 域名: https://your-app.netlify.app                     │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼ API请求
┌─────────────────────────────────────────────────────────────┐
│  ☁️ 云服务器 (2核4G/3-5M带宽)                                │
│  ├── MySQL 8.0 (Docker)                                     │
│  ├── Redis 7 (Docker)                                       │
│  └── Spring Boot应用 (Docker)                               │
└─────────────────────────────────────────────────────────────┘
```

***

## 🚀 部署步骤

### 第一步：准备云服务器

#### 1.1 购买云服务器

推荐配置：2核4G，3-5M带宽，50G SSD

- 阿里云、腾讯云、华为云均可
- 操作系统：Ubuntu 22.04 LTS 或 CentOS 7/8

#### 1.2 安全组配置

开放以下端口：

| 端口   | 用途    | 访问来源  |
| ---- | ----- | ----- |
| 22   | SSH   | 你的IP  |
| 8080 | 后端API | 所有IP  |
| 3306 | MySQL | 建议仅内网 |
| 6379 | Redis | 建议仅内网 |

#### 1.3 连接服务器

```bash
ssh root@你的服务器IP
```

***

### 第二步：云服务器环境准备

#### 2.1 安装Docker

```bash
# Ubuntu/Debian
curl -fsSL https://get.docker.com | sh

# 或 CentOS
# curl -fsSL https://get.docker.com | sh

# 启动Docker
systemctl start docker
systemctl enable docker

# 验证安装
docker --version
docker-compose --version
```

如果docker-compose未安装：

```bash
# 安装docker-compose
curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose
```

#### 2.2 创建工作目录

```bash
mkdir -p /opt/aikey
cd /opt/aikey
```

***

### 第三步：部署后端服务

#### 3.1 上传部署文件

将以下文件上传到 `/opt/aikey` 目录：

- `docker-compose.prod.yml`
- `Dockerfile.backend`
- `.env` (从.env.example复制并修改)
- `init-db/` 目录（包含数据库初始化脚本）

使用SCP或SFTP上传：

```bash
# 本地执行，上传整个deploy目录
scp -r deploy/* root@你的服务器IP:/opt/aikey/
```

#### 3.2 配置环境变量

```bash
cd /opt/aikey
cp .env.example .env
nano .env  # 修改数据库密码和跨域配置
```

必须修改的项：

- `MYSQL_ROOT_PASSWORD` - MySQL root密码
- `MYSQL_PASSWORD` - MySQL应用用户密码
- `CORS_ALLOWED_ORIGINS` - 你的Netlify域名

#### 3.3 准备数据库初始化脚本

```bash
mkdir -p /opt/aikey/init-db
```

将后端项目中的数据库脚本复制到init-db目录：

```bash
# 如果你有schema.sql和data.sql
cp /path/to/backend/src/main/resources/db/*.sql /opt/aikey/init-db/
```

#### 3.4 启动服务

```bash
cd /opt/aikey

# 构建并启动
docker-compose -f docker-compose.prod.yml up -d --build

# 查看日志
docker-compose -f docker-compose.prod.yml logs -f

# 查看运行状态
docker-compose -f docker-compose.prod.yml ps
```

#### 3.5 验证后端部署

```bash
# 测试健康检查接口
curl http://localhost:8080/api/health

# 测试登录接口
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

***

### 第四步：部署前端到Netlify

#### 4.1 准备代码

确保你的代码已推送到GitHub/GitLab仓库。

#### 4.2 修改Netlify配置

编辑 `netlify.toml` 文件，修改API代理地址：

```toml
[[redirects]]
  from = "/api/*"
  to = "http://你的服务器IP:8080/api/:splat"  # ← 修改这里
  status = 200
  force = true
```

#### 4.3 部署到Netlify

**方式一：通过Git自动部署（推荐）**

1. 访问 [Netlify](https://app.netlify.com/)
2. 点击 "Add new site" → "Import an existing project"
3. 选择你的Git提供商（GitHub/GitLab）
4. 选择项目仓库
5. 构建设置：
   - Base directory: `frontend`
   - Build command: `npm run build`
   - Publish directory: `dist`
6. 点击 "Deploy site"

**方式二：手动部署**

```bash
# 本地构建
cd frontend
npm install
npm run build

# 安装Netlify CLI
npm install -g netlify-cli

# 登录并部署
netlify login
netlify deploy --prod --dir=dist
```

#### 4.4 配置环境变量（Netlify）

在Netlify控制台 → Site settings → Environment variables 中设置：

- `NODE_VERSION`: `20`
- `NPM_VERSION`: `10`

***

### 第五步：跨域配置

#### 5.1 修改后端跨域配置

后端需要允许Netlify域名访问。编辑 `.env` 文件：

```bash
CORS_ALLOWED_ORIGINS=https://your-app-name.netlify.app
```

然后重启后端：

```bash
cd /opt/aikey
docker-compose -f docker-compose.prod.yml restart backend
```

#### 5.2 验证跨域

打开浏览器访问你的Netlify地址，打开开发者工具(F12) → Network，检查API请求是否成功。

***

## 🔧 常用运维命令

### 查看日志

```bash
# 查看所有服务日志
docker-compose -f docker-compose.prod.yml logs

# 查看后端日志
docker-compose -f docker-compose.prod.yml logs -f backend

# 查看MySQL日志
docker-compose -f docker-compose.prod.yml logs -f mysql
```

### 重启服务

```bash
# 重启所有服务
docker-compose -f docker-compose.prod.yml restart

# 重启单个服务
docker-compose -f docker-compose.prod.yml restart backend
```

### 更新部署

```bash
# 拉取最新代码后重新构建
docker-compose -f docker-compose.prod.yml down
docker-compose -f docker-compose.prod.yml up -d --build
```

### 备份数据库

```bash
# 备份MySQL
docker exec aikey-mysql mysqldump -u root -p ai_key_management > backup_$(date +%Y%m%d).sql
```

### 进入容器

```bash
# 进入MySQL容器
docker exec -it aikey-mysql mysql -u root -p

# 进入Redis容器
docker exec -it aikey-redis redis-cli
```

***

## ⚠️ 安全建议

### 1. 修改默认密码

- [ ] MySQL root密码
- [ ] MySQL应用用户密码
- [ ] JWT密钥
- [ ] AES加密密钥

### 2. 安全组配置

- [ ] 限制3306端口仅内网访问
- [ ] 限制6379端口仅内网访问
- [ ] 限制22端口仅你的IP访问

### 3. 定期备份

```bash
# 创建备份脚本
cat > /opt/aikey/backup.sh << 'EOF'
#!/bin/bash
BACKUP_DIR="/opt/aikey/backups"
DATE=$(date +%Y%m%d_%H%M%S)
mkdir -p $BACKUP_DIR

# 备份数据库
docker exec aikey-mysql mysqldump -u root -pYourPassword ai_key_management > $BACKUP_DIR/db_backup_$DATE.sql

# 保留最近7天备份
find $BACKUP_DIR -name "db_backup_*.sql" -mtime +7 -delete
EOF

chmod +x /opt/aikey/backup.sh

# 添加到定时任务
crontab -e
# 添加: 0 2 * * * /opt/aikey/backup.sh
```

***

## 🐛 常见问题

### Q1: 前端无法连接后端

**检查清单：**

1. 云服务器安全组是否开放8080端口
2. netlify.toml中的API地址是否正确
3. 后端CORS配置是否包含Netlify域名
4. 云服务器防火墙是否放行8080端口

### Q2: 数据库连接失败

**检查清单：**

1. MySQL容器是否正常运行：`docker ps`
2. 数据库密码是否正确配置
3. 查看MySQL日志：`docker logs aikey-mysql`

### Q3: 部署后样式丢失

**解决：** 检查vite.config.js中的base配置，生产环境可能需要设置为 `./` 或 `/`

***

## 📞 获取帮助

如有问题，请检查：

1. 云服务器安全组配置
2. Docker容器日志
3. 浏览器开发者工具Network面板

