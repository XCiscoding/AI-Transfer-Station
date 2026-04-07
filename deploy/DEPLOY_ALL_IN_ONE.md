# AI调度中心 - 单机全栈部署指南

## 📋 部署架构

所有服务部署在**一台云服务器**上，通过Docker容器化运行：

```
┌─────────────────────────────────────────────────────────────┐
│  ☁️ 云服务器 (2核4G/3-5M带宽)                                │
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   Nginx      │  │  Spring Boot │  │   MySQL 8.0  │      │
│  │   (前端)     │◄─┤   (后端)     │◄─┤   (数据库)   │      │
│  │   端口:80    │  │   端口:8080  │  │   端口:3306  │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│         │                 │                                 │
│         └─────────────────┘                                 │
│                    │                                        │
│            ┌──────────────┐                                 │
│            │  Redis 7     │                                 │
│            │  (缓存)      │                                 │
│            │  端口:6379   │                                 │
│            └──────────────┘                                 │
│                                                             │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    用户浏览器访问
                    http://服务器IP
```

---

## 🚀 快速部署步骤

### 第一步：准备云服务器

#### 1.1 购买云服务器
- **配置**: 2核4G，3-5M带宽，50G SSD
- **系统**: Ubuntu 22.04 LTS（推荐）或 CentOS 7/8
- **厂商**: 阿里云、腾讯云、华为云均可

#### 1.2 安全组配置
只需开放**2个端口**：

| 端口 | 用途 | 访问来源 |
|------|------|----------|
| 22 | SSH管理 | 你的IP |
| 80 | Web访问 | 所有IP |

> 💡 **注意**: MySQL(3306)和Redis(6379)不对外暴露，仅容器内网访问，更安全！

#### 1.3 连接服务器
```bash
ssh root@你的服务器IP
```

---

### 第二步：安装Docker

```bash
# 一键安装Docker
curl -fsSL https://get.docker.com | sh

# 启动Docker
systemctl start docker
systemctl enable docker

# 验证安装
docker --version
```

安装docker-compose插件：
```bash
# 新版Docker已内置compose插件，验证一下
docker compose version

# 如果提示未找到，手动安装：
# curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
# chmod +x /usr/local/bin/docker-compose
```

---

### 第三步：上传项目代码

#### 3.1 在服务器创建工作目录
```bash
mkdir -p /opt/aikey
cd /opt/aikey
```

#### 3.2 上传代码
**方式一：Git克隆（推荐）**
```bash
cd /opt
# 替换为你的仓库地址
git clone https://github.com/yourusername/ai-key-management.git aikey
```

**方式二：本地打包上传**
```bash
# 本地执行，打包项目
cd 项目目录
tar czvf aikey.tar.gz . --exclude=node_modules --exclude=target --exclude=.git

# 上传到服务器
scp aikey.tar.gz root@服务器IP:/opt/

# 在服务器解压
ssh root@服务器IP "cd /opt && tar xzvf aikey.tar.gz -C aikey && rm aikey.tar.gz"
```

---

### 第四步：配置环境变量

```bash
cd /opt/aikey/deploy
cp .env.example .env
nano .env
```

**必须修改的配置项：**
```bash
# MySQL密码（必须修改！）
MYSQL_ROOT_PASSWORD=YourStrongPassword123!
MYSQL_PASSWORD=YourUserPassword123!
```

---

### 第五步：启动所有服务

```bash
cd /opt/aikey/deploy

# 构建并启动所有服务（首次需要几分钟）
docker compose -f docker-compose.all-in-one.yml up -d --build

# 查看启动状态
docker compose -f docker-compose.all-in-one.yml ps

# 查看日志
docker compose -f docker-compose.all-in-one.yml logs -f
```

---

### 第六步：验证部署

#### 6.1 检查容器状态
```bash
docker ps
```
应该看到4个容器都在运行：
- aikey-frontend
- aikey-backend
- aikey-mysql
- aikey-redis

#### 6.2 浏览器访问
打开浏览器访问：`http://你的服务器IP`

#### 6.3 测试API
```bash
# 测试登录接口
curl -X POST http://你的服务器IP/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

---

## 🔧 常用运维命令

### 查看日志
```bash
cd /opt/aikey/deploy

# 查看所有服务日志
docker compose -f docker-compose.all-in-one.yml logs

# 查看前端日志
docker compose -f docker-compose.all-in-one.yml logs -f frontend

# 查看后端日志
docker compose -f docker-compose.all-in-one.yml logs -f backend
```

### 重启服务
```bash
# 重启所有服务
docker compose -f docker-compose.all-in-one.yml restart

# 重启单个服务
docker compose -f docker-compose.all-in-one.yml restart backend
```

### 停止服务
```bash
docker compose -f docker-compose.all-in-one.yml down
```

### 更新部署（拉取新代码后）
```bash
cd /opt/aikey
git pull  # 如果用git

cd deploy
docker compose -f docker-compose.all-in-one.yml down
docker compose -f docker-compose.all-in-one.yml up -d --build
```

### 进入容器调试
```bash
# 进入后端容器
docker exec -it aikey-backend sh

# 进入MySQL
docker exec -it aikey-mysql mysql -u root -p

# 进入Redis
docker exec -it aikey-redis redis-cli
```

---

## 💾 数据备份

### 自动备份脚本
```bash
cat > /opt/aikey/backup.sh << 'EOF'
#!/bin/bash
BACKUP_DIR="/opt/aikey/backups"
DATE=$(date +%Y%m%d_%H%M%S)
mkdir -p $BACKUP_DIR

# 备份数据库
docker exec aikey-mysql mysqldump -u root -p${MYSQL_ROOT_PASSWORD} ai_key_management > $BACKUP_DIR/db_backup_$DATE.sql

# 压缩备份
gzip $BACKUP_DIR/db_backup_$DATE.sql

# 保留最近7天备份
find $BACKUP_DIR -name "db_backup_*.gz" -mtime +7 -delete

echo "备份完成: $BACKUP_DIR/db_backup_$DATE.sql.gz"
EOF

chmod +x /opt/aikey/backup.sh
```

### 添加定时任务
```bash
crontab -e
# 添加以下行（每天凌晨2点备份）
0 2 * * * cd /opt/aikey && ./backup.sh >> /var/log/aikey-backup.log 2>&1
```

---

## 🐛 常见问题

### Q1: 访问服务器IP无响应
**检查清单：**
1. 安全组是否开放80端口
2. 容器是否正常运行：`docker ps`
3. 查看Nginx日志：`docker logs aikey-frontend`

### Q2: 前端页面空白或报错
**解决：**
```bash
# 检查前端构建
docker logs aikey-frontend

# 重新构建前端
docker compose -f docker-compose.all-in-one.yml up -d --build frontend
```

### Q3: 数据库连接失败
**检查清单：**
1. MySQL容器状态：`docker ps | grep mysql`
2. 查看MySQL日志：`docker logs aikey-mysql`
3. 检查环境变量密码是否正确

### Q4: 如何修改端口？
如果80端口被占用，修改 `docker-compose.all-in-one.yml`：
```yaml
frontend:
  ports:
    - "8080:80"  # 改为其他端口，如8080
```
然后访问 `http://服务器IP:8080`

---

## 📊 资源占用参考

| 服务 | 内存占用 | CPU占用 |
|------|----------|---------|
| Nginx(前端) | ~20MB | 低 |
| Spring Boot | ~800MB | 中 |
| MySQL | ~400MB | 中 |
| Redis | ~10MB | 低 |
| **总计** | **~1.2GB** | - |

> 2核4G配置可以支撑日活1000左右的访问量

---

## 🔄 升级HTTPS（可选）

如需HTTPS，可以使用Nginx Proxy Manager或手动配置证书：

```bash
# 安装certbot
apt install certbot python3-certbot-nginx

# 申请证书（需要域名）
certbot --nginx -d yourdomain.com
```

或者使用 [Nginx Proxy Manager](https://nginxproxymanager.com/) 图形化管理。

---

## 📞 获取帮助

遇到问题请检查：
1. `docker ps` 查看容器状态
2. `docker logs 容器名` 查看日志
3. 安全组配置
