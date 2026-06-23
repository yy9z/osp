-- 失物招领完整流程升级
-- 执行前请备份数据库；本脚本面向 MySQL 8。

USE `campus_platform`;

ALTER TABLE `lostfound`
    MODIFY COLUMN `images` TEXT NULL COMMENT '图片URL列表，JSON数组格式',
    MODIFY COLUMN `location` VARCHAR(255) NULL COMMENT '丢失/拾取地点',
    MODIFY COLUMN `status` VARCHAR(20) DEFAULT 'OPEN' COMMENT '状态: OPEN, RESOLVED, REMOVED';

SET @add_lost_time = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `lostfound` ADD COLUMN `lost_time` DATETIME NULL COMMENT ''丢失/拾取时间'' AFTER `location`',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'lostfound'
      AND column_name = 'lost_time'
);
PREPARE add_lost_time_stmt FROM @add_lost_time;
EXECUTE add_lost_time_stmt;
DEALLOCATE PREPARE add_lost_time_stmt;

SET @add_contact = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `lostfound` ADD COLUMN `contact` VARCHAR(100) NULL COMMENT ''本条信息联系方式'' AFTER `reward`',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'lostfound'
      AND column_name = 'contact'
);
PREPARE add_contact_stmt FROM @add_contact;
EXECUTE add_contact_stmt;
DEALLOCATE PREPARE add_contact_stmt;

SET @add_remove_reason = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `lostfound` ADD COLUMN `remove_reason` VARCHAR(500) NULL COMMENT ''管理员下架原因'' AFTER `status`',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'lostfound'
      AND column_name = 'remove_reason'
);
PREPARE add_remove_reason_stmt FROM @add_remove_reason;
EXECUTE add_remove_reason_stmt;
DEALLOCATE PREPARE add_remove_reason_stmt;

-- 兼容旧管理端曾使用的状态值。
UPDATE `lostfound` SET `status` = 'OPEN' WHERE `status` = 'ACTIVE';
UPDATE `lostfound` SET `status` = 'RESOLVED' WHERE `status` = 'CLAIMED';

-- 防止并发请求产生重复认领记录。
DELETE newer
FROM `lostfound_claim` newer
INNER JOIN `lostfound_claim` older
        ON newer.`lostfound_id` = older.`lostfound_id`
       AND newer.`claimer_id` = older.`claimer_id`
       AND newer.`id` > older.`id`;

SET @add_claim_unique_key = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `lostfound_claim` ADD UNIQUE KEY `uk_lostfound_claimer` (`lostfound_id`, `claimer_id`)',
        'SELECT 1'
    )
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'lostfound_claim'
      AND index_name = 'uk_lostfound_claimer'
);
PREPARE add_claim_unique_key_stmt FROM @add_claim_unique_key;
EXECUTE add_claim_unique_key_stmt;
DEALLOCATE PREPARE add_claim_unique_key_stmt;
