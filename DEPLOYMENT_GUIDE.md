# AI调度中心 - 服务器部署指南

## 当前正式部署方案

这个项目在当前云服务器上的正式部署方式已经收口为：**单机 all-in-one + GitHub Actions 上传发布包部署**。

### 部署目标
- 前端正式入口：`http://服务器IP:8083`
- 前端由 Nginx 容器提供
- 后端由 Spring Boot 容器提供
- 后端连接服务器现有 MySQL / Redis
- GitHub Actions 不再依赖服务器自己 `git pull`

---

## 为什么要这样改

之前 GitHub Actions 失败的真实原因不是 SSH 建连，而是部署链路本身有两个不稳定点：

1. 服务器执行 `git pull origin main` 时访问 GitHub 不稳定，已出现：
   - `fatal: unable to access 'https://github.com/...': Empty reply from server`
2. 服务器环境里存在旧部署栈 / 旧服务冲突，尤其是 3306 端口冲突，继续混用不同 compose 会让问题反复出现。

所以新的正式链路是：

- GitHub Runner 打包源码
- 发布包 tar.gz 生成在 runner 临时目录（而不是仓库根目录）
- SCP 上传到服务器
- 服务器解压发布包并执行 `docker compose -f deploy/docker-compose.all-in-one.yml up -d --build`

---

## 自动部署流程

`.github/workflows/deploy.yml` 现在应承担以下职责：

1. checkout 代码
2. 在 runner 临时目录生成发布包 tar.gz（不要写回仓库工作目录）
3. 上传到服务器：`/root/AI-center/releases`
4. 在服务器解压到新 release 目录
5. 复制已有 `deploy/.env`
6. 启动：
   ```bash
   docker compose -f deploy/docker-compose.all-in-one.yml up -d --build
   ```
7. 验证：
   ```bash
   docker compose -f deploy/docker-compose.all-in-one.yml ps
   curl -I http://127.0.0.1:8083
   curl http://127.0.0.1:8083/api/health
   ```

---

## 服务器目录约定

```bash
/root/AI-center/releases
/root/AI-center/AI-Transfer-Station
```

含义：
- `releases`：存放上传的 tar.gz 和临时解压版本
- `AI-Transfer-Station`：当前线上运行目录

---

## 正式部署栈

当前线上只允许使用：

- `deploy/docker-compose.all-in-one.yml`

不要再把以下文件当成这台服务器的正式部署入口：

- 根目录 `docker-compose.yml`
- `deploy/docker-compose.prod.yml`

原因：
- 根 compose 不是完整前后端栈
- `deploy/docker-compose.prod.yml` 更适合另一种部署模型，且容易和服务器已有服务端口冲突

---

## 环境变量基线

`deploy/.env` 中至少应确认：

```bash
SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/ai_key_management?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=root
SPRING_DATA_REDIS_HOST=host.docker.internal
SPRING_DATA_REDIS_PORT=6379
```

如果服务器 MySQL / Redis 不是这个配置，按真实环境改。

---

## 部署后验证

```bash
cd /root/AI-center/AI-Transfer-Station
docker compose -f deploy/docker-compose.all-in-one.yml ps
curl -I http://127.0.0.1:8083
curl http://127.0.0.1:8083/api/health
```

浏览器访问：

```text
http://服务器IP:8083
```

---

## 运维提醒

如果线上再次出问题，先排查：

```bash
docker ps --format 'table {{.Names}}\t{{.Ports}}\t{{.Status}}'
ss -ltnp | grep -E ':3306|:6379|:8080|:8083'
cd /root/AI-center/AI-Transfer-Station
docker compose -f deploy/docker-compose.all-in-one.yml ps || true
docker compose -f deploy/docker-compose.prod.yml ps || true
docker compose -f docker-compose.yml ps || true
```

优先确认是否又混跑了旧栈。
