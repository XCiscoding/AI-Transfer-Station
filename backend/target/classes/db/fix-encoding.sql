-- =====================================================
-- 中文编码乱码数据修复脚本
-- 执行时间：2026-04-07
-- 问题描述：由于MySQL客户端连接字符集配置错误（latin1），
--           导致通过API创建的中文数据在写入时被转换为问号
-- 修复方案：删除损坏数据，重新插入正确的中文字符
-- =====================================================

-- 设置字符集
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- =====================================================
-- 1. 修复 channels 表数据
-- =====================================================

-- 删除损坏的渠道数据（ID 3,4,5）
DELETE FROM channels WHERE id IN (3, 4, 5);

-- 重新插入正确的中文渠道数据
INSERT INTO channels (id, channel_name, channel_code, channel_type, base_url, weight, priority, status, health_status, created_at, updated_at, deleted)
VALUES
(3, '通义千问', 'qwen-tongyi', 'qwen', 'https://dashscope.aliyuncs.com/compatible-mode/v1', 100, 0, 1, 1, NOW(), NOW(), 0),
(4, '文心一言', 'wenxin-baidu', 'wenxin', 'https://qianfan.baidubce.com/v2', 100, 0, 1, 1, NOW(), NOW(), 0),
(5, '测试-已禁用', 'test-disabled', 'test', 'https://test.example.com/v1', 100, 0, 2, 1, NOW(), NOW(), 0);

-- 同时修复 ID=1 的"官方"部分（如果也损坏了）
UPDATE channels SET channel_name = 'OpenAI官方' WHERE id = 1;

-- =====================================================
-- 2. 修复 models 表数据
-- =====================================================

-- 删除损坏的模型数据（ID 4,5）
DELETE FROM models WHERE id IN (4, 5);

-- 重新插入正确的中文模型数据
INSERT INTO models (id, model_name, model_code, model_alias, channel_id, model_type, max_tokens, status, created_at, updated_at, deleted)
VALUES
(4, '通义千问-Max', 'qwen-max', 'Qwen Max', 3, 'chat', 8192, 1, NOW(), NOW(), 0),
(5, '文心一言-4.0', 'ernie-4.0', 'ERNIE 4.0', 4, 'chat', 4096, 1, NOW(), NOW(), 0);

-- =====================================================
-- 验证修复结果
-- =====================================================

SELECT '=== 渠道数据验证 ===' AS info;
SELECT id, channel_name, channel_code, channel_type FROM channels ORDER BY id;

SELECT '=== 模型数据验证 ===' AS info;
SELECT id, model_name, model_code, channel_id FROM models ORDER BY id;

SELECT '=== 修复完成 ===' AS info;
