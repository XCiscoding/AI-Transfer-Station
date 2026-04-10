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

SET @has_target_type = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'quota_transactions'
    AND column_name = 'target_type'
);
SET @sql = IF(
  @has_target_type = 0,
  'ALTER TABLE `quota_transactions` ADD COLUMN `target_type` VARCHAR(20) NOT NULL DEFAULT ''virtual_key'' COMMENT ''virtual_key/team/project''',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_target_id = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'quota_transactions'
    AND column_name = 'target_id'
);
SET @sql = IF(
  @has_target_id = 0,
  'ALTER TABLE `quota_transactions` ADD COLUMN `target_id` BIGINT DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_idx_target = (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'quota_transactions'
    AND index_name = 'idx_target'
);
SET @sql = IF(
  @has_idx_target = 0,
  'ALTER TABLE `quota_transactions` ADD INDEX `idx_target` (`target_type`, `target_id`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
