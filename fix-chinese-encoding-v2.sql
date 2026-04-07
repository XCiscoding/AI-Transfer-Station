-- =====================================================
-- 第二轮修复：中文数据修复脚本
-- 创建时间: 2026-04-07
-- 执行方式: docker cp 到容器后执行
-- 编码: UTF-8 with BOM (确保Windows兼容性)
-- =====================================================

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 清理损坏数据
DELETE FROM models WHERE id IN (4, 5);
DELETE FROM channels WHERE id IN (3, 4, 5);

-- 插入channels数据（正确UTF-8编码）
INSERT INTO channels (id, channel_name, channel_code, channel_type, base_url, weight, priority, status, health_status, created_at, updated_at, deleted)
VALUES
(3, '通义千问', 'qwen-tongyi', 'qwen', 'https://dashscope.aliyuncs.com/compatible-mode/v1', 100, 0, 1, 1, NOW(), NOW(), 0),
(4, '文心一言', 'wenxin-baidu', 'wenxin', 'https://qianfan.baidubce.com/v2', 100, 0, 1, 1, NOW(), NOW(), 0),
(5, 'Test-Disabled', 'test-disabled', 'test', 'https://test.example.com/v1', 100, 0, 2, 1, NOW(), NOW(), 0);

-- 插入models数据（正确UTF-8编码）
INSERT INTO models (id, model_name, model_code, model_alias, channel_id, model_type, max_tokens, status, created_at, updated_at, deleted)
VALUES
(4, '通义千问-Max', 'qwen-max', 'Qwen Max', 3, 'chat', 8192, 1, NOW(), NOW(), 0),
(5, '文心一言-4.0', 'ernie-4.0', 'ERNIE 4.0', 4, 'chat', 4096, 1, NOW(), NOW(), 0);

-- 验证HEX值（关键检查点）
SELECT '=== Channels HEX Verification ===' as info;
SELECT id, channel_name, HEX(channel_name) as hex_value,
       CASE
         WHEN id = 3 AND HEX(channel_name) = 'E9809AE4B49EE8A099' THEN '✅ CORRECT'
         WHEN id = 4 AND HEX(channel_name) = 'E69687E5B081E8A180' THEN '✅ CORRECT'
         ELSE '❌ WRONG'
       END as validation
FROM channels WHERE id IN (3, 4);

SELECT '=== Models HEX Verification ===' as info;
SELECT id, model_name, HEX(model_name) as hex_value,
       CASE
         WHEN id = 4 AND HEX(model_name) = 'E9809AE4B49EE8A0992D4D6178' THEN '✅ CORRECT'
         WHEN id = 5 AND HEX(model_name) = 'E69687E5B081E8A1802D342E30' THEN '✅ CORRECT'
         ELSE '❌ WRONG'
       END as validation
FROM models WHERE id IN (4, 5);
