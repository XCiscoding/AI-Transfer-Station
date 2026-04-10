SET @has_allowed_group_ids = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'virtual_keys'
    AND column_name = 'allowed_group_ids'
);
SET @sql = IF(
  @has_allowed_group_ids = 0,
  'ALTER TABLE `virtual_keys` ADD COLUMN `allowed_group_ids` TEXT DEFAULT NULL COMMENT ''允许的模型分组ID列表（JSON数组）''',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_channel_id = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'virtual_keys'
    AND column_name = 'channel_id'
);
SET @sql = IF(
  @has_channel_id = 0,
  'ALTER TABLE `virtual_keys` ADD COLUMN `channel_id` BIGINT DEFAULT NULL COMMENT ''绑定渠道ID，为空表示自动调度''',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_idx_channel_id = (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'virtual_keys'
    AND index_name = 'idx_channel_id'
);
SET @sql = IF(
  @has_idx_channel_id = 0,
  'ALTER TABLE `virtual_keys` ADD INDEX `idx_channel_id` (`channel_id`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @fk_exists = (
  SELECT COUNT(*)
  FROM information_schema.table_constraints
  WHERE constraint_schema = DATABASE()
    AND table_name = 'virtual_keys'
    AND constraint_name = 'fk_virtual_keys_channel'
    AND constraint_type = 'FOREIGN KEY'
);
SET @sql = IF(
  @fk_exists = 0,
  'ALTER TABLE `virtual_keys` ADD CONSTRAINT `fk_virtual_keys_channel` FOREIGN KEY (`channel_id`) REFERENCES `channels` (`id`) ON DELETE SET NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
