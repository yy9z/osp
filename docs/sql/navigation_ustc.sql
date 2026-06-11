-- ============================================================
-- 校园导航模块升级 - 中科大校园数据
-- ============================================================

-- 1. 创建POI表（不使用空间索引，简化处理）
DROP TABLE IF EXISTS campus_poi;
CREATE TABLE campus_poi (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL COMMENT '地点名称',
    category VARCHAR(20) NOT NULL COMMENT '类别: TEACHING,DINING,LIBRARY,DORMITORY,SPORTS,ADMIN,SCENIC,ENTRANCE',
    description VARCHAR(200),
    latitude DECIMAL(10,7) NOT NULL,
    longitude DECIMAL(10,7) NOT NULL,
    floor VARCHAR(50),
    image_url VARCHAR(200),
    open_time VARCHAR(100),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category (category),
    INDEX idx_location (latitude, longitude)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 插入中科大校园POI数据（中心：31.8384, 117.2167）
INSERT INTO campus_poi (name, category, description, latitude, longitude, floor, open_time) VALUES
-- 教学楼
('西区教学楼', 'TEACHING', '西区主要教学楼，包含多个多媒体教室', 31.8395, 117.2155, '1-6层', '07:00-22:00'),
('东区教学楼', 'TEACHING', '东区教学楼，包含实验室和研讨室', 31.8372, 117.2185, '1-5层', '07:00-22:00'),
-- 食堂
('学生食堂', 'DINING', '主要学生食堂，提供多种菜系', 31.8388, 117.2172, '1-3层', '06:30-21:00'),
('教工食堂', 'DINING', '教职员工专用食堂', 31.8378, 117.2158, '1-2层', '06:30-20:00'),
-- 宿舍
('研究生宿舍楼', 'DORMITORY', '硕士研究生宿舍楼', 31.8402, 117.2168, '1-6层', '全天'),
('博士生楼', 'DORMITORY', '博士研究生及博士后公寓', 31.8398, 117.2178, '1-8层', '全天'),
-- 图书馆
('图书馆', 'LIBRARY', '校图书馆，藏书丰富，提供自习空间', 31.8384, 117.2167, '1-5层', '07:00-22:00'),
-- 运动场
('体育馆', 'SPORTS', '室内体育馆，包含篮球场、羽毛球馆等', 31.8368, 117.2175, '1-2层', '06:00-21:00'),
('田径场', 'SPORTS', '标准400米田径场', 31.8362, 117.2182, '室外', '全天'),
-- 行政楼
('行政楼', 'ADMIN', '学校行政办公主楼', 31.8392, 117.2158, '1-10层', '08:00-17:30'),
-- 景点
('樱花广场', 'SCENIC', '校园赏樱胜地，每年春季吸引众多游客', 31.8380, 117.2162, NULL, '全天'),
('人工湖', 'SCENIC', '校园人工湖，环境优美，是晨读好去处', 31.8390, 117.2180, NULL, '全天'),
-- 校门
('东门', 'ENTRANCE', '校园东侧主入口', 31.8384, 117.2200, NULL, '全天'),
('西门', 'ENTRANCE', '校园西侧入口', 31.8384, 117.2135, NULL, '全天'),
('南门', 'ENTRANCE', '校园南侧入口', 31.8355, 117.2167, NULL, '全天'),
('北门', 'ENTRANCE', '校园北侧入口', 31.8412, 117.2167, NULL, '全天');

-- 3. 创建路径节点表
DROP TABLE IF EXISTS campus_path_edge;
DROP TABLE IF EXISTS campus_path_node;

CREATE TABLE campus_path_node (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    latitude DECIMAL(10,7) NOT NULL,
    longitude DECIMAL(10,7) NOT NULL,
    node_type VARCHAR(20) DEFAULT 'WAYPOINT',
    poi_id BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_location (latitude, longitude)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE campus_path_edge (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    from_node_id BIGINT NOT NULL,
    to_node_id BIGINT NOT NULL,
    distance DOUBLE NOT NULL,
    way_type VARCHAR(20) DEFAULT 'WALK',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (from_node_id) REFERENCES campus_path_node(id),
    FOREIGN KEY (to_node_id) REFERENCES campus_path_node(id),
    INDEX idx_from (from_node_id),
    INDEX idx_to (to_node_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入路径节点（关联POI）
INSERT INTO campus_path_node (name, latitude, longitude, node_type, poi_id) VALUES
('东门', 31.8384, 117.2200, 'POI', 13),
('西区教学楼', 31.8395, 117.2155, 'POI', 1),
('学生食堂', 31.8388, 117.2172, 'POI', 3),
('图书馆', 31.8384, 117.2167, 'POI', 7),
('研究生宿舍楼', 31.8402, 117.2168, 'POI', 5),
('体育馆', 31.8368, 117.2175, 'POI', 8),
('田径场', 31.8362, 117.2182, 'POI', 9),
('东区教学楼', 31.8372, 117.2185, 'POI', 2),
('教工食堂', 31.8378, 117.2158, 'POI', 4),
('行政楼', 31.8392, 117.2158, 'POI', 10),
('樱花广场', 31.8380, 117.2162, 'POI', 11),
('人工湖', 31.8390, 117.2180, 'POI', 12),
('博士生楼', 31.8398, 117.2178, 'POI', 6),
('西门', 31.8384, 117.2135, 'POI', 14),
('南门', 31.8355, 117.2167, 'POI', 15),
('北门', 31.8412, 117.2167, 'POI', 16),
-- 交叉路口节点
('交叉口1', 31.8386, 117.2180, 'INTERSECTION', NULL),
('交叉口2', 31.8386, 117.2160, 'INTERSECTION', NULL),
('交叉口3', 31.8395, 117.2170, 'INTERSECTION', NULL),
('交叉口4', 31.8375, 117.2165, 'INTERSECTION', NULL);

-- 插入路径边（连接相近的节点）
INSERT INTO campus_path_edge (from_node_id, to_node_id, distance, way_type) VALUES
-- 东门区域
(1, 4, 280, 'WALK'),
(1, 17, 180, 'WALK'),
(4, 3, 60, 'WALK'),
(4, 17, 120, 'WALK'),
(4, 11, 50, 'WALK'),
(3, 17, 80, 'WALK'),
-- 图书馆区域
(11, 18, 40, 'WALK'),
(18, 10, 35, 'WALK'),
-- 西区教学楼区域
(2, 19, 150, 'WALK'),
(2, 10, 80, 'WALK'),
(10, 18, 60, 'WALK'),
(19, 3, 100, 'WALK'),
(19, 9, 120, 'WALK'),
-- 食堂区域
(3, 9, 40, 'WALK'),
(9, 8, 80, 'WALK'),
(8, 7, 60, 'WALK'),
-- 宿舍区域
(5, 12, 130, 'WALK'),
(12, 6, 80, 'WALK'),
(6, 19, 90, 'WALK'),
-- 田径场区域
(7, 17, 60, 'WALK'),
(8, 17, 50, 'WALK'),
-- 校门连接
(14, 18, 250, 'WALK'),
(15, 4, 320, 'WALK'),
(16, 5, 120, 'WALK'),
-- 双向边
(4, 1, 280, 'WALK'),
(17, 4, 120, 'WALK'),
(3, 4, 60, 'WALK'),
(17, 3, 80, 'WALK'),
(11, 4, 50, 'WALK'),
(18, 4, 70, 'WALK'),
(18, 11, 40, 'WALK'),
(10, 18, 35, 'WALK'),
(2, 19, 150, 'WALK'),
(10, 2, 80, 'WALK'),
(18, 10, 60, 'WALK'),
(19, 2, 150, 'WALK'),
(3, 19, 100, 'WALK'),
(19, 9, 120, 'WALK'),
(9, 3, 40, 'WALK'),
(9, 8, 80, 'WALK'),
(8, 9, 80, 'WALK'),
(5, 12, 130, 'WALK'),
(12, 5, 130, 'WALK'),
(6, 12, 80, 'WALK'),
(12, 6, 80, 'WALK'),
(19, 6, 90, 'WALK'),
(6, 19, 90, 'WALK'),
(7, 17, 60, 'WALK'),
(17, 7, 60, 'WALK'),
(8, 17, 50, 'WALK'),
(17, 8, 50, 'WALK'),
(18, 14, 250, 'WALK'),
(14, 18, 250, 'WALK'),
(4, 15, 320, 'WALK'),
(15, 4, 320, 'WALK'),
(5, 16, 120, 'WALK'),
(16, 5, 120, 'WALK');

-- ============================================================
-- 4. GeoJSON数据（校园区域）
-- ============================================================
DROP TABLE IF EXISTS campus_region;
CREATE TABLE campus_region (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    type VARCHAR(20) NOT NULL,
    geojson TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入田径场区域
INSERT INTO campus_region (name, type, geojson) VALUES
('田径场区域', 'SPORTS', '{"type":"Polygon","coordinates":[[[117.2175,31.8365],[117.2190,31.8365],[117.2190,31.8360],[117.2175,31.8360],[117.2175,31.8365]]]}'),
('人工湖区域', 'SCENIC', '{"type":"Polygon","coordinates":[[[117.2175,31.8385],[117.2185,31.8385],[117.2185,31.8395],[117.2175,31.8395],[117.2175,31.8385]]]}');

SELECT '数据导入完成！' AS message;
