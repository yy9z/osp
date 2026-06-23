package com.caspar.agent.registry;

import com.caspar.agent.model.ToolArgs;
import com.caspar.agent.model.ToolResult;
import com.caspar.agent.tool.AgentTool;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Exposes existing AgentTool implementations as Spring AI ToolCallbacks.
 */
@Component
@RequiredArgsConstructor
public class AgentToolCallbackProvider {

    private static final Set<String> RESERVED_ARGUMENT_KEYS =
            Set.of("userId", "user_id", "token", "authorization");

    private final AgentToolCatalog toolCatalog;
    private final ToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;

    public ToolCallback[] getToolCallbacks() {
        return buildCallbacks(toolCatalog.getDefinitions()).toArray(ToolCallback[]::new);
    }

    public List<ToolCallback> getReadOnlyToolCallbacks() {
        return buildCallbacks(toolCatalog.getReadOnlyDefinitions());
    }

    private List<ToolCallback> buildCallbacks(List<AgentToolDefinition> definitions) {
        List<ToolCallback> callbacks = new ArrayList<>(definitions.size());
        for (AgentToolDefinition definition : definitions) {
            callbacks.add(new CatalogToolCallback(definition));
        }
        return callbacks;
    }

    private final class CatalogToolCallback implements ToolCallback {

        private final AgentToolDefinition catalogDefinition;
        private final ToolDefinition springDefinition;

        private CatalogToolCallback(AgentToolDefinition catalogDefinition) {
            this.catalogDefinition = catalogDefinition;
            this.springDefinition = ToolDefinition.builder()
                    .name(catalogDefinition.name())
                    .description(catalogDefinition.description())
                    .inputSchema(buildInputSchema(catalogDefinition))
                    .build();
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return springDefinition;
        }

        @Override
        public String call(String toolInput) {
            return serialize(ToolResult.fail("工具缺少可信用户上下文，已拒绝执行"));
        }

        @Override
        public String call(String toolInput, ToolContext toolContext) {
            Long userId = extractUserId(toolContext);
            if (userId == null) {
                return call(toolInput);
            }

            AgentTool tool = toolRegistry.getTool(catalogDefinition.name());
            if (tool == null) {
                return serialize(ToolResult.fail("工具 [" + catalogDefinition.name() + "] 不存在"));
            }

            try {
                Map<String, Object> params = parseArguments(toolInput);
                RESERVED_ARGUMENT_KEYS.forEach(params::remove);
                ToolArgs args = ToolArgs.builder()
                        .toolName(catalogDefinition.name())
                        .params(params)
                        .userId(userId)
                        .build();
                return serialize(tool.execute(args));
            } catch (Exception e) {
                return serialize(ToolResult.fail("工具执行失败: " + e.getMessage()));
            }
        }
    }

    private String buildInputSchema(AgentToolDefinition definition) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.put("additionalProperties", false);

        ObjectNode properties = schema.putObject("properties");
        for (AgentToolParameter parameter : definition.parameters()) {
            ObjectNode property = properties.putObject(parameter.name());
            property.put("type", parameter.type());
            property.put("description", parameter.description());
            if ("array".equals(parameter.type())) {
                property.putObject("items").put("type", "string");
            }
        }

        ArrayNode required = schema.putArray("required");
        definition.requiredSlots().forEach(required::add);
        return schema.toString();
    }

    private Map<String, Object> parseArguments(String toolInput) throws Exception {
        if (toolInput == null || toolInput.isBlank()) {
            return new java.util.HashMap<>();
        }
        Map<String, Object> parsed = objectMapper.readValue(toolInput, new TypeReference<>() {});
        return parsed == null ? new java.util.HashMap<>() : new java.util.HashMap<>(parsed);
    }

    private Long extractUserId(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) {
            return null;
        }
        Object raw = toolContext.getContext().get("userId");
        if (raw instanceof Number number) {
            return number.longValue();
        }
        if (raw == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(raw));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String serialize(ToolResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return "{\"success\":false,\"errorMessage\":\"工具结果序列化失败\"}";
        }
    }
}
