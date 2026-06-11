-- =====================================================
-- 中科大校园POI经纬度修正脚本
-- 基于高德地图API查询的真实坐标
-- 执行前请备份数据库
-- =====================================================

SET NAMES utf8mb4;

-- 更新东校区POI数据
-- 北门(黄山路) - 高德查询: 117.270031,31.841473
UPDATE campus_poi SET latitude = 31.841473, longitude = 117.270031 WHERE name = '北门(黄山路)';

-- 西大门(金寨路) - 高德查询中科大东校区: 117.265483,31.834480
UPDATE campus_poi SET latitude = 31.834480, longitude = 117.265483 WHERE name = '西大门(金寨路)';

-- 老图书馆(红楼) - 高德查询东校区图书馆: 117.269232,31.837000
UPDATE campus_poi SET latitude = 31.837000, longitude = 117.269232 WHERE name = '老图书馆(红楼)';

-- 第一教学楼 - 高德查询: 117.269058,31.839021
UPDATE campus_poi SET latitude = 31.839021, longitude = 117.269058 WHERE name = '第一教学楼';

-- 郭沫若广场 - 高德查询黄山路中科大: 117.263031,31.838762
UPDATE campus_poi SET latitude = 31.838762, longitude = 117.263031 WHERE name = '郭沫若广场';

-- 东区学生食堂 - 高德查询科大美食广场: 117.270654,31.836411
UPDATE campus_poi SET latitude = 31.836411, longitude = 117.270654 WHERE name = '东区学生食堂';

-- 西校区POI数据
-- 北正门(黄山路) - 保持原值，在西区内
UPDATE campus_poi SET latitude = 31.838945, longitude = 117.257303 WHERE name = '北正门(黄山路)';

-- 西区图书馆 - 高德查询: 117.257402,31.839202
UPDATE campus_poi SET latitude = 31.839202, longitude = 117.257402 WHERE name = '西区图书馆';

-- 第三教学楼(三教) - 西区内估算值
UPDATE campus_poi SET latitude = 31.838500, longitude = 117.252800 WHERE name = '第三教学楼(三教)';

-- 一丹人工湖 - 保持原值
UPDATE campus_poi SET latitude = 31.837500, longitude = 117.254000 WHERE name = '一丹人工湖';

-- 西区活动中心 - 西区内估算值
UPDATE campus_poi SET latitude = 31.837800, longitude = 117.254500 WHERE name = '西区活动中心';

-- 中校区POI数据
-- 中区北门(黄山路) - 保持原值
UPDATE campus_poi SET latitude = 31.833015, longitude = 117.271542 WHERE name = '中区北门(黄山路)';

-- 综合体育馆 - 高德查询中校区体育馆: 117.263746,31.837785
UPDATE campus_poi SET latitude = 31.837785, longitude = 117.263746 WHERE name = '综合体育馆';

-- 现代艺术中心 - 保持原值
UPDATE campus_poi SET latitude = 31.830530, longitude = 117.271010 WHERE name = '现代艺术中心';

-- 南校区和高新园区
-- 南校区-东正门 - 保持原值
UPDATE campus_poi SET latitude = 31.822151, longitude = 117.284343 WHERE name = '南校区-东正门';

-- 高新园区-图文中心 - 保持原值
UPDATE campus_poi SET latitude = 31.823530, longitude = 117.128720 WHERE name = '高新园区-图文中心';

-- 其他重要建筑
-- 行政楼 - 东校区估算
UPDATE campus_poi SET latitude = 31.836500, longitude = 117.267500 WHERE name = '行政楼';

-- 校医院 - 保持原值
UPDATE campus_poi SET latitude = 31.838100, longitude = 117.268200 WHERE name = '校医院';

-- 东校区操场 - 保持原值
UPDATE campus_poi SET latitude = 31.839500, longitude = 117.271000 WHERE name = '东校区操场';

-- 西区操场 - 保持原值
UPDATE campus_poi SET latitude = 31.839000, longitude = 117.255000 WHERE name = '西区操场';

-- 验证更新结果
SELECT id, name, category, latitude, longitude FROM campus_poi ORDER BY category, id;
