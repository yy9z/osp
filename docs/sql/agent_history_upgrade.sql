USE campus_platform;

CREATE TABLE IF NOT EXISTS `agent_session` (
    `id`            VARCHAR(64)     NOT NULL COMMENT '会话ID',
    `user_id`       BIGINT          NOT NULL COMMENT '用户ID',
    `title`         VARCHAR(200)    COMMENT '会话标题',
    `message_count` INT             NOT NULL DEFAULT 0,
    `last_intent`   VARCHAR(50)     COMMENT '最近意图',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_updated_at` (`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent会话表';

CREATE TABLE IF NOT EXISTS `agent_log` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT,
    `session_id`    VARCHAR(64)     NOT NULL COMMENT '会话ID',
    `user_id`       BIGINT          NOT NULL COMMENT '用户ID',
    `turn`          INT             NOT NULL DEFAULT 1 COMMENT '轮次',
    `user_input`    TEXT            COMMENT '用户输入',
    `intent`        VARCHAR(50)     COMMENT '识别意图',
    `slots`         JSON            COMMENT '槽位参数',
    `tools_used`    JSON            COMMENT '工具列表',
    `response_payload` JSON         COMMENT '响应载荷(卡片/建议等)',
    `reply`         TEXT            COMMENT 'Agent回复',
    `success`       TINYINT(1)      NOT NULL DEFAULT 1,
    `error_msg`     VARCHAR(500)    COMMENT '失败原因',
    `cost_ms`       INT             COMMENT '耗时毫秒',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_session_id` (`session_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent对话日志表';

SET @agent_log_response_payload_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'agent_log'
      AND COLUMN_NAME = 'response_payload'
);
SET @agent_log_response_payload_sql := IF(
    @agent_log_response_payload_exists = 0,
    'ALTER TABLE `agent_log` ADD COLUMN `response_payload` JSON COMMENT ''响应载荷(卡片/建议等)'' AFTER `tools_used`',
    'SELECT 1'
);
PREPARE stmt_agent_log_response_payload FROM @agent_log_response_payload_sql;
EXECUTE stmt_agent_log_response_payload;
DEALLOCATE PREPARE stmt_agent_log_response_payload;
