-- 修复 Flyway checksum 不匹配问题
-- 删除 flyway_schema_history 表中 V4 的记录，让它重新执行

DELETE FROM flyway_schema_history WHERE version = '4';
