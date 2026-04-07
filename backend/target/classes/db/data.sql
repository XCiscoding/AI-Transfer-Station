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
('模型管理', 'channel:model', 'menu', '/channel/model', 6, '模型管理菜单', 202),

-- Key管理
('Key管理', 'key', 'menu', '/key', 0, 'Key管理菜单', 300),
('真实Key', 'key:real', 'menu', '/key/real', 11, '真实Key菜单', 301),
('虚拟Key', 'key:virtual', 'menu', '/key/virtual', 11, '虚拟Key菜单', 302),

-- 调度管理
('调度管理', 'dispatch', 'menu', '/dispatch', 0, '调度管理菜单', 400),
('调度策略', 'dispatch:strategy', 'menu', '/dispatch/strategy', 16, '调度策略菜单', 401),
('Auto模式', 'dispatch:auto', 'menu', '/dispatch/auto', 16, 'Auto模式菜单', 402),

-- 额度管理
('额度管理', 'quota', 'menu', '/quota', 0, '额度管理菜单', 500),
('额度查询', 'quota:query', 'menu', '/quota/query', 21, '额度查询菜单', 501),
('计费规则', 'quota:billing', 'menu', '/quota/billing', 21, '计费规则菜单', 502),

-- 监控日志
('监控日志', 'monitor', 'menu', '/monitor', 0, '监控日志菜单', 600),
('调用日志', 'monitor:call-log', 'menu', '/monitor/call-log', 26, '调用日志菜单', 601),
('登录日志', 'monitor:login-log', 'menu', '/monitor/login-log', 26, '登录日志菜单', 602),
('数据看板', 'monitor:dashboard', 'menu', '/monitor/dashboard', 26, '数据看板菜单', 603),
('告警管理', 'monitor:alert', 'menu', '/monitor/alert', 26, '告警管理菜单', 604),

-- 团队管理
('团队管理', 'team', 'menu', '/team', 0, '团队管理菜单', 700),
('团队列表', 'team:list', 'menu', '/team/list', 31, '团队列表菜单', 701),
('项目管理', 'team:project', 'menu', '/team/project', 31, '项目管理菜单', 702);

-- 操作权限
INSERT INTO `permissions` (`permission_name`, `permission_code`, `resource_type`, `resource_path`, `http_method`, `parent_id`, `description`, `sort_order`) VALUES
-- 用户管理操作
('查看用户', 'system:user:view', 'button', '/api/v1/users/*', 'GET', 2, '查看用户权限', 1001),
('创建用户', 'system:user:create', 'button', '/api/v1/users', 'POST', 2, '创建用户权限', 1002),
('更新用户', 'system:user:update', 'button', '/api/v1/users/*', 'PUT', 2, '更新用户权限', 1003),
('删除用户', 'system:user:delete', 'button', '/api/v1/users/*', 'DELETE', 2, '删除用户权限', 1004),

-- 角色管理操作
('查看角色', 'system:role:view', 'button', '/api/v1/roles/*', 'GET', 3, '查看角色权限', 1101),
('创建角色', 'system:role:create', 'button', '/api/v1/roles', 'POST', 3, '创建角色权限', 1102),
('更新角色', 'system:role:update', 'button', '/api/v1/roles/*', 'PUT', 3, '更新角色权限', 1103),
('删除角色', 'system:role:delete', 'button', '/api/v1/roles/*', 'DELETE', 3, '删除角色权限', 1104),

-- 渠道管理操作
('查看渠道', 'channel:list:view', 'button', '/api/v1/channels/*', 'GET', 7, '查看渠道权限', 2001),
('创建渠道', 'channel:list:create', 'button', '/api/v1/channels', 'POST', 7, '创建渠道权限', 2002),
('更新渠道', 'channel:list:update', 'button', '/api/v1/channels/*', 'PUT', 7, '更新渠道权限', 2003),
('删除渠道', 'channel:list:delete', 'button', '/api/v1/channels/*', 'DELETE', 7, '删除渠道权限', 2004),
('测试渠道', 'channel:list:test', 'button', '/api/v1/channels/*/test', 'POST', 7, '测试渠道权限', 2005),

-- Key管理操作
('查看真实Key', 'key:real:view', 'button', '/api/v1/real-keys/*', 'GET', 12, '查看真实Key权限', 3001),
('创建真实Key', 'key:real:create', 'button', '/api/v1/real-keys', 'POST', 12, '创建真实Key权限', 3002),
('删除真实Key', 'key:real:delete', 'button', '/api/v1/real-keys/*', 'DELETE', 12, '删除真实Key权限', 3003),

('查看虚拟Key', 'key:virtual:view', 'button', '/api/v1/virtual-keys/*', 'GET', 13, '查看虚拟Key权限', 3101),
('创建虚拟Key', 'key:virtual:create', 'button', '/api/v1/virtual-keys', 'POST', 13, '创建虚拟Key权限', 3102),
('更新虚拟Key', 'key:virtual:update', 'button', '/api/v1/virtual-keys/*', 'PUT', 13, '更新虚拟Key权限', 3103),
('删除虚拟Key', 'key:virtual:delete', 'button', '/api/v1/virtual-keys/*', 'DELETE', 13, '删除虚拟Key权限', 3104),

-- Auto模式操作
('查看映射规则', 'dispatch:auto:view', 'button', '/api/v1/auto/mappings/*', 'GET', 17, '查看映射规则权限', 4001),
('创建映射规则', 'dispatch:auto:create', 'button', '/api/v1/auto/mappings', 'POST', 17, '创建映射规则权限', 4002),
('更新映射规则', 'dispatch:auto:update', 'button', '/api/v1/auto/mappings/*', 'PUT', 17, '更新映射规则权限', 4003),
('删除映射规则', 'dispatch:auto:delete', 'button', '/api/v1/auto/mappings/*', 'DELETE', 17, '删除映射规则权限', 4004),

-- 额度管理操作
('查看额度', 'quota:query:view', 'button', '/api/v1/quotas/*', 'GET', 22, '查看额度权限', 5001),
('分配额度', 'quota:query:allocate', 'button', '/api/v1/quotas', 'POST', 22, '分配额度权限', 5002),

-- 告警管理操作
('查看告警', 'monitor:alert:view', 'button', '/api/v1/alerts/*', 'GET', 30, '查看告警权限', 6001),
('创建告警', 'monitor:alert:create', 'button', '/api/v1/alerts', 'POST', 30, '创建告警权限', 6002),
('更新告警', 'monitor:alert:update', 'button', '/api/v1/alerts/*', 'PUT', 30, '更新告警权限', 6003),
('删除告警', 'monitor:alert:delete', 'button', '/api/v1/alerts/*', 'DELETE', 30, '删除告警权限', 6004);

-- =====================================================
-- 3. 角色权限关联数据
-- =====================================================

-- 超级管理员拥有所有权限
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT 1, `id` FROM `permissions`;

-- 企业管理员权限
INSERT INTO `role_permissions` (`role_id`, `permission_id`) VALUES
-- 菜单权限
(2, 6), (2, 7), (2, 8), -- 渠道管理
(2, 9), (2, 10), (2, 11), -- Key管理
(2, 12), (2, 13), (2, 14), -- 调度管理
(2, 15), (2, 16), (2, 17), -- 额度管理
(2, 18), (2, 19), (2, 20), (2, 21), (2, 22), -- 监控日志
(2, 23), (2, 24), (2, 25), -- 团队管理
-- 操作权限
(2, 2001), (2, 2002), (2, 2003), (2, 2004), (2, 2005), -- 渠道操作
(2, 3001), (2, 3002), (2, 3003), -- 真实Key操作
(2, 3101), (2, 3102), (2, 3103), (2, 3104), -- 虚拟Key操作
(2, 4001), (2, 4002), (2, 4003), (2, 4004), -- Auto模式操作
(2, 5001), (2, 5002), -- 额度操作
(2, 6001), (2, 6002), (2, 6003), (2, 6004); -- 告警操作

-- 普通用户权限
INSERT INTO `role_permissions` (`role_id`, `permission_id`) VALUES
-- 菜单权限
(3, 11), -- 虚拟Key
(3, 16), -- 额度查询
(3, 19), (3, 21), -- 调用日志、数据看板
-- 操作权限
(3, 3101), -- 查看虚拟Key
(3, 5001); -- 查看额度

-- =====================================================
-- 4. 初始系统配置数据
-- =====================================================

INSERT INTO `system_configs` (`config_group`, `config_key`, `config_value`, `config_type`, `description`, `is_public`, `sort_order`) VALUES
-- 基础配置
('basic', 'site_name', 'AI调度中心', 'string', '站点名称', 1, 1),
('basic', 'site_description', '企业级大模型API Key管理与调度平台', 'string', '站点描述', 1, 2),
('basic', 'site_logo', '/logo.png', 'string', '站点Logo', 1, 3),
('basic', 'default_quota', '100000', 'number', '新用户默认额度（Token）', 0, 4),

-- 安全配置
('security', 'jwt_secret', 'ai-key-management-jwt-secret-key-2026', 'string', 'JWT密钥', 0, 1),
('security', 'jwt_expiration', '7200', 'number', 'JWT过期时间（秒）', 0, 2),
('security', 'jwt_refresh_expiration', '604800', 'number', 'JWT刷新过期时间（秒）', 0, 3),
('security', 'password_min_length', '8', 'number', '密码最小长度', 0, 4),
('security', 'login_fail_threshold', '5', 'number', '登录失败锁定阈值', 0, 5),
('security', 'lock_duration', '1800', 'number', '账户锁定时长（秒）', 0, 6),
('security', 'encryption_key', 'ai-key-management-aes256-encryption-key', 'string', 'AES-256加密密钥', 0, 7),

-- 告警配置
('alert', 'email_enabled', 'false', 'boolean', '是否启用邮件告警', 0, 1),
('alert', 'email_smtp_host', '', 'string', 'SMTP服务器地址', 0, 2),
('alert', 'email_smtp_port', '587', 'number', 'SMTP服务器端口', 0, 3),
('alert', 'email_username', '', 'string', 'SMTP用户名', 0, 4),
('alert', 'email_password', '', 'string', 'SMTP密码', 0, 5),
('alert', 'dingtalk_enabled', 'false', 'boolean', '是否启用钉钉告警', 0, 6),
('alert', 'dingtalk_webhook', '', 'string', '钉钉Webhook地址', 0, 7),
('alert', 'feishu_enabled', 'false', 'boolean', '是否启用飞书告警', 0, 8),
('alert', 'feishu_webhook', '', 'string', '飞书Webhook地址', 0, 9),

-- 日志配置
('log', 'log_retention_days', '90', 'number', '日志保留天数', 0, 1),
('log', 'log_sensitive_fields', 'password,api_key,token', 'string', '敏感字段列表（逗号分隔）', 0, 2),
('log', 'log_mask_enabled', 'true', 'boolean', '是否启用日志脱敏', 0, 3),

-- Auto模式配置
('auto', 'auto_mode_enabled', 'true', 'boolean', '是否启用Auto模式', 1, 1),
('auto', 'default_strategy', 'BALANCED', 'string', '默认选择策略', 1, 2),
('auto', 'selection_timeout', '50', 'number', '选择超时时间（毫秒）', 0, 3),
('auto', 'accuracy_evaluation_cycle', 'weekly', 'string', '准确率评估周期', 0, 4);

-- =====================================================
-- 5. 初始角色模型映射数据（示例）
-- =====================================================

INSERT INTO `role_model_mappings` (`role_id`, `role_name`, `recommended_models`, `selection_strategy`, `fallback_models`, `is_enabled`, `priority`) VALUES
(3, 'USER', '["gpt-4", "gpt-3.5-turbo", "claude-3-sonnet"]', 'BALANCED', '["gpt-3.5-turbo", "claude-3-haiku"]', 1, 10);

-- =====================================================
-- 6. 初始管理员账户
-- =====================================================

-- 默认管理员密码: admin123 (BCrypt加密后的值)
INSERT INTO `users` (`username`, `password`, `email`, `real_name`, `status`, `is_locked`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'admin@example.com', '系统管理员', 1, 0);

-- 关联管理员角色
INSERT INTO `user_roles` (`user_id`, `role_id`) VALUES (1, 1);

-- =====================================================
-- 7. 初始额度数据
-- =====================================================

INSERT INTO `quotas` (`user_id`, `quota_type`, `quota_limit`, `quota_used`, `quota_remaining`, `reset_cycle`) VALUES
(1, 'token', 1000000.00, 0.00, 1000000.00, 'never');

-- =====================================================
-- 8. 初始渠道数据（修复编码问题）
-- =====================================================

INSERT INTO channels (
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

('通义千问', 'qwen-tongyi', 'qwen', 'https://dashscope.aliyuncs.com/api-mode/v1',
 NULL, '["qwen-max", "qwen-plus", "qwen-turbo"]', 80, 6, 1, 1,
 '阿里云通义千问大模型接口'),

('百度文心', 'wenxin-baidu', 'wenxin', 'https://aip.baidubce.com/rpc/2.0/ai_custom/v1',
 NULL, '["ernie-4.0", "ernie-3.5", "ernie-turbo"]', 80, 6, 1, 1,
 '百度文心一言国产大模型接口'),

('Test-Disabled', 'test-disabled', 'openai', 'https://api.test.com/v1',
 NULL, '["test-model"]', 0, 0, 0, 0,
 '测试用禁用渠道');
