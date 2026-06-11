package com.caspar.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Agent 对话轮次记录。
 */
@Data
public class AgentLogRecord {
    private Long id;
    private String sessionId;
    private Long userId;
    private Integer turn;
    private String userInput;
    private String intent;
    private String slots;
    private String toolsUsed;
    private String responsePayload;
    private String reply;
    private Boolean success;
    private String errorMsg;
    private Integer costMs;
    private LocalDateTime createdAt;
}
