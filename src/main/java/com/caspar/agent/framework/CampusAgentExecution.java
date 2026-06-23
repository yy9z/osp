package com.caspar.agent.framework;

import com.caspar.agent.model.AgentSession;

import java.io.Serializable;

/**
 * Immutable request-scoped input passed through the Agent graph.
 */
public record CampusAgentExecution(
        AgentSession session,
        Long userId,
        String sessionId,
        String userInput,
        int currentTurn,
        long startTime
) implements Serializable {
}
