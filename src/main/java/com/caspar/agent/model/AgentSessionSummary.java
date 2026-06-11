package com.caspar.agent.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Agent 历史会话摘要。
 */
@Data
public class AgentSessionSummary {
    private String sessionId;
    private String title;
    private Integer messageCount;
    private String lastIntent;
    private String lastMessage;
    private LocalDateTime updatedAt;
}

