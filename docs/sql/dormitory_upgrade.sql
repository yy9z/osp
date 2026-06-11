-- 宿舍管理模块数据库表结构

-- 1. 楼栋表
CREATE TABLE IF NOT EXISTS dorm_building (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '楼栋ID',
    name VARCHAR(50) NOT NULL COMMENT '楼栋名称',
    code VARCHAR(20) NOT NULL COMMENT '楼栋编号',
    floors INT NOT NULL DEFAULT 6 COMMENT '楼层数',
    gender VARCHAR(10) COMMENT '性别限制: MALE/FEMALE',
    manager_id BIGINT COMMENT '宿管员ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宿舍楼栋表';

-- 2. 修改宿舍表添加楼栋关联
ALTER TABLE dormitory ADD COLUMN building_id BIGINT COMMENT '楼栋ID';
ALTER TABLE dormitory ADD CONSTRAINT fk_dormitory_building FOREIGN KEY (building_id) REFERENCES dorm_building(id);

-- 3. 修改报修表添加新字段
ALTER TABLE dormitory_repair ADD COLUMN urgency VARCHAR(20) DEFAULT 'low' COMMENT '紧急程度: high/medium/low' AFTER images;
ALTER TABLE dormitory_repair ADD COLUMN handle_images TEXT COMMENT '处理图片JSON数组' AFTER remark;
ALTER TABLE dormitory_repair ADD COLUMN handler_id BIGINT COMMENT '处理人ID' AFTER handle_images;

-- 4. 插入测试楼栋数据
INSERT INTO dorm_building (name, code, floors, gender) VALUES
('1号楼', 'A1', 6, 'MALE'),
('2号楼', 'A2', 6, 'MALE'),
('3号楼', 'B1', 6, 'FEMALE'),
('4号楼', 'B2', 6, 'FEMALE'),
('5号楼', 'C1', 4, 'MALE');

-- 5. 更新现有宿舍数据的楼栋关联
UPDATE dormitory d SET building_id = (SELECT id FROM dorm_building WHERE code = 'A1' LIMIT 1) WHERE building = 'A栋' OR building LIKE '%A%';
UPDATE dormitory d SET building_id = (SELECT id FROM dorm_building WHERE code = 'B1' LIMIT 1) WHERE building = 'B栋' OR building LIKE '%B%';

-- 6. 为报修表添加索引
CREATE INDEX idx_repair_status ON dormitory_repair(status);
CREATE INDEX idx_repair_urgency ON dormitory_repair(urgency);
CREATE INDEX idx_repair_handler ON dormitory_repair(handler_id);
