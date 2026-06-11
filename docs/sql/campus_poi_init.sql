-- =====================================================
-- 校园POI空间数据表初始化脚本
-- 使用MySQL空间数据类型POINT
-- =====================================================

-- 启用空间扩展
SET NAMES utf8mb4;

-- 删除旧表（如需要重新初始化）
-- DROP TABLE IF EXISTS campus_poi;

-- 创建校园POI表（使用空间数据类型）
CREATE TABLE IF NOT EXISTS `campus_poi` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(100) NOT NULL COMMENT '地点名称',
  `category` VARCHAR(50) NOT NULL COMMENT '分类：TEACHING,DINING,DORMITORY,LIBRARY,SPORTS,ADMIN,SCENIC,ENTRANCE',
  `description` TEXT COMMENT '地点描述',
  `latitude` DECIMAL(10,7) NOT NULL COMMENT '纬度(WGS-84)',
  `longitude` DECIMAL(10,7) NOT NULL COMMENT '经度(WGS-84)',
  `floor` VARCHAR(50) COMMENT '楼层信息',
  `image_url` VARCHAR(500) COMMENT '图片URL',
  `open_time` VARCHAR(50) COMMENT '开放时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_category (`category`),
  INDEX idx_name (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='校园POI地点表';

-- 插入中科大校园POI数据（原始WGS-84坐标）
-- 注意：实际项目中建议使用GCJ-02坐标（已偏移）
-- 这里使用近似的中科大坐标
INSERT INTO campus_poi (name, category, description, latitude, longitude, floor, open_time) VALUES
-- 中心区域
('图书馆', 'LIBRARY', '校图书馆，藏书丰富，自习空间充足', 31.8382, 117.2158, '1-5层', '07:00-22:00'),
('樱花广场', 'SCENIC', '校园赏樱胜地，每年春季吸引众多游客', 31.8376, 117.2156, NULL, '全天'),
('人工湖', 'SCENIC', '校园人工湖，环境优美，是晨读好去处', 31.8388, 117.2168, NULL, '全天'),

-- 教学楼
('西区教学楼', 'TEACHING', '西区主要教学楼，包含多个多媒体教室', 31.8392, 117.2142, '1-6层', '07:00-22:00'),
('东区教学楼', 'TEACHING', '东区教学楼，包含实验室和研讨室', 31.8368, 117.2182, '1-5层', '07:00-22:00'),
('特种楼', 'TEACHING', '特种材料实验室楼', 31.8378, 117.2175, '1-8层', '07:00-22:00'),

-- 食堂
('学生食堂', 'DINING', '主要学生食堂，提供多种菜系', 31.8385, 117.2168, '1-3层', '06:30-21:00'),
('教工食堂', 'DINING', '教职员工专用食堂', 31.8378, 117.2148, '1-2层', '06:30-20:00'),
('星座餐厅', 'DINING', '特色餐厅', 31.8395, 117.2155, '1-2层', '07:00-21:00'),

-- 宿舍
('研究生宿舍楼', 'DORMITORY', '硕士研究生宿舍楼', 31.8402, 117.2165, '1-6层', '全天'),
('博士生楼', 'DORMITORY', '博士研究生及博士后公寓', 31.8398, 117.2178, '1-8层', '全天'),
('研究生公寓', 'DORMITORY', '研究生公寓', 31.8375, 117.2138, '1-6层', '全天'),

-- 运动场
('体育馆', 'SPORTS', '室内体育馆，包含篮球场、羽毛球馆等', 31.8362, 117.2172, '1-2层', '06:00-21:00'),
('田径场', 'SPORTS', '标准400米田径场', 31.8358, 117.2178, '室外', '全天'),
('网球场', 'SPORTS', '室外网球场', 31.8365, 117.2138, '室外', '06:00-21:00'),

-- 行政楼
('行政楼', 'ADMIN', '学校行政办公主楼', 31.8388, 117.2148, '1-10层', '08:00-17:30'),

-- 校门
('东门', 'ENTRANCE', '校园东侧主入口', 31.8382, 117.2210, NULL, '全天'),
('西门', 'ENTRANCE', '校园西侧入口', 31.8382, 117.2105, NULL, '全天'),
('南门', 'ENTRANCE', '校园南侧入口', 31.8352, 117.2158, NULL, '全天'),
('北门', 'ENTRANCE', '校园北侧入口', 31.8412, 117.2158, NULL, '全天');

-- 查询验证
SELECT id, name, category, latitude, longitude FROM campus_poi ORDER BY category;
