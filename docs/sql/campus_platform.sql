/*
 Navicat Premium Dump SQL

 Source Server         : Learn_Javaweb
 Source Server Type    : MySQL
 Source Server Version : 80040 (8.0.40)
 Source Host           : localhost:3306
 Source Schema         : campus_platform

 Target Server Type    : MySQL
 Target Server Version : 80040 (8.0.40)
 File Encoding         : 65001

 Date: 08/03/2026 21:38:02
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for broadcast_read
-- ----------------------------
DROP TABLE IF EXISTS `broadcast_read`;
CREATE TABLE `broadcast_read` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `message_id` bigint NOT NULL COMMENT '系统公告ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `read_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '读取时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_message_user` (`message_id`,`user_id`),
  KEY `idx_user_id` (`user_id`),
  CONSTRAINT `broadcast_read_ibfk_1` FOREIGN KEY (`message_id`) REFERENCES `message` (`id`) ON DELETE CASCADE,
  CONSTRAINT `broadcast_read_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统公告读取记录表';

-- ----------------------------
-- Records of broadcast_read
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for campus_map
-- ----------------------------
DROP TABLE IF EXISTS `campus_map`;
CREATE TABLE `campus_map` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '地图ID',
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '校园名称',
  `center_lat` decimal(10,7) DEFAULT NULL COMMENT '地图中心纬度',
  `center_lng` decimal(10,7) DEFAULT NULL COMMENT '地图中心经度',
  `zoom` int DEFAULT '16' COMMENT '默认缩放级别',
  `image_url` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '校园地图图片URL',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='校园地图表';

-- ----------------------------
-- Records of campus_map
-- ----------------------------
BEGIN;
INSERT INTO `campus_map` (`id`, `name`, `center_lat`, `center_lng`, `zoom`, `image_url`, `create_time`, `update_time`) VALUES (1, '校园地图', 39.1234560, 116.2345670, 16, '/images/campus-map.jpg', '2026-02-23 11:09:14', '2026-02-23 11:09:14');
COMMIT;

-- ----------------------------
-- Table structure for campus_path_edge
-- ----------------------------
DROP TABLE IF EXISTS `campus_path_edge`;
CREATE TABLE `campus_path_edge` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `from_node_id` bigint NOT NULL,
  `to_node_id` bigint NOT NULL,
  `distance` double NOT NULL,
  `way_type` varchar(20) DEFAULT 'WALK',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_from` (`from_node_id`),
  KEY `idx_to` (`to_node_id`),
  CONSTRAINT `campus_path_edge_ibfk_1` FOREIGN KEY (`from_node_id`) REFERENCES `campus_path_node` (`id`),
  CONSTRAINT `campus_path_edge_ibfk_2` FOREIGN KEY (`to_node_id`) REFERENCES `campus_path_node` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=58 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of campus_path_edge
-- ----------------------------
BEGIN;
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (1, 1, 4, 280, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (2, 1, 17, 180, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (3, 4, 3, 60, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (4, 4, 17, 120, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (5, 4, 11, 50, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (6, 3, 17, 80, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (7, 11, 18, 40, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (8, 18, 10, 35, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (9, 2, 19, 150, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (10, 2, 10, 80, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (11, 10, 18, 60, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (12, 19, 3, 100, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (13, 19, 9, 120, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (14, 3, 9, 40, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (15, 9, 8, 80, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (16, 8, 7, 60, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (17, 5, 12, 130, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (18, 12, 6, 80, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (19, 6, 19, 90, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (20, 7, 17, 60, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (21, 8, 17, 50, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (22, 14, 18, 250, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (23, 15, 4, 320, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (24, 16, 5, 120, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (25, 4, 1, 280, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (26, 17, 4, 120, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (27, 3, 4, 60, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (28, 17, 3, 80, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (29, 11, 4, 50, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (30, 18, 4, 70, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (31, 18, 11, 40, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (32, 10, 18, 35, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (33, 2, 19, 150, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (34, 10, 2, 80, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (35, 18, 10, 60, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (36, 19, 2, 150, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (37, 3, 19, 100, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (38, 19, 9, 120, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (39, 9, 3, 40, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (40, 9, 8, 80, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (41, 8, 9, 80, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (42, 5, 12, 130, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (43, 12, 5, 130, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (44, 6, 12, 80, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (45, 12, 6, 80, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (46, 19, 6, 90, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (47, 6, 19, 90, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (48, 7, 17, 60, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (49, 17, 7, 60, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (50, 8, 17, 50, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (51, 17, 8, 50, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (52, 18, 14, 250, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (53, 14, 18, 250, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (54, 4, 15, 320, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (55, 15, 4, 320, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (56, 5, 16, 120, 'WALK', '2026-02-23 12:26:30');
INSERT INTO `campus_path_edge` (`id`, `from_node_id`, `to_node_id`, `distance`, `way_type`, `create_time`) VALUES (57, 16, 5, 120, 'WALK', '2026-02-23 12:26:30');
COMMIT;

-- ----------------------------
-- Table structure for campus_path_node
-- ----------------------------
DROP TABLE IF EXISTS `campus_path_node`;
CREATE TABLE `campus_path_node` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `latitude` decimal(10,7) NOT NULL,
  `longitude` decimal(10,7) NOT NULL,
  `node_type` varchar(20) DEFAULT 'WAYPOINT',
  `poi_id` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_location` (`latitude`,`longitude`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of campus_path_node
-- ----------------------------
BEGIN;
INSERT INTO `campus_path_node` (`id`, `name`, `latitude`, `longitude`, `node_type`, `poi_id`, `create_time`) VALUES (1, '东门', 31.8414430, 117.2698320, 'POI', 13, '2026-02-23 12:26:30');
INSERT INTO `campus_path_node` (`id`, `name`, `latitude`, `longitude`, `node_type`, `poi_id`, `create_time`) VALUES (2, '西区教学楼', 31.8385200, 117.2705100, 'POI', 1, '2026-02-23 12:26:30');
INSERT INTO `campus_path_node` (`id`, `name`, `latitude`, `longitude`, `node_type`, `poi_id`, `create_time`) VALUES (3, '学生食堂', 31.8358200, 117.2715400, 'POI', 3, '2026-02-23 12:26:30');
INSERT INTO `campus_path_node` (`id`, `name`, `latitude`, `longitude`, `node_type`, `poi_id`, `create_time`) VALUES (4, '图书馆', 31.8371800, 117.2695900, 'POI', 7, '2026-02-23 12:26:30');
INSERT INTO `campus_path_node` (`id`, `name`, `latitude`, `longitude`, `node_type`, `poi_id`, `create_time`) VALUES (5, '研究生宿舍楼', 31.8402000, 117.2168000, 'POI', 5, '2026-02-23 12:26:30');
INSERT INTO `campus_path_node` (`id`, `name`, `latitude`, `longitude`, `node_type`, `poi_id`, `create_time`) VALUES (6, '体育馆', 31.8295100, 117.2718200, 'POI', 8, '2026-02-23 12:26:30');
INSERT INTO `campus_path_node` (`id`, `name`, `latitude`, `longitude`, `node_type`, `poi_id`, `create_time`) VALUES (7, '田径场', 31.8390000, 117.2550000, 'POI', 9, '2026-02-23 12:26:30');
INSERT INTO `campus_path_node` (`id`, `name`, `latitude`, `longitude`, `node_type`, `poi_id`, `create_time`) VALUES (8, '东区教学楼', 31.8385200, 117.2705100, 'POI', 2, '2026-02-23 12:26:30');
INSERT INTO `campus_path_node` (`id`, `name`, `latitude`, `longitude`, `node_type`, `poi_id`, `create_time`) VALUES (9, '教工食堂', 31.8378500, 117.2701200, 'POI', 4, '2026-02-23 12:26:30');
INSERT INTO `campus_path_node` (`id`, `name`, `latitude`, `longitude`, `node_type`, `poi_id`, `create_time`) VALUES (10, '行政楼', 31.8372000, 117.2675000, 'POI', 10, '2026-02-23 12:26:30');
INSERT INTO `campus_path_node` (`id`, `name`, `latitude`, `longitude`, `node_type`, `poi_id`, `create_time`) VALUES (11, '樱花广场', 31.8378500, 117.2701200, 'POI', 11, '2026-02-23 12:26:30');
INSERT INTO `campus_path_node` (`id`, `name`, `latitude`, `longitude`, `node_type`, `poi_id`, `create_time`) VALUES (12, '人工湖', 31.8375000, 117.2540000, 'POI', 12, '2026-02-23 12:26:30');
INSERT INTO `campus_path_node` (`id`, `name`, `latitude`, `longitude`, `node_type`, `poi_id`, `create_time`) VALUES (13, '博士生楼', 31.8398000, 117.2178000, 'POI', 6, '2026-02-23 12:26:30');
INSERT INTO `campus_path_node` (`id`, `name`, `latitude`, `longitude`, `node_type`, `poi_id`, `create_time`) VALUES (14, '西门', 31.8375100, 117.2668500, 'POI', 14, '2026-02-23 12:26:30');
INSERT INTO `campus_path_node` (`id`, `name`, `latitude`, `longitude`, `node_type`, `poi_id`, `create_time`) VALUES (15, '南门', 31.8330150, 117.2715420, 'POI', 15, '2026-02-23 12:26:30');
INSERT INTO `campus_path_node` (`id`, `name`, `latitude`, `longitude`, `node_type`, `poi_id`, `create_time`) VALUES (16, '北门', 31.8414430, 117.2698320, 'POI', 16, '2026-02-23 12:26:30');
INSERT INTO `campus_path_node` (`id`, `name`, `latitude`, `longitude`, `node_type`, `poi_id`, `create_time`) VALUES (17, '交叉口1', 31.8385000, 117.2680000, 'INTERSECTION', NULL, '2026-02-23 12:26:30');
INSERT INTO `campus_path_node` (`id`, `name`, `latitude`, `longitude`, `node_type`, `poi_id`, `create_time`) VALUES (18, '交叉口2', 31.8385000, 117.2650000, 'INTERSECTION', NULL, '2026-02-23 12:26:30');
INSERT INTO `campus_path_node` (`id`, `name`, `latitude`, `longitude`, `node_type`, `poi_id`, `create_time`) VALUES (19, '交叉口3', 31.8390000, 117.2670000, 'INTERSECTION', NULL, '2026-02-23 12:26:30');
INSERT INTO `campus_path_node` (`id`, `name`, `latitude`, `longitude`, `node_type`, `poi_id`, `create_time`) VALUES (20, '交叉口4', 31.8375000, 117.2660000, 'INTERSECTION', NULL, '2026-02-23 12:26:30');
COMMIT;

-- ----------------------------
-- Table structure for campus_poi
-- ----------------------------
DROP TABLE IF EXISTS `campus_poi`;
CREATE TABLE `campus_poi` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL COMMENT '地点名称',
  `category` varchar(20) NOT NULL COMMENT '类别: TEACHING,DINING,LIBRARY,DORMITORY,SPORTS,ADMIN,SCENIC,ENTRANCE',
  `description` varchar(200) DEFAULT NULL,
  `latitude` decimal(10,7) NOT NULL,
  `longitude` decimal(10,7) NOT NULL,
  `floor` varchar(50) DEFAULT NULL,
  `visibility` varchar(20) DEFAULT 'PUBLIC' COMMENT '可见性: PUBLIC-公共, PRIVATE-私有',
  `created_by` bigint DEFAULT NULL COMMENT '创建者ID',
  `image_url` varchar(200) DEFAULT NULL,
  `open_time` varchar(100) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`),
  KEY `idx_location` (`latitude`,`longitude`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of campus_poi
-- ----------------------------
BEGIN;
INSERT INTO `campus_poi` (`id`, `name`, `category`, `description`, `latitude`, `longitude`, `floor`, `visibility`, `created_by`, `image_url`, `open_time`, `create_time`, `update_time`) VALUES (1, '北门(黄山路)', 'ENTRANCE', '靠近北区宿舍，导航首选北入口', 31.8410000, 117.2700000, NULL, 'PUBLIC', NULL, NULL, '全天', '2026-02-23 15:56:59', '2026-03-05 23:34:42');
INSERT INTO `campus_poi` (`id`, `name`, `category`, `description`, `latitude`, `longitude`, `floor`, `visibility`, `created_by`, `image_url`, `open_time`, `create_time`, `update_time`) VALUES (2, '西大门(金寨路)', 'ENTRANCE', '标志性主校门，近行政楼', 31.8368540, 117.2696030, NULL, 'PUBLIC', NULL, NULL, '全天', '2026-02-23 15:56:59', '2026-03-05 23:34:42');
INSERT INTO `campus_poi` (`id`, `name`, `category`, `description`, `latitude`, `longitude`, `floor`, `visibility`, `created_by`, `image_url`, `open_time`, `create_time`, `update_time`) VALUES (3, '老图书馆(红楼)', 'LIBRARY', '核心景观，校史馆所在地', 31.8370000, 117.2692320, '1-5层', 'PUBLIC', NULL, NULL, '07:00-22:00', '2026-02-23 15:56:59', '2026-03-05 23:21:27');
INSERT INTO `campus_poi` (`id`, `name`, `category`, `description`, `latitude`, `longitude`, `floor`, `visibility`, `created_by`, `image_url`, `open_time`, `create_time`, `update_time`) VALUES (4, '第一教学楼', 'TEACHING', '核心教学区，近樱花大道', 31.8385000, 117.2690000, '1-6层', 'PUBLIC', NULL, NULL, '07:00-22:00', '2026-02-23 15:56:59', '2026-03-05 23:34:42');
INSERT INTO `campus_poi` (`id`, `name`, `category`, `description`, `latitude`, `longitude`, `floor`, `visibility`, `created_by`, `image_url`, `open_time`, `create_time`, `update_time`) VALUES (5, '郭沫若广场', 'SCENIC', '校园活动中心，铜像所在地', 31.8382180, 117.2691740, NULL, 'PUBLIC', NULL, NULL, '全天', '2026-02-23 15:56:59', '2026-03-05 23:34:42');
INSERT INTO `campus_poi` (`id`, `name`, `category`, `description`, `latitude`, `longitude`, `floor`, `visibility`, `created_by`, `image_url`, `open_time`, `create_time`, `update_time`) VALUES (6, '东区学生食堂', 'DINING', '生活区核心，近221/222宿舍', 31.8370710, 117.2706000, '1-3层', 'PUBLIC', NULL, NULL, '06:30-21:00', '2026-02-23 15:56:59', '2026-03-05 23:34:42');
INSERT INTO `campus_poi` (`id`, `name`, `category`, `description`, `latitude`, `longitude`, `floor`, `visibility`, `created_by`, `image_url`, `open_time`, `create_time`, `update_time`) VALUES (7, '北正门(黄山路)', 'ENTRANCE', '西区主入口，近三教', 31.8410390, 117.2567520, NULL, 'PUBLIC', NULL, NULL, '全天', '2026-02-23 15:56:59', '2026-03-06 00:04:24');
INSERT INTO `campus_poi` (`id`, `name`, `category`, `description`, `latitude`, `longitude`, `floor`, `visibility`, `created_by`, `image_url`, `open_time`, `create_time`, `update_time`) VALUES (8, '西区图书馆', 'LIBRARY', '校园最高建筑之一，高层自习区', 31.8392020, 117.2574020, '1-10层', 'PUBLIC', NULL, NULL, '07:00-22:00', '2026-02-23 15:56:59', '2026-03-05 23:21:27');
INSERT INTO `campus_poi` (`id`, `name`, `category`, `description`, `latitude`, `longitude`, `floor`, `visibility`, `created_by`, `image_url`, `open_time`, `create_time`, `update_time`) VALUES (9, '第三教学楼(三教)', 'TEACHING', '最大的公共教学楼', 31.8385000, 117.2530000, '1-8层', 'PUBLIC', NULL, NULL, '07:00-22:00', '2026-02-23 15:56:59', '2026-03-05 23:34:42');
INSERT INTO `campus_poi` (`id`, `name`, `category`, `description`, `latitude`, `longitude`, `floor`, `visibility`, `created_by`, `image_url`, `open_time`, `create_time`, `update_time`) VALUES (10, '一丹人工湖', 'SCENIC', '休闲景观区，黑天鹅栖息地', 31.8387620, 117.2630310, NULL, 'PUBLIC', NULL, NULL, '全天', '2026-02-23 15:56:59', '2026-03-05 23:34:42');
INSERT INTO `campus_poi` (`id`, `name`, `category`, `description`, `latitude`, `longitude`, `floor`, `visibility`, `created_by`, `image_url`, `open_time`, `create_time`, `update_time`) VALUES (11, '西区活动中心', 'DINING', '包含校内电影院与食堂', 31.8395460, 117.2534920, '1-4层', 'PUBLIC', NULL, NULL, '07:00-23:00', '2026-02-23 15:56:59', '2026-03-05 23:34:42');
INSERT INTO `campus_poi` (`id`, `name`, `category`, `description`, `latitude`, `longitude`, `floor`, `visibility`, `created_by`, `image_url`, `open_time`, `create_time`, `update_time`) VALUES (12, '中区北门(黄山路)', 'ENTRANCE', '步行连廊北起点', 31.8330150, 117.2715420, NULL, 'PUBLIC', NULL, NULL, '全天', '2026-02-23 15:56:59', '2026-02-23 15:56:59');
INSERT INTO `campus_poi` (`id`, `name`, `category`, `description`, `latitude`, `longitude`, `floor`, `visibility`, `created_by`, `image_url`, `open_time`, `create_time`, `update_time`) VALUES (13, '综合体育馆', 'SPORTS', '游泳馆、球场所在地', 31.8392250, 117.2628750, '1-3层', 'PUBLIC', NULL, NULL, '08:00-22:00', '2026-02-23 15:56:59', '2026-03-05 23:34:42');
INSERT INTO `campus_poi` (`id`, `name`, `category`, `description`, `latitude`, `longitude`, `floor`, `visibility`, `created_by`, `image_url`, `open_time`, `create_time`, `update_time`) VALUES (14, '现代艺术中心', 'SCENIC', '素质教育基地', 31.8383620, 117.2610290, '1-2层', 'PUBLIC', NULL, NULL, '09:00-17:00', '2026-02-23 15:56:59', '2026-03-05 23:34:42');
INSERT INTO `campus_poi` (`id`, `name`, `category`, `description`, `latitude`, `longitude`, `floor`, `visibility`, `created_by`, `image_url`, `open_time`, `create_time`, `update_time`) VALUES (15, '南校区-东正门', 'ENTRANCE', '徽州大道1129号', 31.8221510, 117.2843430, NULL, 'PUBLIC', NULL, NULL, '全天', '2026-02-23 15:56:59', '2026-02-23 15:56:59');
INSERT INTO `campus_poi` (`id`, `name`, `category`, `description`, `latitude`, `longitude`, `floor`, `visibility`, `created_by`, `image_url`, `open_time`, `create_time`, `update_time`) VALUES (16, '高新园区-图文中心', 'LIBRARY', '最新校区地标', 31.8235300, 117.1287200, '1-8层', 'PUBLIC', NULL, NULL, '07:00-22:00', '2026-02-23 15:56:59', '2026-02-23 15:56:59');
INSERT INTO `campus_poi` (`id`, `name`, `category`, `description`, `latitude`, `longitude`, `floor`, `visibility`, `created_by`, `image_url`, `open_time`, `create_time`, `update_time`) VALUES (17, '行政楼', 'ADMIN', '学校行政办公中心', 31.8365000, 117.2675000, '1-8层', 'PUBLIC', NULL, NULL, '08:00-17:30', '2026-02-23 15:56:59', '2026-03-05 23:21:27');
INSERT INTO `campus_poi` (`id`, `name`, `category`, `description`, `latitude`, `longitude`, `floor`, `visibility`, `created_by`, `image_url`, `open_time`, `create_time`, `update_time`) VALUES (18, '校医院', 'ADMIN', '校园医疗服务', 31.8334070, 117.2660390, '1-3层', 'PUBLIC', NULL, NULL, '08:00-17:00', '2026-02-23 15:56:59', '2026-03-05 23:34:42');
INSERT INTO `campus_poi` (`id`, `name`, `category`, `description`, `latitude`, `longitude`, `floor`, `visibility`, `created_by`, `image_url`, `open_time`, `create_time`, `update_time`) VALUES (19, '东校区操场', 'SPORTS', '标准田径场', 31.8364690, 117.2669330, NULL, 'PUBLIC', NULL, NULL, '全天', '2026-02-23 15:56:59', '2026-03-05 23:34:42');
INSERT INTO `campus_poi` (`id`, `name`, `category`, `description`, `latitude`, `longitude`, `floor`, `visibility`, `created_by`, `image_url`, `open_time`, `create_time`, `update_time`) VALUES (20, '西区操场', 'SPORTS', '标准田径场', 31.8390000, 117.2550000, NULL, 'PUBLIC', NULL, NULL, '全天', '2026-02-23 15:56:59', '2026-02-23 15:56:59');
COMMIT;

-- ----------------------------
-- Table structure for campus_region
-- ----------------------------
DROP TABLE IF EXISTS `campus_region`;
CREATE TABLE `campus_region` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `type` varchar(20) NOT NULL,
  `geojson` text,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of campus_region
-- ----------------------------
BEGIN;
INSERT INTO `campus_region` (`id`, `name`, `type`, `geojson`, `create_time`) VALUES (1, '田径场区域', 'SPORTS', '{\"type\":\"Polygon\",\"coordinates\":[[[117.2175,31.8365],[117.2190,31.8365],[117.2190,31.8360],[117.2175,31.8360],[117.2175,31.8365]]]}', '2026-02-23 12:26:30');
INSERT INTO `campus_region` (`id`, `name`, `type`, `geojson`, `create_time`) VALUES (2, '人工湖区域', 'SCENIC', '{\"type\":\"Polygon\",\"coordinates\":[[[117.2175,31.8385],[117.2185,31.8385],[117.2185,31.8395],[117.2175,31.8395],[117.2175,31.8385]]]}', '2026-02-23 12:26:30');
COMMIT;

-- ----------------------------
-- Table structure for conversation_settings
-- ----------------------------
DROP TABLE IF EXISTS `conversation_settings`;
CREATE TABLE `conversation_settings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `other_user_id` bigint NOT NULL COMMENT '对方用户ID',
  `is_pinned` tinyint DEFAULT '0' COMMENT '是否置顶 0-否 1-是',
  `is_deleted` tinyint DEFAULT '0' COMMENT '是否删除 0-否 1-是',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_other` (`user_id`,`other_user_id`),
  KEY `idx_user_pinned` (`user_id`,`is_pinned`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会话设置表';

-- ----------------------------
-- Records of conversation_settings
-- ----------------------------
BEGIN;
INSERT INTO `conversation_settings` (`id`, `user_id`, `other_user_id`, `is_pinned`, `is_deleted`, `create_time`, `update_time`) VALUES (4, 4, 8, 0, NULL, '2026-03-01 00:02:52', '2026-03-01 01:53:50');
COMMIT;

-- ----------------------------
-- Table structure for dorm_building
-- ----------------------------
DROP TABLE IF EXISTS `dorm_building`;
CREATE TABLE `dorm_building` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `code` varchar(20) NOT NULL,
  `campus` varchar(50) DEFAULT NULL COMMENT '校区',
  `floors` int NOT NULL DEFAULT '6',
  `rooms_per_floor` int DEFAULT NULL COMMENT '每层房间数',
  `capacity_per_room` int DEFAULT NULL COMMENT '每间房入住人数',
  `gender` varchar(10) DEFAULT NULL,
  `manager_id` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of dorm_building
-- ----------------------------
BEGIN;
INSERT INTO `dorm_building` (`id`, `name`, `code`, `campus`, `floors`, `rooms_per_floor`, `capacity_per_room`, `gender`, `manager_id`, `create_time`) VALUES (1, '学生公寓3栋', '东校区-学生公寓3栋', '东校区', 6, 10, 4, 'MALE', 7, '2026-03-07 00:34:57');
COMMIT;

-- ----------------------------
-- Table structure for dormitory
-- ----------------------------
DROP TABLE IF EXISTS `dormitory`;
CREATE TABLE `dormitory` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '宿舍ID',
  `building` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '楼栋号，如"A栋"',
  `floor` int NOT NULL COMMENT '楼层',
  `room_no` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '房间号，如"101"',
  `type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '房间类型，如"4人间"',
  `capacity` int NOT NULL COMMENT '容量',
  `current_count` int DEFAULT '0' COMMENT '当前人数',
  `gender` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'MALE(男), FEMALE(女)',
  `head_id` bigint DEFAULT NULL COMMENT '宿舍长ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_building` (`building`),
  KEY `idx_floor` (`floor`),
  KEY `idx_room_no` (`room_no`),
  KEY `idx_gender` (`gender`)
) ENGINE=InnoDB AUTO_INCREMENT=61 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='宿舍表';

-- ----------------------------
-- Records of dormitory
-- ----------------------------
BEGIN;
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (1, '学生公寓3栋', 1, '101', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (2, '学生公寓3栋', 1, '102', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (3, '学生公寓3栋', 1, '103', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (4, '学生公寓3栋', 1, '104', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (5, '学生公寓3栋', 1, '105', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (6, '学生公寓3栋', 1, '106', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (7, '学生公寓3栋', 1, '107', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (8, '学生公寓3栋', 1, '108', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (9, '学生公寓3栋', 1, '109', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (10, '学生公寓3栋', 1, '110', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (11, '学生公寓3栋', 2, '201', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (12, '学生公寓3栋', 2, '202', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (13, '学生公寓3栋', 2, '203', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (14, '学生公寓3栋', 2, '204', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (15, '学生公寓3栋', 2, '205', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (16, '学生公寓3栋', 2, '206', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (17, '学生公寓3栋', 2, '207', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (18, '学生公寓3栋', 2, '208', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (19, '学生公寓3栋', 2, '209', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (20, '学生公寓3栋', 2, '210', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (21, '学生公寓3栋', 3, '301', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (22, '学生公寓3栋', 3, '302', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (23, '学生公寓3栋', 3, '303', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (24, '学生公寓3栋', 3, '304', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (25, '学生公寓3栋', 3, '305', '4人间', 4, 1, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 01:05:43');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (26, '学生公寓3栋', 3, '306', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (27, '学生公寓3栋', 3, '307', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (28, '学生公寓3栋', 3, '308', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (29, '学生公寓3栋', 3, '309', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (30, '学生公寓3栋', 3, '310', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (31, '学生公寓3栋', 4, '401', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (32, '学生公寓3栋', 4, '402', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (33, '学生公寓3栋', 4, '403', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (34, '学生公寓3栋', 4, '404', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (35, '学生公寓3栋', 4, '405', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (36, '学生公寓3栋', 4, '406', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (37, '学生公寓3栋', 4, '407', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (38, '学生公寓3栋', 4, '408', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (39, '学生公寓3栋', 4, '409', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (40, '学生公寓3栋', 4, '410', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (41, '学生公寓3栋', 5, '501', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (42, '学生公寓3栋', 5, '502', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (43, '学生公寓3栋', 5, '503', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (44, '学生公寓3栋', 5, '504', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (45, '学生公寓3栋', 5, '505', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (46, '学生公寓3栋', 5, '506', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (47, '学生公寓3栋', 5, '507', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (48, '学生公寓3栋', 5, '508', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (49, '学生公寓3栋', 5, '509', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (50, '学生公寓3栋', 5, '510', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (51, '学生公寓3栋', 6, '601', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (52, '学生公寓3栋', 6, '602', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (53, '学生公寓3栋', 6, '603', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (54, '学生公寓3栋', 6, '604', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (55, '学生公寓3栋', 6, '605', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (56, '学生公寓3栋', 6, '606', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (57, '学生公寓3栋', 6, '607', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (58, '学生公寓3栋', 6, '608', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (59, '学生公寓3栋', 6, '609', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
INSERT INTO `dormitory` (`id`, `building`, `floor`, `room_no`, `type`, `capacity`, `current_count`, `gender`, `head_id`, `create_time`, `update_time`) VALUES (60, '学生公寓3栋', 6, '610', '4人间', 4, 0, 'MALE', NULL, '2026-03-07 00:34:57', '2026-03-07 00:34:57');
COMMIT;

-- ----------------------------
-- Table structure for dormitory_member
-- ----------------------------
DROP TABLE IF EXISTS `dormitory_member`;
CREATE TABLE `dormitory_member` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `dormitory_id` bigint NOT NULL COMMENT '宿舍ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `join_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user` (`user_id`),
  KEY `idx_dormitory_id` (`dormitory_id`),
  CONSTRAINT `dormitory_member_ibfk_1` FOREIGN KEY (`dormitory_id`) REFERENCES `dormitory` (`id`),
  CONSTRAINT `dormitory_member_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='宿舍成员表';

-- ----------------------------
-- Records of dormitory_member
-- ----------------------------
BEGIN;
INSERT INTO `dormitory_member` (`id`, `dormitory_id`, `user_id`, `join_time`) VALUES (1, 25, 4, '2026-03-07 01:05:43');
COMMIT;

-- ----------------------------
-- Table structure for dormitory_repair
-- ----------------------------
DROP TABLE IF EXISTS `dormitory_repair`;
CREATE TABLE `dormitory_repair` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '报修ID',
  `dormitory_id` bigint DEFAULT NULL,
  `campus` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `building` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `room_no` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `user_id` bigint NOT NULL COMMENT '报修用户ID',
  `description` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '报修描述',
  `images` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图片URL，JSON数组格式',
  `urgency` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'low' COMMENT '紧急程度',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING' COMMENT '状态: PENDING, PROCESSING, COMPLETED',
  `remark` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '处理备注',
  `handle_images` text COLLATE utf8mb4_unicode_ci COMMENT '处理图片',
  `handler_id` bigint DEFAULT NULL COMMENT '处理人ID',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `processing_started_at` datetime DEFAULT NULL COMMENT '开始处理时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_dormitory_id` (`dormitory_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `dormitory_repair_ibfk_1` FOREIGN KEY (`dormitory_id`) REFERENCES `dormitory` (`id`),
  CONSTRAINT `dormitory_repair_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='宿舍报修表';

-- ----------------------------
-- Records of dormitory_repair
-- ----------------------------
BEGIN;
INSERT INTO `dormitory_repair` (`id`, `dormitory_id`, `campus`, `building`, `room_no`, `user_id`, `description`, `images`, `urgency`, `status`, `remark`, `handle_images`, `handler_id`, `handle_time`, `processing_started_at`, `create_time`, `update_time`) VALUES (1, 25, '西校区', '学生公寓3栋', '305', 4, '3435354', NULL, 'high', 'COMPLETED', '维修完成', NULL, 7, '2026-02-28 17:20:20', NULL, '2026-02-28 16:46:25', '2026-03-07 13:31:54');
INSERT INTO `dormitory_repair` (`id`, `dormitory_id`, `campus`, `building`, `room_no`, `user_id`, `description`, `images`, `urgency`, `status`, `remark`, `handle_images`, `handler_id`, `handle_time`, `processing_started_at`, `create_time`, `update_time`) VALUES (2, 25, '西校区', '学生公寓3栋', '305', 4, '234244444444444444444444444', NULL, 'medium', 'COMPLETED', '已经处理完成', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/2026/03/4a289790-5ad7-4b94-b544-a73ed04d68c6.jpg\"]', 7, '2026-03-01 01:40:20', '2026-03-01 01:39:55', '2026-02-28 16:46:13', '2026-03-07 13:31:54');
INSERT INTO `dormitory_repair` (`id`, `dormitory_id`, `campus`, `building`, `room_no`, `user_id`, `description`, `images`, `urgency`, `status`, `remark`, `handle_images`, `handler_id`, `handle_time`, `processing_started_at`, `create_time`, `update_time`) VALUES (3, 25, '西校区', '学生公寓3栋', '305', 4, '234244444444444444444444444', NULL, 'medium', 'COMPLETED', '已处理结束', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/2026/02/9cd398c0-2d34-4cfd-93c6-672eba224592.jpg\"]', 7, '2026-02-28 18:08:44', '2026-02-28 17:58:12', '2026-02-28 02:40:56', '2026-03-07 13:31:54');
INSERT INTO `dormitory_repair` (`id`, `dormitory_id`, `campus`, `building`, `room_no`, `user_id`, `description`, `images`, `urgency`, `status`, `remark`, `handle_images`, `handler_id`, `handle_time`, `processing_started_at`, `create_time`, `update_time`) VALUES (4, 25, '西校区', '学生公寓3栋', '305', 4, '水龙头坏了', NULL, 'low', 'PENDING', NULL, NULL, NULL, NULL, NULL, '2026-03-01 01:34:05', '2026-03-07 13:31:54');
INSERT INTO `dormitory_repair` (`id`, `dormitory_id`, `campus`, `building`, `room_no`, `user_id`, `description`, `images`, `urgency`, `status`, `remark`, `handle_images`, `handler_id`, `handle_time`, `processing_started_at`, `create_time`, `update_time`) VALUES (5, 25, '西校区', '学生公寓3栋', '305', 4, '桌子有问题', NULL, 'medium', 'COMPLETED', '已修复', NULL, 7, '2026-03-01 11:59:07', '2026-03-01 11:58:49', '2026-03-01 11:56:45', '2026-03-07 13:31:54');
INSERT INTO `dormitory_repair` (`id`, `dormitory_id`, `campus`, `building`, `room_no`, `user_id`, `description`, `images`, `urgency`, `status`, `remark`, `handle_images`, `handler_id`, `handle_time`, `processing_started_at`, `create_time`, `update_time`) VALUES (8, 25, '东校区', '学生公寓3栋', '305', 4, '空调坏了，风很小', NULL, 'medium', 'PENDING', NULL, NULL, NULL, NULL, NULL, '2026-03-07 13:35:33', '2026-03-07 13:35:33');
INSERT INTO `dormitory_repair` (`id`, `dormitory_id`, `campus`, `building`, `room_no`, `user_id`, `description`, `images`, `urgency`, `status`, `remark`, `handle_images`, `handler_id`, `handle_time`, `processing_started_at`, `create_time`, `update_time`) VALUES (9, 25, '东校区', '学生公寓3栋', '305', 4, '宿管最新动态通知验证', NULL, 'medium', 'PENDING', NULL, NULL, NULL, NULL, NULL, '2026-03-07 13:43:54', '2026-03-07 13:43:54');
COMMIT;

-- ----------------------------
-- Table structure for goods_favorite
-- ----------------------------
DROP TABLE IF EXISTS `goods_favorite`;
CREATE TABLE `goods_favorite` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `goods_id` bigint NOT NULL,
  `create_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_goods` (`user_id`,`goods_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_goods_id` (`goods_id`)
) ENGINE=InnoDB AUTO_INCREMENT=40 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Records of goods_favorite
-- ----------------------------
BEGIN;
INSERT INTO `goods_favorite` (`id`, `user_id`, `goods_id`, `create_time`) VALUES (2, 2, 4, '2026-02-25 00:45:40');
INSERT INTO `goods_favorite` (`id`, `user_id`, `goods_id`, `create_time`) VALUES (4, 2, 5, '2026-02-25 00:46:56');
INSERT INTO `goods_favorite` (`id`, `user_id`, `goods_id`, `create_time`) VALUES (6, 4, 6, '2026-02-25 01:53:58');
INSERT INTO `goods_favorite` (`id`, `user_id`, `goods_id`, `create_time`) VALUES (8, 2, 68, '2026-02-25 23:58:16');
INSERT INTO `goods_favorite` (`id`, `user_id`, `goods_id`, `create_time`) VALUES (9, 2, 6, '2026-02-25 23:58:21');
INSERT INTO `goods_favorite` (`id`, `user_id`, `goods_id`, `create_time`) VALUES (10, 2, 67, '2026-02-25 23:58:26');
INSERT INTO `goods_favorite` (`id`, `user_id`, `goods_id`, `create_time`) VALUES (11, 2, 55, '2026-02-25 23:58:27');
INSERT INTO `goods_favorite` (`id`, `user_id`, `goods_id`, `create_time`) VALUES (12, 2, 77, '2026-02-25 23:58:29');
INSERT INTO `goods_favorite` (`id`, `user_id`, `goods_id`, `create_time`) VALUES (13, 2, 95, '2026-02-25 23:58:35');
INSERT INTO `goods_favorite` (`id`, `user_id`, `goods_id`, `create_time`) VALUES (14, 2, 24, '2026-02-25 23:58:43');
INSERT INTO `goods_favorite` (`id`, `user_id`, `goods_id`, `create_time`) VALUES (15, 2, 66, '2026-02-25 23:58:47');
INSERT INTO `goods_favorite` (`id`, `user_id`, `goods_id`, `create_time`) VALUES (16, 2, 38, '2026-02-25 23:58:48');
INSERT INTO `goods_favorite` (`id`, `user_id`, `goods_id`, `create_time`) VALUES (17, 2, 61, '2026-02-25 23:58:50');
INSERT INTO `goods_favorite` (`id`, `user_id`, `goods_id`, `create_time`) VALUES (18, 2, 36, '2026-02-25 23:59:11');
INSERT INTO `goods_favorite` (`id`, `user_id`, `goods_id`, `create_time`) VALUES (19, 2, 48, '2026-02-25 23:59:13');
INSERT INTO `goods_favorite` (`id`, `user_id`, `goods_id`, `create_time`) VALUES (20, 2, 44, '2026-02-25 23:59:26');
INSERT INTO `goods_favorite` (`id`, `user_id`, `goods_id`, `create_time`) VALUES (21, 4, 68, '2026-02-26 02:13:06');
INSERT INTO `goods_favorite` (`id`, `user_id`, `goods_id`, `create_time`) VALUES (22, 4, 44, '2026-02-26 02:13:07');
INSERT INTO `goods_favorite` (`id`, `user_id`, `goods_id`, `create_time`) VALUES (23, 4, 77, '2026-02-26 02:13:09');
INSERT INTO `goods_favorite` (`id`, `user_id`, `goods_id`, `create_time`) VALUES (24, 4, 40, '2026-02-26 02:13:12');
INSERT INTO `goods_favorite` (`id`, `user_id`, `goods_id`, `create_time`) VALUES (25, 4, 98, '2026-02-26 02:13:14');
INSERT INTO `goods_favorite` (`id`, `user_id`, `goods_id`, `create_time`) VALUES (26, 4, 7, '2026-02-26 02:13:15');
INSERT INTO `goods_favorite` (`id`, `user_id`, `goods_id`, `create_time`) VALUES (27, 4, 89, '2026-02-26 02:13:18');
INSERT INTO `goods_favorite` (`id`, `user_id`, `goods_id`, `create_time`) VALUES (28, 4, 103, '2026-03-01 01:28:50');
INSERT INTO `goods_favorite` (`id`, `user_id`, `goods_id`, `create_time`) VALUES (29, 4, 18, '2026-03-01 01:28:52');
INSERT INTO `goods_favorite` (`id`, `user_id`, `goods_id`, `create_time`) VALUES (30, 4, 9, '2026-03-01 01:28:56');
INSERT INTO `goods_favorite` (`id`, `user_id`, `goods_id`, `create_time`) VALUES (31, 4, 101, '2026-03-01 01:29:27');
INSERT INTO `goods_favorite` (`id`, `user_id`, `goods_id`, `create_time`) VALUES (32, 4, 67, '2026-03-01 01:29:44');
INSERT INTO `goods_favorite` (`id`, `user_id`, `goods_id`, `create_time`) VALUES (33, 4, 92, '2026-03-01 01:29:46');
INSERT INTO `goods_favorite` (`id`, `user_id`, `goods_id`, `create_time`) VALUES (34, 4, 105, '2026-03-01 01:30:06');
INSERT INTO `goods_favorite` (`id`, `user_id`, `goods_id`, `create_time`) VALUES (35, 4, 104, '2026-03-01 01:30:09');
INSERT INTO `goods_favorite` (`id`, `user_id`, `goods_id`, `create_time`) VALUES (37, 4, 45, '2026-03-01 01:30:13');
INSERT INTO `goods_favorite` (`id`, `user_id`, `goods_id`, `create_time`) VALUES (38, 4, 56, '2026-03-01 01:30:21');
INSERT INTO `goods_favorite` (`id`, `user_id`, `goods_id`, `create_time`) VALUES (39, 4, 25, '2026-03-01 11:55:29');
COMMIT;

-- ----------------------------
-- Table structure for lostfound
-- ----------------------------
DROP TABLE IF EXISTS `lostfound`;
CREATE TABLE `lostfound` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `type` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类型: LOST(失物), FOUND(招领)',
  `title` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题',
  `description` text COLLATE utf8mb4_unicode_ci COMMENT '描述',
  `category` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分类: ELECTRONICS, DOCUMENTS, KEY, CLOTHING, OTHER',
  `images` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图片URL列表，JSON数组格式',
  `location` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '丢失/拾取地点',
  `reward` decimal(10,2) DEFAULT '0.00' COMMENT '悬赏金额',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE-有效, CLAIMED-已认领, REMOVED-已下架',
  `remove_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '下架原因',
  `remover_id` bigint DEFAULT NULL COMMENT '下架操作人ID',
  `remove_time` datetime DEFAULT NULL COMMENT '下架时间',
  `publisher_id` bigint NOT NULL COMMENT '发布者ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_publisher_id` (`publisher_id`),
  KEY `idx_type` (`type`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `lostfound_ibfk_1` FOREIGN KEY (`publisher_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='失物招领表';

-- ----------------------------
-- Records of lostfound
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for lostfound_claim
-- ----------------------------
DROP TABLE IF EXISTS `lostfound_claim`;
CREATE TABLE `lostfound_claim` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `lostfound_id` bigint NOT NULL COMMENT '失物招领ID',
  `claimer_id` bigint NOT NULL COMMENT '认领者ID',
  `message` text COLLATE utf8mb4_unicode_ci COMMENT '认领说明',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING' COMMENT '状态: PENDING, APPROVED, REJECTED',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_lostfound_id` (`lostfound_id`),
  KEY `idx_claimer_id` (`claimer_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `lostfound_claim_ibfk_1` FOREIGN KEY (`lostfound_id`) REFERENCES `lostfound` (`id`),
  CONSTRAINT `lostfound_claim_ibfk_2` FOREIGN KEY (`claimer_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='失物招领认领记录表';

-- ----------------------------
-- Records of lostfound_claim
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for message
-- ----------------------------
DROP TABLE IF EXISTS `message`;
CREATE TABLE `message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PERSONAL' COMMENT '消息类型: BROADCAST-系统公告, PERSONAL-个人通知',
  `title` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '消息标题',
  `sender_id` bigint NOT NULL COMMENT '发送者ID',
  `receiver_id` bigint DEFAULT NULL COMMENT '接收者ID（系统公告时为空）',
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息内容',
  `related_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联类型: SECONDHAND, LOSTFOUND, SYSTEM',
  `related_id` bigint DEFAULT NULL COMMENT '关联ID',
  `is_read` tinyint DEFAULT '0' COMMENT '是否已读: 0-未读, 1-已读',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_sender` (`sender_id`),
  KEY `idx_receiver` (`receiver_id`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `message_ibfk_1` FOREIGN KEY (`sender_id`) REFERENCES `user` (`user_id`),
  CONSTRAINT `message_ibfk_2` FOREIGN KEY (`receiver_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=33 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- ----------------------------
-- Records of message
-- ----------------------------
BEGIN;
INSERT INTO `message` (`id`, `type`, `title`, `sender_id`, `receiver_id`, `content`, `related_type`, `related_id`, `is_read`, `create_time`) VALUES (20, 'PERSONAL', NULL, 2, 4, '在吗？东西出不出？', NULL, NULL, 1, '2026-02-26 02:11:54');
INSERT INTO `message` (`id`, `type`, `title`, `sender_id`, `receiver_id`, `content`, `related_type`, `related_id`, `is_read`, `create_time`) VALUES (21, 'PERSONAL', NULL, 2, 4, '能不能小刀一下', NULL, NULL, 1, '2026-02-26 02:12:05');
INSERT INTO `message` (`id`, `type`, `title`, `sender_id`, `receiver_id`, `content`, `related_type`, `related_id`, `is_read`, `create_time`) VALUES (22, 'PERSONAL', NULL, 4, 2, '可以的，你说个价格', NULL, NULL, 1, '2026-02-26 02:12:39');
INSERT INTO `message` (`id`, `type`, `title`, `sender_id`, `receiver_id`, `content`, `related_type`, `related_id`, `is_read`, `create_time`) VALUES (23, 'PERSONAL', NULL, 4, 2, '我考虑考虑', NULL, NULL, 1, '2026-02-26 02:12:48');
INSERT INTO `message` (`id`, `type`, `title`, `sender_id`, `receiver_id`, `content`, `related_type`, `related_id`, `is_read`, `create_time`) VALUES (24, 'PERSONAL', NULL, 8, 4, '这个可以小刀吗～', NULL, NULL, 1, '2026-03-01 00:01:14');
INSERT INTO `message` (`id`, `type`, `title`, `sender_id`, `receiver_id`, `content`, `related_type`, `related_id`, `is_read`, `create_time`) VALUES (25, 'PERSONAL', NULL, 8, 4, '滴滴', NULL, NULL, 1, '2026-03-01 00:01:21');
INSERT INTO `message` (`id`, `type`, `title`, `sender_id`, `receiver_id`, `content`, `related_type`, `related_id`, `is_read`, `create_time`) VALUES (26, 'PERSONAL', NULL, 2, 4, '111怎么样？', NULL, NULL, 1, '2026-03-01 00:02:12');
INSERT INTO `message` (`id`, `type`, `title`, `sender_id`, `receiver_id`, `content`, `related_type`, `related_id`, `is_read`, `create_time`) VALUES (27, 'PERSONAL', NULL, 4, 2, '你好', NULL, NULL, 1, '2026-03-01 01:28:16');
INSERT INTO `message` (`id`, `type`, `title`, `sender_id`, `receiver_id`, `content`, `related_type`, `related_id`, `is_read`, `create_time`) VALUES (28, 'PERSONAL', NULL, 4, 2, '能不能小刀', NULL, NULL, 1, '2026-03-01 01:28:25');
INSERT INTO `message` (`id`, `type`, `title`, `sender_id`, `receiver_id`, `content`, `related_type`, `related_id`, `is_read`, `create_time`) VALUES (29, 'PERSONAL', NULL, 4, 8, '你好呀', NULL, NULL, 1, '2026-03-01 01:31:05');
INSERT INTO `message` (`id`, `type`, `title`, `sender_id`, `receiver_id`, `content`, `related_type`, `related_id`, `is_read`, `create_time`) VALUES (30, 'PERSONAL', NULL, 4, 8, '可以小刀的', NULL, NULL, 1, '2026-03-01 01:31:09');
INSERT INTO `message` (`id`, `type`, `title`, `sender_id`, `receiver_id`, `content`, `related_type`, `related_id`, `is_read`, `create_time`) VALUES (31, 'PERSONAL', NULL, 2, 4, '能', NULL, NULL, 1, '2026-03-01 01:52:16');
INSERT INTO `message` (`id`, `type`, `title`, `sender_id`, `receiver_id`, `content`, `related_type`, `related_id`, `is_read`, `create_time`) VALUES (32, 'PERSONAL', NULL, 4, 2, '你好', NULL, NULL, 0, '2026-03-01 11:55:44');
COMMIT;

-- ----------------------------
-- Table structure for notification
-- ----------------------------
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `user_id` bigint DEFAULT NULL COMMENT '接收通知的用户ID',
  `type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类型: SECONDHAND, LOSTFOUND, DORMITORY_REPAIR, SYSTEM',
  `title` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题',
  `content` text COLLATE utf8mb4_unicode_ci COMMENT '内容',
  `related_id` bigint DEFAULT NULL COMMENT '关联业务ID',
  `related_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联类型',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_type` (`type`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_user_id` (`user_id`),
  CONSTRAINT `fk_notification_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';

-- ----------------------------
-- Records of notification
-- ----------------------------
BEGIN;
INSERT INTO `notification` (`id`, `user_id`, `type`, `title`, `content`, `related_id`, `related_type`, `create_time`) VALUES (3, 4, 'SECONDHAND', '商品已售出', '您发布的商品【手机支架保修中】已标记为已售出', 101, 'SECONDHAND', '2026-02-26 02:13:24');
INSERT INTO `notification` (`id`, `user_id`, `type`, `title`, `content`, `related_id`, `related_type`, `create_time`) VALUES (4, 4, 'SECONDHAND', '商品已售出', '您发布的商品【手机支架保修中】已标记为已售出', 101, 'SECONDHAND', '2026-02-26 02:13:32');
INSERT INTO `notification` (`id`, `user_id`, `type`, `title`, `content`, `related_id`, `related_type`, `create_time`) VALUES (5, 4, 'SECONDHAND', '商品已售出', '您发布的商品【手机支架保修中】已标记为已售出', 101, 'SECONDHAND', '2026-02-26 02:13:41');
INSERT INTO `notification` (`id`, `user_id`, `type`, `title`, `content`, `related_id`, `related_type`, `create_time`) VALUES (6, 4, 'DORMITORY_REPAIR', '新的宿舍报修请求', '学生公寓3栋-305 室提交了报修申请', 1, 'DORMITORY_REPAIR', '2026-02-27 17:17:35');
INSERT INTO `notification` (`id`, `user_id`, `type`, `title`, `content`, `related_id`, `related_type`, `create_time`) VALUES (7, 4, 'DORMITORY_REPAIR', '新的宿舍报修请求', '学生公寓3栋-305 室提交了报修申请', 2, 'DORMITORY_REPAIR', '2026-02-27 17:32:58');
INSERT INTO `notification` (`id`, `user_id`, `type`, `title`, `content`, `related_id`, `related_type`, `create_time`) VALUES (8, 4, 'DORMITORY_REPAIR', '新的宿舍报修请求', '学生公寓3栋-305 室提交了报修申请', 3, 'DORMITORY_REPAIR', '2026-02-28 02:15:23');
INSERT INTO `notification` (`id`, `user_id`, `type`, `title`, `content`, `related_id`, `related_type`, `create_time`) VALUES (9, 4, 'REPAIR_COMPLETED', '报修已完成', '您的报修申请已处理完成', 1, 'DORMITORY_REPAIR', '2026-02-28 17:20:20');
INSERT INTO `notification` (`id`, `user_id`, `type`, `title`, `content`, `related_id`, `related_type`, `create_time`) VALUES (10, 2, 'SECONDHAND', '商品审核通过', '您发布的商品【洗衣液搬家急出】已审核通过并上架', 7, 'SECONDHAND', '2026-02-28 19:44:18');
INSERT INTO `notification` (`id`, `user_id`, `type`, `title`, `content`, `related_id`, `related_type`, `create_time`) VALUES (11, 2, 'SECONDHAND', '商品已下架', '您发布的商品【正版《机器学习》】已被管理员下架，原因：测试', 54, 'SECONDHAND', '2026-02-28 19:54:39');
INSERT INTO `notification` (`id`, `user_id`, `type`, `title`, `content`, `related_id`, `related_type`, `create_time`) VALUES (12, 2, 'SECONDHAND', '商品已下架', '您发布的商品【转让蓝牙音箱自用】已下架', 43, 'SECONDHAND', '2026-02-28 20:53:22');
INSERT INTO `notification` (`id`, `user_id`, `type`, `title`, `content`, `related_id`, `related_type`, `create_time`) VALUES (13, 2, 'SECONDHAND', '商品审核通过', '您发布的商品【转让蓝牙音箱自用】已审核通过并上架', 43, 'SECONDHAND', '2026-02-28 20:53:50');
INSERT INTO `notification` (`id`, `user_id`, `type`, `title`, `content`, `related_id`, `related_type`, `create_time`) VALUES (14, 2, 'SECONDHAND', '商品已下架', '您发布的商品【拖把搬家急出】已被管理员下架，原因：测试', 77, 'SECONDHAND', '2026-02-28 21:06:30');
INSERT INTO `notification` (`id`, `user_id`, `type`, `title`, `content`, `related_id`, `related_type`, `create_time`) VALUES (15, 2, 'SECONDHAND', '商品已下架', '您发布的商品【足球专业级】已被管理员下架，原因：测试', 24, 'SECONDHAND', '2026-02-28 21:06:35');
INSERT INTO `notification` (`id`, `user_id`, `type`, `title`, `content`, `related_id`, `related_type`, `create_time`) VALUES (16, 4, 'SECONDHAND', '商品已下架', '您发布的商品【晾衣绳节省空间】已被管理员下架，原因：测试', 55, 'SECONDHAND', '2026-02-28 21:06:38');
INSERT INTO `notification` (`id`, `user_id`, `type`, `title`, `content`, `related_id`, `related_type`, `create_time`) VALUES (17, 4, 'SECONDHAND', '商品已下架', '您发布的商品【操作系统相关书籍】已被管理员下架，原因：测试', 47, 'SECONDHAND', '2026-02-28 21:06:46');
INSERT INTO `notification` (`id`, `user_id`, `type`, `title`, `content`, `related_id`, `related_type`, `create_time`) VALUES (18, 4, 'SECONDHAND', '商品审核被拒绝', '您发布的商品【操作系统相关书籍】未通过审核，原因：测试', 47, 'SECONDHAND', '2026-02-28 21:10:28');
INSERT INTO `notification` (`id`, `user_id`, `type`, `title`, `content`, `related_id`, `related_type`, `create_time`) VALUES (19, 4, 'SECONDHAND', '商品已下架', '您发布的商品【晾衣绳节省空间】已下架', 55, 'SECONDHAND', '2026-02-28 21:18:30');
INSERT INTO `notification` (`id`, `user_id`, `type`, `title`, `content`, `related_id`, `related_type`, `create_time`) VALUES (20, 4, 'SECONDHAND', '商品已售出', '您发布的商品【镜子九成新】已标记为已售出', 40, 'SECONDHAND', '2026-02-28 21:18:44');
INSERT INTO `notification` (`id`, `user_id`, `type`, `title`, `content`, `related_id`, `related_type`, `create_time`) VALUES (21, 4, 'SECONDHAND', '商品已下架', '您发布的商品【转让工具箱闲置】已下架', 46, 'SECONDHAND', '2026-02-28 21:39:39');
INSERT INTO `notification` (`id`, `user_id`, `type`, `title`, `content`, `related_id`, `related_type`, `create_time`) VALUES (22, 4, 'SECONDHAND', '商品审核被拒绝', '您发布的商品【操作系统相关书籍】未通过审核，原因：测试\n', 47, 'SECONDHAND', '2026-02-28 21:40:13');
INSERT INTO `notification` (`id`, `user_id`, `type`, `title`, `content`, `related_id`, `related_type`, `create_time`) VALUES (23, 4, 'SECONDHAND', '商品已下架', '您发布的商品【123】已下架', 107, 'SECONDHAND', '2026-03-01 01:33:29');
INSERT INTO `notification` (`id`, `user_id`, `type`, `title`, `content`, `related_id`, `related_type`, `create_time`) VALUES (24, 4, 'DORMITORY_REPAIR', '新的宿舍报修请求', '学生公寓3栋-305 室提交了报修申请', 4, 'DORMITORY_REPAIR', '2026-03-01 01:34:06');
INSERT INTO `notification` (`id`, `user_id`, `type`, `title`, `content`, `related_id`, `related_type`, `create_time`) VALUES (25, 4, 'SECONDHAND', '商品已售出', '您发布的商品【考研高等数学参考书】已标记为已售出', 103, 'SECONDHAND', '2026-03-01 11:55:58');
INSERT INTO `notification` (`id`, `user_id`, `type`, `title`, `content`, `related_id`, `related_type`, `create_time`) VALUES (26, 4, 'DORMITORY_REPAIR', '新的宿舍报修请求', '学生公寓3栋-305 室提交了报修申请', 5, 'DORMITORY_REPAIR', '2026-03-01 11:56:46');
INSERT INTO `notification` (`id`, `user_id`, `type`, `title`, `content`, `related_id`, `related_type`, `create_time`) VALUES (27, 4, 'SECONDHAND', '商品审核被拒绝', '您发布的商品【晾衣绳节省空间】未通过审核，原因：测试', 55, 'SECONDHAND', '2026-03-01 12:00:13');
INSERT INTO `notification` (`id`, `user_id`, `type`, `title`, `content`, `related_id`, `related_type`, `create_time`) VALUES (28, 4, 'DORMITORY_REPAIR', '新的宿舍报修请求', '学生公寓3栋-305 室提交了报修申请', 6, 'DORMITORY_REPAIR', '2026-03-07 13:13:34');
INSERT INTO `notification` (`id`, `user_id`, `type`, `title`, `content`, `related_id`, `related_type`, `create_time`) VALUES (29, 4, 'DORMITORY_REPAIR', '新的宿舍报修请求', '学生公寓3栋-305 室提交了报修申请', 7, 'DORMITORY_REPAIR', '2026-03-07 13:31:54');
INSERT INTO `notification` (`id`, `user_id`, `type`, `title`, `content`, `related_id`, `related_type`, `create_time`) VALUES (30, 4, 'DORMITORY_REPAIR', '新的宿舍报修请求', '学生公寓3栋-305 室提交了报修申请', 8, 'DORMITORY_REPAIR', '2026-03-07 13:35:34');
INSERT INTO `notification` (`id`, `user_id`, `type`, `title`, `content`, `related_id`, `related_type`, `create_time`) VALUES (31, 7, 'DORMITORY_REPAIR', '新的宿舍报修申请', 'user 提交了 学生公寓3栋-305 室的报修申请', 9, 'DORMITORY_REPAIR', '2026-03-07 13:39:58');
COMMIT;

-- ----------------------------
-- Table structure for secondhand
-- ----------------------------
DROP TABLE IF EXISTS `secondhand`;
CREATE TABLE `secondhand` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `title` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '商品标题',
  `description` text COLLATE utf8mb4_unicode_ci COMMENT '商品描述',
  `price` decimal(10,2) NOT NULL COMMENT '商品价格',
  `category` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分类: ELECTRONICS, BOOKS, CLOTHING, SPORTS, DAILY, OTHER',
  `images` text COLLATE utf8mb4_unicode_ci,
  `condition` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '新旧程度: NEW, LIKE_NEW, GOOD, FAIR',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING' COMMENT '状态: PENDING-待审核, APPROVED-已上架, REJECTED-审核拒绝, REMOVED-已下架, SOLD-已售出',
  `reject_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '拒绝原因',
  `remove_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '下架原因',
  `handler_id` bigint DEFAULT NULL COMMENT '处理人ID',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `seller_id` bigint NOT NULL COMMENT '卖家ID',
  `view_count` int DEFAULT '0' COMMENT '浏览次数',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '软删除标记',
  `deleted_by` bigint DEFAULT NULL COMMENT '删除操作者',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间',
  `delete_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '删除原因',
  PRIMARY KEY (`id`),
  KEY `idx_seller_id` (`seller_id`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `secondhand_ibfk_1` FOREIGN KEY (`seller_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=108 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='二手商品表';

-- ----------------------------
-- Records of secondhand
-- ----------------------------
BEGIN;
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (7, '洗衣液搬家急出', '洗衣液搬家急出低价出，全新，先到先得。急出可优惠', 148.28, 'DAILY', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031747209_7_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031748620_7_1.jpg\"]', '八成新', 'APPROVED', NULL, NULL, 8, '2026-02-28 19:44:17', 2, 438, '2026-02-06 07:55:53', '2026-02-28 21:22:32', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (8, '小电锅宿舍好物', '自用小电锅宿舍好物转让，八成新，诚心出的来。价格可谈', 294.96, 'DAILY', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031750888_8_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031751678_8_1.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031752474_8_2.jpg\"]', '八成新', 'APPROVED', NULL, NULL, NULL, NULL, 2, 5, '2026-02-08 10:50:31', '2026-02-28 19:47:06', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (9, '乒乓球专业级', '因毕业/搬家/换新，转让自用乒乓球专业级，八成新，功能完好无损坏。有意者私聊详询', 295.39, 'OTHER', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031754793_9_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031755580_9_1.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031756518_9_2.jpg\"]', '八成新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 298, '2026-02-20 04:05:46', '2026-03-01 01:29:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (10, '大学物理相关书籍', '大学物理相关书籍闲置出，九成新，不议价谢谢。有意者私聊详询', 97.84, 'BOOKS', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031757427_10_0.jpg\"]', '七成新', 'APPROVED', NULL, NULL, NULL, NULL, 2, 203, '2026-02-14 07:50:04', '2026-02-28 19:47:06', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (11, '《深度学习》几乎全新', '《深度学习》几乎全新低价出，九成新，先到先得。可面交验货', 40.79, 'BOOKS', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031758312_11_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031759050_11_1.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031760083_11_2.jpg\"]', '全新', 'APPROVED', NULL, NULL, NULL, NULL, 2, 187, '2026-02-14 00:57:10', '2026-02-28 19:47:06', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (12, '羽毛球拍专业级', '闲置转让羽毛球拍专业级，全新，价格可小刀。不包邮', 229.73, 'OTHER', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031760968_12_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031762389_12_1.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031763333_12_2.jpg\"]', '九成新', 'APPROVED', NULL, NULL, NULL, NULL, 2, 234, '2026-02-14 14:39:22', '2026-02-28 19:47:06', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (13, '线性代数专业书籍转让', '闲置转让线性代数专业书籍转让，七成新，价格可小刀。功能正常', 42.81, 'BOOKS', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031764182_13_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031765049_13_1.jpg\"]', '八成新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 334, '2026-02-10 02:08:13', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (14, '折叠椅小户型适用', '折叠椅小户型适用闲置出，全新，不议价谢谢。急出可优惠', 171.21, 'DAILY', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031765819_14_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031768180_14_1.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031769222_14_2.jpg\"]', '九成新', 'APPROVED', NULL, NULL, NULL, NULL, 2, 408, '2026-02-12 09:46:12', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (15, '充电宝带配件', '闲置转让充电宝带配件，七成新，价格可小刀。顺丰到付', 1431.05, 'DIGITAL', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031770031_15_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031770981_15_1.jpg\"]', '八成新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 195, '2026-02-13 10:04:34', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (16, '泳帽运动装备', '闲置转让泳帽运动装备，全新，价格可小刀。支持校内交易', 431.84, 'OTHER', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031771897_16_0.jpg\"]', '全新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 117, '2026-01-26 15:09:52', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (17, '收纳挂袋收纳神器', '收纳挂袋收纳神器闲置出，全新，不议价谢谢。不包邮', 163.61, 'DAILY', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031772659_17_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031773534_17_1.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031774443_17_2.jpg\"]', '八成新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 319, '2026-01-28 13:01:48', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (18, '自用运动毛巾', '转让自用运动毛巾，全新，科大西区可面交。质量保证', 336.40, 'OTHER', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031775396_18_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031776342_18_1.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031778215_18_2.jpg\"]', '八成新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 170, '2026-02-22 01:07:52', '2026-03-01 01:29:38', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (19, '羽毛球拍带包', '羽毛球拍带包闲置，全新，可小刀。支持校内交易', 142.41, 'OTHER', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031779063_19_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031781558_19_1.jpg\"]', '八成新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 185, '2026-01-27 13:11:42', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (20, '九成新《大学物理》教材', '九成新《大学物理》教材急出，七成新，有意私聊。可面交验货', 61.54, 'BOOKS', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031784118_20_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031784991_20_1.jpg\"]', '八成新', 'APPROVED', NULL, NULL, NULL, NULL, 2, 274, '2026-02-06 16:53:26', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (21, '《大学物理》几乎全新', '毕业清仓《大学物理》几乎全新，七成新，数量有限。质量保证', 89.28, 'BOOKS', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031785923_21_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031788046_21_1.jpg\"]', '九成新', 'APPROVED', NULL, NULL, NULL, NULL, 2, 137, '2026-02-15 13:53:48', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (22, '拖把搬家急出', '拖把搬家急出急出，全新，有意私聊。支持校内交易', 160.60, 'DAILY', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031789117_22_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031790064_22_1.jpg\"]', '七成新', 'APPROVED', NULL, NULL, NULL, NULL, 2, 53, '2026-01-26 20:40:02', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (23, '水杯自用转让', '水杯自用转让低价出，全新，先到先得。功能正常', 111.35, 'DAILY', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031790942_23_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031793212_23_1.jpg\"]', '全新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 216, '2026-02-07 15:13:52', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (24, '足球专业级', '足球专业级闲置，八成新，可小刀。可面交验货', 165.23, 'OTHER', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031794422_24_0.jpg\"]', '八成新', 'REMOVED', NULL, '测试', 8, '2026-02-28 21:06:34', 2, 81, '2026-02-22 18:20:50', '2026-02-28 21:06:34', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (25, '正版《机器学习》', '正版《机器学习》低价出，七成新，先到先得。急出可优惠', 17.46, 'BOOKS', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031795361_25_0.jpg\"]', '九成新', 'APPROVED', NULL, NULL, NULL, NULL, 2, 258, '2026-01-31 08:32:08', '2026-03-07 20:39:19', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (26, '考研大学物理参考书', '转让考研大学物理参考书，八成新，科大西区可面交。无任何问题', 43.78, 'BOOKS', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031796276_26_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031797084_26_1.jpg\"]', '九成新', 'APPROVED', NULL, NULL, NULL, NULL, 2, 266, '2026-02-07 03:03:20', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (27, '转让《数据库系统概念》课本', '闲置转让转让《数据库系统概念》课本，八成新，价格可小刀。不包邮', 93.75, 'BOOKS', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031797889_27_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031798750_27_1.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031799558_27_2.jpg\"]', '七成新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 352, '2026-01-30 02:29:29', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (28, '转让拖鞋闲置', '转让转让拖鞋闲置，九成新，科大西区可面交。顺丰到付', 33.78, 'DAILY', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031800362_28_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031801137_28_1.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031802198_28_2.jpg\"]', '八成新', 'APPROVED', NULL, NULL, NULL, NULL, 2, 296, '2026-02-11 01:07:59', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (29, '床边挂篮宿舍好物', '床边挂篮宿舍好物闲置，八成新，可小刀。急出可优惠', 52.02, 'DAILY', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031802990_29_0.jpg\"]', '全新', 'APPROVED', NULL, NULL, NULL, NULL, 2, 432, '2026-02-04 13:25:27', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (30, '正版《计算机网络》', '毕业清仓正版《计算机网络》，八成新，数量有限。西区宿舍可自提', 69.76, 'BOOKS', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031804523_30_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031805441_30_1.jpg\"]', '八成新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 17, '2026-02-10 07:54:02', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (31, '护膝适合新手', '护膝适合新手闲置，七成新，可小刀。有意者私聊详询', 217.61, 'OTHER', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031806162_31_0.jpg\"]', '九成新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 145, '2026-02-05 19:54:49', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (32, '移动硬盘功能完好', '自用移动硬盘功能完好转让，九成新，诚心出的来。可面交验货', 1402.86, 'DIGITAL', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031806950_32_0.jpg\"]', '七成新', 'APPROVED', NULL, NULL, NULL, NULL, 2, 162, '2026-02-06 04:26:06', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (33, '拖鞋九成新', '拖鞋九成新闲置，八成新，可小刀。支持校内交易', 40.30, 'DAILY', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031807707_33_0.jpg\"]', '七成新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 485, '2026-01-27 21:54:48', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (34, '大学物理相关书籍', '大学物理相关书籍闲置，八成新，可小刀。诚心出的来', 22.81, 'BOOKS', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031808562_34_0.jpg\"]', '全新', 'APPROVED', NULL, NULL, NULL, NULL, 2, 50, '2026-01-30 18:16:14', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (35, '科大Python编程课程教材', '自用科大Python编程课程教材转让，全新，诚心出的来。可面交验货', 28.58, 'BOOKS', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031809371_35_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031810263_35_1.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031811100_35_2.jpg\"]', '八成新', 'APPROVED', NULL, NULL, NULL, NULL, 2, 345, '2026-01-30 14:43:54', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (36, '操作系统专业书籍转让', '毕业清仓操作系统专业书籍转让，八成新，数量有限。诚心出的来', 95.13, 'BOOKS', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031811996_36_0.jpg\"]', '八成新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 121, '2026-02-13 12:10:57', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (37, '转让转接头自用', '毕业清仓转让转接头自用，全新，数量有限。急出可优惠', 1022.04, 'DIGITAL', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031812858_37_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031813671_37_1.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031814608_37_2.jpg\"]', '八成新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 17, '2026-02-04 06:35:57', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (38, '手机支架急出', '闲置转让手机支架急出，九成新，价格可小刀。不包邮', 260.59, 'DIGITAL', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031815444_38_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031816342_38_1.jpg\"]', '八成新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 386, '2026-02-10 13:51:35', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (39, '路由器无划痕', '因毕业/搬家/换新，转让自用路由器无划痕，八成新，功能完好无损坏。无任何问题', 326.05, 'DIGITAL', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031817193_39_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031818112_39_1.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031818932_39_2.jpg\"]', '七成新', 'APPROVED', NULL, NULL, NULL, NULL, 2, 140, '2026-02-10 02:30:17', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (40, '镜子九成新', '镜子九成新闲置，七成新，可小刀。不包邮', 57.47, 'DAILY', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031819804_40_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031820758_40_1.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031821625_40_2.jpg\"]', '八成新', 'SOLD', NULL, NULL, NULL, NULL, 4, 43, '2026-02-22 07:36:34', '2026-02-28 21:18:44', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (41, 'Java核心技术考试用书', '自用Java核心技术考试用书转让，全新，诚心出的来。西区宿舍可自提', 16.82, 'BOOKS', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031822490_41_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031823240_41_1.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031824224_41_2.jpg\"]', '七成新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 221, '2026-01-29 10:01:39', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (42, '瑜伽垫适合新手', '因毕业/搬家/换新，转让自用瑜伽垫适合新手，七成新，功能完好无损坏。质量保证', 180.95, 'OTHER', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031825199_42_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031826033_42_1.jpg\"]', '全新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 105, '2026-02-11 14:13:39', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (43, '转让蓝牙音箱自用', '因毕业/搬家/换新，转让自用转让蓝牙音箱自用，全新，功能完好无损坏。有意者私聊详询', 80.83, 'DIGITAL', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031826894_43_0.jpg\"]', '九成新', 'APPROVED', NULL, NULL, 8, '2026-02-28 20:53:50', 2, 14, '2026-02-24 11:30:26', '2026-03-01 01:27:40', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (45, '插排便宜出', '自用插排便宜出转让，八成新，诚心出的来。无任何问题', 184.22, 'DAILY', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031830462_45_0.jpg\"]', '九成新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 390, '2026-02-18 04:16:47', '2026-03-07 17:13:12', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (46, '转让工具箱闲置', '转让工具箱闲置闲置，八成新，可小刀。急出可优惠', 164.11, 'DAILY', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031831295_46_0.jpg\"]', '八成新', 'REMOVED', NULL, '', NULL, NULL, 4, 107, '2026-02-20 00:58:16', '2026-02-28 21:39:39', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (47, '操作系统相关书籍', '操作系统相关书籍低价出，八成新，先到先得。顺丰到付', 78.79, 'BOOKS', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031832104_47_0.jpg\"]', '全新', 'REJECTED', '测试\n', NULL, 8, '2026-02-28 21:40:12', 4, 17, '2026-02-22 09:01:08', '2026-02-28 21:40:12', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (48, '床边挂篮寝室必备', '毕业清仓床边挂篮寝室必备，九成新，数量有限。质量保证', 270.86, 'DAILY', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031833111_48_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031833872_48_1.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031834753_48_2.jpg\"]', '七成新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 444, '2026-02-10 20:20:36', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (49, '计算机组成原理专业书籍转让', '闲置转让计算机组成原理专业书籍转让，七成新，价格可小刀。诚心出的来', 53.57, 'BOOKS', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031835617_49_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031836456_49_1.jpg\"]', '八成新', 'APPROVED', NULL, NULL, NULL, NULL, 2, 445, '2026-01-26 00:09:26', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (50, '九成新《大学物理》教材', '九成新《大学物理》教材急出，九成新，有意私聊。无任何问题', 15.10, 'BOOKS', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031837259_50_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031838016_50_1.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031838935_50_2.jpg\"]', '九成新', 'APPROVED', NULL, NULL, NULL, NULL, 2, 296, '2026-02-17 07:54:49', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (51, '小电锅节省空间', '转让小电锅节省空间，八成新，科大西区可面交。无任何问题', 294.11, 'DAILY', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031839866_51_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031840796_51_1.jpg\"]', '八成新', 'APPROVED', NULL, NULL, NULL, NULL, 2, 276, '2026-01-31 21:52:34', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (52, '小风扇低价出', '自用小风扇低价出转让，九成新，诚心出的来。西区宿舍可自提', 145.49, 'DAILY', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031841629_52_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031842338_52_1.jpg\"]', '九成新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 361, '2026-02-15 17:34:45', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (53, 'Java核心技术相关书籍', '转让Java核心技术相关书籍，七成新，科大西区可面交。质量保证', 63.56, 'BOOKS', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031843194_53_0.jpg\"]', '九成新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 223, '2026-02-03 06:28:18', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (54, '正版《机器学习》', '正版《机器学习》闲置出，七成新，不议价谢谢。可面交验货', 78.27, 'BOOKS', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031844061_54_0.jpg\"]', '七成新', 'APPROVED', NULL, '测试', 8, '2026-02-28 19:54:39', 2, 223, '2026-02-04 00:09:52', '2026-02-28 19:55:18', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (55, '晾衣绳节省空间', '因毕业/搬家/换新，转让自用晾衣绳节省空间，七成新，功能完好无损坏。支持校内交易', 291.46, 'DAILY', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031844851_55_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031845715_55_1.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031846582_55_2.jpg\"]', '全新', 'REJECTED', '测试', NULL, 8, '2026-03-01 12:00:12', 4, 392, '2026-02-23 08:07:43', '2026-03-01 12:00:21', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (56, '正版《计算机网络》', '正版《计算机网络》闲置出，八成新，不议价谢谢。急出可优惠', 39.15, 'BOOKS', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031847540_56_0.jpg\"]', '七成新', 'APPROVED', NULL, NULL, NULL, NULL, 2, 198, '2026-01-31 03:27:52', '2026-03-07 17:13:07', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (57, '蚊帐收纳神器', '蚊帐收纳神器低价出，八成新，先到先得。无任何问题', 120.26, 'DAILY', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031848350_57_0.jpg\"]', '全新', 'APPROVED', NULL, NULL, NULL, NULL, 2, 313, '2026-02-17 21:56:07', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (58, '闹钟搬家急出', '闹钟搬家急出急出，全新，有意私聊。诚心出的来', 50.25, 'DAILY', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031849191_58_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031850093_58_1.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031851041_58_2.jpg\"]', '全新', 'APPROVED', NULL, NULL, NULL, NULL, 2, 137, '2026-02-17 13:36:21', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (59, '寝室扫把转让', '寝室扫把转让急出，七成新，有意私聊。急出可优惠', 172.45, 'DAILY', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031851994_59_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031852896_59_1.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031853709_59_2.jpg\"]', '九成新', 'APPROVED', NULL, NULL, NULL, NULL, 2, 412, '2026-02-10 10:19:54', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (60, '护膝尺码可调', '护膝尺码可调急出，七成新，有意私聊。无任何问题', 188.23, 'OTHER', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031854478_60_0.jpg\"]', '八成新', 'APPROVED', NULL, NULL, NULL, NULL, 2, 492, '2026-02-17 09:56:55', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (61, '转接头带配件', '转接头带配件闲置，七成新，可小刀。无任何问题', 1222.34, 'DIGITAL', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031855420_61_0.jpg\"]', '七成新', 'APPROVED', NULL, NULL, NULL, NULL, 2, 390, '2026-02-11 23:02:28', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (62, '护腕尺码可调', '护腕尺码可调低价出，八成新，先到先得。支持校内交易', 74.64, 'OTHER', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031856270_62_0.jpg\"]', '全新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 461, '2026-01-26 05:43:04', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (63, '操作系统考试用书', '操作系统考试用书急出，九成新，有意私聊。诚心出的来', 43.26, 'BOOKS', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031857162_63_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031857975_63_1.jpg\"]', '九成新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 115, '2026-02-05 21:24:25', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (64, '自用运动水壶', '自用自用运动水壶转让，九成新，诚心出的来。功能正常', 242.56, 'OTHER', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031858865_64_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031859636_64_1.jpg\"]', '八成新', 'APPROVED', NULL, NULL, NULL, NULL, 2, 386, '2026-02-03 23:23:05', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (65, '概率论相关书籍', '毕业清仓概率论相关书籍，九成新，数量有限。价格可谈', 33.42, 'BOOKS', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031860402_65_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031861326_65_1.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031862184_65_2.jpg\"]', '九成新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 174, '2026-01-28 21:55:43', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (66, '台灯带配件', '转让台灯带配件，全新，科大西区可面交。质量保证', 905.46, 'DIGITAL', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031862976_66_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031863879_66_1.jpg\"]', '八成新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 447, '2026-02-10 16:08:13', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (67, '考研数据结构参考书', '考研数据结构参考书闲置出，全新，不议价谢谢。支持校内交易', 69.44, 'BOOKS', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031864660_67_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031865437_67_1.jpg\"]', '七成新', 'APPROVED', NULL, NULL, NULL, NULL, 2, 267, '2026-02-24 09:28:17', '2026-03-01 01:29:49', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (69, '机器学习考试用书', '机器学习考试用书低价出，全新，先到先得。西区宿舍可自提', 51.75, 'BOOKS', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031868895_69_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031869738_69_1.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031870499_69_2.jpg\"]', '八成新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 306, '2026-02-03 15:09:57', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (70, '运动背包几乎没用过', '运动背包几乎没用过闲置，八成新，可小刀。急出可优惠', 446.67, 'OTHER', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031871375_70_0.jpg\"]', '七成新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 197, '2026-02-02 15:03:37', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (71, '衣柜分隔板生活必备', '衣柜分隔板生活必备低价出，七成新，先到先得。诚心出的来', 189.80, 'DAILY', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031872241_71_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031873132_71_1.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031873958_71_2.jpg\"]', '八成新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 432, '2026-02-06 07:59:44', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (72, '转让运动手环九成新', '转让运动手环九成新急出，全新，有意私聊。西区宿舍可自提', 94.29, 'OTHER', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031874805_72_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031875694_72_1.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031876374_72_2.jpg\"]', '九成新', 'APPROVED', NULL, NULL, NULL, NULL, 2, 184, '2026-02-02 10:50:13', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (73, '乒乓球拍适合科大操场', '转让乒乓球拍适合科大操场，八成新，科大西区可面交。无任何问题', 362.87, 'OTHER', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031877175_73_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031877882_73_1.jpg\"]', '八成新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 85, '2026-02-10 02:13:19', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (74, '寝室针线包转让', '自用寝室针线包转让转让，七成新，非诚勿扰。功能正常', 99.06, 'DAILY', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031878698_74_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031879515_74_1.jpg\"]', '全新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 379, '2026-02-06 20:31:50', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (75, '泳帽适合科大操场', '自用泳帽适合科大操场转让，八成新，非诚勿扰。顺丰到付', 386.39, 'OTHER', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031880420_75_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031881221_75_1.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031881992_75_2.jpg\"]', '全新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 471, '2026-02-17 20:57:43', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (76, '科大C++ Primer课程教材', '转让科大C++ Primer课程教材，九成新，科大西区可面交。质量保证', 10.28, 'BOOKS', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031882703_76_0.jpg\"]', '七成新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 378, '2026-02-16 06:04:29', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (77, '拖把搬家急出', '自用拖把搬家急出转让，九成新，非诚勿扰。无任何问题', 146.85, 'DAILY', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031884698_77_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031885574_77_1.jpg\"]', '七成新', 'REMOVED', NULL, '测试', 8, '2026-02-28 21:06:30', 2, 474, '2026-02-23 01:22:06', '2026-02-28 21:06:30', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (78, '加湿器小户型适用', '加湿器小户型适用闲置，九成新，可小刀。顺丰到付', 171.72, 'DAILY', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031886385_78_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031887158_78_1.jpg\"]', '九成新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 171, '2026-02-08 04:33:20', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (79, '运动毛巾尺码可调', '运动毛巾尺码可调闲置出，七成新，不议价谢谢。功能正常', 86.50, 'OTHER', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031888027_79_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031888801_79_1.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031889647_79_2.jpg\"]', '七成新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 384, '2026-02-02 18:02:28', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (80, '床边挂篮节省空间', '转让床边挂篮节省空间，九成新，科大西区可面交。无任何问题', 298.29, 'DAILY', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031890608_80_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031891589_80_1.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031892608_80_2.jpg\"]', '七成新', 'APPROVED', NULL, NULL, NULL, NULL, 2, 210, '2026-02-16 08:23:16', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (81, '科大西区小米耳机转让', '毕业清仓科大西区小米耳机转让，八成新，数量有限。急出可优惠', 1552.06, 'DIGITAL', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031893486_81_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031894306_81_1.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031895075_81_2.jpg\"]', '八成新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 51, '2026-02-04 07:28:09', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (82, '机器学习相关书籍', '闲置转让机器学习相关书籍，九成新，价格可小刀。可面交验货', 84.65, 'BOOKS', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031895836_82_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031896614_82_1.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031897513_82_2.jpg\"]', '七成新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 232, '2026-02-19 23:50:25', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (83, '网球拍适合科大操场', '网球拍适合科大操场急出，全新，有意私聊。顺丰到付', 93.86, 'OTHER', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031898399_83_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031899174_83_1.jpg\"]', '全新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 250, '2026-02-16 19:14:30', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (84, '转让跑步腰包九成新', '转让转让跑步腰包九成新，全新，科大西区可面交。不包邮', 363.01, 'OTHER', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031899957_84_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031900906_84_1.jpg\"]', '七成新', 'APPROVED', NULL, NULL, NULL, NULL, 2, 284, '2026-01-29 18:02:21', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (85, '充电宝无划痕', '自用充电宝无划痕转让，九成新，诚心出的来。支持校内交易', 1577.16, 'DIGITAL', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031901774_85_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031902628_85_1.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031903415_85_2.jpg\"]', '八成新', 'APPROVED', NULL, NULL, NULL, NULL, 2, 8, '2026-01-28 12:53:39', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (86, '收纳挂袋宿舍神器', '毕业清仓收纳挂袋宿舍神器，九成新，数量有限。不包邮', 167.09, 'DAILY', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031904336_86_0.jpg\"]', '九成新', 'APPROVED', NULL, NULL, NULL, NULL, 2, 320, '2026-01-26 21:18:10', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (87, '羽毛球尺码可调', '转让羽毛球尺码可调，八成新，科大西区可面交。无任何问题', 29.30, 'OTHER', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031905163_87_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031906009_87_1.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031906809_87_2.jpg\"]', '九成新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 321, '2026-01-29 04:56:19', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (88, '小风扇自用转让', '自用小风扇自用转让转让，七成新，诚心出的来。可面交验货', 164.45, 'DAILY', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031907663_88_0.jpg\"]', '七成新', 'APPROVED', NULL, NULL, NULL, NULL, 2, 61, '2026-01-25 18:30:33', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (89, '线性代数学习资料', '自用线性代数学习资料转让，七成新，非诚勿扰。不包邮', 85.80, 'BOOKS', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031908462_89_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031909232_89_1.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031910038_89_2.jpg\"]', '七成新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 20, '2026-02-15 09:25:18', '2026-02-28 21:22:28', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (90, '羽毛球运动装备', '羽毛球运动装备低价出，八成新，先到先得。功能正常', 354.73, 'OTHER', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031910918_90_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031911759_90_1.jpg\"]', '全新', 'APPROVED', NULL, NULL, NULL, NULL, 2, 101, '2026-02-02 16:16:23', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (91, '拖鞋实用款', '拖鞋实用款急出，七成新，有意私聊。不包邮', 54.00, 'DAILY', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031912563_91_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031913396_91_1.jpg\"]', '九成新', 'APPROVED', NULL, NULL, NULL, NULL, 2, 267, '2026-02-02 11:46:26', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (92, '运动毛巾尺码可调', '运动毛巾尺码可调闲置，全新，可小刀。可面交验货', 349.28, 'OTHER', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031914161_92_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031914929_92_1.jpg\"]', '八成新', 'APPROVED', NULL, NULL, NULL, NULL, 2, 242, '2026-02-22 14:25:54', '2026-03-07 17:13:18', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (93, '寝室针线包转让', '闲置转让寝室针线包转让，七成新，价格可小刀。西区宿舍可自提', 152.66, 'DAILY', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031915685_93_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031917175_93_1.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031917890_93_2.jpg\"]', '全新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 98, '2026-01-30 12:48:50', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (94, '化妆品收纳盒节省空间', '化妆品收纳盒节省空间闲置出，七成新，不议价谢谢。价格可谈', 263.88, 'DAILY', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031918739_94_0.jpg\"]', '八成新', 'APPROVED', NULL, NULL, NULL, NULL, 2, 195, '2026-02-18 13:51:03', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (96, '哑铃几乎没用过', '自用哑铃几乎没用过转让，九成新，诚心出的来。支持校内交易', 361.24, 'OTHER', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031920389_96_0.jpg\"]', '九成新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 299, '2026-01-29 09:16:14', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (97, '九成新台灯', '毕业清仓九成新台灯，八成新，数量有限。功能正常', 237.75, 'DIGITAL', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031922088_97_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031922870_97_1.jpg\"]', '七成新', 'APPROVED', NULL, NULL, NULL, NULL, 2, 493, '2026-02-02 14:03:34', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (98, '书立节省空间', '闲置转让书立节省空间，全新，价格可小刀。支持校内交易', 277.86, 'DAILY', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031923658_98_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031924434_98_1.jpg\"]', '全新', 'APPROVED', NULL, NULL, NULL, NULL, 2, 137, '2026-02-06 05:31:58', '2026-02-28 21:22:38', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (99, 'iPad保护壳闲置出售', 'iPad保护壳闲置出售闲置出，全新，不议价谢谢。急出可优惠', 642.15, 'DIGITAL', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031925301_99_0.jpg\"]', '八成新', 'APPROVED', NULL, NULL, NULL, NULL, 2, 293, '2026-02-02 01:53:45', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (100, '自用小米耳机低价出', '自用小米耳机低价出急出，九成新，有意私聊。无任何问题', 1248.23, 'DIGITAL', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031926161_100_0.jpg\"]', '七成新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 494, '2026-02-14 19:58:12', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (101, '手机支架保修中', '手机支架保修中急出，全新，有意私聊。价格可谈', 165.72, 'DIGITAL', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031927131_101_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031927990_101_1.jpg\"]', '八成新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 140, '2026-02-24 11:47:44', '2026-03-01 11:55:15', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (102, '门后挂钩节省空间', '门后挂钩节省空间急出，七成新，有意私聊。功能正常', 81.79, 'DAILY', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031928805_102_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031929859_102_1.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031930706_102_2.jpg\"]', '七成新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 332, '2026-02-05 16:37:44', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (103, '考研高等数学参考书', '自用考研高等数学参考书转让，七成新，诚心出的来。功能正常', 82.56, 'BOOKS', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031931464_103_0.jpg\"]', '全新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 252, '2026-02-22 01:16:55', '2026-03-01 11:56:08', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (104, '网线功能完好', '自用网线功能完好转让，七成新，非诚勿扰。诚心出的来', 1825.01, 'DIGITAL', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031932324_104_0.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031933121_104_1.jpg\",\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031933887_104_2.jpg\"]', '九成新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 345, '2026-02-18 07:00:58', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (105, '《离散数学》几乎全新', '《离散数学》几乎全新低价出，八成新，先到先得。不包邮', 42.85, 'BOOKS', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031934658_105_0.jpg\"]', '七成新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 8, '2026-02-19 22:40:34', '2026-03-07 17:13:15', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (106, '科大设计模式课程教材', '闲置转让科大设计模式课程教材，九成新，价格可小刀。顺丰到付', 14.38, 'BOOKS', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/secondhand/1772031935593_106_0.jpg\"]', '全新', 'APPROVED', NULL, NULL, NULL, NULL, 4, 359, '2026-02-07 18:14:24', '2026-02-28 14:03:05', 0, NULL, NULL, NULL);
INSERT INTO `secondhand` (`id`, `title`, `description`, `price`, `category`, `images`, `condition`, `status`, `reject_reason`, `remove_reason`, `handler_id`, `handle_time`, `seller_id`, `view_count`, `create_time`, `update_time`, `deleted`, `deleted_by`, `deleted_at`, `delete_reason`) VALUES (107, '123', '1444', 440.00, 'DIGITAL', '[\"https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/2026/03/ce8e662d-0a38-4a13-8da4-a8be2a079019.jpg\"]', 'LIKE_NEW', 'PENDING', NULL, NULL, NULL, NULL, 4, 1, '2026-03-01 01:32:50', '2026-03-01 11:59:58', 0, NULL, NULL, NULL);
COMMIT;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码',
  `real_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '真实姓名',
  `student_id` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '学号',
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
  `role` enum('ADMIN','DORM_MANAGER','USER','STUDENT','TEACHER') COLLATE utf8mb4_unicode_ci DEFAULT 'USER',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'ACTIVE' COMMENT '账号状态: ACTIVE-正常, DISABLED-禁用',
  `avatar` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像URL',
  `bio` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '个人简介',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `username` (`username`),
  KEY `idx_username` (`username`),
  KEY `idx_role` (`role`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ----------------------------
-- Records of user
-- ----------------------------
BEGIN;
INSERT INTO `user` (`user_id`, `username`, `password`, `real_name`, `student_id`, `phone`, `email`, `role`, `status`, `avatar`, `bio`, `create_time`) VALUES (2, 'testuser123', '$2a$10$xdAXqjttTT3zvd7M5Nh82.tVRnfWC71YbhG4q617ugUE3HJdLzZDO', '测试用户', 'SA25225843', '14414242453', '244@qq.com', 'TEACHER', 'ACTIVE', 'https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/2026/02/c1fec3b5-7f22-4598-890e-3f61db0a8abd.jpg', 'hello~', '2026-02-23 12:00:54');
INSERT INTO `user` (`user_id`, `username`, `password`, `real_name`, `student_id`, `phone`, `email`, `role`, `status`, `avatar`, `bio`, `create_time`) VALUES (4, 'user', '$2a$10$06t1GC1rpFXn9UA.JeRsoOyB2LGRC7GAVpRQJqu6tb.WLoDB63TRW', 'user', 'SA242424222', '15375234456', '1244@qq.com', 'STUDENT', 'ACTIVE', 'https://caspar-java-tlias-pratice.oss-cn-beijing.aliyuncs.com/2026/02/b6243ee5-e530-4620-a7c7-649d8591e19b.jpg', '你好～', '2026-02-25 01:53:43');
INSERT INTO `user` (`user_id`, `username`, `password`, `real_name`, `student_id`, `phone`, `email`, `role`, `status`, `avatar`, `bio`, `create_time`) VALUES (6, 'testuser', '$2a$10$TCVdShxT2oKUnz5uxcDMxe00QjFF8jSzaft0BukdkwAcSHNlCH7cm', '测试用户', NULL, '13900000001', 'test@test.com', 'TEACHER', 'ACTIVE', NULL, NULL, '2026-02-28 13:13:04');
INSERT INTO `user` (`user_id`, `username`, `password`, `real_name`, `student_id`, `phone`, `email`, `role`, `status`, `avatar`, `bio`, `create_time`) VALUES (7, 'dorm_manager', '$2a$10$f27geq12CHCoEdK.mCUDUO0GgxncE2XJb1gWUQPODNcwwEvkw3T62', '宿舍管理员', NULL, '13800138001', 'dorm@campus.com', 'DORM_MANAGER', 'ACTIVE', NULL, NULL, '2026-02-28 13:13:38');
INSERT INTO `user` (`user_id`, `username`, `password`, `real_name`, `student_id`, `phone`, `email`, `role`, `status`, `avatar`, `bio`, `create_time`) VALUES (8, 'root', '$2a$10$eXO863PQu1NgvyUh3Gn1GehvonE4nzTeIQJQWG1EiUBTuXeJ6ivfa', '管理员', NULL, '15345674567', 'root@campus.com', 'ADMIN', 'ACTIVE', NULL, NULL, '2026-02-28 16:51:23');
INSERT INTO `user` (`user_id`, `username`, `password`, `real_name`, `student_id`, `phone`, `email`, `role`, `status`, `avatar`, `bio`, `create_time`) VALUES (9, 'test', '$2a$10$KWZe7Kr0Y5kQa9Z5/bMOQO9ZZFrIhWnE5dko1i1QpZYUgBtYVn93S', 'test', NULL, '13443242423', '333@qq.com', 'STUDENT', 'DISABLED', NULL, NULL, '2026-03-01 11:54:27');
COMMIT;

-- ----------------------------
-- Table structure for user_dormitory
-- ----------------------------
DROP TABLE IF EXISTS `user_dormitory`;
CREATE TABLE `user_dormitory` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `campus` varchar(50) DEFAULT NULL COMMENT '校区',
  `building` varchar(50) DEFAULT NULL COMMENT '楼栋号',
  `room` varchar(20) DEFAULT NULL COMMENT '房间号',
  `bed` varchar(10) DEFAULT NULL COMMENT '床位号',
  `check_in_date` date DEFAULT NULL COMMENT '入住日期',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户宿舍信息表';

-- ----------------------------
-- Records of user_dormitory
-- ----------------------------
BEGIN;
INSERT INTO `user_dormitory` (`id`, `user_id`, `campus`, `building`, `room`, `bed`, `check_in_date`, `create_time`, `update_time`) VALUES (1, 4, '东校区', '学生公寓3栋', '305', '4', '2025-09-01', '2026-02-27 13:59:24', '2026-03-07 01:05:43');
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
