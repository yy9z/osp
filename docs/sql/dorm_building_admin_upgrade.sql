SET @db_name = DATABASE();

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'dorm_building' AND COLUMN_NAME = 'campus') = 0,
    'ALTER TABLE dorm_building ADD COLUMN campus VARCHAR(50) NULL COMMENT ''校区'' AFTER code',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'dorm_building' AND COLUMN_NAME = 'rooms_per_floor') = 0,
    'ALTER TABLE dorm_building ADD COLUMN rooms_per_floor INT NULL COMMENT ''每层房间数'' AFTER floors',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'dorm_building' AND COLUMN_NAME = 'capacity_per_room') = 0,
    'ALTER TABLE dorm_building ADD COLUMN capacity_per_room INT NULL COMMENT ''每间房入住人数'' AFTER rooms_per_floor',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE user_dormitory ud
JOIN user u ON u.user_id = ud.user_id
SET ud.campus = NULL,
    ud.building = NULL,
    ud.room = NULL,
    ud.bed = NULL,
    ud.check_in_date = NULL
WHERE u.username = 'user';