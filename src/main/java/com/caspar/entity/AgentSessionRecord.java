package com.caspar.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Agent 会话持久化记录。
 */
@Data
public class AgentSessionRecord {
    private String id;
    private Long userId;
    private String title;
    private Integer messageCount;
    private String lastIntent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

