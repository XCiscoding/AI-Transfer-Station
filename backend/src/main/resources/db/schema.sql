-- =====================================================
-- AI调度中心 - 企业API Key管理系统
-- 数据库初始化脚本
-- 版本: V1.0
-- 创建时间: 2026-03-30
-- 数据库: MySQL 8.0+
-- =====================================================

-- 设置字符集
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================
-- 1. 用户与权限表
-- =====================================================

-- 用户表
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
  `email` VARCHAR(100) NOT NULL COMMENT '邮箱',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `is_locked` TINYINT NOT NULL DEFAULT 0 COMMENT '是否锁定：0-未锁定，1-已锁定',
  `locked_until` DATETIME DEFAULT NULL COMMENT '锁定截止时间',
  `login_fail_count` INT NOT NULL DEFAULT 0 COMMENT '连续登录失败次数',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 角色表
DROP TABLE IF EXISTS `roles`;
CREATE TABLE `roles` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
  `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '角色描述',
  `is_system` TINYINT NOT NULL DEFAULT 0 COMMENT '是否系统内置：0-否，1-是',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 权限表
DROP TABLE IF EXISTS `permissions`;
CREATE TABLE `permissions` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '权限ID',
  `permission_name` VARCHAR(100) NOT NULL COMMENT '权限名称',
  `permission_code` VARCHAR(100) NOT NULL COMMENT '权限编码',
  `resource_type` VARCHAR(50) NOT NULL COMMENT '资源类型：menu-菜单，button-按钮，api-接口',
  `resource_path` VARCHAR(255) DEFAULT NULL COMMENT '资源路径',
  `http_method` VARCHAR(10) DEFAULT NULL COMMENT 'HTTP方法：GET, POST, PUT, DELETE',
  `parent_id` BIGINT DEFAULT 0 COMMENT '父权限ID',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '权限描述',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permission_code` (`permission_code`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_resource_type` (`resource_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- 用户角色关联表
DROP TABLE IF EXISTS `user_roles`;
CREATE TABLE `user_roles` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
  KEY `idx_role_id` (`role_id`),
  CONSTRAINT `fk_user_roles_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_roles_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- 角色权限关联表
DROP TABLE IF EXISTS `role_permissions`;
CREATE TABLE `role_permissions` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `permission_id` BIGINT NOT NULL COMMENT '权限ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
  KEY `idx_permission_id` (`permission_id`),
  CONSTRAINT `fk_role_permissions_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_role_permissions_permission` FOREIGN KEY (`permission_id`) REFERENCES `permissions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- =====================================================
-- 2. 渠道与模型表
-- =====================================================

-- 渠道表
DROP TABLE IF EXISTS `channels`;
CREATE TABLE `channels` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '渠道ID',
  `channel_name` VARCHAR(100) NOT NULL COMMENT '渠道名称',
  `channel_code` VARCHAR(50) NOT NULL COMMENT '渠道编码',
  `channel_type` VARCHAR(50) NOT NULL COMMENT '渠道类型：openai, qwen, wenxin, doubao, claude, gemini, deepseek',
  `base_url` VARCHAR(255) NOT NULL COMMENT 'API Base URL',
  `api_key_encrypted` TEXT COMMENT 'API Key（AES-256加密）',
  `models` TEXT COMMENT '支持的模型列表（JSON数组）',
  `weight` INT NOT NULL DEFAULT 100 COMMENT '权重（0-100）',
  `priority` INT NOT NULL DEFAULT 0 COMMENT '优先级（数值越大优先级越高）',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用，2-维护中',
  `health_status` TINYINT NOT NULL DEFAULT 1 COMMENT '健康状态：0-不健康，1-健康',
  `health_check_time` DATETIME DEFAULT NULL COMMENT '最后健康检查时间',
  `success_count` BIGINT NOT NULL DEFAULT 0 COMMENT '成功调用次数',
  `fail_count` BIGINT NOT NULL DEFAULT 0 COMMENT '失败调用次数',
  `avg_response_time` INT DEFAULT 0 COMMENT '平均响应时间（毫秒）',
  `config` TEXT COMMENT '渠道配置（JSON格式）',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_channel_code` (`channel_code`),
  KEY `idx_channel_type` (`channel_type`),
  KEY `idx_status` (`status`),
  KEY `idx_health_status` (`health_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='渠道表';

-- 模型表
DROP TABLE IF EXISTS `models`;
CREATE TABLE `models` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '模型ID',
  `model_name` VARCHAR(100) NOT NULL COMMENT '模型名称',
  `model_code` VARCHAR(100) NOT NULL COMMENT '模型编码',
  `model_alias` VARCHAR(100) DEFAULT NULL COMMENT '模型别名',
  `channel_id` BIGINT NOT NULL COMMENT '所属渠道ID',
  `model_type` VARCHAR(50) NOT NULL COMMENT '模型类型：chat, embedding, image',
  `capability_tags` TEXT COMMENT '能力标签（JSON数组）：code, analysis, creative, translation',
  `max_tokens` INT DEFAULT 4096 COMMENT '最大Token数',
  `input_price` DECIMAL(10, 6) DEFAULT 0.000000 COMMENT '输入价格（每千Token，美元）',
  `output_price` DECIMAL(10, 6) DEFAULT 0.000000 COMMENT '输出价格（每千Token，美元）',
  `quota_weight` DECIMAL(5, 2) NOT NULL DEFAULT 1.00 COMMENT '额度倍率（消耗时乘以此系数）',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-下线，1-上线',
  `config` TEXT COMMENT '模型配置（JSON格式）',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_model_code_channel` (`model_code`, `channel_id`),
  KEY `idx_channel_id` (`channel_id`),
  KEY `idx_model_type` (`model_type`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_models_channel` FOREIGN KEY (`channel_id`) REFERENCES `channels` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型表';

-- 模型分组表
DROP TABLE IF EXISTS `model_groups`;
CREATE TABLE `model_groups` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分组ID',
  `group_name` VARCHAR(100) NOT NULL COMMENT '分组名称',
  `group_code` VARCHAR(50) NOT NULL COMMENT '分组编码',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '分组描述',
  `models` TEXT COMMENT '包含的模型列表（JSON数组）',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_code` (`group_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型分组表';

-- 模型配置表
DROP TABLE IF EXISTS `model_configs`;
CREATE TABLE `model_configs` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `model_name` VARCHAR(100) NOT NULL COMMENT '模型名称',
  `model_code` VARCHAR(100) NOT NULL COMMENT '模型编码',
  `model_type` VARCHAR(50) NOT NULL COMMENT '模型类型：chat, embedding, image',
  `provider` VARCHAR(50) NOT NULL COMMENT '提供商：openai, anthropic, baidu, xunfei',
  `input_price` DECIMAL(10, 2) DEFAULT 0.00 COMMENT '输入价格（每千Token，美元）',
  `output_price` DECIMAL(10, 2) DEFAULT 0.00 COMMENT '输出价格（每千Token，美元）',
  `context_window` INT DEFAULT 4096 COMMENT '上下文窗口大小',
  `max_tokens` INT DEFAULT 4096 COMMENT '最大Token数',
  `is_streaming` TINYINT NOT NULL DEFAULT 1 COMMENT '是否支持流式：0-否，1-是',
  `is_vision` TINYINT NOT NULL DEFAULT 0 COMMENT '是否支持视觉：0-否，1-是',
  `is_function_calling` TINYINT NOT NULL DEFAULT 0 COMMENT '是否支持函数调用：0-否，1-是',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_model_code` (`model_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型配置表';

-- =====================================================
-- 3. APIKey表
-- =====================================================

-- 真实Key表
DROP TABLE IF EXISTS `real_keys`;
CREATE TABLE `real_keys` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Key ID',
  `key_name` VARCHAR(100) NOT NULL COMMENT 'Key名称',
  `key_value_encrypted` TEXT NOT NULL COMMENT 'Key值（AES-256加密）',
  `key_mask` VARCHAR(50) NOT NULL COMMENT 'Key掩码（如sk-***...***abc）',
  `channel_id` BIGINT NOT NULL COMMENT '所属渠道ID',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间',
  `usage_count` BIGINT NOT NULL DEFAULT 0 COMMENT '使用次数',
  `last_used_time` DATETIME DEFAULT NULL COMMENT '最后使用时间',
  `base_url` VARCHAR(500) DEFAULT NULL COMMENT 'Key对应接口地址，留空则使用渠道默认地址',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_channel_id` (`channel_id`),
  KEY `idx_status` (`status`),
  KEY `idx_key_mask` (`key_mask`),
  CONSTRAINT `fk_real_keys_channel` FOREIGN KEY (`channel_id`) REFERENCES `channels` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='真实Key表';

-- 虚拟Key表
DROP TABLE IF EXISTS `virtual_keys`;
CREATE TABLE `virtual_keys` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Key ID',
  `key_name` VARCHAR(100) NOT NULL COMMENT 'Key名称',
  `key_value` VARCHAR(100) NOT NULL COMMENT 'Key值（sk-xxx格式）',
  `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
  `team_id` BIGINT DEFAULT NULL COMMENT '所属团队ID',
  `project_id` BIGINT DEFAULT NULL COMMENT '所属项目ID',
  `allowed_models` TEXT COMMENT '允许的模型列表（JSON数组，为空表示不限制）',
  `allowed_group_ids` TEXT DEFAULT NULL COMMENT '允许的模型分组ID列表（JSON数组）',
  `channel_id` BIGINT DEFAULT NULL COMMENT '绑定渠道ID，为空表示自动调度',
  `quota_type` VARCHAR(20) NOT NULL DEFAULT 'token' COMMENT '额度类型：token, count, amount',
  `quota_limit` DECIMAL(20, 2) NOT NULL DEFAULT 0.00 COMMENT '额度上限',
  `quota_used` DECIMAL(20, 2) NOT NULL DEFAULT 0.00 COMMENT '已使用额度',
  `quota_remaining` DECIMAL(20, 2) NOT NULL DEFAULT 0.00 COMMENT '剩余额度',
  `rate_limit_qpm` INT DEFAULT 60 COMMENT '每分钟请求限制',
  `rate_limit_qpd` INT DEFAULT 0 COMMENT '每日请求限制（0表示不限制）',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间',
  `last_used_time` DATETIME DEFAULT NULL COMMENT '最后使用时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key_value` (`key_value`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_team_id` (`team_id`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_channel_id` (`channel_id`),
  KEY `idx_status` (`status`),
  KEY `idx_expire_time` (`expire_time`),
  CONSTRAINT `fk_virtual_keys_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_virtual_keys_channel` FOREIGN KEY (`channel_id`) REFERENCES `channels` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='虚拟Key表';

-- =====================================================
-- 4. 额度与计费表
-- =====================================================

-- 额度表
DROP TABLE IF EXISTS `quotas`;
CREATE TABLE `quotas` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '额度ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
  `team_id` BIGINT DEFAULT NULL COMMENT '团队ID',
  `project_id` BIGINT DEFAULT NULL COMMENT '项目ID',
  `quota_type` VARCHAR(20) NOT NULL COMMENT '额度类型：token, count, amount',
  `quota_limit` DECIMAL(20, 2) NOT NULL DEFAULT 0.00 COMMENT '额度上限',
  `quota_used` DECIMAL(20, 2) NOT NULL DEFAULT 0.00 COMMENT '已使用额度',
  `quota_remaining` DECIMAL(20, 2) NOT NULL DEFAULT 0.00 COMMENT '剩余额度',
  `reset_cycle` VARCHAR(20) DEFAULT 'monthly' COMMENT '重置周期：daily, weekly, monthly, never',
  `last_reset_time` DATETIME DEFAULT NULL COMMENT '最后重置时间',
  `next_reset_time` DATETIME DEFAULT NULL COMMENT '下次重置时间',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_team_id` (`team_id`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_quota_type` (`quota_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='额度表';

-- 计费规则表
DROP TABLE IF EXISTS `billing_rules`;
CREATE TABLE `billing_rules` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  `model_id` BIGINT NOT NULL COMMENT '模型ID',
  `billing_type` VARCHAR(20) NOT NULL COMMENT '计费类型：token, count',
  `input_price` DECIMAL(10, 6) DEFAULT 0.000000 COMMENT '输入价格（每千Token，美元）',
  `output_price` DECIMAL(10, 6) DEFAULT 0.000000 COMMENT '输出价格（每千Token，美元）',
  `fixed_price` DECIMAL(10, 4) DEFAULT 0.0000 COMMENT '固定价格（按次计费时使用）',
  `currency` VARCHAR(10) DEFAULT 'USD' COMMENT '货币类型',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `effective_time` DATETIME DEFAULT NULL COMMENT '生效时间',
  `expire_time` DATETIME DEFAULT NULL COMMENT '失效时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_model_id` (`model_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_billing_rules_model` FOREIGN KEY (`model_id`) REFERENCES `models` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='计费规则表';

-- 额度流水表
DROP TABLE IF EXISTS `quota_transactions`;
CREATE TABLE `quota_transactions` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '流水ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `virtual_key_id` BIGINT DEFAULT NULL COMMENT '虚拟Key ID',
  `call_log_id` BIGINT DEFAULT NULL COMMENT '调用日志ID',
  `quota_type` VARCHAR(20) NOT NULL COMMENT '额度类型：token, count, amount',
  `transaction_type` VARCHAR(20) NOT NULL COMMENT '交易类型：consume, recharge, refund, adjust, reset',
  `target_type` VARCHAR(20) NOT NULL COMMENT '被操作对象类型：virtual_key, team, project',
  `target_id` BIGINT NOT NULL COMMENT '被操作对象ID',
  `amount` DECIMAL(20, 2) NOT NULL COMMENT '变动金额',
  `balance_before` DECIMAL(20, 2) NOT NULL COMMENT '变动前余额',
  `balance_after` DECIMAL(20, 2) NOT NULL COMMENT '变动后余额',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '描述',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_virtual_key_id` (`virtual_key_id`),
  KEY `idx_call_log_id` (`call_log_id`),
  KEY `idx_target` (`target_type`, `target_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='额度流水表';

-- =====================================================
-- 5. 日志表
-- =====================================================

-- 调用日志表
DROP TABLE IF EXISTS `call_logs`;
CREATE TABLE `call_logs` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `trace_id` VARCHAR(64) NOT NULL COMMENT '追踪ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `virtual_key_id` BIGINT DEFAULT NULL COMMENT '虚拟Key ID',
  `channel_id` BIGINT DEFAULT NULL COMMENT '渠道ID',
  `model_id` BIGINT DEFAULT NULL COMMENT '模型ID',
  `model_name` VARCHAR(100) DEFAULT NULL COMMENT '模型名称',
  `request_type` VARCHAR(50) NOT NULL COMMENT '请求类型：chat, embedding, image',
  `request_model` VARCHAR(100) NOT NULL COMMENT '请求的模型',
  `is_auto_mode` TINYINT NOT NULL DEFAULT 0 COMMENT '是否Auto模式：0-否，1-是',
  `selected_model` VARCHAR(100) DEFAULT NULL COMMENT 'Auto模式选中的模型',
  `selection_strategy` VARCHAR(50) DEFAULT NULL COMMENT '选择策略',
  `prompt_tokens` INT DEFAULT 0 COMMENT '输入Token数',
  `completion_tokens` INT DEFAULT 0 COMMENT '输出Token数',
  `total_tokens` INT DEFAULT 0 COMMENT '总Token数',
  `cost` DECIMAL(10, 6) DEFAULT 0.000000 COMMENT '费用（美元）',
  `response_time` INT DEFAULT 0 COMMENT '响应时间（毫秒）',
  `status` TINYINT NOT NULL COMMENT '状态：0-失败，1-成功',
  `error_code` VARCHAR(50) DEFAULT NULL COMMENT '错误码',
  `error_message` TEXT COMMENT '错误信息',
  `client_ip` VARCHAR(50) DEFAULT NULL COMMENT '客户端IP',
  `user_agent` VARCHAR(500) DEFAULT NULL COMMENT '用户代理',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_trace_id` (`trace_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_virtual_key_id` (`virtual_key_id`),
  KEY `idx_channel_id` (`channel_id`),
  KEY `idx_model_id` (`model_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_is_auto_mode` (`is_auto_mode`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='调用日志表';

-- 登录日志表
DROP TABLE IF EXISTS `login_logs`;
CREATE TABLE `login_logs` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `login_type` VARCHAR(20) NOT NULL COMMENT '登录类型：password, sso, ldap',
  `login_status` TINYINT NOT NULL COMMENT '登录状态：0-失败，1-成功',
  `login_ip` VARCHAR(50) DEFAULT NULL COMMENT '登录IP',
  `login_location` VARCHAR(100) DEFAULT NULL COMMENT '登录地点',
  `browser` VARCHAR(100) DEFAULT NULL COMMENT '浏览器',
  `os` VARCHAR(100) DEFAULT NULL COMMENT '操作系统',
  `device` VARCHAR(100) DEFAULT NULL COMMENT '设备',
  `fail_reason` VARCHAR(255) DEFAULT NULL COMMENT '失败原因',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_login_status` (`login_status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志表';

-- =====================================================
-- 6. 团队与项目表
-- =====================================================

-- 团队表
DROP TABLE IF EXISTS `teams`;
CREATE TABLE `teams` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '团队ID',
  `team_name` VARCHAR(100) NOT NULL COMMENT '团队名称',
  `team_code` VARCHAR(50) NOT NULL COMMENT '团队编码',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '团队描述',
  `owner_id` BIGINT NOT NULL COMMENT '团队所有者ID',
  `member_count` INT NOT NULL DEFAULT 0 COMMENT '成员数量',
  `quota_limit` DECIMAL(20, 2) NOT NULL DEFAULT 0.00 COMMENT '额度上限',
  `quota_used` DECIMAL(20, 2) NOT NULL DEFAULT 0.00 COMMENT '已使用额度',
  `quota_remaining` DECIMAL(20, 2) NOT NULL DEFAULT 0.00 COMMENT '剩余额度',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `quota_weight` DECIMAL(5, 2) NOT NULL DEFAULT 1.00 COMMENT '额度倍率（消耗时乘以此系数）',
  `allowed_group_ids` TEXT DEFAULT NULL COMMENT '团队允许使用的模型分组ID列表（JSON数组）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_team_code` (`team_code`),
  KEY `idx_owner_id` (`owner_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_teams_owner` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='团队表';

-- 团队成员表
DROP TABLE IF EXISTS `team_members`;
CREATE TABLE `team_members` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `team_id` BIGINT NOT NULL COMMENT '团队ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role` VARCHAR(20) NOT NULL DEFAULT 'member' COMMENT '角色：owner, admin, member',
  `joined_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_team_user` (`team_id`, `user_id`),
  KEY `idx_user_id` (`user_id`),
  CONSTRAINT `fk_team_members_team` FOREIGN KEY (`team_id`) REFERENCES `teams` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_team_members_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='团队成员表';

-- 项目表
DROP TABLE IF EXISTS `projects`;
CREATE TABLE `projects` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '项目ID',
  `project_name` VARCHAR(100) NOT NULL COMMENT '项目名称',
  `project_code` VARCHAR(50) NOT NULL COMMENT '项目编码',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '项目描述',
  `team_id` BIGINT DEFAULT NULL COMMENT '所属团队ID',
  `owner_id` BIGINT NOT NULL COMMENT '项目所有者ID',
  `quota_limit` DECIMAL(20, 2) NOT NULL DEFAULT 0.00 COMMENT '额度上限',
  `quota_used` DECIMAL(20, 2) NOT NULL DEFAULT 0.00 COMMENT '已使用额度',
  `quota_remaining` DECIMAL(20, 2) NOT NULL DEFAULT 0.00 COMMENT '剩余额度',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `quota_weight` DECIMAL(5, 2) NOT NULL DEFAULT 1.00 COMMENT '额度倍率（消耗时乘以此系数）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_code` (`project_code`),
  KEY `idx_team_id` (`team_id`),
  KEY `idx_owner_id` (`owner_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_projects_team` FOREIGN KEY (`team_id`) REFERENCES `teams` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_projects_owner` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目表';

-- =====================================================
-- 7. Auto模式表
-- =====================================================

-- 角色模型映射表
DROP TABLE IF EXISTS `role_model_mappings`;
CREATE TABLE `role_model_mappings` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '映射ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称（冗余字段）',
  `recommended_models` TEXT NOT NULL COMMENT '推荐模型列表（JSON格式）',
  `selection_strategy` VARCHAR(50) NOT NULL DEFAULT 'BALANCED' COMMENT '选择策略：PERFORMANCE_FIRST, COST_FIRST, AVAILABILITY_FIRST, BALANCED',
  `fallback_models` TEXT COMMENT '降级模型列表（JSON格式）',
  `is_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
  `priority` INT NOT NULL DEFAULT 0 COMMENT '优先级（数值越大优先级越高）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_id` (`role_id`),
  KEY `idx_is_enabled` (`is_enabled`),
  KEY `idx_priority` (`priority`),
  CONSTRAINT `fk_role_model_mappings_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色模型映射表';

-- 模型选择日志表
DROP TABLE IF EXISTS `model_selection_logs`;
CREATE TABLE `model_selection_logs` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `call_id` BIGINT DEFAULT NULL COMMENT '调用ID（关联call_logs表）',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_id` BIGINT DEFAULT NULL COMMENT '角色ID',
  `role_name` VARCHAR(50) DEFAULT NULL COMMENT '角色名称',
  `requested_model` VARCHAR(100) NOT NULL COMMENT '请求的模型（可能为auto）',
  `selected_model` VARCHAR(100) NOT NULL COMMENT '选中的模型',
  `selection_reason` TEXT COMMENT '选择理由（JSON格式，包含评分、决策过程）',
  `selection_strategy` VARCHAR(50) DEFAULT NULL COMMENT '使用的选择策略',
  `candidate_models` TEXT COMMENT '候选模型列表（JSON格式）',
  `is_fallback` TINYINT NOT NULL DEFAULT 0 COMMENT '是否使用降级模型：0-否，1-是',
  `response_time` INT DEFAULT 0 COMMENT '选择耗时（毫秒）',
  `user_feedback` TINYINT DEFAULT NULL COMMENT '用户反馈：0-不满意，1-满意',
  `feedback_time` DATETIME DEFAULT NULL COMMENT '反馈时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_call_id` (`call_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_selected_model` (`selected_model`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_is_fallback` (`is_fallback`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型选择日志表';

-- =====================================================
-- 8. 系统配置表
-- =====================================================

-- 系统配置表
DROP TABLE IF EXISTS `system_configs`;
CREATE TABLE `system_configs` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `config_group` VARCHAR(50) NOT NULL COMMENT '配置分组：basic, security, alert, log',
  `config_key` VARCHAR(100) NOT NULL COMMENT '配置键',
  `config_value` TEXT COMMENT '配置值',
  `config_type` VARCHAR(20) DEFAULT 'string' COMMENT '配置类型：string, number, boolean, json',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '配置描述',
  `is_public` TINYINT NOT NULL DEFAULT 0 COMMENT '是否公开：0-否，1-是',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_group_key` (`config_group`, `config_key`),
  KEY `idx_config_group` (`config_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- 告警规则表
DROP TABLE IF EXISTS `alert_rules`;
CREATE TABLE `alert_rules` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  `rule_name` VARCHAR(100) NOT NULL COMMENT '规则名称',
  `rule_type` VARCHAR(50) NOT NULL COMMENT '规则类型：quota, channel, frequency, error',
  `target_type` VARCHAR(50) NOT NULL COMMENT '目标类型：user, team, project, channel, system',
  `target_id` BIGINT DEFAULT NULL COMMENT '目标ID',
  `condition_config` TEXT NOT NULL COMMENT '条件配置（JSON格式）',
  `action_config` TEXT NOT NULL COMMENT '动作配置（JSON格式）',
  `notification_channels` VARCHAR(255) DEFAULT NULL COMMENT '通知渠道：email, dingtalk, feishu, webhook',
  `is_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
  `last_triggered_time` DATETIME DEFAULT NULL COMMENT '最后触发时间',
  `trigger_count` INT NOT NULL DEFAULT 0 COMMENT '触发次数',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_rule_type` (`rule_type`),
  KEY `idx_target_type` (`target_type`),
  KEY `idx_target_id` (`target_id`),
  KEY `idx_is_enabled` (`is_enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='告警规则表';

-- 告警历史表
DROP TABLE IF EXISTS `alert_histories`;
CREATE TABLE `alert_histories` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '历史ID',
  `rule_id` BIGINT NOT NULL COMMENT '规则ID',
  `alert_type` VARCHAR(50) NOT NULL COMMENT '告警类型',
  `alert_level` VARCHAR(20) NOT NULL COMMENT '告警级别：info, warning, error, critical',
  `alert_title` VARCHAR(255) NOT NULL COMMENT '告警标题',
  `alert_content` TEXT COMMENT '告警内容',
  `target_type` VARCHAR(50) DEFAULT NULL COMMENT '目标类型',
  `target_id` BIGINT DEFAULT NULL COMMENT '目标ID',
  `notification_status` VARCHAR(20) DEFAULT 'pending' COMMENT '通知状态：pending, sent, failed',
  `notification_time` DATETIME DEFAULT NULL COMMENT '通知时间',
  `error_message` TEXT COMMENT '错误信息',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_rule_id` (`rule_id`),
  KEY `idx_alert_level` (`alert_level`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='告警历史表';

SET FOREIGN_KEY_CHECKS = 1;
