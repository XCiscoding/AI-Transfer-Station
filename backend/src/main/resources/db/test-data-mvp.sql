-- =============================================
-- MVP业务闭环测试数据
-- 创建时间: 2026-04-06
-- 用途: 验证渠道管理、Key管理、调用统计全流程
-- 设计: 5个渠道 + 10个真实API Key + 3个虚拟Key
-- ID范围: 渠道(100-104), 真实Key(200-209), 虚拟Key(300-302)
-- =============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================
-- 1. 清理旧测试数据（可选，取消注释可启用）
-- =============================================
-- DELETE FROM virtual_keys WHERE id >= 300;
-- DELETE FROM real_keys WHERE id >= 200;
-- DELETE FROM channels WHERE id >= 100;

-- =============================================
-- 2. 插入测试渠道（5条）- 覆盖不同业务场景
-- =============================================

INSERT INTO channels (
    id, channel_name, channel_code, channel_type, base_url,
    api_key_encrypted, models, weight, priority, status,
    health_status, health_check_time, success_count, fail_count,
    avg_response_time, config, remark, created_at, updated_at, deleted
) VALUES
-- 渠道1: OpenAI官方 - 主流LLM，已启用，高优先级
(
    100,
    'OpenAI官方',
    'openai_official',
    'openai',
    'https://api.openai.com/v1',
    NULL,  -- 测试环境不设置加密密钥
    '["gpt-4", "gpt-4-turbo", "gpt-3.5-turbo"]',
    100,
    10,
    1,  -- 已启用
    1,  -- 健康
    NOW(),
    1523,
    12,
    450,
    '{"timeout": 30, "max_retries": 3}',
    'OpenAI官方API渠道，用于GPT-4/GPT-3.5接口',
    NOW(),
    NOW(),
    0
),

-- 渠道2: Claude Anthropic - 主流LLM，已启用
(
    101,
    'Claude Anthropic',
    'claude_anthropic',
    'claude',
    'https://api.anthropic.com/v1',
    NULL,
    '["claude-3-opus", "claude-3-sonnet", "claude-3-haiku"]',
    90,
    8,
    1,  -- 已启用
    1,  -- 健康
    NOW(),
    876,
    5,
    520,
    '{"timeout": 60, "max_retries": 2}',
    'Anthropic Claude系列接口',
    NOW(),
    NOW(),
    0
),

-- 渠道3: 百度文心一言 - 国内LLM，已启用
(
    102,
    '百度文心一言',
    'wenxin_baidu',
    'wenxin',
    'https://aip.baidubce.com/rpc/2.0/ai_custom/v1',
    NULL,
    '["ernie-4.0", "ernie-3.5", "ernie-turbo"]',
    80,
    6,
    1,  -- 已启用
    1,  -- 健康
    NOW(),
    654,
    8,
    380,
    '{"timeout": 30, "max_retries": 3}',
    '百度文心一言国产大模型接口',
    NOW(),
    NOW(),
    0
),

-- 渠道4: 阿里通义千问 - 国内LLM，已启用（备用）
(
    103,
    '阿里通义千问',
    'qwen_alibaba',
    'qwen',
    'https://dashscope.aliyuncs.com/api/v1',
    NULL,
    '["qwen-max", "qwen-plus", "qwen-turbo"]',
    70,
    4,
    1,  -- 已启用（备用渠道）
    1,  -- 健康
    NOW(),
    321,
    3,
    420,
    '{"timeout": 30, "max_retries": 2}',
    '阿里通义千问国产大模型接口，作为备用渠道',
    NOW(),
    NOW(),
    0
),

-- 渠道5: 测试渠道-已禁用 - 验证禁用逻辑
(
    104,
    '测试渠道-已禁用',
    'test_disabled',
    'openai',
    'https://test-api.example.com/v1',
    NULL,
    '["gpt-4", "gpt-3.5-turbo"]',
    50,
    0,
    0,  -- 已禁用
    0,  -- 不健康
    '2026-03-15 10:00:00',
    0,
    0,
    0,
    NULL,
    '测试专用渠道，用于验证禁用逻辑和隔离机制',
    '2026-03-01 08:00:00',
    '2026-04-01 12:00:00',
    0
);

-- =============================================
-- 3. 插入真实API Key（10条）- 覆盖多种状态场景
-- =============================================

INSERT INTO real_keys (
    id, key_name, key_value_encrypted, key_mask, channel_id,
    status, expire_time, usage_count, last_used_time,
    remark, created_at, updated_at, deleted
) VALUES
-- ===== OpenAI官方渠道的Key (3个) =====
-- Key 200: OpenAI活跃Key #1 - 正常使用中
(
    200,
    'OpenAI-生产环境-Key01',
    'encrypted_sk-test-openai-prod-001-abc123xyz',
    'sk-test-***...***abc123',
    100,  -- OpenAI官方渠道
    1,     -- 启用
    '2027-12-31 23:59:59',  -- 未过期
    1523,
    '2026-04-05 14:32:18',
    'OpenAI主生产环境Key，用于日常调用',
    NOW(),
    NOW(),
    0
),

-- Key 201: OpenAI活跃Key #2 - 正常使用中（用于轮询测试）
(
    201,
    'OpenAI-生产环境-Key02',
    'encrypted_sk-test-openai-prod-002-def456uvw',
    'sk-test-***...***def456',
    100,  -- OpenAI官方渠道
    1,     -- 启用
    '2027-06-30 23:59:59',  -- 未过期
    892,
    '2026-04-05 15:20:33',
    'OpenAI备生产环境Key，用于负载均衡轮询',
    NOW(),
    NOW(),
    0
),

-- Key 202: OpenAI耗尽Key - 用于故障转移测试
(
    202,
    'OpenAI-测试环境-Key03-已耗尽',
    'encrypted_sk-test-openai-test-003-ghi789rst',
    'sk-test-***...***ghi789',
    100,  -- OpenAI官方渠道
    1,     -- 启用（但额度已耗尽）
    '2026-04-01 23:59:59',  -- 已过期（模拟耗尽场景）
    99999,  -- 使用次数达到上限
    '2026-04-01 09:15:42',
    'OpenAI测试Key，已达到使用限额，用于测试故障转移逻辑',
    NOW(),
    NOW(),
    0
),

-- ===== Claude Anthropic渠道的Key (2个) =====
-- Key 203: Claude活跃Key #1
(
    203,
    'Claude-生产环境-Key01',
    'encrypted_sk-test-claude-prod-001-jkl012mno',
    'sk-test-***...***jkl012',
    101,  -- Claude渠道
    1,     -- 启用
    '2027-12-31 23:59:59',
    876,
    '2026-04-05 16:45:21',
    'Claude主生产环境Key',
    NOW(),
    NOW(),
    0
),

-- Key 204: Claude活跃Key #2
(
    204,
    'Claude-生产环境-Key02',
    'encrypted_sk-test-claude-prod-002-pqr345tuv',
    'sk-test-***...***pqr345',
    101,  -- Claude渠道
    1,     -- 启用
    '2028-01-31 23:59:59',
    654,
    '2026-04-05 17:10:55',
    'Claude备生产环境Key',
    NOW(),
    NOW(),
    0
),

-- ===== 百度文心渠道的Key (2个) =====
-- Key 205: 百度文心活跃Key
(
    205,
    '百度文心-生产环境-Key01',
    'encrypted_sk-test-wenxin-prod-001-wxy678zab',
    'sk-test-***...***wxy678',
    102,  -- 百度文心渠道
    1,     -- 启用
    '2027-09-30 23:59:59',
    520,
    '2026-04-05 11:22:19',
    '百度文心主生产环境Key',
    NOW(),
    NOW(),
    0
),

-- Key 206: 百度文心过期Key - 测试过期清理
(
    206,
    '百度文心-测试环境-Key02-已过期',
    'encrypted_sk-test-wenxin-test-002-cde901fgh',
    'sk-test-***...***cde901',
    102,  -- 百度文心渠道
    1,     -- 启用（但已过期）
    '2026-03-15 23:59:59',  -- 已过期！
    134,
    '2026-03-14 20:35:07',
    '百度文心测试Key，已过期，用于测试过期清理逻辑',
    '2026-02-01 10:00:00',
    NOW(),
    0
),

-- ===== 阿里通义千问渠道的Key (2个) =====
-- Key 207: 通义千问活跃Key #1
(
    207,
    '通义千问-备用环境-Key01',
    'encrypted_sk-test-qwen-backup-001-ijk234lmn',
    'sk-test-***...***ijk234',
    103,  -- 阿里通义渠道
    1,     -- 启用
    '2027-12-31 23:59:59',
    321,
    '2026-04-05 13:50:44',
    '通义千问备用渠道Key #1',
    NOW(),
    NOW(),
    0
),

-- Key 208: 通义千问活跃Key #2
(
    208,
    '通义千问-备用环境-Key02',
    'encrypted_sk-test-qwen-backup-002-opq567rst',
    'sk-test-***...***opq567',
    103,  -- 阿里通义渠道
    1,     -- 启用
    '2027-12-31 23:59:59',
    198,
    '2026-04-05 14:05:28',
    '通义千问备用渠道Key #2',
    NOW(),
    NOW(),
    0
),

-- ===== 测试渠道（已禁用）的Key (1个) =====
-- Key 209: 测试渠道禁用Key - 验证禁用隔离
(
    209,
    '测试渠道-禁用Key-不参与调度',
    'encrypted_sk-test-disabled-channel-key-001-uvw890xyz',
    'sk-test-***...***uvw890',
    104,  -- 测试渠道（已禁用）
    0,     -- 已禁用
    '2027-12-31 23:59:59',
    0,
    NULL,
    '属于已禁用测试渠道的Key，不应参与任何调度',
    NOW(),
    NOW(),
    0
);

-- =============================================
-- 3.5 插入模型数据 - 调度引擎依赖models表
-- =============================================

INSERT IGNORE INTO models (
    id, model_name, model_code, model_alias, channel_id, model_type,
    capability_tags, max_tokens, input_price, output_price,
    status, config, remark, created_at, updated_at, deleted
) VALUES
-- OpenAI渠道模型
(1, 'GPT-4', 'gpt-4', 'gpt-4', 100, 'chat', '["reasoning","coding","creative"]', 8192, 0.030000, 0.060000, 1, NULL, 'OpenAI GPT-4', NOW(), NOW(), 0),
(2, 'GPT-4 Turbo', 'gpt-4-turbo', 'gpt-4-turbo', 100, 'chat', '["reasoning","coding","creative"]', 128000, 0.010000, 0.030000, 1, NULL, 'OpenAI GPT-4 Turbo', NOW(), NOW(), 0),
(3, 'GPT-3.5 Turbo', 'gpt-3.5-turbo', 'gpt-3.5-turbo', 100, 'chat', '["general"]', 16385, 0.001000, 0.002000, 1, NULL, 'OpenAI GPT-3.5', NOW(), NOW(), 0),
-- 阿里通义千问渠道模型
(4, 'Qwen Turbo', 'qwen-turbo', 'qwen-turbo', 103, 'chat', '["general","chinese"]', 8000, 0.002000, 0.006000, 1, NULL, 'Qwen Turbo', NOW(), NOW(), 0),
(5, 'Qwen Plus', 'qwen-plus', 'qwen-plus', 103, 'chat', '["reasoning","chinese"]', 32768, 0.004000, 0.012000, 1, NULL, 'Qwen Plus', NOW(), NOW(), 0),
-- 百度文心渠道模型
(6, 'ERNIE Bot', 'ernie-bot', 'ernie-bot', 102, 'chat', '["general","chinese"]', 4096, 0.008000, 0.008000, 1, NULL, 'Baidu ERNIE Bot', NOW(), NOW(), 0),
-- DeepSeek模型 (也挂在通义渠道下，模拟中转场景)
(7, 'DeepSeek Chat', 'deepseek-chat', 'deepseek-chat', 103, 'chat', '["reasoning","coding"]', 32768, 0.001000, 0.002000, 1, NULL, 'DeepSeek Chat', NOW(), NOW(), 0);

-- =============================================
-- 4. 插入虚拟Key（3条）- 用户视角的Key管理测试
-- 注意: 需要先确保users表中存在ID为1的用户
-- 如果不存在用户数据，请先创建测试用户
-- =============================================

INSERT INTO virtual_keys (
    id, key_name, key_value, user_id, team_id, project_id,
    allowed_models, quota_type, quota_limit, quota_used, quota_remaining,
    rate_limit_qpm, rate_limit_qpd, status, expire_time,
    last_used_time, remark, created_at, updated_at, deleted
) VALUES
-- 虚拟Key 300: 开发团队虚拟Key - 高配额
(
    300,
    '开发团队-Main-VirtualKey',
    'sk-virt-dev-team-main-001',
    1,      -- 假设用户ID=1存在
    NULL,   -- 不限制团队
    NULL,   -- 不限制项目
    NULL,   -- 允许所有模型
    'token',
    1000000.00,  -- 100万Token额度
    125430.50,   -- 已使用12.5万Token
    874569.50,   -- 剩余87.5万Token
    60,          -- 每分钟60次请求
    10000,       -- 每天10000次请求
    1,           -- 启用
    '2027-06-30 23:59:59',
    '2026-04-05 16:30:22',
    '开发团队主虚拟Key，高配额，用于日常开发测试',
    NOW(),
    NOW(),
    0
),

-- 虚拟Key 301: 测试项目虚拟Key - 中等配额
(
    301,
    '测试项目-Limited-VirtualKey',
    'sk-virt-test-project-limited-002',
    1,      -- 假设用户ID=1存在
    NULL,
    NULL,
    '["gpt-4", "claude-3-sonnet"]',  -- 仅允许特定模型
    'count',
    5000.00,     -- 5000次调用限额
    1234.00,     -- 已调用1234次
    3766.00,     -- 剩余3766次
    30,          -- 每分钟30次请求
    500,         -- 每天500次请求
    1,           -- 启用
    '2026-12-31 23:59:59',
    '2026-04-05 14:20:15',
    '测试项目虚拟Key，中等配额，仅限特定模型',
    NOW(),
    NOW(),
    0
),

-- 虚拟Key 302: 过期虚拟Key - 测试过期处理
(
    302,
    '演示Demo-Expired-VirtualKey',
    'sk-virt-demo-expired-003',
    1,      -- 假设用户ID=1存在
    NULL,
    NULL,
    NULL,        -- 不限制模型
    'amount',
    100.00,      -- 100美元金额限额
    98.50,       -- 已使用98.50美元
    1.50,        -- 剩余1.50美元（接近耗尽）
    10,          -- 每分钟10次请求
    100,         -- 每天100次请求
    1,           -- 启用（但即将过期）
    '2026-04-10 23:59:59',  -- 即将过期！
    '2026-04-04 09:15:33',
    '演示用的虚拟Key，即将过期且接近额度耗尽',
    '2026-01-15 10:00:00',
    NOW(),
    0
);

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================
-- 数据插入完成统计
-- =============================================
-- 渠道: 5条 (ID: 100-104)
--   - 已启用渠道: 4个 (OpenAI, Claude, 百度文心, 阿里通义)
--   - 已禁用渠道: 1个 (测试渠道)
--
-- 真实API Key: 10条 (ID: 200-209)
--   - OpenAI渠道: 3个 (2个活跃 + 1个耗尽)
--   - Claude渠道: 2个 (全部活跃)
--   - 百度文心渠道: 2个 (1个活跃 + 1个过期)
--   - 阿里通义渠道: 2个 (全部活跃)
--   - 测试渠道: 1个 (已禁用)
--
-- 虚拟Key: 3条 (ID: 300-302)
--   - 高配额虚拟Key: 1个
--   - 中等配额+模型限制: 1个
--   - 即将过期+接近耗尽: 1个
--
-- 支持的测试场景:
-- 1. 渠道列表展示（含启用/禁用状态筛选）
-- 2. Key列表加载（按渠道分组显示）
-- 3. Key状态筛选（活跃/耗尽/过期/禁用）
-- 4. 轮询机制（同一渠道多活跃Key轮流选中）
-- 5. 故障转移（耗尽Key自动切换到下一个可用Key）
-- 6. 禁用隔离（禁用渠道和Key不参与调度）
-- 7. 过期处理（过期Key在列表显示但不被选用）
-- 8. 统计展示（使用次数、剩余额度、响应时间等）
-- 9. 虚拟Key管理（配额控制、速率限制、模型白名单）
-- 10. 多维度查询（按渠道类型、状态、使用量等）
-- =============================================
