
USE campus_platform;

-- ============================================================
-- 1. 二手商品图片更新 (使用 Unsplash 真实图片)
-- ============================================================

-- 书籍类 (分科细化)
UPDATE secondhand SET images = '["https://images.unsplash.com/photo-1532012197267-da84d127e765?w=800"]' 
WHERE (title LIKE '%书%' OR title LIKE '%教材%') AND id % 4 = 0;

UPDATE secondhand SET images = '["https://images.unsplash.com/photo-1589829085413-56de8ae18c73?w=800"]' 
WHERE (title LIKE '%书%' OR title LIKE '%教材%') AND id % 4 = 1;

UPDATE secondhand SET images = '["https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=800"]' 
WHERE (title LIKE '%书%' OR title LIKE '%教材%') AND id % 4 = 2;

UPDATE secondhand SET images = '["https://images.unsplash.com/photo-1512820790803-83ca734da794?w=800"]' 
WHERE (title LIKE '%书%' OR title LIKE '%教材%') AND id % 4 = 3;

-- 特定书籍封面模拟
-- 深度学习/花书
UPDATE secondhand SET images = '["https://images.unsplash.com/photo-1620712943543-bcc4688e7485?w=800"]' 
WHERE title LIKE '%深度学习%' OR title LIKE '%花书%';

-- 计算机/编程
UPDATE secondhand SET images = '["https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=800"]' 
WHERE title LIKE '%代码%' OR title LIKE '%Java%' OR title LIKE '%C++%' OR title LIKE '%Python%';

-- 电子数码类
-- 手机
UPDATE secondhand SET images = '["https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=800"]' 
WHERE title LIKE '%手机%' AND id % 2 = 0;
UPDATE secondhand SET images = '["https://images.unsplash.com/photo-1598327105666-5b89351aff23?w=800"]' 
WHERE title LIKE '%手机%' AND id % 2 = 1;

-- 耳机
UPDATE secondhand SET images = '["https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800"]' 
WHERE title LIKE '%耳机%' AND id % 2 = 0;
UPDATE secondhand SET images = '["https://images.unsplash.com/photo-1546435770-a3757475acda?w=800"]' 
WHERE title LIKE '%耳机%' AND id % 2 = 1;

-- 电脑/iPad
UPDATE secondhand SET images = '["https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=800"]' 
WHERE title LIKE '%电脑%' OR title LIKE '%本%' OR title LIKE '%iPad%';

-- 运动器材
-- 羽毛球
UPDATE secondhand SET images = '["https://images.unsplash.com/photo-1626224583764-847890e0b3b0?w=800"]' 
WHERE title LIKE '%羽毛球%';

-- 篮球/足球
UPDATE secondhand SET images = '["https://images.unsplash.com/photo-1519861531473-920026393112?w=800"]' 
WHERE title LIKE '%篮%' OR title LIKE '%球%';

-- 生活用品
-- 台灯
UPDATE secondhand SET images = '["https://images.unsplash.com/photo-1507473888900-52e1adad5481?w=800"]' 
WHERE title LIKE '%灯%';

-- 收纳/其他
UPDATE secondhand SET images = '["https://images.unsplash.com/photo-1584622650111-993a426fbf0a?w=800"]' 
WHERE title LIKE '%杯%' OR title LIKE '%壶%';

UPDATE secondhand SET images = '["https://images.unsplash.com/photo-1595341888016-a392ef81b7de?w=800"]' 
WHERE title LIKE '%鞋%';

-- ============================================================
-- 2. 失物招领图片更新 (更真实的场景图)
-- ============================================================

-- 黑色双肩包
UPDATE lostfound SET images = '["https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=800"]' 
WHERE title LIKE '%双肩包%';

-- AirPods
UPDATE lostfound SET images = '["https://images.unsplash.com/photo-1588423771073-b8903fbb85b5?w=800"]' 
WHERE title LIKE '%AirPods%';

-- 校园卡 (用 generic card image)
UPDATE lostfound SET images = '["https://images.unsplash.com/photo-1621905251189-08b45d6a269e?w=800"]' 
WHERE title LIKE '%校园卡%';

-- 雨伞
UPDATE lostfound SET images = '["https://images.unsplash.com/photo-1590402494682-cd3fb53b1f70?w=800"]' 
WHERE title LIKE '%雨伞%';

-- 高数课本
UPDATE lostfound SET images = '["https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=800"]' 
WHERE title LIKE '%高数%';

-- 钥匙
UPDATE lostfound SET images = '["https://images.unsplash.com/photo-1582139329536-e7284fece509?w=800"]' 
WHERE title LIKE '%钥匙%';

-- 保温杯
UPDATE lostfound SET images = '["https://images.unsplash.com/photo-1584622650111-993a426fbf0a?w=800"]' 
WHERE title LIKE '%保温杯%';

-- 鼠标
UPDATE lostfound SET images = '["https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?w=800"]' 
WHERE title LIKE '%鼠标%';
