package com.caspar.agent.registry;

import java.util.List;

/**
 * Single source of truth for an Agent tool's planning and execution metadata.
 */
public record AgentToolDefinition(
        String intent,
        String name,
        String description,
        List<AgentToolParameter> parameters,
        List<String> requiredSlots,
        boolean readOnly
) {

    public AgentToolDefinition {
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
        requiredSlots = requiredSlots == null ? List.of() : List.copyOf(requiredSlots);
    }
}
