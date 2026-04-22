-- V4: 告警规则与告警历史表

CREATE TABLE IF NOT EXISTS `alert_rules` (
    `id`                    BIGINT          NOT NULL AUTO_INCREMENT,
    `rule_name`             VARCHAR(100)    NOT NULL COMMENT '规则名称',
    `rule_type`             VARCHAR(50)     NOT NULL COMMENT '规则类型: quota_low/channel_down/call_volume/error_rate',
    `target_type`           VARCHAR(50)     NOT NULL COMMENT '目标类型: global/project/channel',
    `target_id`             BIGINT          DEFAULT NULL COMMENT '目标ID，global时为NULL',
    `condition_config`      TEXT            NOT NULL COMMENT '条件配置 JSON',
    `action_config`         TEXT            NOT NULL COMMENT '动作配置 JSON',
    `notification_channels` VARCHAR(255)    DEFAULT NULL COMMENT '通知渠道',
    `is_enabled`            TINYINT         NOT NULL DEFAULT 1 COMMENT '是否启用',
    `last_triggered_time`   DATETIME        DEFAULT NULL COMMENT '最近触发时间',
    `trigger_count`         INT             NOT NULL DEFAULT 0 COMMENT '累计触发次数',
    `created_at`            DATETIME        NOT NULL COMMENT '创建时间',
    `updated_at`            DATETIME        NOT NULL COMMENT '更新时间',
    `deleted`               TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警规则';

CREATE TABLE IF NOT EXISTS `alert_histories` (
    `id`                    BIGINT          NOT NULL AUTO_INCREMENT,
    `rule_id`               BIGINT          NOT NULL COMMENT '关联规则ID',
    `alert_type`            VARCHAR(50)     NOT NULL COMMENT '告警类型',
    `alert_level`           VARCHAR(20)     NOT NULL COMMENT '告警级别: info/warning/critical',
    `alert_title`           VARCHAR(255)    NOT NULL COMMENT '告警标题',
    `alert_content`         TEXT            DEFAULT NULL COMMENT '告警内容',
    `target_type`           VARCHAR(50)     DEFAULT NULL COMMENT '目标类型',
    `target_id`             BIGINT          DEFAULT NULL COMMENT '目标ID',
    `notification_status`   VARCHAR(20)     DEFAULT 'pending' COMMENT '通知状态: pending/sent/failed',
    `notification_time`     DATETIME        DEFAULT NULL COMMENT '通知发送时间',
    `error_message`         TEXT            DEFAULT NULL COMMENT '通知失败原因',
    `created_at`            DATETIME        NOT NULL COMMENT '告警触发时间',
    PRIMARY KEY (`id`),
    KEY `idx_rule_id` (`rule_id`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警历史记录';
