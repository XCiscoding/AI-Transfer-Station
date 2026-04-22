# Agent Execution Summary

> 这是当前项目的执行标准文档。
> 目标不是复盘聊天，而是让任何接手的 Agent 在最短时间内获得统一认知，并按同一标准继续执行。
>
> 默认规则：**后续在这个项目里的执行，都按本文档为准；如果用户当次消息给出了更明确的新要求，以用户当次要求优先。**

---

## 1. Project Snapshot

项目：AI调度中心——企业 API Key 管理系统

核心能力：

- 用户 / 角色 / 权限管理
- 团队管理
- 渠道（Channel）管理
- 模型（Model）管理
- 模型分组（ModelGroup）管理
- 真实 Key（RealKey）管理
- 虚拟 Key（VirtualKey）管理
- 网关鉴权、额度控制、限流、调度、转发、日志记录

技术栈：

- Backend: Spring Boot 3.2.5 + Spring Security + Spring Data JPA + MySQL + Redis
- Frontend: Vue 3 + Element Plus + Vite
- DB migration: Flyway

---

## 2. Source of Truth: Confirmed Product Direction

这是本轮已经和用户确认过的最终产品方向。后续实现必须以此为准。

### 2.1 Role Boundary

当前确认的角色边界：

- **企业管理员 = SUPER_ADMIN**
- **团队管理员 = Team.owner**

职责划分：

#### 企业管理员负责
- 创建团队
- 查看所有团队
- 指定 / 变更团队管理员
- 配置团队可用模型分组
- 维护企业级资源

#### 团队管理员负责
- 查看自己可管理的团队
- 管理团队成员
- 在团队上下文内发放 Key
- 使用企业管理员已配置好的模型分组

### 2.2 Team Page Form

- `TeamManagement` 不再被定义成单一后台页。
- **团队管理页是“双视角页”**。
- 同一个页面内根据角色展示不同内容，而不是先拆成两个独立页面。

#### 企业管理员进入团队页时
应看到：
- 团队列表
- 新增团队按钮
- 编辑团队基础信息
- 指定 / 变更团队管理员
- 配置团队可用模型分组
- 可进入团队详情 / 团队管理员工作区

#### 团队管理员进入团队页时
应看到：
- 自己可管理的团队列表（通常是一个或少量）
- 团队成员管理区
- 团队内发放 Key 入口
- 当前团队可用模型分组查看 / 使用区

同时：
- 团队管理员**不能看到“新增团队”按钮**
- 团队管理员**不能看到“创建模型分组”入口**

### 2.3 Model Group Ownership

- **模型分组是企业级资源。**
- 模型分组创建 / 编辑 / 删除继续放在模型分组管理页。
- 不要把模型分组创建塞进团队管理页。
- 团队管理员不能创建模型分组本体。
- 团队管理员只能查看和使用“当前团队被允许使用的模型分组”。

### 2.4 Team Creation Ownership

- **创建团队是企业管理员职责。**
- 团队管理员不能创建团队。
- 后续开发企业管理员独立界面时，当前这套企业管理员视角的团队管理模块应能直接复用，不需要重新设计一套。

### 2.5 Team Member Management

- 团队管理员应具备成员管理能力。
- 成员管理是团队页的重要模块，不是附属功能。
- 当前系统若还没有成员关系表 / 成员管理 API，需要后续补齐。

### 2.6 Key Creation Rules

- **所有 Key 都必须归属团队。**
- Key 创建必须发生在团队上下文内。
- 企业管理员可从团队列表进入某个团队的发放流程。
- 团队管理员可直接在自己的团队上下文内发放。
- 团队管理员发放 Key 时，长期目标是从“本团队成员列表”中选用户，而不是手填用户 ID。

### 2.7 New Key Form Rules

- 新建 Key 时，模型分组 **单选**。
- 选中模型分组后，再加载该分组下可用渠道。
- 渠道不是自由输入 URL，而是从系统已有渠道中选。
- 提交时仍沿用后端现有结构：`allowedGroupIds: [selectedGroupId]`。

### 2.8 Edit Legacy Data Rule

旧 VirtualKey 数据如果存在以下任一情况：

- 没有 `channelId`
- `allowedGroupIds` 包含多个分组

则：

- 允许进入编辑
- 必须提示用户补齐或收敛数据
- 未补齐前禁止保存

---

## 3. Current Status

### 3.1 Confirmed current state

当前已确认：

- `enterprise_admin` 已可成功登录，可用于企业管理员视角验证
- 企业管理员视角仍由 `SUPER_ADMIN` 驱动
- 前端当前登录链路使用：
  - `POST /api/v1/auth/login`
  - `GET /api/v1/auth/me`
- 登录成功后前端已写入：
  - `token`
  - `username`
  - `roles`
- `403` 当前只提示“权限不足”，不再强制清 token 跳登录
- Analytics 当前已不再依赖 `/api/v1/users`

### 3.2 Team membership nuance that is now confirmed

这一轮运行态联调进一步确认了一个关键事实：

- 代码和页面已经开始支持 `team_members` 维度的成员管理
- 但团队管理主链路的权限边界已明确收口：`SUPER_ADMIN` 或 `Team.owner` 才能进入团队管理工作区与团队管理接口
- 普通 `team_members` 可以作为被管理成员存在，也可在团队内作为 Key 发放对象，但**不会仅因 membership 自动获得团队管理页 / 成员管理 / 团队发 Key 工作区权限**

当前系统的真实边界是：
- **企业管理员 = `SUPER_ADMIN`**
- **团队管理员 = `Team.owner`**
- **普通团队成员 = 可归属团队、可被发放 Key，但不具备团队管理权限**

后续不要把“成员已加入 team_members”误等同于“成员已具备团队页 / 项目页 / Key 管理页访问能力”。当前代码已经按 owner 边界收口团队管理权限。

### 3.3 What was actually resolved already

下面这些问题不要再当成“当前主阻塞”重复排查：

- `frontend/src/api/auth.js` 已使用 `/auth/me`
- `frontend/src/views/Login.vue` 已把 `roles` 写入本地
- `frontend/src/utils/request.js` 中 `403` 已不再触发退登
- `frontend/src/views/Analytics.vue` 当前按团队/项目维度请求：
  - `/api/v1/teams`
  - `/api/v1/projects`
- 团队权限主链路本轮已进一步收口：
  - `backend/src/main/java/com/aikey/service/TeamService.java` 已改成 `SUPER_ADMIN` 看全部、非超管只看自己作为 `Team.owner` 的团队
  - `TeamService.getManageableTeam(...)` 已不再因为 `team_members` membership 放行
- 前端以下页面已补齐 owner 边界与无权限上下文处理：
  - `frontend/src/views/TeamManagement.vue`
  - `frontend/src/views/TokenManagement.vue`
  - `frontend/src/views/ProjectManagement.vue`
  - `frontend/src/views/Analytics.vue`
- 因此，之前那类“普通成员因前端误判进入团队管理/项目管理/团队发 Key 主流程”的问题，这一轮已经完成主要收口

### 3.4 Latest runtime findings

这一轮运行态排查已确认两件事：

- **团队列表 / 模型分组触发的系统性后端 500 已修复，不应再作为当前主阻塞重复排查**
- **当前新的主阻塞是：真实 `Team.owner` 账号进入团队页后，仍未稳定进入“团队管理员视角”**

已确认的运行态结论：
- `backend/src/main/java/com/aikey/entity/ModelGroup.java` 之前把模型分组字段映射成了不存在的 `model_ids`
- 数据库真实字段是 `models`
- 这会导致 `/api/v1/model-groups/all` 查询直接报 SQL 1054
- 团队列表在组装 `allowedGroups` 时又会调用模型分组链路，所以 `/api/v1/teams` 也被连带打成 500
- 把实体映射改回 `models` 后，`/api/v1/model-groups/all` 与 `/api/v1/teams` 已恢复 200

因此当前不要再把：
- 团队列表 500
- 模型分组接口 500
- 由此引起的“服务器内部错误”

继续当作团队页主问题。

当前真正要继续收口的是：
- `/api/v1/auth/me` 对 owner 账号是否稳定返回 `isTeamOwner=true`
- `frontend/src/views/TeamManagement.vue` 是否仍有视角判断、初始化顺序或工作区渲染分支没有完全按 `isSuperAdmin / isTeamOwner` 收口
- owner 账号虽然不再报系统性后端错误，但页面仍没有真正走进团队管理员工作区

### 3.5 Latest diagnosis updates

本次对话新增确认了两条关键结论：

1. **`admin` 被识别为企业管理员，当前高概率不是前端展示问题，而是后端身份真相源漂移**
2. **团队管理页的“多账号多视角”当前实现已经基本清楚：同页双视角，但优先级是企业管理员视角高于团队管理员视角**

已确认的证据链：
- 运行态配置仍连本地 MySQL：`jdbc:mysql://localhost:3306/ai_key_management`，账号 `root/root`
- `server.port` 仍是 `8080`
- 本地 8080 当前确实已有本项目后端实例在运行，应优先复用，不要重复启动
- 直接调用运行态 `POST /api/v1/auth/login`，`admin/admin123` 返回 `401 用户名或密码错误`
- 这说明当前运行数据库中的 `admin` 密码状态已经和 `db/data.sql` 预期不完全一致，运行态数据存在漂移

更关键的是，代码层已发现明确冲突：

- `backend/src/main/resources/db/data.sql` 明确写的是：
  - `enterprise_admin` = `SUPER_ADMIN`
  - `admin` = `USER`
  - `admin` 通过 `Team.owner` 成为团队管理员
- 但以下后端逻辑却把 `admin` 也纳入了 bootstrap 超级管理员名单：
  - `backend/src/main/java/com/aikey/config/DataInitializer.java`
  - `backend/src/main/java/com/aikey/config/DataRepairRunner.java`
  - `backend/src/main/java/com/aikey/service/AuthService.java`

当前最重要的冲突结论：
- **SQL / 产品定义认为 `admin` 是团队管理员账号**
- **后端 bootstrap / repair / auth 逻辑却会把 `admin` 当成超级管理员候选处理**
- 这正是 `admin` 容易被拉回企业管理员视角的核心风险点

### 3.6 Dual-view implementation reality now clarified

本次对话还明确了当前“多账号多视角”的真实实现方式：

#### 后端身份真相源
- `/api/v1/auth/me` 返回：
  - `roles`
  - `isSuperAdmin`
  - `isTeamOwner`
- 判定基础：
  - 企业管理员 = `roles` 包含 `SUPER_ADMIN`
  - 团队管理员 = 当前用户是某个 `Team.owner`

#### 前端登录与缓存链路
- `frontend/src/views/Login.vue` 登录成功后，不只使用 `/auth/login` 响应
- 还会继续调用 `/auth/me`
- 最终把 `roles / isSuperAdmin / isTeamOwner` 写入 `localStorage.userInfo`
- 因此前端视角切换本质上是消费后端身份，不是前端自定义权限

#### 顶部角色徽标优先级
- `frontend/src/components/MainLayout.vue` 当前规则：
  - 先显示企业管理员
  - 否则显示团队管理员
- 这意味着：**如果一个账号同时具备 `SUPER_ADMIN` 和 `isTeamOwner`，页面会优先落到企业管理员身份展示**

#### 团队页双视角规则
- `frontend/src/views/TeamManagement.vue` 当前核心规则：
  - `isTeamOwnerView = !isSuperAdmin && isTeamOwner`
  - `hasTeamManagementAccess = isSuperAdmin || isTeamOwnerView`
- 真实含义：
  - 企业管理员：进入企业管理员视角，看全部团队、创建团队、配置团队资源
  - 团队管理员：只有在“不是超管，但确实是 owner”时，才进入团队管理员工作区
- 也就是说：**当前双视角实现不是并列，而是企业管理员视角优先，团队管理员视角次之**

#### 数据范围控制
- `backend/src/main/java/com/aikey/service/TeamService.java`
  - `SUPER_ADMIN` 可看全部团队
  - 非超管只能看自己作为 `Team.owner` 的团队
- 这说明视角不只是 UI 切换，后端数据范围本身也按身份分层

这条规则后续不要再误解为：
- “只要是团队管理员就一定进入团队管理员视角”

当前真实规则是：
- **先判是否企业管理员；如果是，就优先走企业管理员视角**
- **只有不是企业管理员、但又是 owner，才走团队管理员视角**

### 3.7 Current no-code next step

用户本轮最后要求：**先不继续开发，先总结并更新 `SUMMARY.md`。**

因此当前停留点不是继续改代码，而是：
- 把这次关于 `admin` 身份漂移和双视角实现规则的结论固化到总结文档
- 后续若恢复开发，优先处理 `admin` 从 bootstrap 超管链路中移除的问题，再做真实账号联调验证

### 3.9 Deployment and push flow updates

本次对话围绕部署链路又实际做了多轮排查和修正，但**到当前为止，GitHub Actions 自动部署仍未成功稳定恢复**。这里必须把真实过程和失败原因写清楚，避免后续再基于错误前提重复修。

#### What was actually attempted in this round

这轮已经实际做过的事情：

1. **先确认 push 语义**
   - 本地 `Everything up-to-date` 不等于 push 失败
   - 只有本地有新 commit 且成功推到 `origin/main`，GitHub Actions 才会拿到新 workflow / 新代码

2. **先分析 403 与访问入口问题**
   - 实测：`http://111.230.113.110:8083` 返回 `403 Forbidden`
   - 同时发现仓库里原有部署设计和线上入口并不一致：
     - 根目录 `docker-compose.yml` 只起后端栈
     - 真正包含前端的是 `deploy/docker-compose.all-in-one.yml`
     - 用户最终明确要求正式前端入口使用 `http://111.230.113.110:8083`

3. **第一次修 workflow：从根 compose 切到 all-in-one compose**
   - 把 `.github/workflows/deploy.yml` 从：
     - `docker compose down`
     - `docker compose up -d --build`
   - 改成：
     - `docker compose -f deploy/docker-compose.all-in-one.yml down`
     - `docker compose -f deploy/docker-compose.all-in-one.yml up -d --build`
   - 同时把 `deploy/docker-compose.all-in-one.yml` 的 frontend 端口改成 `8083:80`

4. **查看真实 GitHub Actions 失败日志后，再次修部署模型**
   从真实日志里确认了两个关键报错：
   - `fatal: unable to access 'https://github.com/XCiscoding/AI-Transfer-Station.git/': Empty reply from server`
   - `Bind for 0.0.0.0:3306 failed: port is already allocated`

5. **第二次修 compose：改成复用服务器现有 MySQL / Redis**
   - 把 `deploy/docker-compose.all-in-one.yml` 从“自带 mysql / redis 容器”改成：
     - backend 连接 `host.docker.internal:3306`
     - backend 连接 `host.docker.internal:6379`
   - 目标是绕开服务器上已有 MySQL / Redis 的端口冲突

6. **第三次修 workflow：不再让服务器自己 `git pull`**
   - 重新设计 `.github/workflows/deploy.yml`
   - 改成：
     - GitHub runner 本地打包源码
     - 用 `appleboy/scp-action` 上传 tar.gz 到服务器
     - 再用 `appleboy/ssh-action` 解压并部署
   - 目标是绕开服务器访问 GitHub HTTPS 不稳定的问题

7. **同步更新了部署文档**
   - `DEPLOYMENT_GUIDE.md`
   - `deploy/DEPLOY_ALL_IN_ONE.md`
   - `deploy/FINALSHELL_DEPLOY_GUIDE.md`
   - 文档都统一成：当前服务器正式入口是 `http://服务器IP:8083`，正式部署只用 `deploy/docker-compose.all-in-one.yml`

8. **已创建新的本地提交**
   - 新提交：`2f3ec18 fix：启动！`
   - 但截至当前对话，用户反馈 GitHub Actions 页面仍显示部署失败，说明“新方案已经写进仓库”不等于“线上已被验证恢复”

9. **最新确认的新失败点：release 打包 tar 步骤本身也会失败**
   - 用户在 push 最近提交后，GitHub Actions 的 `Create release archive` 步骤报错：
     - `tar: .: file changed as we read it`
   - 这说明当前 workflow 在 runner 工作目录里“边生成 tar.gz，边把当前目录 `.` 作为打包源”时，输出文件落在被打包的同一棵目录树内，导致 tar 在读取过程中观察到目录状态变化并直接退出
   - 这不是服务器部署阶段的问题，而是 **GitHub runner 本地打包阶段就已经失败**
   - 当前已明确的正确修正方向不是继续压 warning，而是：
     - **把 release 压缩包输出到 `${{ runner.temp }}` 这类 workspace 外部临时目录**
     - 再从该临时路径执行 SCP 上传
     - 先确保 runner 打包稳定通过，后面才有资格继续验证服务器部署阶段

10. **再往后暴露出的新失败点：SCP 上传后的落点与 SSH 解压预期不一致**
   - tar 打包阶段修复后，workflow 继续往后执行，但 `Deploy release on server` 报错：
     - `tar (child): release-<sha>.tar.gz: Cannot open: No such file or directory`
   - 这说明问题已经推进到服务器阶段，而且当前失败不是“没上传”，而是：
     - `scp-action` 实际落盘路径
     - 与 ssh 脚本里写死的 `cd /root/AI-center/releases && tar -xzf release-<sha>.tar.gz`
     - **两边并没有对齐**
   - 后续日志进一步确认：
     - release 包实际会落到 `/root/AI-center/releases/github/runner_temp/release-<sha>.tar.gz` 这类路径
     - 并不会稳定落在 `/root/AI-center/releases/release-<sha>.tar.gz`
   - 更关键的是，在最新一次 run 里：
     - 当前环境变量里的 `RELEASE_NAME` 已经变成了新 SHA（如 `release-0a8a7b8...`）
     - 但失败时在服务器上打印出来的仍然是旧 SHA 的压缩包（如 `release-a696202...tar.gz`）
   - 这说明当前问题已经不只是“ssh 该怎么找文件”，而是：
     - **`scp-action` 对来自 `${{ runner.temp }}` 的绝对路径上传行为并不稳定**
     - 服务器 releases 目录里可能只残留旧包，当前这次 run 的新包根本没有按预期落盘
   - 因此当前最值得保留的下一个修法不是继续补 ssh 查找分支，而是：
     - **新增一个 runner 侧 staging 目录**，例如 `${{ github.workspace }}/.release-upload/`
     - 先把 `${{ runner.temp }}/${{ env.RELEASE_NAME }}.tar.gz` 复制到这个 staging 目录
     - 再让 `scp-action` 从相对稳定的 workspace 路径上传，而不是直接上传 runner.temp 绝对路径
     - 同时去掉对 `strip_components` 的依赖，让服务器侧落点更可预测

#### Why this still did not get fixed successfully

到现在还没修成功，真实原因不是单点故障，而是**连续几次判断都只解决了局部问题，没有真正拿到完整线上事实再闭环**。

##### Root cause 1: 一开始把失败原因收窄得过早
最开始确认过一次：
- 服务器脏工作区导致 `git pull` 失败

这个结论在当时是对的，但它**不是唯一根因**。
后面真实日志又暴露出：
- 服务器访问 GitHub HTTPS 也不稳定
- 服务器还存在 3306 端口占用

也就是说：
- **第一次修复只解决了“脏工作区”这一层，没有解决“拉 GitHub 不稳定”和“部署模型冲突”两层**

##### Root cause 2: 部署模型本身长期不统一
当前仓库里一直并存至少三套思路：
- 根目录 `docker-compose.yml`
- `deploy/docker-compose.prod.yml`
- `deploy/docker-compose.all-in-one.yml`

它们分别代表不同部署模型，但线上到底跑的是哪一套、历史上混跑过哪一套，没有先彻底核实清楚。
后果是：
- 一次修的是 workflow
- 一次修的是 compose 端口
- 一次修的是数据库来源
- 但**没有先把“服务器当前真实运行基线”作为唯一事实源固定下来**

##### Root cause 3: 线上事实验证不完整
这轮虽然看了 GitHub Actions 日志，但仍缺少对服务器当前状态的一次完整核查闭环，比如：
- 当前服务器到底有哪些容器在跑
- 哪些 compose project 还活着
- 3306/6379/8083 分别被谁占用
- `/root/AI-center/releases` 和 `/root/AI-center/AI-Transfer-Station` 当前实际是什么状态
- 新 workflow 失败到底卡在：
  - SCP 上传
  - SSH 解压
  - compose up
  - 还是健康检查

**没有这组完整事实，就只能继续“改配置再赌一次”。**
这正是这几次没有真正收口的核心原因。

##### Root cause 4: 改了 workflow，但没有先验证“新 workflow 自己能否跑通”
新 workflow 引入了：
- `appleboy/scp-action@v0.1.7`
- release 打包 / 上传 / 解压目录
- 复制 `.env`
- 删除旧目录再替换运行目录

这些动作比旧 workflow 更复杂，但本轮没有先拿到：
- 新 Actions 失败日志的完整输出
- `scp-action` 是否成功上传
- 服务器 release 目录是否真的生成
- `curl http://127.0.0.1:8083/api/health` 失败在哪一步

并且在最新一次 push 后，又新增确认了一个更前置的失败点：
- `Create release archive` 步骤直接报 `tar: .: file changed as we read it`
- 说明 workflow 甚至可能还没走到上传和服务器部署阶段，就已经在 runner 本地打包时失败
- 当前正确修法也已经明确：**release 包不能再生成在 workspace 内，必须输出到 runner 临时目录，再交给后续上传步骤使用**

再往后，在 tar 打包问题修掉之后，又暴露出新的后继失败点：
- `Deploy release on server` 阶段解压时报：`release-<sha>.tar.gz: Cannot open: No such file or directory`
- 说明 workflow 已经进入服务器阶段，但 `scp-action` 的真实上传落点和 ssh 脚本里假设的 release 包位置并不一致
- 也就是说：**当前除了 runner 打包问题外，上传路径和服务器解压路径之间也存在断层**
- 后续日志又进一步确认：release 包并不会稳定落在 `/root/AI-center/releases/release-<sha>.tar.gz`，而是会落到 `/root/AI-center/releases/github/runner_temp/...` 这类路径
- 更关键的是，最新 run 里环境变量已经切到新的 `RELEASE_NAME`，但服务器侧打印出来的仍是旧 SHA 的压缩包，这说明 **当前真正没对齐的是上传行为本身，而不只是 ssh 查找逻辑**
- 因此这里下一步最值得执行的修法，不是继续加 ssh 判空分支，而是：**先在 runner workspace 下准备一个 staging 目录，再从这个 staging 相对路径上传 release 包**

所以当前只能确认：
- **新方案已经写了**
- **但不能确认新方案已经在线上真实跑通**
- **并且需要先修掉 runner 打包阶段的 tar 自包含失败问题，后面才谈得上继续验证上传和部署**
- **在此基础上，还要把 scp 上传来源改成 workspace staging 目录，避免 runner.temp 绝对路径上传行为不稳定，后面才能继续验证 compose 启动链路**

##### Root cause 5: 我在执行上也存在问题
这次没有修成，我这里有几条明确失误，后续必须避免：

1. **过早下结论**
   - 在只拿到部分现象时，就把根因判断收得太快
   - 结果是每次修一层，后面又暴露新问题

2. **没有先把“线上真实状态核查清单”跑完，再改代码**
   - 用户明确要求是修部署
   - 这种问题本质上比代码逻辑更依赖运行态事实
   - 正确顺序应该是：先完整拿到线上状态，再决定到底改 workflow、改 compose，还是先清理服务器

3. **把“提交了新方案”误当成“问题已被解决”**
   - 实际上当前最多只能说：
     - 已经尝试过几轮修复
     - 已经沉淀出更合理的新方案
     - 但线上恢复仍未验证成功

#### Current verified conclusion

到当前对话结束，能确认的结论只有这些：

1. **GitHub Actions 自动部署仍未恢复成功**
2. **之前至少出现过六类真实失败点：**
   - 服务器脏工作区拦截 `git pull`
   - 服务器访问 GitHub HTTPS 不稳定（`Empty reply from server`）
   - 服务器已有服务/旧栈占用端口（至少出现过 3306 冲突）
   - GitHub runner 本地打包 release 时触发 `tar: .: file changed as we read it`
   - release 包上传后，SSH 解压阶段找不到 `release-<sha>.tar.gz`
   - 新 run 的 release 包未稳定落盘，服务器上只看到旧 SHA 的残留压缩包
3. **当前仓库里的“新部署方案”已经改成：**
   - 前端入口目标：`http://111.230.113.110:8083`
   - 正式部署栈：`deploy/docker-compose.all-in-one.yml`
   - 自动部署策略：GitHub runner 打包 → SCP 上传 → SSH 解压部署
4. **但在没有拿到这次新 workflow 的完整失败日志之前，不能再声称“已经修好”**

#### Conversation continuation: cloud deployment repair progress (2026-04-11, round 2)

这轮对话把部署链路从"frontend build 通过"推进到了"后端实际已经成功启动、数据库已连通"，但 CI 仍然判定失败，原因是健康检查超时太短。

##### 当前真实状态（截至本轮对话结束）

- **frontend build**：✓ 已通过，镜像构建成功
- **backend build**：✓ 已通过，镜像构建成功
- **容器启动**：✓ `aikey-backend` 和 `aikey-frontend` 均已成功创建并启动
- **数据库连接**：✓ 后端已成功连上 `tea-garden-mysql`，Hibernate 查询正常，DataInitializer 已跑完
- **CI 健康检查**：✗ 仍然失败，原因是 `curl --max-time 20` 只等 20 秒，Spring Boot 冷启动需要 60 秒左右

##### 本轮实际解决的问题（按顺序）

1. **Docker Compose project name 不一致导致 `down` 无效**
   - 旧容器是从 `deploy/` 目录启动的，project name = `deploy`，网络叫 `deploy_aikey-network`
   - workflow 里 `down` 从 `APP_DIR` 运行，project name = `ai-transfer-station`，找不到旧容器，`|| true` 静默跳过
   - 修法：在 `up` 之前加 `docker rm -f aikey-backend aikey-frontend 2>/dev/null || true`

2. **旧容器名不同导致清理命中不到**
   - 服务器上还有 `aikey-frontend-8083` 和 `aikey-backend-8082` 两个旧容器占着端口
   - 修法：把这两个旧名字也加进 `docker rm -f` 清理列表

3. **MySQL 密码排查过程**
   - `tea-garden-mysql` 的 root 密码不是 `wml2580.`，也不是 `teagarden666`，真实密码是 `rootpass`
   - 教训：MySQL 密码必须从容器实际运行的 docker-compose 或 `.env` 文件里找，不能靠猜

4. **MySQL 用户创建**
   - 在 `tea-garden-mysql` 里创建了 `aikey@'%'` 用户，密码 `AiKeyUser2024!`，授权 `ai_key_management` 数据库
   - 在服务器 `/root/AI-center/AI-Transfer-Station/deploy/.env` 写入了正确的数据库凭据

5. **`.env` 文件时序问题**
   - workflow 会在部署时把 `APP_DIR/deploy/.env` 复制到新 release
   - 如果 `.env` 是在 CI 已经开始跑之后才创建的，那次 CI 就不会带上新凭据
   - 教训：`.env` 必须在触发 CI 之前就已经存在于服务器上

##### 当前唯一剩余阻塞点

CI 健康检查超时：

```bash
curl --fail --show-error --max-time 20 http://127.0.0.1:8083/api/health
```

Spring Boot 冷启动需要 60 秒左右，20 秒根本等不到。后端其实已经正常启动，只是 CI 判定太急。

**下一步只需要一个改动**：把 workflow 里的健康检查改成重试循环，最多等 2 分钟：

```bash
for i in $(seq 1 12); do
  if curl --fail --silent --max-time 5 http://127.0.0.1:8083/api/health; then
    echo "Backend ready"
    break
  fi
  echo "Waiting... ($i/12)"
  sleep 10
done
```

##### 本轮沉淀的部署规则

1. **Docker Compose 清理必须按容器名来，不能只靠 `compose down`**
   - `compose down` 依赖 project name 匹配，历史遗留容器名不同时会静默跳过
   - 正确做法：在 `up` 之前显式 `docker rm -f` 所有已知容器名（包括历史旧名）

2. **MySQL 密码必须从实际运行配置里找**
   - 不要靠猜，不要靠记忆，直接 `find /root -name ".env" | xargs grep MYSQL_ROOT_PASSWORD`

3. **`.env` 文件必须在 CI 触发前就存在于服务器**
   - workflow 的 `.env` 复制逻辑是"从旧 APP_DIR 复制"，如果旧 APP_DIR 里没有，就用 `.env.example` 兜底
   - 兜底的 `.env.example` 里是占位符，不是真实凭据

4. **CI 健康检查超时必须大于 Spring Boot 冷启动时间**
   - Spring Boot 冷启动通常 30-60 秒，`--max-time 20` 必然超时
   - 应改成重试循环，总等待时间至少 120 秒

#### Conversation continuation: cloud deployment repair progress (2026-04-11)

这次对话把问题继续往前推进了，而且已经能把“云端上传这里到底修到了哪一层”说清楚。

##### 先说结论

截至当前这轮对话，**围绕 GitHub Actions 云端上传/部署链路，已经实际落地了 6 个修复**，其中前 5 个已经被后续日志连续验证为“确实把故障往后推进了”，第 6 个是当前最新锁定并已本地改掉、但还没再次 push 验证的新修复。

这 6 个修复分别是：

1. **修复 runner 本地 tar 自包含失败**
   - 现象：`tar: .: file changed as we read it`
   - 修法：release 包不再直接生成在 workspace 当前目录，而是先输出到 `${{ runner.temp }}`
   - 结果：workflow 能继续进入后续上传/部署阶段，说明打包层已经不再是当前主拦截点

2. **修复 release 上传来源不稳定的问题**
   - 现象：直接从 `runner.temp` 绝对路径给 `scp-action` 上传时，服务器侧落点不稳定
   - 修法：新增 `${{ github.workspace }}/.release-upload/` staging 目录，先把 tar 包复制进去，再从 workspace 相对路径上传
   - 结果：后续日志已经能在服务器 release 目录里看到当前链路继续往下跑，不再停死在“本次包根本没法被后续阶段接住”这一层

3. **补强服务器侧 release 落点与解压诊断日志**
   - 修法：在 ssh 脚本中增加 `pwd`、`ls -la`、`find release-*.tar.gz`、`stat`、`tar -tzf`、解压后目录快照等日志
   - 结果：后面每次失败都能明确看到“卡在哪一层”，不再靠猜

4. **修复服务器部署脚本里的工作目录切换错误**
   - 现象：`mv: cannot stat 'release-<sha>'`
   - 根因：脚本为了执行 `docker compose down` 先 `cd` 到 `${APP_DIR}`，但后面直接用相对路径 `mv "${RELEASE_NAME}" "${APP_DIR}"`，此时当前目录已经不是 `${RELEASE_DIR}`
   - 修法：在 `rm -rf` 和 `mv` 前显式 `cd "${{ env.RELEASE_DIR }}"`
   - 结果：部署流程成功越过“release 目录移动失败”这一层，继续往镜像构建阶段推进

5. **修复 backend 容器健康检查路径错误，并顺手优化后端构建链路**
   - 现象：backend 真实健康接口是 `/actuator/health`，不是 `/api/health`
   - 修法：
     - `deploy/Dockerfile.backend` 健康检查改成 `curl -f http://localhost:8080/actuator/health`
     - 同时把 builder 改成 `maven:3.9.9-eclipse-temurin-17-alpine`，并加入 Maven 仓库缓存挂载，去掉额外 `apk add maven`
   - 结果：后续日志已经表明流程能推进到 backend build 完成之后，说明当前主阻塞点已不在 backend 健康检查这一层

6. **修复 frontend 构建阶段缺少 devDependencies 的根因**
   - 现象：日志明确报错：`sh: vite: not found`
   - 根因：`deploy/Dockerfile.frontend` 使用了 `RUN npm ci --only=production`，但 `vite` 与 `@vitejs/plugin-vue` 在 `frontend/package.json` 里属于 `devDependencies`，而 `npm run build` 构建阶段必须依赖它们
   - 修法：把 `RUN npm ci --only=production` 改成 `RUN npm ci`
   - 当前状态：**这个修复已经本地改掉，但截至当前还没有新的云端 run 来完成最终验证**

##### 现在真正能确认的链路位置

能确认的不是“部署已经修好了”，而是：

- **release 打包层**：已经修通，不再是当前阻塞点
- **release 上传/服务器查包层**：已经从“根本找不到当前包”推进到“能继续执行服务器部署脚本”
- **release 解压与目录切换层**：已经修通到可继续进入 compose build
- **backend 构建层**：已经能继续推进，不是当前最新报错点
- **当前最新锁定的真实阻塞点**：frontend 镜像构建时缺少 `vite` 等构建依赖

换句话说，**云端上传这里本身不是还卡在最前面，而是已经被连续修到能把问题暴露到 frontend build 这一步了。**

##### 为什么这个结论成立

逻辑链要按时间顺序看，不能只看最后一条报错：

1. 最开始卡在 runner 本地打包
   - 证据：`Create release archive` 直接报 `tar: .: file changed as we read it`
   - 说明那时连上传都还没开始

2. tar 修完后，问题推进到服务器找不到 release 包
   - 证据：`Deploy release on server` 报 `release-<sha>.tar.gz: Cannot open: No such file or directory`
   - 说明上传/落点对齐成为新的前置问题

3. 再补 runner staging + 服务器查包日志后
   - 已经能看到 release 目录快照、候选 archive、实际使用 archive、解压目录内容
   - 说明链路已经从“纯黑盒”变成“可观测”

4. 再修 `cd` 路径错误后
   - `mv: cannot stat 'release-<sha>'` 这个故障被消掉
   - 流程继续往 `docker compose up -d --build` 推进

5. 后续日志已经显示 backend build 能继续执行
   - 这说明上传、查包、解压、目录切换这些前置链路至少已经足够正确，否则根本到不了这里

6. 最新完整日志最终停在 frontend build 的 `vite: not found`
   - 这不是上传问题，不是 release 包落点问题，也不是后端健康检查问题
   - 这是一个更靠后的、并且日志能直接闭环到 `frontend/package.json` 的明确根因

所以当前最准确的说法只能是：
- **云端上传链路相关，已经做了 6 个修复**
- **其中前 5 个已经被后续运行日志验证为有效推进**
- **第 6 个（frontend `npm ci`）是当前最新根因修复，已本地落地，待下一次 push/run 验证**
- **现在不能再把问题概括成“上传还是不行”**，因为真实故障位置已经后移到了 frontend build

##### 当前仓库状态补充

当前相关文件已经处于下面这个状态：

- `.github/workflows/deploy.yml`
  - 已包含：runner.temp 打包、workspace staging 上传、服务器查包诊断、回切 `RELEASE_DIR`、compose 启动后日志与健康检查输出
- `deploy/Dockerfile.backend`
  - 已包含：Maven 基础镜像、缓存挂载、`/actuator/health` 健康检查
- `deploy/Dockerfile.frontend`
  - 已本地改成 `RUN npm ci`
  - 这个改动就是当前最新待验证修复

##### 到当前为止的正确表述

如果现在要对外描述当前进度，最准确的话应该是：

- GitHub Actions 部署链路**还没有最终验证成功**
- 但它已经从最早的“runner 打包都过不去 / server 找不到包 / release 目录移动失败”
  推进到“frontend 构建缺少 vite 依赖”
- 这说明前面的上传、查包、解压、目录切换、backend 相关修复，已经把故障层层往后推开了
- 下一次验证的重点，不再是怀疑上传是否还坏着，而是验证 `deploy/Dockerfile.frontend` 的 `npm ci` 修复是否让前端镜像成功构建


后续如果继续修这个部署问题，必须按下面顺序来，不能再跳步：

1. **先确认这次 tar 打包修复后的新 workflow 日志**
   - 必须先看 `Create release archive` 是否已经通过
   - 如果仍失败，要继续看失败时是否仍是 tar 自包含 / runner 工作目录变化问题

2. **如果 tar 已通过，再看失败停在哪一步：**
   - Upload release archive
   - Deploy release on server
   - curl health check
   - 特别是先确认 release 包在服务器上的真实落点，不能再默认它一定就在 `/root/AI-center/releases/release-<sha>.tar.gz`
   - 如果服务器上只出现旧 SHA 的残留包，而没有当前 run 的新包，优先怀疑 `scp-action` 对 runner.temp 绝对路径上传行为不稳定

3. **再拿服务器运行态事实**
   先在服务器执行并留档：
   ```bash
   docker ps --format 'table {{.Names}}\t{{.Ports}}\t{{.Status}}'
   ss -ltnp | grep -E ':3306|:6379|:8080|:8083'
   ls -la /root/AI-center
   ls -la /root/AI-center/releases
   ls -la /root/AI-center/AI-Transfer-Station
   cd /root/AI-center/AI-Transfer-Station
   docker compose -f deploy/docker-compose.all-in-one.yml ps || true
   docker compose -f deploy/docker-compose.prod.yml ps || true
   docker compose -f docker-compose.yml ps || true
   ```

3. **最后才决定下一步改哪一层**
   - 如果失败在 tar 打包：先修 runner 打包逻辑
   - 如果失败在 SCP：优先把上传来源改成 workspace staging 目录，再看服务器落点是否恢复可预测
   - 如果失败在 SSH 解压：先核实 release 包实际落点，再修服务器解压路径和查找逻辑
   - 如果失败在 compose up：修容器/环境变量/端口
   - 如果失败在 curl health check：修服务启动顺序或后端可达性

明确规则：
- **部署类问题，先拿完整失败日志和运行态状态，再改代码。**
- **GitHub Actions 新 workflow 要先保证本地打包步骤自己能稳定通过，再谈上传和服务器部署。**
- **不要再只根据部分报错就宣布”根因已经锁定”。**

#### Conversation continuation: deployment fully resolved + CORS login fix (2026-04-11, round 3)

##### 最终状态

**GitHub Actions 自动部署已完全恢复，云端可正常访问和登录。**

这轮解决了最后两个阻塞点：

**阻塞点1：CI 健康检查方式错误**

- 现象：nginx 日志报 `connect() failed (111: Connection refused) while connecting to upstream`，健康检查始终失败
- 根因叠加了两层：
  1. 健康检查 URL `http://127.0.0.1:8083/api/health` 经 nginx 转发到后端，但后端没有 `/api/health` endpoint（真实是 `/actuator/health`）
  2. 后端端口 8080 未暴露给宿主机，只能在 Docker 内网访问
- 修法：改成 `docker exec aikey-backend curl http://localhost:8080/actuator/health`，直接在容器内检查，绕开 nginx 和端口暴露问题
- 等待时间从 120s 延长到 240s（24 次 × 10s），给 Spring Boot 足够冷启动时间

**阻塞点2：CORS 白名单缺少云端地址**

- 现象：部署成功、后端正常启动、数据库账号存在密码正确，但云端浏览器无法登录
- 诊断过程：先跑两条命令确认数据层（后端日志 + 数据库查询），确认账号和密码均正常后，转向请求链路排查
- 根因：`SecurityConfig.java` 的 CORS 白名单只有 `localhost` 系列，没有 `http://111.230.113.110:8083`
- 关键机制：Spring Security 的 CORS 过滤器在请求到达 Controller 之前就检查 `Origin` 头，即使请求经过 nginx 反向代理，浏览器发出的 `Origin` 仍是前端地址，不在白名单就直接拦截
- 修法：在 `SecurityConfig.java` 的 `allowedOrigins` 列表中加入 `http://111.230.113.110:8083`

##### 这轮沉淀的规则

1. **健康检查要直接检查目标服务，不要绕路经过代理**
   - 用 `docker exec <container> curl http://localhost:<port>/<real-health-path>`
   - 不要用宿主机 curl 经过 nginx 代理检查后端
   - Spring Boot 默认健康 endpoint 是 `/actuator/health`，不是 `/api/health`

2. **「本地能用、云端不能用」的标准排查顺序**
   - 第一步：确认数据层（账号是否存在、密码是否正确、角色是否关联）
   - 数据层没问题 → 排查请求链路：**CORS 白名单 → 认证过滤器 → 业务逻辑**
   - CORS 是最容易被忽略的，因为本地走 Vite proxy 不触发 CORS，云端浏览器直接发请求才会触发

3. **部署问题要层层推进，每次只修一层**
   - 每次 CI 失败，先读完整日志确认失败停在哪一步，再改代码
   - 不要在没有新日志的情况下连续改多处
   - 修完一层后明确说「当前阻塞点已后移到 X」

##### 当前云端状态（已验证）

- CI/CD：✓ GitHub Actions 自动部署稳定运行
- 前端：✓ `http://111.230.113.110:8083` 可正常访问
- 后端：✓ 容器正常启动，数据库连通
- 登录：✓ `enterprise_admin / admin123` 可正常登录
- 部署栈：`deploy/docker-compose.all-in-one.yml`（前端 + 后端，复用服务器现有 MySQL/Redis）



### 4.1 Backend capabilities already available

下面这些能力已经存在或已经确认具备，后续优先复用，不要重复造轮子。

#### Runtime-verified user creation and member bootstrap

Files:
- `backend/src/main/java/com/aikey/controller/UserController.java`
- `backend/src/main/java/com/aikey/service/UserService.java`
- `backend/src/main/java/com/aikey/dto/user/UserCreateRequest.java`
- `backend/src/main/java/com/aikey/dto/user/UserCreateResponse.java`
- `backend/src/main/java/com/aikey/controller/TeamMemberController.java`
- `backend/src/main/java/com/aikey/service/TeamMemberService.java`

Current state:
- 已新增 `POST /api/v1/users` 最小创建用户接口
- 创建逻辑已复用 `PasswordEncoder` + `RoleRepository`，默认分配 `USER` 角色
- 运行态已成功创建两个真实测试成员：
  - `team_member_01 / test123456`
  - `team_member_02 / test123456`
- 已通过现有 `POST /api/v1/teams/1/members` 将这两个成员加入 team 1
- 已验证：
  - `GET /api/v1/users` 可看到新成员
  - 两个成员都能登录
  - `/api/v1/auth/me` 对成员返回 `roles: ["USER"]`
  - `GET /api/v1/teams/1/members` 对 admin 已能返回真实成员列表

Important note:
- 当前“创建真实用户”这条链路已经成立，后续如果再需要测试成员，优先复用接口，不要再回退到手工改 SQL 的方式。

#### Team / role baseline

Files:
- `backend/src/main/java/com/aikey/service/TeamService.java`
- `backend/src/main/java/com/aikey/controller/TeamController.java`
- `backend/src/main/java/com/aikey/entity/Team.java`
- `backend/src/main/java/com/aikey/service/AuthService.java`
- `backend/src/main/java/com/aikey/dto/auth/UserInfoResponse.java`

Current state:
- `Team.owner` 已存在，可先作为团队管理员身份基础
- `SUPER_ADMIN` 已存在，可先作为企业管理员身份基础
- `TeamService.listTeams()` 已具备“SUPER_ADMIN 看全部、其他人看自己团队”的基础方向
- `/auth/me` 已返回 `roles` 与 `isSuperAdmin`
- 团队管理页企业管理员 / 团队管理员双视角已进入实际实现和联调状态

#### Model group related

Files:
- `backend/src/main/java/com/aikey/controller/ModelGroupController.java`
- `backend/src/main/java/com/aikey/service/ModelGroupService.java`

Available APIs:
- `GET /api/v1/model-groups/all`
- `GET /api/v1/model-groups/{id}/channels`

Meaning:
- `/model-groups/all`：用于模型分组下拉
- `/model-groups/{id}/channels`：用于模型分组 -> 渠道联动加载

Confirmed direction:
- 模型分组当前就是全局资源
- 这与“模型分组是企业级资源”的产品方向一致
- 后续不要把模型分组改造成团队私有资源，除非用户再次明确要求

#### Virtual key related

Files:
- `backend/src/main/java/com/aikey/entity/VirtualKey.java`
- `backend/src/main/java/com/aikey/dto/virtualkey/VirtualKeyCreateRequest.java`
- `backend/src/main/java/com/aikey/dto/virtualkey/VirtualKeyUpdateRequest.java`
- `backend/src/main/java/com/aikey/dto/virtualkey/VirtualKeyVO.java`
- `backend/src/main/java/com/aikey/service/VirtualKeyService.java`

Confirmed direction:
- `VirtualKey` 已支持 `channelId`
- 创建 / 更新 DTO 已带 `channelId`
- `VirtualKeyService` 已围绕 `teamId + allowedGroupIds + channelId` 做约束收口
- “所有 Key 必须归属团队”的后端方向已经成立，应继续保留

#### Startup / port linkage rule

Critical files:
- `backend/src/main/resources/application.yml`
- `frontend/vite.config.js`
- `start.ps1`
- `stop.ps1`
- `docker-compose.yml`

Rule:
- 后端端口不是随便改一个文件就结束。
- 端口变更必须作为“联动变更”处理，上面 5 个文件必须一起核对。
- 否则很容易出现“账号权限看起来有问题、页面能打开但接口全挂、登录失败、Swagger 打不开、脚本误判服务状态”这类假象。

Current verified baseline:
- 本地开发基线当前已验证可运行：
  - backend: `8080`
  - frontend: `5173`
- `start.ps1` / `stop.ps1` 当前也已按这组端口完成冷启动与登录验证。

Important note:
- 端口本身不决定账号权限。
- 但端口漂移会直接影响页面能否请求到正确接口，因此非常容易被误判成“不同账号、不同页面效果不一样”。


下面这些不是这轮讨论中的假设，而是当前系统确实存在的结构缺口。

#### Team member management gap

Current state:
- 团队成员管理链路已经进入实际实现阶段
- 已有真实 `team_members` 数据，成员列表接口也已可返回真实成员
- 本轮已完成团队管理主链路 owner 边界收口：
  - `TeamManagement` 已基于 `canManageRow(row)` 和当前选中团队做前端权限控制
  - `TeamService` 已移除基于 membership 的团队列表 / 可管理团队放行
- 当前未闭环点已从“普通成员会误进管理页”转成“还需要继续核对剩余页面和真实联调结果是否完全一致”，重点是：
  - 是否还存在其他未检查页面沿用旧的团队上下文假设
  - 是否还存在运行态账号 / 数据与文档描述漂移
- 后续开发应继续围绕团队页工作区、团队上下文页面和真实账号联调推进，而不是回到旧登录问题反复排查

#### Frontend role rendering gap

Files:
- `frontend/src/views/TeamManagement.vue`
- `frontend/src/components/MainLayout.vue`
- `frontend/src/router/index.js`

Current problem:
- 当前已经有企业管理员身份提示和团队页双视角基础
- 但导航、角色渲染、页面职责仍未完全彻底收口
- 还需要继续做到“企业管理员看到团队创建配置，团队管理员看到团队运营工作台”

#### Frontend anti-pattern already found

Known bad pattern:
- 不应使用前端本地用户名硬编码决定权限

Rule:
- 权限由后端真实判定
- 前端展示分支应基于后端返回的角色 / 身份信息

---

## 5. Required Next Development Focus

如果后续继续开发，优先按下面顺序推进。

### Step 1: Remove `admin` from bootstrap super admin chain

Target files:
- `backend/src/main/resources/db/data.sql`
- `backend/src/main/java/com/aikey/config/DataInitializer.java`
- `backend/src/main/java/com/aikey/config/DataRepairRunner.java`
- `backend/src/main/java/com/aikey/service/AuthService.java`

Focus:
1. 保留 `enterprise_admin` 作为 bootstrap 超管账号
2. 把 `admin` 从 bootstrap 超管名单里移除
3. 避免启动期、修复期、登录期继续把 `admin` 自动往 `SUPER_ADMIN` 方向修
4. 先收口后端身份真相源，再继续前端联调

### Step 2: Verify owner view entry with real accounts

Target files / pages:
- `frontend/src/views/TeamManagement.vue`
- `frontend/src/components/MainLayout.vue`
- `backend/src/main/java/com/aikey/service/AuthService.java`
- `backend/src/main/java/com/aikey/dto/auth/UserInfoResponse.java`

Focus:
1. 用真实 `Team.owner` 账号核对 `/api/v1/auth/me` 是否稳定返回 `isTeamOwner=true`
2. 核对团队页初始化后 `isSuperAdmin / isTeamOwner` 如何落到实际渲染分支
3. 查清为什么 owner 账号在系统性 500 已修复后，仍没有进入团队管理员视角
4. 先收口 owner 视角问题，再继续扩展其他页面联调

### Step 3: Run real-account integration checks on the tightened boundary

Target files / pages:
- `frontend/src/views/TeamManagement.vue`
- `frontend/src/views/TokenManagement.vue`
- `frontend/src/views/ProjectManagement.vue`
- `frontend/src/views/Analytics.vue`
- `backend/src/main/java/com/aikey/service/TeamService.java`

Focus:
1. 用 `SUPER_ADMIN` / `Team.owner` / 普通成员三类真实账号分别验证页面与接口行为
2. 验证 401/403、空团队、空项目、手改 query 上下文时前端是否进入正确失败态
3. 验证团队页进入 Token / 项目 / Analytics 的链路是否与 owner 边界完全一致

### Step 3: Continue polishing TeamManagement dual-role behavior

Target file:
- `frontend/src/views/TeamManagement.vue`

Focus:
1. 企业管理员视角继续收口团队列表、团队配置、管理员指定能力
2. 团队管理员视角继续收口成员管理、团队内发 Key、团队上下文操作
3. 明确哪些交互是企业管理员专属，哪些是团队管理员专属

### Step 3: Align backend team permission boundary

Target files:
- `backend/src/main/java/com/aikey/service/TeamService.java`
- `backend/src/main/java/com/aikey/controller/TeamController.java`
- `backend/src/main/java/com/aikey/service/AuthService.java`
- `backend/src/main/java/com/aikey/dto/auth/UserInfoResponse.java`

Focus:
1. 企业管理员（SUPER_ADMIN）创建团队
2. 非企业管理员只能看到自己作为 `Team.owner` 的团队
3. 企业管理员与团队管理员更新团队时能力边界分开

### Step 4: Keep model groups enterprise-level

Target files:
- `backend/src/main/java/com/aikey/service/ModelGroupService.java`
- `backend/src/main/java/com/aikey/controller/ModelGroupController.java`
- `frontend/src/views/TeamManagement.vue`

Focus:
1. 模型分组创建 / 编辑 / 删除仍由企业管理员负责
2. 团队页里只出现配置团队可用模型分组与查看团队可用模型分组
3. 不要把团队页继续演化成模型分组创建页

### Step 5: Stabilize team-scoped key issuance

Target files:
- `frontend/src/views/TeamManagement.vue`
- `frontend/src/views/TokenManagement.vue`
- `backend/src/main/java/com/aikey/service/VirtualKeyService.java`

Focus:
1. 保持所有 Key 必须归属团队
2. 保持 Key 创建必须带团队上下文
3. 企业管理员可从团队列表进入发放流程
4. 团队管理员可直接在自己团队上下文里发放
5. 继续核对 Token 页对团队上下文、团队成员、所属项目的加载是否完全遵循 `SUPER_ADMIN` / `Team.owner` 边界

### Step 6: Decide whether to formalize enterprise admin bootstrap

Target files:
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/db/data.sql`
- `backend/src/main/java/com/aikey/config/DataInitializer.java`
- `backend/src/main/java/com/aikey/config/DataRepairRunner.java`
- `backend/src/main/java/com/aikey/service/AuthService.java`

Focus:
1. 决定 `enterprise_admin` 是否作为长期引导账号保留
2. 如果保留，应确定正式迁移 / 初始化方案
3. 避免后续继续依赖手工补库才能在已有环境使用企业管理员账号

---

## 6. Acceptance Criteria

后续开发和文档同步，至少要满足下面这些标准。

### 6.1 Documentation accuracy

- `SUMMARY.md` 不再把旧登录故障写成当前主问题
- 文档明确写出 `enterprise_admin` 已可登录
- 文档明确写出已有数据库不会自动应用 `db/data.sql` 变更
- 文档结论与当前代码和运行态一致

### 6.2 Role visibility

- 企业管理员登录后：
  - 能进入企业管理员视角
  - 能看到团队创建与配置能力
- 团队管理员登录后：
  - `/api/v1/auth/me` 稳定返回 `isTeamOwner=true`
  - 能真正进入团队管理员视角与团队工作区
  - 只能看到自己团队相关内容
  - 不看到企业级资源创建入口
- 普通团队成员登录后：
  - 不应因 membership 被前端误导进入团队管理 / 项目管理 / 团队发 Key 主流程
  - 在团队、项目、Analytics 等页面应看到明确的空状态或无权限提示，而不是错误进入工作区
- 若账号同时具备 `SUPER_ADMIN` 与 `isTeamOwner`：
  - 当前产品 / 实现规则下，页面应优先进入企业管理员视角
  - 不应同时把该账号渲染成团队管理员主视角

### 6.3 Team page behavior

- 团队管理页是双视角页
- 企业管理员视角与团队管理员视角在同一页面内切换展示
- 页面职责不再混成“谁都能新建团队、谁都能配企业资源”

### 6.4 Key behavior

- 所有 Key 必须归属团队
- Key 创建必须带团队上下文
- 团队页到发 Key 链路稳定可用

---

## 7. Environment Notes

### 7.1 Runtime config reality

关键事实：
- `backend/src/main/resources/application.yml` 当前使用 `ddl-auto: validate`
- Flyway 已启用
- 因此已有数据库不会因为 `db/data.sql` 变更自动补账号

### 7.2 Enterprise admin login debugging takeaway

本轮排查出的真实结论：
- `enterprise_admin` 最终已可成功登录
- 但这次成功不应被表述为“只要改了代码就自动生效”
- 真正应该记住的是：涉及账号、角色、种子数据时，必须验证数据库运行态和接口返回，而不是只根据代码改动下结论

### 7.3 Admin role misclassification takeaway

本次新增确认：
- `admin` 被识别为企业管理员，当前高概率根因不是前端标签渲染，而是后端身份真相源冲突
- `db/data.sql` 把 `admin` 定义为 `USER + Team.owner`
- 但 `DataInitializer` / `DataRepairRunner` / `AuthService` 又把 `admin` 纳入 bootstrap super admin 处理链路
- 因此只要后端继续保留这条链路，`admin` 的运行态身份就可能继续漂移

额外运行态证据：
- 当前本地 8080 已有项目后端实例在运行，重复 `spring-boot:run` 会直接端口冲突
- 当前运行态 `admin/admin123` 登录返回 401，说明已有数据库与种子预期并不完全一致

后续规则：
- 不要再把 `admin` 身份问题简单归因成前端展示错
- 必须先对齐 SQL 定义、bootstrap 初始化、运行态数据库和 `/auth/me` 返回，再决定代码修改方向

---

## 8. One-Sentence Takeaway

**当前项目已进入「部署链路完全恢复、云端可正常登录」的阶段；下一步重点是收口 `admin` 身份漂移问题（从 bootstrap 超管链路中移除），再用真实账号验证企业管理员 / 团队管理员双视角。**

---

## 9. Pending Feature: 全局调度策略切换（2026-04-18）

### 背景

四种调度策略（轮询、加权轮询、最低延迟、最低成本）代码已全部实现，但 `GatewayOrchestrationService.java` 第 190 行硬编码为 `DispatchStrategyType.WEIGHTED`，运行时只走加权轮询。需要接入前端可切换的全局策略配置，作为比赛答辩创新点展示。

### 方案：全局配置，最小改动

不新建设置页面，在 Overview 总览页加策略选择器，后端用已有的 `system_configs` 表存一条记录。

### 改动清单

**后端（4 个文件）**

1. **新建** `backend/src/main/java/com/aikey/entity/SystemConfig.java`
   - 映射 `system_configs` 表，只需 id / configGroup / configKey / configValue 核心字段

2. **新建** `backend/src/main/java/com/aikey/repository/SystemConfigRepository.java`
   - 只需 `findByConfigGroupAndConfigKey(String group, String key)` 方法

3. **新建** `backend/src/main/java/com/aikey/controller/DispatchConfigController.java`
   - `GET /api/v1/dispatch/strategy` — 读取当前策略，默认 WEIGHTED
   - `PUT /api/v1/dispatch/strategy` — 更新策略

4. **修改** `backend/src/main/java/com/aikey/service/GatewayOrchestrationService.java`
   - 注入 `SystemConfigRepository`
   - 第 190 行从数据库读取 `configGroup="dispatch", configKey="strategy"`，替代硬编码
   - 读不到时 fallback 为 WEIGHTED

**数据库（1 个迁移）**

5. **新建** `backend/src/main/resources/db/migration/V4__add_dispatch_strategy_config.sql`
   - `INSERT INTO system_configs (config_group, config_key, config_value, config_type, description) VALUES ('dispatch', 'strategy', 'WEIGHTED', 'string', '全局调度策略');`

**前端（2 个文件）**

6. **新建** `frontend/src/api/dispatch.js`
   - 封装 GET / PUT 调度策略接口

7. **修改** `frontend/src/views/Overview.vue`
   - 在总览页加 el-select 下拉框（加权轮询 / 轮询 / 最低延迟 / 最低成本）
   - 页面加载时 GET 当前策略，切换时 PUT 更新

### 不改动的文件

- `DispatchService.java` — 已通过参数接收策略类型，无需改动
- 四个策略实现类 — 已完整实现
- `SecurityConfig` — 需确认 `/api/v1/dispatch/**` 是否在认证路径内

### 验证方式

1. 登录后在总览页看到策略选择器，默认「加权轮询」
2. 切换为「最低延迟」，刷新页面后仍为「最低延迟」
3. 发 API 调用，查看日志确认 `strategyUsed` 字段变为 `LOWEST_LATENCY`

---

## 5. v2.0 Phase 1 总结（2026-04-21）

### 5.1 本轮完成的工作

**T1：智谱渠道接入（核心任务）**

| 子任务 | 状态 |
|--------|------|
| T1.1 智谱渠道种子数据（data.sql）| ✅ 完成 |
| T1.2 ProxyForwardService 适配智谱 URL 规范 | ✅ 完成 |
| T1.3 渠道连通性测试前端接入 | ✅ 完成 |

**运行态阻塞修复（8 项）**

| # | 问题 | 结果 |
|---|------|------|
| 1 | spring.sql.init.mode=always 每次重启清库 | ✅ 固定为 never |
| 2 | 种子数据 quota=0 触发三层漏斗全拒绝 | ✅ 修复初始额度 |
| 3 | team_members / projects 种子不完整 | ✅ 补齐所有 NOT NULL 字段 |
| 4 | model_groups.models 格式不一致 | ✅ 服务层加 legacy 兼容 |
| 5 | quota_transactions 表缺 target_type / target_id 列 | ✅ schema.sql + patch_v2.sql 补齐 |
| 6 | GatewayOrchestrationService 用 stale 实体 save 覆盖额度 | ✅ 改为定向 updateLastUsedTime |
| 7 | start.ps1 不应参与初始化 | ✅ 职责收口，仅做就绪检查 |
| 8 | ChannelManagement.vue 编辑态 API Key 交互误导 | ✅ 增加已保存掩码提示 |

### 5.2 当前系统状态

**已验证可用**

| 功能 | 验证方式 |
|------|---------|
| 虚拟 Key 鉴权网关 | curl Bearer 测试 |
| 三层额度漏斗扣减 + 流水 | quota_transactions 三条记录 |
| 智谱渠道代理转发（非流式）| Mock 联调通过 |
| 渠道连通性测试 | /api/v1/channels/{id}/test |
| 渠道管理 API Key CRUD | 编辑留空保持不变 |
| 启动脚本（start.ps1）| 实跑输出符合预期 |

**未完成 / 待验证**

| 项 | 状态 | 说明 |
|----|------|------|
| 智谱真实 API Key 端到端 | ⚠️ 未验证 | Mock 通了，真实 Key 未跑 |
| 流式支持（stream=true）| ❌ 未实现 | T2.1，当前直接返回 400 |
| 日志导出 CSV | ❌ 未实现 | T3.1/T3.2 |
| 告警引擎 | ❌ 未实现 | T4.1/T4.2 |

### 5.3 核心调用链路（非流式，当前实现）

```
调用方 POST /v1/chat/completions (Bearer 虚拟Key)
  → VirtualKeyAuthFilter (鉴权 + 黑名单)
    → GatewayOrchestrationService (编排器)
      → RateLimitService (QPM/QPD, Redis)
      → QuotaService.checkQuotaWithFunnel (三层预检: VirtualKey → Team → Project)
      → DispatchService.dispatch (4策略选渠道)
      → ProxyForwardService.forwardChatCompletion
          (AES解密 RealKey → buildUrl 构建目标URL → RestTemplate 转发)
      → QuotaService.deductQuotaWithFunnel (三层原子扣减)
      → CallLogService.recordAsync (异步写 call_logs + quota_transactions)
```

**关键：buildUrl 渠道类型判断**（T1.2 核心改动）

```java
boolean isZhipu = "zhipu".equalsIgnoreCase(channelType)
    || normalizedBase.contains("/api/paas/v4");
// 智谱：直接 baseUrl + path（不加 /v1）
// OpenAI 兼容：baseUrl + /v1 + path
```

### 5.4 关键数据模型

```
Team → 创建 VirtualKey → 绑定 ModelGroup → 实际使用包含的 Channel
                                              ↑
                                         Channel 关联 RealKey (AES加密存储)
```

三层额度漏斗：`VirtualKey.quota_remaining` → `Team.quota_remaining` → `Project.quota_remaining`，任意一层不足即拒绝。

### 5.5 接手后 P0 任务：真实 Key 端到端验证

当前智谱渠道 base_url 指向本地 Mock，需换回真实地址：

1. 渠道管理页 → 编辑「智谱AI」，base_url 改为 `https://open.bigmodel.cn/api/paas/v4`，填入真实 API Key
2. 真实 Key 管理页 → 为该渠道新增真实 Key
3. 新建虚拟 Key，绑定智谱模型分组，额度设非零
4. 验证命令：

```powershell
$resp = Invoke-RestMethod -Method Post `
  -Uri "http://127.0.0.1:8080/v1/chat/completions" `
  -Headers @{ Authorization = "Bearer <虚拟Key>"; "Content-Type" = "application/json" } `
  -Body '{"model":"glm-4.7-flash","messages":[{"role":"user","content":"你好"}]}'
$resp.choices[0].message.content
```

5. 检查 call_logs 和 quota_transactions 是否有新记录

### 5.6 下一步任务（按优先级）

| 优先级 | 任务 |
|--------|------|
| P0 | 真实智谱 Key 端到端验证（参见 5.5）|
| P1 | T2.1 流式 SSE 支持（stream=true 当前直接 400）|
| P2 | T3.1 日志 CSV 导出 |
| P3 | T4.1 告警规则 CRUD |

### 5.7 本地快速启动

```powershell
# 首次或需重建数据库
.\init-db.ps1

# 日常启动
.\start.ps1

# 访问
# 前端：http://localhost:5173
# 后端：http://localhost:8080
# Swagger：http://localhost:8080/swagger-ui/index.html
# 默认账号：admin / admin123
```

> `start.ps1` 只检查就绪状态，不做任何初始化。数据库为空时提示先运行 `init-db.ps1`。

---

## 6. v2.0 Phase 2 总结（2026-04-21 ~ 04-22）

### 6.1 本轮完成的工作

**T2.1 SSE 流式转发（完成）**

| 子任务 | 状态 |
|--------|------|
| GatewayController 增加流式分支，返回 SseEmitter | ✅ 完成 |
| ProxyForwardService.forwardChatCompletionStream | ✅ 完成 |
| GatewayOrchestrationService 删除 400 占位拦截，新增流式编排方法 | ✅ 完成 |
| TokenManagement.vue「接入信息」弹窗 | ✅ 完成 |
| deploy/nginx.conf SSE 支持（proxy_buffering off） | ✅ 完成 |
| vite.config.js /v1 代理 | ✅ 完成 |

**curl 端到端验证（已通）**

```bash
curl -N --max-time 25 -X POST http://localhost:8080/v1/chat/completions \
  -H "Authorization: Bearer sk-e745a6b4ea3b469880485138f67e43bb" \
  --data-binary @req.json
# req.json: {"model":"glm-4.7","messages":[...],"stream":true}
```

- 返回 33KB SSE 数据流，`data: {...}` 格式逐行正确
- 末行为 `data: [DONE]`
- `call_logs` 有对应记录，`quota_transactions` 有三层扣减流水
- 后端 PID 42340，持续运行于 8080

**CC Switch 客户端验证（已搁置，不影响开发）**

- CC Switch 选 `OpenAI Chat Completions API` 格式后仍报 502
- 确认原因：CC Switch 内部走云端中转，不支持 localhost 直连，非后端问题
- 不再追查此路径，以 curl 验证为准

### 6.2 当前核心调用链路（含流式）

```
调用方 POST /v1/chat/completions (Bearer 虚拟Key, stream=true)
  → VirtualKeyAuthFilter (鉴权 + 黑名单)
    → GatewayController
      → stream=true ? processChatCompletionStream() : processChatCompletion()

非流式路径（原有，不变）：
  → GatewayOrchestrationService.processChatCompletion
    → (RateLimit → QuotaCheck → Dispatch → ProxyForward → Deduct → Log)
    → ResponseEntity<ChatCompletionResponse>

流式路径（T2.1 新增）：
  → GatewayOrchestrationService.processChatCompletionStream
    → 主线程同步预检：(RateLimit → QuotaCheck → Dispatch)
    → 主线程取 VirtualKeyAuthContext（ThreadLocal，传给 async 避免丢失）
    → SseEmitter(5min 超时) 立即返回给 Controller
    → CompletableFuture.runAsync():
        → ProxyForwardService.forwardChatCompletionStream
          (AES解密 RealKey → HttpClient → BodyHandlers.ofLines() → emit 每行)
        → 流结束：handleSuccess(合成 Response+UsageInfo) → Deduct + Log
        → 流失败：handleFailure() → 推 error 事件 → emitter.complete()
```

### 6.3 关键参数（测试/接入必用）

| 参数 | 值 |
|------|-----|
| 后端地址 | `http://localhost:8080` |
| OpenAI 兼容端点 | `http://localhost:8080/v1` |
| 测试用虚拟 Key | `sk-e745a6b4ea3b469880485138f67e43bb` |
| 测试模型名（DB 中实际值）| `glm-4.7` |
| SseEmitter 超时 | 5 分钟 |
| SSE 实现 | Java 11 原生 HttpClient（Zero new Maven deps）|

### 6.4 当前系统状态

| 功能 | 状态 |
|------|------|
| 非流式网关链路 | ✅ 已验证 |
| 流式 SSE 转发 | ✅ curl 已验证 |
| 三层额度漏斗（含流式） | ✅ quota_transactions 有记录 |
| 智谱渠道代理（非流式 Mock）| ✅ 已验证 |
| 智谱真实 Key 端到端 | ⚠️ 未验证（真实 Key 需在渠道管理页手动录入） |
| 日志导出 CSV | ❌ T3.1 未开始 |
| Overview 首页真实数据 | ❌ T5.1/T5.2 未开始 |
| 告警规则 CRUD | ❌ T4.1 未开始 |

### 6.5 下一步任务（Phase 3/4/5，可并行）

按计划文档，Phase 3 / Phase 4 / Phase 5 彼此无依赖，可任意顺序推进：

| 优先级 | 任务 | 估计改动范围 |
|--------|------|------------|
| 推荐先做 | T5.1 + T5.2 Overview 真实数据 | 新增 DashboardController + Service，修改 Overview.vue |
| 推荐先做 | T3.1 日志导出 CSV | CallLogController 新增 export 端点，RequestLog.vue 加按钮 |
| 中 | T4.1 告警规则 CRUD | 新建 Entity/Repo/Service/Controller + AlertManagement.vue |
| 中 | T3.2 流水导出 | 与 T3.1 同模式，QuotaFlow.vue |
| 低 | T3.3 日志详情增强 | 纯前端弹窗 |
| 低 | T4.2 告警引擎 | 定时任务，T4.1 完成后 |
| 低 | T4.3 告警通知展示 | 铃铛 Badge，T4.2 完成后 |
| 收口 | T7.3 清理 /skills 路由 | 与 T4.1 一起改 router + MainLayout |

### 6.6 PowerShell curl 测试规范（避免坑）

```powershell
# 1. 把 JSON 写到临时文件（不能在 PowerShell 里内联 JSON 给 curl.exe）
'{"model":"glm-4.7","messages":[{"role":"user","content":"你好"}],"stream":true}' `
  | Out-File -Encoding utf8 -FilePath "$env:TEMP\req.json" -NoNewline

# 2. 非流式测试
curl.exe -s -X POST "http://localhost:8080/v1/chat/completions" `
  -H "Authorization: Bearer sk-e745a6b4ea3b469880485138f67e43bb" `
  -H "Content-Type: application/json" `
  --data-binary "@$env:TEMP\req.json"

# 3. 流式测试（-N 关闭缓冲）
curl.exe -N --max-time 25 -s -X POST "http://localhost:8080/v1/chat/completions" `
  -H "Authorization: Bearer sk-e745a6b4ea3b469880485138f67e43bb" `
  -H "Content-Type: application/json" `
  --data-binary "@$env:TEMP\req.json"
```

> 坑：`model_code` 必须用 DB 里的实际值。查询命令：
> ```sql
> SELECT model_code FROM models WHERE deleted=0;
> ```

---

## 10. v2.0 全量进度与下一步（2026-04-22 更新）

> 本节是「当前最新状态」的权威记录，接手时以本节为准，忽略上方旧版 Step 1-6 任务列表。

### 10.1 已完成（Phase 1 ~ Phase 4 + v2.1 修复）

| 阶段 | 任务 | 状态 | 说明 |
|------|------|------|------|
| Phase 1 | T1.1 智谱渠道种子数据 | ✅ | data.sql + Flyway 已含智谱渠道 |
| Phase 1 | T1.2 ProxyForwardService 智谱 URL 适配 | ✅ | buildUrl 分支：isZhipu 判断 |
| Phase 1 | T1.3 渠道连通性测试按钮 | ✅ | `/api/v1/channels/{id}/test` 已上线 |
| Phase 2 | T2.1 SSE 流式转发 | ✅ | curl 验证通过，quota_transactions 有记录 |
| Phase 3 | T3.1 日志 CSV 导出 | ✅ | `/api/v1/logs/export` |
| Phase 3 | T3.2 流水 CSV 导出 | ✅ | `/api/v1/quota/transactions/export` |
| Phase 3 | T3.3 日志详情弹窗增强 | ✅ | RequestLog.vue 详情卡片已更新 |
| Phase 4 | T4.1 告警规则 CRUD | ✅ | `/api/v1/alert-rules/**` |
| Phase 4 | T4.2 告警触发引擎 | ✅ | `@Scheduled` 每分钟扫描 call_logs |
| Phase 4 | T4.3 告警铃铛展示 | ✅ | MainLayout.vue 铃铛 + Badge |
| Phase 7 | T7.3 清理 /skills 路由 | ✅ | router/index.js 已移除 |
| v2.1 修复 | ChatMessage.content: String → JsonNode | ✅ | 兼容牛马AI/Cherry Studio 的 Array 格式 |

**云端部署**：`http://111.230.113.110:8083` 已可正常访问（GitHub Actions + all-in-one compose）

---

### 10.2 ⭐ 下一步：T5.1 + T5.2 — Overview 首页接入真实数据（高优先级）

#### T5.1 — 后端统计 API

**目标**：为首页概览卡片提供真实数据，去掉硬编码 Mock 值。

**新增文件**：
- `backend/src/main/java/com/aikey/controller/DashboardController.java`
- `backend/src/main/java/com/aikey/service/DashboardService.java`

**接口**：`GET /api/v1/dashboard/overview`

**返回结构**：
```json
{
  "todayCalls": 128,
  "todayTokens": 24560,
  "todayCost": 12.80,
  "avgResponseTime": 156,
  "todayCallsTrend": 12.5,
  "todayTokensTrend": 8.3,
  "todayCostTrend": -5.2,
  "avgResponseTimeTrend": 2.1
}
```

**SQL 实现参考**：
```sql
-- todayCalls
SELECT COUNT(*) FROM call_logs WHERE DATE(created_at) = CURDATE();
-- todayTokens
SELECT COALESCE(SUM(total_tokens), 0) FROM call_logs WHERE DATE(created_at) = CURDATE();
-- todayCost
SELECT COALESCE(SUM(cost), 0) FROM call_logs WHERE DATE(created_at) = CURDATE();
-- avgResponseTime（仅成功调用）
SELECT COALESCE(AVG(response_time), 0) FROM call_logs
  WHERE DATE(created_at) = CURDATE() AND status = 1;
-- trend = (今日值 - 昨日值) / 昨日值 * 100，昨日值为 0 时 trend = 0
```

**验收标准**：
1. `GET /api/v1/dashboard/overview` 返回 200，字段不为 null
2. todayCalls 与 `call_logs` 当日行数一致
3. trend 字段为数值，昨日无数据时为 0 而非 NaN/null

---

#### T5.2 — 前端 Overview 接入真实数据

**改动文件**：
- `frontend/src/api/dashboard.js` — 新增 `getOverviewStats()` 方法
- `frontend/src/views/Overview.vue` — onMounted 调用 API，去掉硬编码

**验收标准**：
1. 页面刷新后概览卡片数字与数据库实际数据吻合
2. trend 箭头方向与数值符号一致（正数朝上，负数朝下）
3. API 失败时有合理 fallback（不显示 NaN，可显示 `-`）

---

### 10.3 弱保留 / 备选任务

| 任务 | 优先级 | 说明 |
|------|--------|------|
| T6.1 模型市场页面 | 弱保留 | 不纳入当前排期，有余力再做 |
| T7.1 个人中心页面 | 备选 | 有余力再做 |
| T7.2 登录日志实装 | 备选 | 有余力再做 |

---

### 10.4 文档目录（2026-04-22 整理后）

所有规划文档已从 `docs/` 迁移至 `.trae/`，导航入口：[`.trae/INDEX.md`](.trae/INDEX.md)
