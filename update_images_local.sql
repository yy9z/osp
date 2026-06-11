
USE campus_platform;

-- ============================================================
-- 1. 二手商品图片更新 (使用本地下载的真实图片)
-- ============================================================

-- 书籍类 (分科细化)
UPDATE secondhand SET images = '["/images/book.jpg"]' 
WHERE (title LIKE '%书%' OR title LIKE '%教材%') AND id % 4 = 0;

UPDATE secondhand SET images = '["/images/book.jpg"]' 
WHERE (title LIKE '%书%' OR title LIKE '%教材%') AND id % 4 = 1;

UPDATE secondhand SET images = '["/images/book.jpg"]' 
WHERE (title LIKE '%书%' OR title LIKE '%教材%') AND id % 4 = 2;

UPDATE secondhand SET images = '["/images/book.jpg"]' 
WHERE (title LIKE '%书%' OR title LIKE '%教材%') AND id % 4 = 3;

-- 特定书籍封面模拟
-- 深度学习/花书
UPDATE secondhand SET images = '["/images/book.jpg"]' 
WHERE title LIKE '%深度学习%' OR title LIKE '%花书%';

-- 计算机/编程
UPDATE secondhand SET images = '["/images/book.jpg"]' 
WHERE title LIKE '%代码%' OR title LIKE '%Java%' OR title LIKE '%C++%' OR title LIKE '%Python%';

-- 电子数码类
-- 手机
UPDATE secondhand SET images = '["/images/phone.jpg"]' 
WHERE title LIKE '%手机%';

-- 耳机
UPDATE secondhand SET images = '["/images/headphone.jpg"]' 
WHERE title LIKE '%耳机%';

-- 电脑/iPad
UPDATE secondhand SET images = '["/images/laptop.jpg"]' 
WHERE title LIKE '%电脑%' OR title LIKE '%本%' OR title LIKE '%iPad%';

-- 运动器材
-- 羽毛球
UPDATE secondhand SET images = '["/images/badminton.jpg"]' 
WHERE title LIKE '%羽毛球%';

-- 篮球/足球
UPDATE secondhand SET images = '["/images/basketball.jpg"]' 
WHERE title LIKE '%篮%' OR title LIKE '%球%';

-- 生活用品
-- 台灯
UPDATE secondhand SET images = '["/images/lamp.jpg"]' 
WHERE title LIKE '%灯%';

-- 收纳/其他
UPDATE secondhand SET images = '["/images/cup.jpg"]' 
WHERE title LIKE '%杯%' OR title LIKE '%壶%';

UPDATE secondhand SET images = '["/images/clothes.jpg"]' 
WHERE title LIKE '%鞋%';

-- ============================================================
-- 2. 失物招领图片更新 (更真实的场景图)
-- ============================================================

-- 黑色双肩包
UPDATE lostfound SET images = '["/images/bag.jpg"]' 
WHERE title LIKE '%双肩包%';

-- AirPods
UPDATE lostfound SET images = '["/images/headphone.jpg"]' 
WHERE title LIKE '%AirPods%';

-- 校园卡 (用 generic card image)
UPDATE lostfound SET images = '["/images/card.jpg"]' 
WHERE title LIKE '%校园卡%';

-- 雨伞
UPDATE lostfound SET images = '["/images/umbrella.jpg"]' 
WHERE title LIKE '%雨伞%';

-- 高数课本
UPDATE lostfound SET images = '["/images/book.jpg"]' 
WHERE title LIKE '%高数%';

-- 钥匙
UPDATE lostfound SET images = '["/images/keys.jpg"]' 
WHERE title LIKE '%钥匙%';

-- 保温杯
UPDATE lostfound SET images = '["/images/cup.jpg"]' 
WHERE title LIKE '%保温杯%';

-- 鼠标
UPDATE lostfound SET images = '["/images/mouse.jpg"]' 
WHERE title LIKE '%鼠标%';
