# MVP 测试数据与使用指南

## 文档信息
- **版本**: V1.0
- **创建日期**: 2026-04-06
- **适用对象**: 功能测试人员、演示用户
- **关联文档**: MVP-测试通过标准-CHECKLIST.md、MVP-测试常见问题-TROUBLESHOOTING.md

---

## 一、系统启动说明

### 1.1 启动命令

```bash
# 1. 启动MySQL和Redis（如使用Docker）
docker-compose up -d mysql redis

# 2. 启动后端（新开终端）
cd backend
./mvnw spring-boot:run
# 或 Windows: mvnw.cmd spring-boot:run

# 3. 启动前端（新开终端）
cd frontend
npm run dev
```

### 1.2 访问地址

| 服务 | 地址 | 说明 |
|------|------|------|
| 前端界面 | http://localhost:5173 | 主要操作入口 |
| 后端API | http://localhost:8080 | API接口 |
| Swagger文档 | http://localhost:8080/swagger-ui.html | API调试 |

### 1.3 默认登录账号

```
用户名: admin
密码: admin123
```

---

## 二、模拟测试数据

### 2.1 渠道模拟数据（可直接使用）

#### 渠道1：OpenAI 模拟渠道
```yaml
渠道名称: OpenAI官方
渠道编码: openai-official
渠道类型: openai
Base URL: https://api.openai.com/v1
API Key: sk-test-openai-demo-key-123456789
状态: 启用
优先级: 1
轮询策略: round_robin
```

#### 渠道2：Claude 模拟渠道
```yaml
渠道名称: Claude Anthropic
渠道编码: claude-anthropic
渠道类型: claude
Base URL: https://api.anthropic.com/v1
API Key: sk-ant-test-demo-key-987654321
状态: 启用
优先级: 2
轮询策略: round_robin
```

#### 渠道3：通义千问 模拟渠道
```yaml
渠道名称: 阿里通义千问
渠道编码: qwen-tongyi
渠道类型: qwen
Base URL: https://dashscope.aliyuncs.com/api/v1
API Key: sk-test-qwen-demo-key-abcdef
状态: 启用
优先级: 3
轮询策略: round_robin
```

#### 渠道4：百度文心 模拟渠道
```yaml
渠道名称: 百度文心一言
渠道编码: wenxin-baidu
渠道类型: wenxin
Base URL: https://aip.baidubce.com/rpc/2.0
API Key: sk-test-wenxin-demo-key-xyz789
状态: 启用
优先级: 4
轮询策略: round_robin
```

#### 渠道5：测试禁用渠道
```yaml
渠道名称: 测试渠道-已禁用
渠道编码: test-disabled
渠道类型: openai
Base URL: https://api.test.com/v1
API Key: sk-test-disabled-demo-key
状态: 禁用
优先级: 5
轮询策略: round_robin
```

### 2.2 真实Key模拟数据

> ⚠️ **重要**: 以下Key都是**模拟数据**，仅用于测试界面功能，不能调用真实API

#### Key 1：OpenAI 生产Key（正常）
```yaml
Key名称: OpenAI-生产环境主Key
Key值: sk-prod-openai-xxxxxxxxxxxxxxxxxxxx
所属渠道: OpenAI官方
状态: 正常
```

#### Key 2：Claude 测试Key（正常）
```yaml
Key名称: Claude-测试环境Key
Key值: sk-ant-test-yyyyyyyyyyyyyyyyyyyy
所属渠道: Claude Anthropic
状态: 正常
```

#### Key 3：通义千问 Key（正常）
```yaml
Key名称: 通义千问-开发环境
Key值: sk-qwen-dev-zzzzzzzzzzzzzzzzzzzz
所属渠道: 阿里通义千问
状态: 正常
```

#### Key 4：文心一言 Key（正常）
```yaml
Key名称: 文心一言-生产环境
Key值: sk-wenxin-prod-aaaaaaaaaaaaaaaaaa
所属渠道: 百度文心一言
状态: 正常
```

#### Key 5：OpenAI 备用Key（正常）
```yaml
Key名称: OpenAI-备用Key
Key值: sk-prod-openai-bbbbbbbbbbbbbbbbbb
所属渠道: OpenAI官方
状态: 正常
```

#### Key 6：Claude 备用Key（正常）
```yaml
Key名称: Claude-备用Key
Key值: sk-ant-backup-cccccccccccccccccc
所属渠道: Claude Anthropic
状态: 正常
```

#### Key 7：通义千问 Key（即将耗尽）
```yaml
Key名称: 通义千问-额度紧张
Key值: sk-qwen-limit-dddddddddddddddddd
所属渠道: 阿里通义千问
状态: 耗尽
剩余次数: 99999
```

#### Key 8：文心一言 Key（已过期）
```yaml
Key名称: 文心一言-已过期
Key值: sk-wenxin-exp-eeeeeeeeeeeeeeeeee
所属渠道: 百度文心一言
状态: 过期
过期时间: 2026-03-01
```

#### Key 9：OpenAI Key（已禁用）
```yaml
Key名称: OpenAI-已禁用
Key值: sk-openai-disabled-ffffffffffffff
所属渠道: OpenAI官方
状态: 禁用
```

#### Key 10：测试Key（无效）
```yaml
Key名称: 测试-无效Key
Key值: sk-test-invalid-ggggggggggggggggg
所属渠道: 测试渠道-已禁用
状态: 无效
```

### 2.3 虚拟Key模拟数据

#### 虚拟Key 1：Token配额类型
```yaml
Key名称: 前端团队-Token配额
Key值: sk-a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6
所属用户: admin
配额类型: token
额度上限: 1000000
已用额度: 125000
剩余额度: 875000
每分钟限制: 60
状态: 启用
```

#### 虚拟Key 2：次数配额类型
```yaml
Key名称: 后端团队-次数配额
Key值: sk-q7r8s9t0u1v2w3x4y5z6a7b8c9d0e1f2
所属用户: admin
配额类型: count
额度上限: 10000
已用额度: 2345
剩余额度: 7655
每分钟限制: 120
状态: 启用
```

#### 虚拟Key 3：金额配额类型
```yaml
Key名称: 测试团队-金额配额
Key值: sk-g3h4i5j6k7l8m9n0o1p2q3r4s5t6u7v8
所属用户: admin
配额类型: amount
额度上限: 500.00
已用额度: 125.50
剩余额度: 374.50
每分钟限制: 30
状态: 启用
```

---

## 三、测试场景操作指南

### 3.1 场景1：首次登录体验

**目标**: 验证登录流程完整可用

**步骤**:
1. 打开 http://localhost:5173
2. 输入用户名：`admin`
3. 输入密码：`admin123`
4. 点击"登录"

**预期结果**:
- 登录成功，跳转到控制台首页
- 显示欢迎信息
- 侧边栏显示所有菜单项

### 3.2 场景2：创建第一个渠道

**目标**: 验证渠道管理功能

**步骤**:
1. 点击侧边栏"渠道管理"
2. 点击"新增渠道"按钮
3. 填写以下信息：
   - 渠道名称：`OpenAI官方`
   - 渠道编码：`openai-official`
   - 渠道类型：选择 `OpenAI`
   - Base URL：`https://api.openai.com/v1`
   - API Key：`sk-test-openai-demo-key-123456789`
   - 权重：`100`
   - 优先级：`1`
   - 状态：启用
4. 点击"创建"

**预期结果**:
- 渠道列表显示新记录
- API Key显示为掩码：`sk-t***...***789`
- 状态显示为"启用"

### 3.3 场景3：录入真实Key

**目标**: 验证真实Key管理功能

**步骤**:
1. 点击侧边栏"API Key管理"
2. 确保在"真实Key管理"标签页
3. 点击"录入Key"按钮
4. 填写：
   - Key名称：`OpenAI-生产环境主Key`
   - Key值：`sk-prod-openai-xxxxxxxxxxxxxxxxxxxx`
   - 所属渠道：选择 `OpenAI官方`
   - 状态：启用
5. 点击"保存"

**预期结果**:
- Key列表显示新记录
- 只显示掩码，**绝不显示完整Key**
- 所属渠道显示正确

### 3.4 场景4：生成虚拟Key

**目标**: 验证虚拟Key管理功能

**步骤**:
1. 切换到"虚拟Key管理"标签页
2. 点击"生成Key"按钮
3. 填写：
   - Key名称：`前端团队-Token配额`
   - 所属用户：选择 `admin`
   - 配额类型：Token
   - 额度上限：`1000000`
   - 每分钟限制：`60`
4. 点击"生成"

**预期结果**:
- 生成格式为 `sk-` + 32位字符的Key
- Key值**完整显示**（可点击复制）
- 显示配额进度条

### 3.5 场景5：筛选和搜索

**目标**: 验证筛选查询功能

**步骤**:
1. 在渠道列表页面
2. 点击"状态"筛选下拉框
3. 选择"启用"

**预期结果**:
- 列表只显示启用的渠道
- 禁用的渠道被过滤掉

**继续测试**:
1. 清除状态筛选
2. 在搜索框输入"OpenAI"

**预期结果**:
- 只显示名称包含"OpenAI"的渠道

---

## 四、关于使用真实API Key的说明

### 4.1 可以使用真实Key吗？

**答案：可以，但有以下注意事项：**

| 场景 | 是否可用 | 说明 |
|------|----------|------|
| 界面功能测试 | ✅ 推荐 | 使用模拟Key即可，无需真实Key |
| 连通性测试 | ✅ 可用 | 需要真实Key才能测试连通性 |
| 完整业务闭环 | ⚠️ 部分可用 | 调度网关M004模块完成后才能跑通 |

### 4.2 当前系统能力边界

**已完成的模块（M001-M003）**:
- ✅ 登录认证
- ✅ 渠道管理（CRUD、状态管理）
- ✅ 真实Key管理（加密存储、掩码显示）
- ✅ 虚拟Key管理（生成、配额、刷新）
- ✅ 基础数据展示和筛选

**未完成的模块**:
- ⏳ M004: 调度与代理网关（核心业务闭环）
- ⏳ M005: 额度与计费（实时扣减）
- ⏳ M006: 监控与日志（调用记录）

### 4.3 业务闭环说明

**当前状态**: 
- 可以管理渠道和Key
- 可以生成虚拟Key
- **不能**通过虚拟Key调用真实API（缺少调度网关）

**完整闭环需要**:
```
用户持有虚拟Key 
    ↓
调用平台统一接口 /api/v1/chat/completions
    ↓
调度网关鉴权、选渠道、选真实Key
    ↓
转发到厂商API
    ↓
返回结果给用户
```

**当前MVP阶段**: 完成到虚拟Key生成，调度网关是下一个迭代目标。

---

## 五、常见问题速查

### Q1: 创建渠道时报错 "fail_count cannot be null"

**原因**: 数据库字段约束问题

**解决**: 检查后端实体类是否设置了默认值，或手动给fail_count字段设置默认值0

### Q2: 连通性测试总是失败？

**原因**: 
1. 使用的是模拟Key，不是真实Key
2. 网络无法访问外网
3. 渠道配置错误

**解决**: 
- 使用真实Key进行连通性测试
- 或仅验证界面功能（不点连通性测试）

### Q3: 虚拟Key能调用API吗？

**答案**: 当前MVP阶段**不能**。需要等待M004调度网关模块完成后才能实现。

### Q4: 如何验证系统工作正常？

**验证清单**:
- [ ] 能正常登录
- [ ] 能创建渠道
- [ ] 能录入真实Key（显示掩码）
- [ ] 能生成虚拟Key（显示完整Key）
- [ ] 能筛选和搜索
- [ ] 页面无报错

---

## 六、测试数据SQL（可选）

如需直接插入测试数据到数据库：

```sql
-- 插入测试渠道
INSERT INTO channels (channel_name, channel_code, channel_type, base_url, api_key_encrypted, weight, priority, status, fail_count, success_count, created_at, updated_at) VALUES
('OpenAI官方', 'openai-official', 'openai', 'https://api.openai.com/v1', 'AES加密后的Key', 100, 1, 1, 0, 0, NOW(), NOW()),
('Claude Anthropic', 'claude-anthropic', 'claude', 'https://api.anthropic.com/v1', 'AES加密后的Key', 100, 2, 1, 0, 0, NOW(), NOW()),
('阿里通义千问', 'qwen-tongyi', 'qwen', 'https://dashscope.aliyuncs.com/api/v1', 'AES加密后的Key', 100, 3, 1, 0, 0, NOW(), NOW()),
('百度文心一言', 'wenxin-baidu', 'wenxin', 'https://aip.baidubce.com/rpc/2.0', 'AES加密后的Key', 100, 4, 1, 0, 0, NOW(), NOW()),
('测试渠道-已禁用', 'test-disabled', 'openai', 'https://api.test.com/v1', 'AES加密后的Key', 100, 5, 0, 0, 0, NOW(), NOW());

-- 插入测试真实Key
INSERT INTO real_keys (key_name, key_value_encrypted, key_mask, channel_id, status, created_at, updated_at) VALUES
('OpenAI-生产环境主Key', 'AES加密', 'sk-pr***...***xxx', 1, 'active', NOW(), NOW()),
('Claude-测试环境Key', 'AES加密', 'sk-an***...***yyy', 2, 'active', NOW(), NOW()),
('通义千问-开发环境', 'AES加密', 'sk-qw***...***zzz', 3, 'active', NOW(), NOW()),
('文心一言-生产环境', 'AES加密', 'sk-we***...***aaa', 4, 'active', NOW(), NOW()),
('OpenAI-备用Key', 'AES加密', 'sk-pr***...***bbb', 1, 'active', NOW(), NOW()),
('Claude-备用Key', 'AES加密', 'sk-an***...***ccc', 2, 'active', NOW(), NOW()),
('通义千问-额度紧张', 'AES加密', 'sk-qw***...***ddd', 3, 'depleted', NOW(), NOW()),
('文心一言-已过期', 'AES加密', 'sk-we***...***eee', 4, 'expired', NOW(), NOW()),
('OpenAI-已禁用', 'AES加密', 'sk-pr***...***fff', 1, 'disabled', NOW(), NOW()),
('测试-无效Key', 'AES加密', 'sk-te***...***ggg', 5, 'invalid', NOW(), NOW());

-- 插入测试虚拟Key
INSERT INTO virtual_keys (key_name, key_value, user_id, quota_type, quota_limit, quota_used, quota_remaining, rate_limit_qpm, status, created_at, updated_at) VALUES
('前端团队-Token配额', 'sk-a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6', 1, 'token', 1000000, 125000, 875000, 60, 1, NOW(), NOW()),
('后端团队-次数配额', 'sk-q7r8s9t0u1v2w3x4y5z6a7b8c9d0e1f2', 1, 'count', 10000, 2345, 7655, 120, 1, NOW(), NOW()),
('测试团队-金额配额', 'sk-g3h4i5j6k7l8m9n0o1p2q3r4s5t6u7v8', 1, 'amount', 500.00, 125.50, 374.50, 30, 1, NOW(), NOW());
```

---

## 七、联系与反馈

如遇到测试问题，请记录：
1. 操作步骤
2. 错误截图
3. 浏览器控制台报错信息
4. 后端日志输出

---

*本文档由项目总经理于 2026-04-06 创建*
*最后更新: 2026-04-06*
