package com.caspar.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 消息实体类
 */
@Data
public class Message {
    /**
     * 消息ID
     */
    private Long id;

    /**
     * 发送者ID
     */
    private Long senderId;

    /**
     * 接收者ID
     */
    private Long receiverId;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 关联类型: SECONDHAND, LOSTFOUND, SYSTEM
     */
    private String relatedType;

    /**
     * 关联ID
     */
    private Long relatedId;

    /**
     * 是否已读: 0-未读, 1-已读
     */
    private Integer isRead;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    private String type;

    private String title;
}
