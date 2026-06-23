package com.caspar.agent.registry;

import com.caspar.agent.model.ToolArgs;
import com.caspar.agent.model.ToolResult;
import com.caspar.agent.tool.AgentTool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentToolCallbackProviderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void readOnlyCallbacksShouldExposeGeneratedJsonSchema() throws Exception {
        AgentToolCallbackProvider provider = newProvider(new CapturingTool());

        List<ToolCallback> callbacks = provider.getReadOnlyToolCallbacks();
        assertEquals(7, callbacks.size());
        assertFalse(callbacks.stream()
                .anyMatch(callback -> "secondhand_publish".equals(callback.getToolDefinition().name())));

        ToolCallback search = callbacks.stream()
                .filter(callback -> "secondhand_search".equals(callback.getToolDefinition().name()))
                .findFirst()
                .orElseThrow();
        JsonNode schema = objectMapper.readTree(search.getToolDefinition().inputSchema());
        assertEquals("object", schema.path("type").asText());
        assertEquals("number", schema.path("properties").path("max_price").path("type").asText());
    }

    @Test
    void callbackShouldInjectTrustedUserIdAndStripModelIdentityFields() throws Exception {
        CapturingTool tool = new CapturingTool();
        AgentToolCallbackProvider provider = newProvider(tool);
        ToolCallback callback = provider.getReadOnlyToolCallbacks().stream()
                .filter(item -> "secondhand_search".equals(item.getToolDefinition().name()))
                .findFirst()
                .orElseThrow();

        String raw = callback.call(
                "{\"keyword\":\"台灯\",\"userId\":999,\"token\":\"bad\"}",
                new ToolContext(Map.of("userId", 7L))
        );

        JsonNode result = objectMapper.readTree(raw);
        assertTrue(result.path("success").asBoolean());
        ToolArgs captured = tool.captured.get();
        assertNotNull(captured);
        assertEquals(7L, captured.getUserId());
        assertEquals("台灯", captured.getParams().get("keyword"));
        assertFalse(captured.getParams().containsKey("userId"));
        assertFalse(captured.getParams().containsKey("token"));
    }

    private AgentToolCallbackProvider newProvider(AgentTool tool) {
        return new AgentToolCallbackProvider(
                new AgentToolCatalog(),
                new ToolRegistry(List.of(tool)),
                objectMapper
        );
    }

    private static final class CapturingTool implements AgentTool {

        private final AtomicReference<ToolArgs> captured = new AtomicReference<>();

        @Override
        public String getName() {
            return "secondhand_search";
        }

        @Override
        public ToolResult execute(ToolArgs args) {
            captured.set(args);
            return ToolResult.ok("ok", Map.of("keyword", args.getParams().get("keyword")));
        }
    }
}
