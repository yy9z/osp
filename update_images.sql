
USE campus_platform;

-- 更新二手商品图片 (表名: secondhand)
UPDATE secondhand SET images = '["/images/1187DCBFE140762FFF15F652C58F6DA7.jpg"]' WHERE id % 5 = 0;
UPDATE secondhand SET images = '["/images/4E43F7783C8DF194017C88514B83C51F.jpg"]' WHERE id % 5 = 1;
UPDATE secondhand SET images = '["/images/ACDEF5A41854278A7D5BB14A5B96DEF2.jpg"]' WHERE id % 5 = 2;
UPDATE secondhand SET images = '["/images/IMG_0014.jpg"]' WHERE id % 5 = 3;
UPDATE secondhand SET images = '["/images/building-bg.jpg"]' WHERE id % 5 = 4;

-- 更新失物招领图片 (表名: lostfound)
UPDATE lostfound SET images = '["/images/campus-bg.jpg"]' WHERE id % 5 = 0;
UPDATE lostfound SET images = '["/images/campus-bg1.jpg"]' WHERE id % 5 = 1;
UPDATE lostfound SET images = '["/images/library-bg.jpg"]' WHERE id % 5 = 2;
UPDATE lostfound SET images = '["/images/placeholder.png"]' WHERE id % 5 = 3;
UPDATE lostfound SET images = '["/images/campus-map.jpg"]' WHERE id % 5 = 4;
