package com.caspar.agent.registry;

/**
 * Model-visible tool argument metadata.
 */
public record AgentToolParameter(
        String name,
        String type,
        String description
) {
}
