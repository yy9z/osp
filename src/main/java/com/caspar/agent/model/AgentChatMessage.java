package com.caspar.agent.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Agent 历史消息。
 */
@Data
@Builder
public class AgentChatMessage {
    private String role;
    private String content;
    private List<AgentCard> cards;
    private LocalDateTime timestamp;
    private boolean confirmationRequired;
    private String confirmationId;
    private Map<String, Object> confirmationPreview;
}
