-- ============================================================
-- 校园导航模块数据库升级脚本
-- 中国科学技术大学（USTC）校园
-- 中心坐标：纬度 31.8384, 经度 117.2167
-- ============================================================

-- 启用MySQL空间扩展
SET NAMES utf8mb4;

-- ============================================================
-- 1. 创建校园POI表（使用POINT空间数据类型）
-- ============================================================
DROP TABLE IF EXISTS campus_poi;

CREATE TABLE campus_poi (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'POI ID',
    name VARCHAR(50) NOT NULL COMMENT '地点名称',
    category VARCHAR(20) NOT NULL COMMENT '类别: TEACHING,DINING,LIBRARY,DORMITORY,SPORTS,ADMIN,SCENIC,ENTRANCE,ROAD',
    description VARCHAR(200) COMMENT '地点描述',
    latitude DECIMAL(10,7) NOT NULL COMMENT '纬度',
    longitude DECIMAL(10,7) NOT NULL COMMENT '经度',
    location POINT NOT NULL SRID 4326 COMMENT '空间位置',
    floor VARCHAR(50) COMMENT '楼层信息',
    image_url VARCHAR(200) COMMENT '图片URL',
    open_time VARCHAR(100) COMMENT '开放时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    SPATIAL INDEX idx_location (location)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='校园POI地点表';

-- ============================================================
-- 2. 插入中科大校园POI数据
-- 中心坐标：纬度 31.8384, 经度 117.2167
-- ============================================================

-- 教学楼（TEACHING）
INSERT INTO campus_poi (name, category, description, latitude, longitude, location, floor, open_time) VALUES
('西区教学楼', 'TEACHING', '西区主要教学楼，包含多个多媒体教室', 31.8395, 117.2155, ST_GeomFromText('POINT(117.2155 31.8395)', 4326), '1-6层', '07:00-22:00'),
('东区教学楼', 'TEACHING', '东区教学楼，包含实验室和研讨室', 31.8372, 117.2185, ST_GeomFromText('POINT(117.2185 31.8372)', 4326), '1-5层', '07:00-22:00');

-- 食堂（DINING）
INSERT INTO campus_poi (name, category, description, latitude, longitude, location, floor, open_time) VALUES
('学生食堂', 'DINING', '主要学生食堂，提供多种菜系', 31.8388, 117.2172, ST_GeomFromText('POINT(117.2172 31.8388)', 4326), '1-3层', '06:30-21:00'),
('教工食堂', 'DINING', '教职员工专用食堂', 31.8378, 117.2158, ST_GeomFromText('POINT(117.2158 31.8378)', 4326), '1-2层', '06:30-20:00');

-- 宿舍（DORMITORY）
INSERT INTO campus_poi (name, category, description, latitude, longitude, location, floor, open_time) VALUES
('研究生宿舍楼', 'DORMITORY', '硕士研究生宿舍楼', 31.8402, 117.2168, ST_GeomFromText('POINT(117.2168 31.8402)', 4326), '1-6层', '全天'),
('博士生楼', 'DORMITORY', '博士研究生及博士后公寓', 31.8398, 117.2178, ST_GeomFromText('POINT(117.2178 31.8398)', 4326), '1-8层', '全天');

-- 图书馆（LIBRARY）
INSERT INTO campus_poi (name, category, description, latitude, longitude, location, floor, open_time) VALUES
('图书馆', 'LIBRARY', '校图书馆，藏书丰富，提供自习空间', 31.8384, 117.2167, ST_GeomFromText('POINT(117.2167 31.8384)', 4326), '1-5层', '07:00-22:00');

-- 运动场（SPORTS）
INSERT INTO campus_poi (name, category, description, latitude, longitude, location, floor, open_time) VALUES
('体育馆', 'SPORTS', '室内体育馆，包含篮球场、羽毛球馆等', 31.8368, 117.2175, ST_GeomFromText('POINT(117.2175 31.8368)', 4326), '1-2层', '06:00-21:00'),
('田径场', 'SPORTS', '标准400米田径场', 31.8362, 117.2182, ST_GeomFromText('POINT(117.2182 31.8362)', 4326), '室外', '全天');

-- 行政楼（ADMIN）
INSERT INTO campus_poi (name, category, description, latitude, longitude, location, floor, open_time) VALUES
('行政楼', 'ADMIN', '学校行政办公主楼', 31.8392, 117.2158, ST_GeomFromText('POINT(117.2158 31.8392)', 4326), '1-10层', '08:00-17:30');

-- 景点（SCENIC）
INSERT INTO campus_poi (name, category, description, latitude, longitude, location, floor, open_time) VALUES
('樱花广场', 'SCENIC', '校园赏樱胜地，每年春季吸引众多游客', 31.8380, 117.2162, ST_GeomFromText('POINT(117.2162 31.8380)', 4326), NULL, '全天'),
('人工湖', 'SCENIC', '校园人工湖，环境优美，是晨读好去处', 31.8390, 117.2180, ST_GeomFromText('POINT(117.2180 31.8390)', 4326), NULL, '全天');

-- 校门（ENTRANCE）
INSERT INTO campus_poi (name, category, description, latitude, longitude, location, floor, open_time) VALUES
('东门', 'ENTRANCE', '校园东侧主入口', 31.8384, 117.2200, ST_GeomFromText('POINT(117.2200 31.8384)', 4326), NULL, '全天'),
('西门', 'ENTRANCE', '校园西侧入口', 31.8384, 117.2135, ST_GeomFromText('POINT(117.2135 31.8384)', 4326), NULL, '全天'),
('南门', 'ENTRANCE', '校园南侧入口', 31.8355, 117.2167, ST_GeomFromText('POINT(117.2167 31.8355)', 4326), NULL, '全天'),
('北门', 'ENTRANCE', '校园北侧入口', 31.8412, 117.2167, ST_GeomFromText('POINT(117.2167 31.8412)', 4326), NULL, '全天');

-- ============================================================
-- 3. 创建路径节点表
-- ============================================================
DROP TABLE IF EXISTS campus_path_edge;
DROP TABLE IF EXISTS campus_path_node;

CREATE TABLE campus_path_node (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '节点ID',
    name VARCHAR(50) NOT NULL COMMENT '节点名称',
    latitude DECIMAL(10,7) NOT NULL COMMENT '纬度',
    longitude DECIMAL(10,7) NOT NULL COMMENT '经度',
    node_type VARCHAR(20) DEFAULT 'WAYPOINT' COMMENT '节点类型: POI, INTERSECTION, WAYPOINT',
    poi_id BIGINT COMMENT '关联的POI ID（如果是POI类型）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='校园路径节点表';

-- ============================================================
-- 4. 创建路径边表（邻接表）
-- ============================================================
CREATE TABLE campus_path_edge (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '边ID',
    from_node_id BIGINT NOT NULL COMMENT '起点节点ID',
    to_node_id BIGINT NOT NULL COMMENT '终点节点ID',
    distance DOUBLE NOT NULL COMMENT '距离（米）',
    way_type VARCHAR(20) DEFAULT 'WALK' COMMENT '道路类型: WALK(步行), CYCLE(骑行)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_from_node (from_node_id),
    INDEX idx_to_node (to_node_id),
    FOREIGN KEY (from_node_id) REFERENCES campus_path_node(id),
    FOREIGN KEY (to_node_id) REFERENCES campus_path_node(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='校园路径边表';

-- ============================================================
-- 5. 插入校园路径节点数据
-- 为每个POI创建对应的路径节点
-- ============================================================

-- 插入POI对应的路径节点（与POI一一对应）
INSERT INTO campus_path_node (name, latitude, longitude, node_type, poi_id) VALUES
('西区教学楼', 31.8395, 117.2155, 'POI', 1),
('东区教学楼', 31.8372, 117.2185, 'POI', 2),
('学生食堂', 31.8388, 117.2172, 'POI', 3),
('教工食堂', 31.8378, 117.2158, 'POI', 4),
('研究生宿舍楼', 31.8402, 117.2168, 'POI', 5),
('博士生楼', 31.8398, 117.2178, 'POI', 6),
('图书馆', 31.8384, 117.2167, 'POI', 7),
('体育馆', 31.8368, 117.2175, 'POI', 8),
('田径场', 31.8362, 117.2182, 'POI', 9),
('行政楼', 31.8392, 117.2158, 'POI', 10),
('樱花广场', 31.8380, 117.2162, 'POI', 11),
('人工湖', 31.8390, 117.2180, 'POI', 12),
('东门', 31.8384, 117.2200, 'POI', 13),
('西门', 31.8384, 117.2135, 'POI', 14),
('南门', 31.8355, 117.2167, 'POI', 15),
('北门', 31.8412, 117.2167, 'POI', 16);

-- 插入交叉路口节点（INTERSECTION）
INSERT INTO campus_path_node (name, latitude, longitude, node_type) VALUES
('中心路口', 31.8384, 117.2167, 'INTERSECTION'),
('东区路口', 31.8378, 117.2180, 'INTERSECTION'),
('西区路口', 31.8390, 117.2155, 'INTERSECTION'),
('南区路口', 31.8368, 117.2170, 'INTERSECTION'),
('北区路口', 31.8405, 117.2165, 'INTERSECTION'),
('图书馆东路', 31.8382, 117.2172, 'INTERSECTION'),
('人工湖东路', 31.8388, 117.2185, 'INTERSECTION');

-- ============================================================
-- 6. 插入校园路径边数据（连接各节点）
-- 计算相邻节点之间的距离（使用简单的欧氏距离近似）
-- ============================================================

-- 从东门出发
INSERT INTO campus_path_edge (from_node_id, to_node_id, distance, way_type) VALUES
(13, 7, 330, 'WALK'),   -- 东门 -> 图书馆
(13, 12, 220, 'WALK'),  -- 东门 -> 人工湖
(13, 16, 280, 'WALK'),  -- 东门 -> 东区路口
(13, 17, 350, 'WALK');  -- 东门 -> 图书馆东路

-- 从西门出发
INSERT INTO campus_path_edge (from_node_id, to_node_id, distance, way_type) VALUES
(14, 1, 220, 'WALK'),   -- 西门 -> 西区教学楼
(14, 10, 280, 'WALK'),  -- 西门 -> 行政楼
(14, 11, 330, 'WALK'),  -- 西门 -> 樱花广场
(14, 18, 200, 'WALK'),  -- 西门 -> 西区路口
(14, 21, 400, 'WALK');  -- 西门 -> 中心路口

-- 从南门出发
INSERT INTO campus_path_edge (from_node_id, to_node_id, distance, way_type) VALUES
(15, 8, 250, 'WALK'),   -- 南门 -> 体育馆
(15, 9, 350, 'WALK'),   -- 南门 -> 田径场
(15, 2, 200, 'WALK'),   -- 南门 -> 东区教学楼
(15, 19, 180, 'WALK'),  -- 南门 -> 南区路口
(15, 17, 150, 'WALK');  -- 南门 -> 图书馆东路

-- 从北门出发
INSERT INTO campus_path_edge (from_node_id, to_node_id, distance, way_type) VALUES
(16, 5, 220, 'WALK'),   -- 北门 -> 研究生宿舍楼
(16, 6, 180, 'WALK'),   -- 北门 -> 博士生楼
(16, 20, 150, 'WALK'),  -- 北门 -> 北区路口
(16, 21, 350, 'WALK');  -- 北门 -> 中心路口

-- 从图书馆出发（中心节点）
INSERT INTO campus_path_edge (from_node_id, to_node_id, distance, way_type) VALUES
(7, 21, 30, 'WALK'),    -- 图书馆 -> 中心路口
(7, 11, 80, 'WALK'),    -- 图书馆 -> 樱花广场
(7, 3, 70, 'WALK'),     -- 图书馆 -> 学生食堂
(7, 17, 80, 'WALK'),    -- 图书馆 -> 图书馆东路
(7, 2, 230, 'WALK');    -- 图书馆 -> 东区教学楼

-- 教学楼连接
INSERT INTO campus_path_edge (from_node_id, to_node_id, distance, way_type) VALUES
(1, 18, 100, 'WALK'),   -- 西区教学楼 -> 西区路口
(1, 21, 280, 'WALK'),   -- 西区教学楼 -> 中心路口
(1, 10, 100, 'WALK'),  -- 西区教学楼 -> 行政楼
(2, 16, 200, 'WALK'),   -- 东区教学楼 -> 东区路口
(2, 17, 120, 'WALK'),   -- 东区教学楼 -> 图书馆东路
(2, 19, 280, 'WALK'),   -- 东区教学楼 -> 南区路口
(2, 3, 180, 'WALK');    -- 东区教学楼 -> 学生食堂

-- 食堂连接
INSERT INTO campus_path_edge (from_node_id, to_node_id, distance, way_type) VALUES
(3, 7, 70, 'WALK'),     -- 学生食堂 -> 图书馆
(3, 17, 100, 'WALK'),   -- 学生食堂 -> 图书馆东路
(3, 4, 160, 'WALK'),    -- 学生食堂 -> 教工食堂
(3, 22, 150, 'WALK'),   -- 学生食堂 -> 人工湖东路
(4, 10, 80, 'WALK'),    -- 教工食堂 -> 行政楼
(4, 1, 230, 'WALK');    -- 教工食堂 -> 西区教学楼

-- 宿舍连接
INSERT INTO campus_path_edge (from_node_id, to_node_id, distance, way_type) VALUES
(5, 20, 100, 'WALK'),    -- 研究生宿舍楼 -> 北区路口
(5, 6, 120, 'WALK'),     -- 研究生宿舍楼 -> 博士生楼
(5, 21, 200, 'WALK'),   -- 研究生宿舍楼 -> 中心路口
(6, 20, 200, 'WALK'),   -- 博士生楼 -> 北区路口
(6, 22, 150, 'WALK'),   -- 博士生楼 -> 人工湖东路
(6, 12, 100, 'WALK');   -- 博士生楼 -> 人工湖

-- 运动场连接
INSERT INTO campus_path_edge (from_node_id, to_node_id, distance, way_type) VALUES
(8, 19, 100, 'WALK'),   -- 体育馆 -> 南区路口
(8, 9, 120, 'WALK'),    -- 体育馆 -> 田径场
(8, 17, 150, 'WALK'),   -- 体育馆 -> 图书馆东路
(9, 19, 180, 'WALK'),   -- 田径场 -> 南区路口
(9, 22, 200, 'WALK');   -- 田径场 -> 人工湖东路

-- 行政楼连接
INSERT INTO campus_path_edge (from_node_id, to_node_id, distance, way_type) VALUES
(10, 1, 100, 'WALK'),   -- 行政楼 -> 西区教学楼
(10, 18, 150, 'WALK'),  -- 行政楼 -> 西区路口
(10, 4, 80, 'WALK');    -- 行政楼 -> 教工食堂

-- 景点连接
INSERT INTO campus_path_edge (from_node_id, to_node_id, distance, way_type) VALUES
(11, 21, 120, 'WALK'),  -- 樱花广场 -> 中心路口
(11, 7, 80, 'WALK'),    -- 樱花广场 -> 图书馆
(11, 18, 180, 'WALK'),  -- 樱花广场 -> 西区路口
(12, 6, 100, 'WALK'),   -- 人工湖 -> 博士生楼
(12, 22, 80, 'WALK'),   -- 人工湖 -> 人工湖东路
(12, 13, 220, 'WALK');  -- 人工湖 -> 东门

-- 交叉路口连接
INSERT INTO campus_path_edge (from_node_id, to_node_id, distance, way_type) VALUES
(21, 18, 200, 'WALK'),  -- 中心路口 -> 西区路口
(21, 16, 250, 'WALK'),  -- 中心路口 -> 东区路口
(21, 7, 30, 'WALK'),    -- 中心路口 -> 图书馆
(21, 11, 120, 'WALK'), -- 中心路口 -> 樱花广场
(16, 17, 150, 'WALK'), -- 东区路口 -> 图书馆东路
(17, 22, 100, 'WALK'), -- 图书馆东路 -> 人工湖东路
(19, 17, 150, 'WALK'), -- 南区路口 -> 图书馆东路
(19, 22, 180, 'WALK'); -- 南区路口 -> 人工湖东路

-- 添加反向边（双向路径）
INSERT INTO campus_path_edge (from_node_id, to_node_id, distance, way_type)
SELECT to_node_id, from_node_id, distance, way_type
FROM campus_path_edge;

-- ============================================================
-- 验证数据
-- ============================================================
SELECT 'POI数量' AS info, COUNT(*) AS count FROM campus_poi
UNION ALL
SELECT '路径节点数量', COUNT(*) FROM campus_path_node
UNION ALL
SELECT '路径边数量', COUNT(*) FROM campus_path_edge;

-- 测试空间查询
-- SELECT id, name, ST_Distance_Sphere(location, ST_GeomFromText('POINT(117.2167 31.8384)', 4326)) AS distance_meters
-- FROM campus_poi
-- ORDER BY distance_meters
-- LIMIT 5;
