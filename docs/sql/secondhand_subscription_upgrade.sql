-- 二手求购订阅能力升级脚本
-- 执行库：campus_platform

USE campus_platform;

CREATE TABLE IF NOT EXISTS secondhand_subscription (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '订阅用户ID',
    keyword VARCHAR(80) NOT NULL COMMENT '订阅关键词',
    category VARCHAR(20) NULL COMMENT '可选分类',
    max_price DECIMAL(10,2) NULL COMMENT '预算上限',
    campus VARCHAR(50) NULL COMMENT '校区偏好',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/INACTIVE',
    last_notified_at DATETIME NULL COMMENT '最近提醒时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_sub_user (user_id),
    KEY idx_sub_status (status),
    KEY idx_sub_category (category),
    KEY idx_sub_keyword (keyword)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='二手求购订阅表';

