ALTER TABLE notification
    ADD COLUMN user_id BIGINT NULL COMMENT '接收通知的用户ID' AFTER id;

ALTER TABLE notification
    ADD INDEX idx_user_id (user_id);

ALTER TABLE notification
    ADD CONSTRAINT fk_notification_user
        FOREIGN KEY (user_id) REFERENCES user(user_id);