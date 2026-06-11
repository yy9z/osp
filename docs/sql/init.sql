-- 创建数据库
CREATE DATABASE IF NOT EXISTS campus_platform CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE campus_platform;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `user_id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码',
    `real_name` VARCHAR(50) COMMENT '真实姓名',
    `phone` VARCHAR(20) COMMENT '手机号',
    `email` VARCHAR(100) COMMENT '邮箱',
    `role` ENUM('STUDENT', 'TEACHER', 'ADMIN') NOT NULL DEFAULT 'STUDENT' COMMENT '角色',
    `avatar` VARCHAR(255) COMMENT '头像URL',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_username` (`username`),
    INDEX `idx_role` (`role`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 插入测试管理员用户 (密码: admin123)
INSERT INTO `user` (username, password, real_name, phone, email, role) 
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '系统管理员', '13800138000', 'admin@campus.com', 'ADMIN');

-- 二手商品表
CREATE TABLE IF NOT EXISTS `secondhand` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '商品ID',
    `title` VARCHAR(100) NOT NULL COMMENT '商品标题',
    `description` TEXT COMMENT '商品描述',
    `price` DECIMAL(10,2) NOT NULL COMMENT '商品价格',
    `category` VARCHAR(20) COMMENT '分类: ELECTRONICS, BOOKS, CLOTHING, SPORTS, DAILY, OTHER',
    `images` VARCHAR(500) COMMENT '图片URL列表，JSON数组格式',
    `condition` VARCHAR(20) COMMENT '新旧程度: NEW, LIKE_NEW, GOOD, FAIR',
    `status` VARCHAR(20) DEFAULT 'AVAILABLE' COMMENT '状态: AVAILABLE, SOLD',
    `seller_id` BIGINT NOT NULL COMMENT '卖家ID',
    `view_count` INT DEFAULT 0 COMMENT '浏览次数',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_seller_id` (`seller_id`),
    INDEX `idx_category` (`category`),
    INDEX `idx_status` (`status`),
    INDEX `idx_create_time` (`create_time`),
    FOREIGN KEY (`seller_id`) REFERENCES `user`(`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='二手商品表';

-- 失物招领表
CREATE TABLE IF NOT EXISTS `lostfound` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `type` VARCHAR(10) NOT NULL COMMENT '类型: LOST(失物), FOUND(招领)',
    `title` VARCHAR(100) NOT NULL COMMENT '标题',
    `description` TEXT COMMENT '描述',
    `category` VARCHAR(20) COMMENT '分类: ELECTRONICS, DOCUMENTS, KEY, CLOTHING, OTHER',
    `images` VARCHAR(500) COMMENT '图片URL列表，JSON数组格式',
    `location` VARCHAR(100) COMMENT '丢失/拾取地点',
    `reward` DECIMAL(10,2) DEFAULT 0 COMMENT '悬赏金额',
    `status` VARCHAR(20) DEFAULT 'OPEN' COMMENT '状态: OPEN, RESOLVED',
    `publisher_id` BIGINT NOT NULL COMMENT '发布者ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_publisher_id` (`publisher_id`),
    INDEX `idx_type` (`type`),
    INDEX `idx_category` (`category`),
    INDEX `idx_status` (`status`),
    INDEX `idx_create_time` (`create_time`),
    FOREIGN KEY (`publisher_id`) REFERENCES `user`(`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='失物招领表';

-- 失物招领认领记录表
CREATE TABLE IF NOT EXISTS `lostfound_claim` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `lostfound_id` BIGINT NOT NULL COMMENT '失物招领ID',
    `claimer_id` BIGINT NOT NULL COMMENT '认领者ID',
    `message` TEXT COMMENT '认领说明',
    `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态: PENDING, APPROVED, REJECTED',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_lostfound_id` (`lostfound_id`),
    INDEX `idx_claimer_id` (`claimer_id`),
    INDEX `idx_status` (`status`),
    FOREIGN KEY (`lostfound_id`) REFERENCES `lostfound`(`id`),
    FOREIGN KEY (`claimer_id`) REFERENCES `user`(`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='失物招领认领记录表';

-- 消息表
CREATE TABLE IF NOT EXISTS `message` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    `sender_id` BIGINT NOT NULL COMMENT '发送者ID',
    `receiver_id` BIGINT NOT NULL COMMENT '接收者ID',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `related_type` VARCHAR(20) COMMENT '关联类型: SECONDHAND, LOSTFOUND, SYSTEM',
    `related_id` BIGINT COMMENT '关联ID',
    `is_read` TINYINT DEFAULT 0 COMMENT '是否已读: 0-未读, 1-已读',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_sender` (`sender_id`),
    INDEX `idx_receiver` (`receiver_id`),
    INDEX `idx_create_time` (`create_time`),
    FOREIGN KEY (`sender_id`) REFERENCES `user`(`user_id`),
    FOREIGN KEY (`receiver_id`) REFERENCES `user`(`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- 宿舍表
CREATE TABLE IF NOT EXISTS `dormitory` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '宿舍ID',
    `building` VARCHAR(20) NOT NULL COMMENT '楼栋号，如"A栋"',
    `floor` INT NOT NULL COMMENT '楼层',
    `room_no` VARCHAR(10) NOT NULL COMMENT '房间号，如"101"',
    `type` VARCHAR(20) COMMENT '房间类型，如"4人间"',
    `capacity` INT NOT NULL COMMENT '容量',
    `current_count` INT DEFAULT 0 COMMENT '当前人数',
    `gender` VARCHAR(10) COMMENT 'MALE(男), FEMALE(女)',
    `head_id` BIGINT COMMENT '宿舍长ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_building` (`building`),
    INDEX `idx_floor` (`floor`),
    INDEX `idx_room_no` (`room_no`),
    INDEX `idx_gender` (`gender`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='宿舍表';

-- 宿舍成员表
CREATE TABLE IF NOT EXISTS `dormitory_member` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `dormitory_id` BIGINT NOT NULL COMMENT '宿舍ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `join_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    UNIQUE KEY `uk_user` (`user_id`),
    INDEX `idx_dormitory_id` (`dormitory_id`),
    FOREIGN KEY (`dormitory_id`) REFERENCES `dormitory`(`id`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='宿舍成员表';

-- 宿舍报修表
CREATE TABLE IF NOT EXISTS `dormitory_repair` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '报修ID',
    `dormitory_id` BIGINT NOT NULL COMMENT '宿舍ID',
    `user_id` BIGINT NOT NULL COMMENT '报修用户ID',
    `description` TEXT NOT NULL COMMENT '报修描述',
    `images` VARCHAR(500) COMMENT '图片URL，JSON数组格式',
    `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态: PENDING, PROCESSING, COMPLETED',
    `remark` VARCHAR(200) COMMENT '处理备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_dormitory_id` (`dormitory_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_status` (`status`),
    FOREIGN KEY (`dormitory_id`) REFERENCES `dormitory`(`id`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='宿舍报修表';

-- 校园地点表
CREATE TABLE IF NOT EXISTS `campus_place` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '地点ID',
    `name` VARCHAR(50) NOT NULL COMMENT '地点名称',
    `category` VARCHAR(20) NOT NULL COMMENT '地点类别: TEACHING(教学楼), DINING(食堂), LIBRARY(图书馆), DORMITORY(宿舍), SPORTS(运动场), ADMIN(行政楼), SCENIC(景点)',
    `description` VARCHAR(200) COMMENT '描述',
    `image` VARCHAR(200) COMMENT '图片URL',
    `latitude` DECIMAL(10,7) COMMENT '纬度',
    `longitude` DECIMAL(10,7) COMMENT '经度',
    `floor` VARCHAR(50) COMMENT '楼层信息，如"1-6层"',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_category` (`category`),
    INDEX `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='校园地点表';

-- 校园地图表
CREATE TABLE IF NOT EXISTS `campus_map` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '地图ID',
    `name` VARCHAR(50) NOT NULL COMMENT '校园名称',
    `center_lat` DECIMAL(10,7) COMMENT '地图中心纬度',
    `center_lng` DECIMAL(10,7) COMMENT '地图中心经度',
    `zoom` INT DEFAULT 16 COMMENT '默认缩放级别',
    `image_url` VARCHAR(200) COMMENT '校园地图图片URL',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='校园地图表';

-- 插入校园地图初始数据
INSERT INTO `campus_map` (name, center_lat, center_lng, zoom, image_url)
VALUES ('校园地图', 39.123456, 116.234567, 16, '/images/campus-map.jpg');

-- 插入校园地点示例数据
INSERT INTO `campus_place` (name, category, description, latitude, longitude, floor) VALUES
('第一教学楼', 'TEACHING', '主要教学楼，内有各类教室和实验室', 39.123456, 116.234567, '1-6层'),
('第二教学楼', 'TEACHING', '文科教学楼', 39.124000, 116.235000, '1-5层'),
('图书馆', 'LIBRARY', '学校主图书馆，藏书丰富', 39.125000, 116.236000, '1-4层'),
('第一食堂', 'DINING', '学生食堂，提供多种美食', 39.122000, 116.233000, '1-3层'),
('第二食堂', 'DINING', '教师食堂和风味餐厅', 39.123500, 116.237000, '1-2层'),
('体育馆', 'SPORTS', '室内体育馆，有篮球场、羽毛球场等', 39.126000, 116.238000, '1-2层'),
('田径场', 'SPORTS', '标准400米田径场', 39.126500, 116.239000, NULL),
('行政楼', 'ADMIN', '学校行政办公大楼', 39.124500, 116.235500, '1-8层'),
('樱花景点', 'SCENIC', '校园著名景点，樱花盛开时景色优美', 39.127000, 116.240000, NULL),
('学生宿舍1号楼', 'DORMITORY', '本科生宿舍', 39.121000, 116.232000, '1-6层'),
('学生宿舍2号楼', 'DORMITORY', '本科生宿舍', 39.121500, 116.232500, '1-6层'),
('研究生宿舍', 'DORMITORY', '研究生及博士生宿舍', 39.122500, 116.234500, '1-10层');

-- 通知表
CREATE TABLE IF NOT EXISTS `notification` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '通知ID',
    `user_id` BIGINT COMMENT '接收通知的用户ID',
    `type` VARCHAR(20) NOT NULL COMMENT '类型: SECONDHAND(二手交易), LOSTFOUND(失物招领), DORMITORY_REPAIR(宿舍报修), SYSTEM(系统通知)',
    `title` VARCHAR(100) NOT NULL COMMENT '标题',
    `content` TEXT COMMENT '内容',
    `related_id` BIGINT COMMENT '关联业务ID',
    `related_type` VARCHAR(20) COMMENT '关联类型',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_type` (`type`),
    INDEX `idx_create_time` (`create_time`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';
