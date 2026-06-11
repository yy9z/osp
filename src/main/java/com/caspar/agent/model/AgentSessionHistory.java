package com.caspar.agent.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Agent 会话历史详情。
 */
@Data
@Builder
public class AgentSessionHistory {
    private String sessionId;
    private List<AgentChatMessage> messages;
}

