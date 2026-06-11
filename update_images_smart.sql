
USE campus_platform;

-- 1. 智能匹配二手商品图片
UPDATE secondhand
SET images = CASE
    -- 书籍类
    WHEN title LIKE '%书%' OR title LIKE '%教材%' OR title LIKE '%考研%' OR title LIKE '%课本%' OR title LIKE '%习题%' 
      OR title LIKE '%Python%' OR title LIKE '%Java%' OR title LIKE '%C++%' OR title LIKE '%学习%' 
      OR title LIKE '%数学%' OR title LIKE '%物理%' OR title LIKE '%代数%' OR title LIKE '%概率%'
      THEN '["https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=800"]'
    
    -- 运动器材类
    WHEN title LIKE '%羽毛球%' OR title LIKE '%乒乓%' OR title LIKE '%网球%' OR title LIKE '%拍%'
      THEN '["https://images.unsplash.com/photo-1626224583764-847890e0b3b0?w=800"]'
    WHEN title LIKE '%球%' OR title LIKE '%足%' OR title LIKE '%篮%'
      THEN '["https://images.unsplash.com/photo-1543351611-58f69d7c1781?w=800"]'
    WHEN title LIKE '%瑜伽%' OR title LIKE '%垫%' OR title LIKE '%哑铃%' OR title LIKE '%运动%' OR title LIKE '%护%'
      THEN '["https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?w=800"]'
      
    -- 电子数码类
    WHEN title LIKE '%电脑%' OR title LIKE '%本%' OR title LIKE '%硬盘%' OR title LIKE '%显卡%'
      THEN '["https://images.unsplash.com/photo-1593642632823-8f78536788c6?w=800"]'
    WHEN title LIKE '%手机%' OR title LIKE '%iPhone%' OR title LIKE '%华为%' OR title LIKE '%小米%'
      THEN '["https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=800"]'
    WHEN title LIKE '%耳机%' OR title LIKE '%音箱%' OR title LIKE '%蓝牙%'
      THEN '["https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800"]'
    WHEN title LIKE '%充电%' OR title LIKE '%线%' OR title LIKE '%头%' OR title LIKE '%路由%' OR title LIKE '%插%'
      THEN '["https://images.unsplash.com/photo-1606229365485-93a3b8ee0385?w=800"]'
    WHEN title LIKE '%iPad%' OR title LIKE '%平板%'
      THEN '["https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=800"]'
      
    -- 生活用品类
    WHEN title LIKE '%鞋%' OR title LIKE '%靴%'
      THEN '["https://images.unsplash.com/photo-1595341888016-a392ef81b7de?w=800"]'
    WHEN title LIKE '%包%' OR title LIKE '%袋%'
      THEN '["https://images.unsplash.com/photo-1599058945522-28d584b6f0ff?w=800"]'
    WHEN title LIKE '%水壶%' OR title LIKE '%杯%'
      THEN '["https://images.unsplash.com/photo-1584622650111-993a426fbf0a?w=800"]'
    WHEN title LIKE '%灯%'
      THEN '["https://images.unsplash.com/photo-1507914372368-b2b003611ad2?w=800"]'
    WHEN title LIKE '%扇%' OR title LIKE '%吹%' OR title LIKE '%锅%' OR title LIKE '%加湿%' OR title LIKE '%闹钟%'
      THEN '["https://images.unsplash.com/photo-1585837262512-0682a3a0c54c?w=800"]'
    WHEN title LIKE '%椅%' OR title LIKE '%桌%' OR title LIKE '%柜%' OR title LIKE '%架%' OR title LIKE '%镜%'
      THEN '["https://images.unsplash.com/photo-1503602642458-232111445657?w=800"]'
    WHEN title LIKE '%衣%' OR title LIKE '%裤%' OR title LIKE '%裙%' OR title LIKE '%帽%'
      THEN '["https://images.unsplash.com/photo-1523381210434-271e8be1f52b?w=800"]'
      
    -- 默认兜底图
    ELSE '["https://images.unsplash.com/photo-1513161455079-7dc1de15ef3e?w=800"]'
END;

-- 2. 填充失物招领数据（因为之前是空的）
-- 先清空（如果有少量测试数据）
DELETE FROM lostfound WHERE id < 100;

INSERT INTO lostfound (type, title, description, category, images, location, status, publisher_id, create_time) VALUES
('LOST', '丢失黑色双肩包', '在图书馆二楼自习室丢失一个黑色耐克双肩包，内有电脑和雨伞', 'OTHER', '["https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=800"]', '图书馆二楼', 'OPEN', 6, NOW()),
('LOST', 'AirPods Pro 充电仓', '中午在第一食堂吃饭时不慎遗落', 'ELECTRONICS', '["https://images.unsplash.com/photo-1588423771073-b8903fbb85b5?w=800"]', '第一食堂', 'OPEN', 6, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
('FOUND', '捡到一张校园卡', '在操场看台捡到张三同学的校园卡', 'OTHER', '["https://images.unsplash.com/photo-1621905251189-08b45d6a269e?w=800"]', '东区操场', 'OPEN', 6, DATE_SUB(NOW(), INTERVAL 5 HOUR)),
('FOUND', '黑色雨伞一把', '西区教学楼3A101教室捡到', 'OTHER', '["https://images.unsplash.com/photo-1590402494682-cd3fb53b1f70?w=800"]', '西区教学楼', 'OPEN', 6, DATE_SUB(NOW(), INTERVAL 1 DAY)),
('LOST', '高数课本', '可能落在3C102教室了，书名《高等数学》上册', 'BOOKS', '["https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=800"]', '3C102', 'OPEN', 6, DATE_SUB(NOW(), INTERVAL 1 DAY)),
('FOUND', '钥匙一串', '挂着皮卡丘挂件，在行政楼门口捡到', 'KEYS', '["https://images.unsplash.com/photo-1582139329536-e7284fece509?w=800"]', '行政楼门口', 'OPEN', 6, DATE_SUB(NOW(), INTERVAL 2 DAY)),
('LOST', '白色保温杯', '膳魔师牌子，上面有贴纸', 'OTHER', '["https://images.unsplash.com/photo-1584622650111-993a426fbf0a?w=800"]', '篮球场', 'OPEN', 6, DATE_SUB(NOW(), INTERVAL 3 DAY)),
('FOUND', '罗技鼠标', '无线鼠标，黑色', 'ELECTRONICS', '["https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?w=800"]', '机房', 'OPEN', 6, DATE_SUB(NOW(), INTERVAL 3 DAY));
