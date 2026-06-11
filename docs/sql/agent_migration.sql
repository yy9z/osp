-- ============================================================
-- Agent 模块数据库迁移脚本
-- 版本: 1.0  日期: 2026-03-09
-- 说明: 本脚本为增量脚本，在已有 campus_platform 库上执行
-- ============================================================

USE campus_platform;

-- ------------------------------------------------------------
-- 1. Agent 会话表（持久化）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `agent_session` (
    `id`            VARCHAR(64)     NOT NULL COMMENT '会话ID（UUID）',
    `user_id`       BIGINT          NOT NULL COMMENT '用户ID',
    `title`         VARCHAR(200)    COMMENT '会话标题（首条消息前20字）',
    `message_count` INT             NOT NULL DEFAULT 0,
    `last_intent`   VARCHAR(50)     COMMENT '最近一次意图',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent会话表';

-- ------------------------------------------------------------
-- 2. Agent 调用日志表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `agent_log` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT,
    `session_id`    VARCHAR(64)     NOT NULL COMMENT 'Agent会话ID',
    `user_id`       BIGINT          NOT NULL COMMENT '发起用户ID',
    `turn`          INT             NOT NULL DEFAULT 1 COMMENT '第几轮对话',
    `user_input`    TEXT            COMMENT '用户输入原文',
    `intent`        VARCHAR(50)     COMMENT '识别意图',
    `slots`         JSON            COMMENT '提取的槽位参数',
    `tools_used`    JSON            COMMENT '本次调用的工具列表',
    `reply`         TEXT            COMMENT 'Agent最终回复',
    `success`       TINYINT(1)      NOT NULL DEFAULT 1,
    `error_msg`     VARCHAR(500)    COMMENT '失败原因',
    `cost_ms`       INT             COMMENT '响应耗时（毫秒）',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_session_id` (`session_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent调用日志表';

-- ------------------------------------------------------------
-- 3. 失物匹配结果表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `lost_match` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT,
    `lost_id`       BIGINT          NOT NULL COMMENT '失物记录ID（lost_found.id where type=LOST）',
    `found_id`      BIGINT          NOT NULL COMMENT '拾物记录ID（lost_found.id where type=FOUND）',
    `similarity`    DECIMAL(5,4)    NOT NULL COMMENT '相似度（0~1）',
    `match_detail`  JSON            COMMENT '各维度得分详情',
    `notified`      TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否已发送通知',
    `status`        VARCHAR(20)     NOT NULL DEFAULT 'CANDIDATE'
                    COMMENT '状态: CANDIDATE/CONFIRMED/REJECTED',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_lost_id` (`lost_id`),
    INDEX `idx_found_id` (`found_id`),
    INDEX `idx_similarity` (`similarity` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='失物匹配结果表';

-- ------------------------------------------------------------
-- 4. 为 dormitory_repair 表添加 source 字段（区分 Agent 提交）
-- 使用 IF NOT EXISTS 防止重复执行报错
-- ------------------------------------------------------------
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'dormitory_repair'
      AND COLUMN_NAME = 'source'
);

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `dormitory_repair` ADD COLUMN `source` VARCHAR(20) NOT NULL DEFAULT \'MANUAL\' COMMENT \'来源: MANUAL/AGENT\'',
    'SELECT ''source column already exists, skipping.'''
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 5. 为 secondhand_goods 添加全文索引（若不存在）
-- ------------------------------------------------------------
SET @idx_exists = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'secondhand_goods'
      AND INDEX_NAME = 'ft_title_desc'
);

SET @sql2 = IF(@idx_exists = 0,
    'ALTER TABLE `secondhand_goods` ADD FULLTEXT INDEX `ft_title_desc` (`title`, `description`)',
    'SELECT ''fulltext index already exists, skipping.'''
);

PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- ------------------------------------------------------------
-- 6. 为 lost_found 添加全文索引（若不存在）
-- ------------------------------------------------------------
SET @idx_lf = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lost_found'
      AND INDEX_NAME = 'ft_name_desc'
);

SET @sql3 = IF(@idx_lf = 0,
    'ALTER TABLE `lost_found` ADD FULLTEXT INDEX `ft_name_desc` (`title`, `description`, `location`)',
    'SELECT ''lost_found fulltext index already exists, skipping.'''
);

PREPARE stmt3 FROM @sql3;
EXECUTE stmt3;
DEALLOCATE PREPARE stmt3;
