-- =====================================================
-- 中科大校园POI经纬度精确修正脚本
-- 基于高德地图API精确查询的中科大各校区地标
-- =====================================================

SET NAMES utf8mb4;

-- ==================== ADMIN 行政 ====================
-- 行政楼 - 东校区行政中心，估算值
UPDATE campus_poi SET latitude = 31.836500, longitude = 117.267500 WHERE name = '行政楼';
-- 校医院 - 高德精确查询
UPDATE campus_poi SET latitude = 31.833407, longitude = 117.266039 WHERE name = '校医院';

-- ==================== DINING 餐饮 ====================
-- 东区学生食堂 - 高德精确查询(东区东苑学生餐厅)
UPDATE campus_poi SET latitude = 31.837071, longitude = 117.270600 WHERE name = '东区学生食堂';
-- 西区活动中心 - 高德精确查询(西校区学生活动中心)
UPDATE campus_poi SET latitude = 31.839546, longitude = 117.253492 WHERE name = '西区活动中心';

-- ==================== ENTRANCE 校门 ====================
-- 北门(黄山路) - 东校区北门，估算值
UPDATE campus_poi SET latitude = 31.841000, longitude = 117.270000 WHERE name = '北门(黄山路)';
-- 西大门(金寨路) - 东校区西门，金寨路
UPDATE campus_poi SET latitude = 31.836854, longitude = 117.269603 WHERE name = '西大门(金寨路)';
-- 北正门(黄山路) - 西校区北门，黄山路
UPDATE campus_poi SET latitude = 31.838945, longitude = 117.257303 WHERE name = '北正门(黄山路)';
-- 中区北门(黄山路) - 中校区北门
UPDATE campus_poi SET latitude = 31.833015, longitude = 117.271542 WHERE name = '中区北门(黄山路)';
-- 南校区-东正门
UPDATE campus_poi SET latitude = 31.822151, longitude = 117.284343 WHERE name = '南校区-东正门';

-- ==================== LIBRARY 图书馆 ====================
-- 老图书馆(红楼) - 东校区图书馆
UPDATE campus_poi SET latitude = 31.837000, longitude = 117.269232 WHERE name = '老图书馆(红楼)';
-- 西区图书馆 - 高德精确查询
UPDATE campus_poi SET latitude = 31.839202, longitude = 117.257402 WHERE name = '西区图书馆';
-- 高新园区-图文中心
UPDATE campus_poi SET latitude = 31.823530, longitude = 117.128720 WHERE name = '高新园区-图文中心';

-- ==================== SCENIC 景点 ====================
-- 郭沫若广场 - 高德精确查询
UPDATE campus_poi SET latitude = 31.838218, longitude = 117.269174 WHERE name = '郭沫若广场';
-- 一丹人工湖 - 西区黄山路附近
UPDATE campus_poi SET latitude = 31.838762, longitude = 117.263031 WHERE name = '一丹人工湖';
-- 现代艺术中心 - 高德精确查询(中国科大艺术教学中心)
UPDATE campus_poi SET latitude = 31.838362, longitude = 117.261029 WHERE name = '现代艺术中心';

-- ==================== SPORTS 运动 ====================
-- 综合体育馆 - 高德精确查询(中校区综合体育中心)
UPDATE campus_poi SET latitude = 31.839225, longitude = 117.262875 WHERE name = '综合体育馆';
-- 东校区操场 - 高德精确查询
UPDATE campus_poi SET latitude = 31.836469, longitude = 117.266933 WHERE name = '东校区操场';
-- 西区操场 - 西区内估算
UPDATE campus_poi SET latitude = 31.839000, longitude = 117.255000 WHERE name = '西区操场';

-- ==================== TEACHING 教学 ====================
-- 第一教学楼 - 东校区第一教学楼
UPDATE campus_poi SET latitude = 31.838500, longitude = 117.269000 WHERE name = '第一教学楼';
-- 第三教学楼(三教) - 西校区
UPDATE campus_poi SET latitude = 31.838500, longitude = 117.253000 WHERE name = '第三教学楼(三教)';

-- 验证更新结果
SELECT id, name, category, latitude, longitude FROM campus_poi ORDER BY category, id;
