package com.caspar.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ConversationSetting {
    private Long id;
    private Long userId;
    private Long otherUserId;
    private Integer isPinned;
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
