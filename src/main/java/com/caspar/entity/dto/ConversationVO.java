package com.caspar.entity.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ConversationVO {
    private Long userId;
    private String username;
    private String realName;
    private String avatar;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private Integer unreadCount;
    private Integer isPinned;
}
