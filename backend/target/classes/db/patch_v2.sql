-- =====================================================
-- patch_v2.sql - 数据库补丁（v2 新增功能）
-- 执行前请确保已执行 schema.sql
-- =====================================================

-- 1. 模型分组表
CREATE TABLE IF NOT EXISTS `model_groups` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `group_name` VARCHAR(100) NOT NULL,
  `description` VARCHAR(500) DEFAULT NULL,
  `model_ids` TEXT NOT NULL COMMENT '模型ID列表（JSON数组）',
  `status` TINYINT NOT NULL DEFAULT 1,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_name` (`group_name`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型分组表';

-- 2. virtual_keys 表新增 allowed_group_ids 字段
ALTER TABLE `virtual_keys`
  ADD COLUMN IF NOT EXISTS `allowed_group_ids` TEXT DEFAULT NULL COMMENT '允许的模型分组ID列表（JSON数组）';

-- 3. 确保 quota_transactions 表存在（使用 schema.sql 中的定义，这里加 IF NOT EXISTS）
CREATE TABLE IF NOT EXISTS `quota_transactions` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '流水ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `virtual_key_id` BIGINT DEFAULT NULL,
  `call_log_id` BIGINT DEFAULT NULL,
  `quota_type` VARCHAR(20) NOT NULL,
  `transaction_type` VARCHAR(20) NOT NULL COMMENT 'consume/recharge/refund/adjust/reset',
  `target_type` VARCHAR(20) NOT NULL COMMENT 'virtual_key/team/project',
  `target_id` BIGINT NOT NULL,
  `amount` DECIMAL(20,2) NOT NULL,
  `balance_before` DECIMAL(20,2) NOT NULL,
  `balance_after` DECIMAL(20,2) NOT NULL,
  `description` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_virtual_key_id` (`virtual_key_id`),
  KEY `idx_target` (`target_type`, `target_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='额度流水表';
