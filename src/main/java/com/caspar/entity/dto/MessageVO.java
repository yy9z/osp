package com.caspar.entity.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 消息详情VO
 */
@Data
public class MessageVO {
    /**
     * 消息ID
     */
    private Long id;

    /**
     * 发送者ID
     */
    private Long senderId;

    /**
     * 发送者用户名
     */
    private String senderUsername;

    /**
     * 发送者真实姓名
     */
    private String senderRealName;

    /**
     * 发送者头像
     */
    private String senderAvatar;

    /**
     * 接收者ID
     */
    private Long receiverId;

    /**
     * 接收者用户名
     */
    private String receiverUsername;

    /**
     * 接收者真实姓名
     */
    private String receiverRealName;

    /**
     * 接收者头像
     */
    private String receiverAvatar;

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
}
