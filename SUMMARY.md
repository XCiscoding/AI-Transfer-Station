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
     - release 包实际已经上传到了 `/root/AI-center/releases/github/runner_temp/release-<sha>.tar.gz`
     - 但 ssh 脚本第一次修法里，`find` 虽然把路径打印出来了，脚本仍然继续进入“找不到文件”分支
   - 这说明当时还不只是“路径没对齐”，还包括：
     - **shell 里的 archive 路径赋值 / 判空逻辑本身也没有写稳**
   - 当前正确修正方向应当是：
     - 上传目标路径显式收口
     - ssh 解压前先把 `find` 结果赋值给变量，再显式打印最终使用的 archive 路径
     - 找不到时再打印 releases 目录文件清单后失败退出

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
- 后续日志又进一步确认：release 包其实已经上传到了 `/root/AI-center/releases/github/runner_temp/...`，只是 ssh 脚本第一次补的 `find + 变量赋值` 逻辑还不够稳，导致“明明找到了路径，仍然按未找到处理”

所以当前只能确认：
- **新方案已经写了**
- **但不能确认新方案已经在线上真实跑通**
- **并且需要先修掉 runner 打包阶段的 tar 自包含失败问题，后面才谈得上继续验证上传和部署**
- **在此基础上，还要把 scp 上传落点和服务器解压查找逻辑对齐，后面才能继续验证 compose 启动链路**

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
2. **之前至少出现过五类真实失败点：**
   - 服务器脏工作区拦截 `git pull`
   - 服务器访问 GitHub HTTPS 不稳定（`Empty reply from server`）
   - 服务器已有服务/旧栈占用端口（至少出现过 3306 冲突）
   - GitHub runner 本地打包 release 时触发 `tar: .: file changed as we read it`
   - release 包上传后，SSH 解压阶段找不到 `release-<sha>.tar.gz`
3. **当前仓库里的“新部署方案”已经改成：**
   - 前端入口目标：`http://111.230.113.110:8083`
   - 正式部署栈：`deploy/docker-compose.all-in-one.yml`
   - 自动部署策略：GitHub runner 打包 → SCP 上传 → SSH 解压部署
4. **但在没有拿到这次新 workflow 的完整失败日志之前，不能再声称“已经修好”**

#### Correct next rule

后续如果继续修这个部署问题，必须按下面顺序来，不能再跳步：

1. **先确认这次 tar 打包修复后的新 workflow 日志**
   - 必须先看 `Create release archive` 是否已经通过
   - 如果仍失败，要继续看失败时是否仍是 tar 自包含 / runner 工作目录变化问题

2. **如果 tar 已通过，再看失败停在哪一步：**
   - Upload release archive
   - Deploy release on server
   - curl health check
   - 特别是先确认 release 包在服务器上的真实落点，不能再默认它一定就在 `/root/AI-center/releases/release-<sha>.tar.gz`

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
   - 如果失败在 SCP：修上传链路
   - 如果失败在 SSH 解压：先核实 release 包实际落点，再修服务器解压路径和查找逻辑
   - 如果失败在 compose up：修容器/环境变量/端口
   - 如果失败在 curl health check：修服务启动顺序或后端可达性

明确规则：
- **部署类问题，先拿完整失败日志和运行态状态，再改代码。**
- **GitHub Actions 新 workflow 要先保证本地打包步骤自己能稳定通过，再谈上传和服务器部署。**
- **不要再只根据部分报错就宣布“根因已经锁定”。**


## 4. Current Technical State

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

**当前项目已经进入“`admin` 身份真相源需要先收口、团队管理双视角规则已明确”的阶段；后续最重要的不是继续猜前端显示，而是先对齐后端 bootstrap 身份链路，再用真实账号验证企业管理员 / 团队管理员双视角。**
