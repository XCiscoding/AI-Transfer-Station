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
| 80 | Web访问 | 所有IP |

> 💡 **注意**: 数据库端口(3306)不对外暴露，更安全！

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

### 步骤2: 上传项目代码

**方式A: 通过 FinalShell 文件管理器上传**

1. 在 FinalShell 左侧找到 "文件管理器"
2. 进入远程服务器的 `/opt` 目录
3. 创建 `aikey` 文件夹
4. 将本地项目文件拖拽上传到 `/opt/aikey/`

**方式B: 通过 Git 克隆**

```bash
# 在 FinalShell 终端执行
cd /opt
git clone https://github.com/你的用户名/ai-key-management.git aikey
```

---

### 步骤3: 一键部署

在 FinalShell 终端执行以下命令：

```bash
# 1. 进入项目目录
cd /opt/aikey/deploy

# 2. 给脚本添加执行权限
chmod +x finalshell-deploy.sh

# 3. 运行部署脚本
bash finalshell-deploy.sh
```

脚本会自动完成：
- ✅ 安装 Docker（如未安装）
- ✅ 安装 Docker Compose
- ✅ 配置环境变量（自动生成随机密码）
- ✅ 构建并启动所有服务
- ✅ 检查部署状态

---

### 步骤4: 等待部署完成

首次部署需要 **5-10分钟**，请耐心等待。

当看到以下输出表示部署成功：
```
========================================
  部署成功！
========================================
服务状态:
NAMES                STATUS              PORTS
aikey-frontend       Up 10 seconds       0.0.0.0:80->80/tcp
aikey-backend        Up 10 seconds       8080/tcp
aikey-mysql          Up 15 seconds       3306/tcp
aikey-redis          Up 15 seconds       6379/tcp

访问地址: http://你的服务器IP
```

---

### 步骤5: 访问系统

打开浏览器，访问：
```
http://你的服务器IP
```

---

## 🔧 常用运维操作

### 查看服务状态
```bash
cd /opt/aikey/deploy
docker compose -f docker-compose.all-in-one.yml ps
```

### 查看日志
```bash
# 查看所有日志
docker compose -f docker-compose.all-in-one.yml logs -f

# 只看后端日志
docker compose -f docker-compose.all-in-one.yml logs -f backend

# 只看前端日志
docker compose -f docker-compose.all-in-one.yml logs -f frontend
```

### 重启服务
```bash
cd /opt/aikey/deploy
docker compose -f docker-compose.all-in-one.yml restart
```

### 停止服务
```bash
cd /opt/aikey/deploy
docker compose -f docker-compose.all-in-one.yml down
```

### 更新部署（代码更新后）
```bash
cd /opt/aikey

# 如果有git，先拉取最新代码
git pull

# 或者通过 FinalShell 重新上传文件

# 然后重启服务
cd deploy
docker compose -f docker-compose.all-in-one.yml down
docker compose -f docker-compose.all-in-one.yml up -d --build
```

---

## 💾 数据备份

### 手动备份
```bash
# 备份数据库
docker exec aikey-mysql mysqldump -u root -p ai_key_management > /opt/aikey/backup_$(date +%Y%m%d).sql
```

### 自动备份（推荐）
```bash
# 创建备份脚本
cat > /opt/aikey/backup.sh << 'EOF'
#!/bin/bash
BACKUP_DIR="/opt/aikey/backups"
DATE=$(date +%Y%m%d_%H%M%S)
mkdir -p $BACKUP_DIR

# 备份数据库
docker exec aikey-mysql mysqldump -u root -p$(grep MYSQL_ROOT_PASSWORD /opt/aikey/deploy/.env | cut -d= -f2) ai_key_management > $BACKUP_DIR/db_backup_$DATE.sql

# 压缩
gzip $BACKUP_DIR/db_backup_$DATE.sql

# 保留7天
find $BACKUP_DIR -name "db_backup_*.gz" -mtime +7 -delete
EOF

chmod +x /opt/aikey/backup.sh

# 添加定时任务
crontab -e
# 添加: 0 2 * * * /opt/aikey/backup.sh
```

---

## 🐛 常见问题

### Q1: 部署脚本执行失败
**解决：**
1. 检查是否以 root 用户运行
2. 检查服务器是否能访问外网（下载Docker需要）
3. 手动安装Docker后重试：
   ```bash
   curl -fsSL https://get.docker.com | sh
   ```

### Q2: 访问服务器IP无响应
**检查清单：**
1. 安全组是否开放80端口
2. 容器是否正常运行：`docker ps`
3. 查看Nginx日志：`docker logs aikey-frontend`

### Q3: 如何修改端口（80被占用）
编辑 `docker-compose.all-in-one.yml`：
```yaml
frontend:
  ports:
    - "8080:80"  # 改为8080或其他端口
```
然后重启：`docker compose -f docker-compose.all-in-one.yml up -d`

### Q4: 忘记数据库密码
查看 `.env` 文件：
```bash
cat /opt/aikey/deploy/.env
```

---

## 📊 资源监控

在 FinalShell 中可以实时查看：
- CPU使用率
- 内存使用率
- 网络流量
- 磁盘使用

建议内存占用超过80%时考虑升级服务器配置。

---

## 🔄 升级HTTPS（可选）

如果有域名，可以申请SSL证书：

```bash
# 安装certbot
apt install certbot python3-certbot-nginx -y

# 申请证书（替换为你的域名）
certbot --nginx -d yourdomain.com

# 自动续期已配置，无需手动操作
```

---

## 📞 获取帮助

遇到问题：
1. 查看容器日志：`docker logs 容器名`
2. 检查服务状态：`docker ps`
3. 查看系统资源：`free -h` / `df -h`
