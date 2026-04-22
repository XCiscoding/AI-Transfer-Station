-- =====================================================
-- AI调度中心 - 企业API Key管理系统
-- 初始数据脚本
-- 版本: V1.0
-- 创建时间: 2026-03-30
-- =====================================================

SET NAMES utf8mb4;

-- =====================================================
-- 1. 初始角色数据
-- =====================================================

INSERT INTO `roles` (`role_name`, `role_code`, `description`, `is_system`, `status`, `sort_order`) VALUES
('超级管理员', 'SUPER_ADMIN', '系统超级管理员，拥有所有权限', 1, 1, 100),
('企业管理员', 'ADMIN', '企业管理员，可管理团队、项目和用户', 1, 1, 90),
('普通用户', 'USER', '普通用户，可使用虚拟Key调用模型', 1, 1, 10);

-- =====================================================
-- 2. 初始权限数据
-- =====================================================

-- 菜单权限
INSERT INTO `permissions` (`permission_name`, `permission_code`, `resource_type`, `resource_path`, `parent_id`, `description`, `sort_order`) VALUES
-- 系统管理
('系统管理', 'system', 'menu', '/system', 0, '系统管理菜单', 100),
('用户管理', 'system:user', 'menu', '/system/user', 1, '用户管理菜单', 101),
('角色管理', 'system:role', 'menu', '/system/role', 1, '角色管理菜单', 102),
('权限管理', 'system:permission', 'menu', '/system/permission', 1, '权限管理菜单', 103),
('系统配置', 'system:config', 'menu', '/system/config', 1, '系统配置菜单', 104),

-- 渠道管理
('渠道管理', 'channel', 'menu', '/channel', 0, '渠道管理菜单', 200),
('渠道列表', 'channel:list', 'menu', '/channel/list', 6, '渠道列表菜单', 201),
('渠道健康', 'channel:health', 'menu', '/channel/health', 6, '渠道健康菜单', 202),

-- 模型管理
('模型管理', 'model', 'menu', '/model', 0, '模型管理菜单', 300),
('模型列表', 'model:list', 'menu', '/model/list', 9, '模型列表菜单', 301),
('模型分组', 'model:group', 'menu', '/model/group', 9, '模型分组菜单', 302),

-- 虚拟Key管理
('虚拟Key管理', 'token', 'menu', '/token', 0, '虚拟Key管理菜单', 400),
('虚拟Key列表', 'token:list', 'menu', '/token/list', 12, '虚拟Key列表菜单', 401),
('发放管理', 'token:issue', 'menu', '/token/issue', 12, '发放管理菜单', 402),

-- 团队管理
('团队管理', 'team', 'menu', '/team', 0, '团队管理菜单', 500),
('团队列表', 'team:list', 'menu', '/team/list', 15, '团队列表菜单', 501),
('团队成员', 'team:member', 'menu', '/team/member', 15, '团队成员菜单', 502),

-- 项目管理
('项目管理', 'project', 'menu', '/project', 0, '项目管理菜单', 600),
('项目列表', 'project:list', 'menu', '/project/list', 18, '项目列表菜单', 601),

-- 数据看板
('数据看板', 'dashboard', 'menu', '/dashboard', 0, '数据看板菜单', 700),
('请求日志', 'log:request', 'menu', '/log/request', 20, '请求日志菜单', 701),
('调用统计', 'stat:usage', 'menu', '/stat/usage', 20, '调用统计菜单', 702);

-- =====================================================
-- 3. 角色权限关联数据
-- =====================================================

-- SUPER_ADMIN 拥有所有权限
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `roles` r
CROSS JOIN `permissions` p
WHERE r.role_code = 'SUPER_ADMIN';

-- ADMIN 拥有大部分权限（除系统配置外）
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `roles` r
CROSS JOIN `permissions` p
WHERE r.role_code = 'ADMIN'
AND p.permission_code NOT LIKE 'system:%';

-- USER 只拥有基本查看权限
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `roles` r
JOIN `permissions` p ON p.permission_code IN ('token:list', 'log:request', 'dashboard')
WHERE r.role_code = 'USER';

-- =====================================================
-- 4. 初始模型分组数据
-- 先用 model_code 占位，待 models 表落库后再回填为 modelIds
-- =====================================================

INSERT INTO `model_groups` (`group_name`, `group_code`, `description`, `models`, `status`, `sort_order`) VALUES
('GPT系列', 'gpt-series', 'OpenAI GPT系列模型', '["gpt-4", "gpt-4-turbo", "gpt-3.5-turbo"]', 1, 10),
('Claude系列', 'claude-series', 'Anthropic Claude系列模型', '["claude-3-opus", "claude-3-sonnet", "claude-3-haiku"]', 1, 20),
('国产大模型', 'china-llm', '国产大模型集合', '["ernie-4.0", "ernie-3.5", "spark-desk", "glm-4.7-flash", "glm-4-flash-250414"]', 1, 30);

-- =====================================================
-- 5. 模型配置数据
-- =====================================================

INSERT INTO `model_configs` (
    `model_name`, `model_code`, `model_type`, `provider`,
    `input_price`, `output_price`, `context_window`, `max_tokens`,
    `is_streaming`, `is_vision`, `is_function_calling`, `status`, `sort_order`
) VALUES
-- OpenAI 模型
('GPT-4 Turbo', 'gpt-4-turbo', 'chat', 'openai', 10.00, 30.00, 128000, 4096, 1, 1, 1, 1, 10),
('GPT-4', 'gpt-4', 'chat', 'openai', 30.00, 60.00, 8192, 4096, 1, 1, 1, 1, 20),
('GPT-3.5 Turbo', 'gpt-3.5-turbo', 'chat', 'openai', 0.50, 1.50, 16385, 4096, 1, 0, 1, 1, 30),

-- Claude 模型
('Claude 3 Opus', 'claude-3-opus', 'chat', 'anthropic', 15.00, 75.00, 200000, 4096, 1, 1, 1, 1, 40),
('Claude 3 Sonnet', 'claude-3-sonnet', 'chat', 'anthropic', 3.00, 15.00, 200000, 4096, 1, 1, 1, 1, 50),
('Claude 3 Haiku', 'claude-3-haiku', 'chat', 'anthropic', 0.25, 1.25, 200000, 4096, 1, 1, 0, 1, 60),

-- 国产模型
('文心一言 4.0', 'ernie-4.0', 'chat', 'baidu', 5.00, 15.00, 8192, 4096, 1, 0, 0, 1, 70),
('文心一言 3.5', 'ernie-3.5', 'chat', 'baidu', 2.00, 6.00, 8192, 4096, 1, 0, 0, 1, 80),
('讯飞星火', 'spark-desk', 'chat', 'xunfei', 3.00, 9.00, 8192, 4096, 1, 0, 0, 1, 90),

-- 智谱免费模型
('GLM-4.7 Flash', 'glm-4.7-flash', 'chat', 'zhipu', 0.00, 0.00, 200000, 128000, 1, 0, 0, 1, 100),
('GLM-4-Flash-250414', 'glm-4-flash-250414', 'chat', 'zhipu', 0.00, 0.00, 128000, 16384, 1, 0, 0, 1, 110);

-- =====================================================
-- 6. 初始管理员账户
-- =====================================================

-- 默认管理员密码: admin123 (BCrypt加密后的值)
INSERT INTO `users` (`username`, `password`, `email`, `real_name`, `status`, `is_locked`) VALUES
('admin', '$2a$10$4ic5pj1ml91AhyKwSrkyD.mclr34jpGkpParb.VNhlvuSvbgVen8.', 'admin@example.com', '系统管理员', 1, 0),
('enterprise_admin', '$2a$10$4ic5pj1ml91AhyKwSrkyD.mclr34jpGkpParb.VNhlvuSvbgVen8.', 'enterprise_admin@example.com', '企业管理员', 1, 0);

-- 关联管理员角色
-- enterprise_admin 为企业管理员(SUPER_ADMIN)
INSERT INTO `user_roles` (`user_id`, `role_id`)
SELECT u.id, r.id
FROM `users` u
JOIN `roles` r ON r.role_code = 'SUPER_ADMIN'
WHERE u.username = 'enterprise_admin';

-- admin 为普通用户(USER)，后续通过团队关联成为团队管理员
INSERT INTO `user_roles` (`user_id`, `role_id`)
SELECT u.id, r.id
FROM `users` u
JOIN `roles` r ON r.role_code = 'USER'
WHERE u.username = 'admin';

-- =====================================================
-- 7. 初始额度数据
-- =====================================================

INSERT INTO `quotas` (`user_id`, `quota_type`, `quota_limit`, `quota_used`, `quota_remaining`, `reset_cycle`)
SELECT u.id, 'token', 1000000.00, 0.00, 1000000.00, 'never'
FROM `users` u
WHERE u.username IN ('admin', 'enterprise_admin');

-- =====================================================
-- 8. 初始渠道数据（修复编码问题）
-- =====================================================

INSERT IGNORE INTO channels (
    channel_name, channel_code, channel_type, base_url,
    api_key_encrypted, models, weight, priority, status,
    health_status, remark
) VALUES
('OpenAI官方', 'openai-official', 'openai', 'https://api.openai.com/v1',
 NULL, '["gpt-4", "gpt-4-turbo", "gpt-3.5-turbo"]', 100, 10, 1, 1,
 'OpenAI官方API渠道，用于GPT-4/GPT-3.5接口'),

('Claude Anthropic', 'claude-anthropic', 'claude', 'https://api.anthropic.com/v1',
 NULL, '["claude-3-opus", "claude-3-sonnet", "claude-3-haiku"]', 90, 8, 1, 1,
 'Anthropic Claude系列接口'),

('智谱AI', 'zhipu', 'zhipu', 'https://open.bigmodel.cn/api/paas/v4',
 NULL, '["glm-4.7-flash", "glm-4-flash-250414"]', 100, 9, 1, 1,
 '智谱AI官方渠道，首期接入免费文本模型'),

('百度文心一言', 'wenxin-baidu', 'wenxin', 'https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat',
 NULL, '["ernie-4.0", "ernie-3.5", "ernie-turbo"]', 80, 6, 1, 1,
 '百度文心一言国产大模型接口'),

('Test-Disabled', 'test-disabled', 'openai', 'https://api.test.com/v1',
 NULL, '["test-model"]', 0, 0, 0, 0,
 '测试用禁用渠道');

-- =====================================================
-- 9. 智谱模型种子数据
-- =====================================================

INSERT INTO `models` (
    `model_name`, `model_code`, `model_alias`, `channel_id`, `model_type`,
    `capability_tags`, `max_tokens`, `input_price`, `output_price`,
    `quota_weight`, `status`, `config`, `remark`
)
SELECT 'GLM-4.7 Flash', 'glm-4.7-flash', 'GLM 4.7 Flash', c.id, 'chat',
       '["chat", "general"]', 128000, 0.000000, 0.000000,
       1.00, 1,
       '{"provider":"zhipu","contextWindow":200000,"isStreaming":true,"isFunctionCalling":false}',
       '智谱免费首选文本模型'
FROM `channels` c
WHERE c.`channel_code` = 'zhipu'
UNION ALL
SELECT 'GLM-4-Flash-250414', 'glm-4-flash-250414', 'GLM 4 Flash 250414', c.id, 'chat',
       '["chat", "fast"]', 16384, 0.000000, 0.000000,
       1.00, 1,
       '{"provider":"zhipu","contextWindow":128000,"isStreaming":true,"isFunctionCalling":false}',
       '智谱免费备选文本模型'
FROM `channels` c
WHERE c.`channel_code` = 'zhipu';

-- 将国产大模型分组回填为实际 modelIds，避免启动后因旧的 model_code 种子导致分组渠道映射失效
UPDATE `model_groups`
SET `models` = (
    SELECT IFNULL(
        CONCAT(
            '[',
            GROUP_CONCAT(m.`id` ORDER BY FIELD(m.`model_code`, 'glm-4.7-flash', 'glm-4-flash-250414') SEPARATOR ','),
            ']'
        ),
        '[]'
    )
    FROM `models` m
    WHERE m.`deleted` = 0
      AND m.`model_code` IN ('glm-4.7-flash', 'glm-4-flash-250414')
)
WHERE `group_code` = 'china-llm';

-- =====================================================
-- 10. 初始团队数据（admin作为团队管理员）
-- =====================================================

-- 先插入团队，owner_id 暂时为 1（admin 用户的 ID）
INSERT IGNORE INTO `teams` (
    `id`, `team_name`, `team_code`, `description`, `owner_id`,
    `quota_limit`, `quota_used`, `quota_remaining`, `quota_weight`,
    `status`, `allowed_group_ids`
)
VALUES (
    1, '默认团队', 'default-team', '系统默认团队，用于测试团队管理员功能', 1,
    1000000.00, 0.00, 1000000.00, 1.00,
    1, '[1, 2, 3]'
);

-- 默认团队成员（owner 同时写入 team_members，保证团队内发 Key/额度链路可直接测试）
INSERT IGNORE INTO `team_members` (`team_id`, `user_id`, `role`)
VALUES (1, 1, 'owner');

-- =====================================================
-- 11. 初始项目数据
-- =====================================================

INSERT IGNORE INTO `projects` (
    `id`, `project_name`, `project_code`, `description`, `team_id`, `owner_id`,
    `quota_limit`, `quota_used`, `quota_remaining`, `quota_weight`,
    `status`
)
VALUES (
    1, '默认项目', 'default-project', '系统默认项目', 1, 1,
    1000000.00, 0.00, 1000000.00, 1.00,
    1
);
