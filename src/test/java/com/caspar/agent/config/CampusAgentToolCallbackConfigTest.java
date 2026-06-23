package com.caspar.agent.config;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants;
import com.caspar.agent.model.ToolArgs;
import com.caspar.agent.model.ToolResult;
import com.caspar.agent.registry.ToolRegistry;
import com.caspar.agent.service.AgentToolExecutionRecorder;
import com.caspar.agent.service.CampusAgentContextKeys;
import com.caspar.agent.tool.AgentTool;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CampusAgentToolCallbackConfigTest {

    private static final List<String> TOOL_NAMES = List.of(
            "dorm_repair",
            "dorm_query",
            "repair_query",
            "secondhand_search",
            "secondhand_publish",
            "lostfound_lost",
            "lostfound_found",
            "navigation_v2",
            "message_query",
            "campus_tips",
            "campus_knowledge_query"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void campusAgentToolCallbacks_shouldRegisterLocalToolsAndKnowledgeTool() {
        TestFixture fixture = newFixture();

        List<String> names = fixture.callbacks().stream()
                .map(callback -> callback.getToolDefinition().name())
                .toList();

        assertEquals(TOOL_NAMES, names);
    }

    @Test
    void toolCallback_shouldPassUserIdAndStripIdentityArguments() {
        TestFixture fixture = newFixture();
        ToolCallback callback = findCallback(fixture.callbacks(), "message_query");

        callback.call("""
                {
                  "keyword": "未读",
                  "userId": 99,
                  "token": "bad",
                  "authorization": "Bearer bad"
                }
                """, toolContext("trace-1", 42L));

        RecordingTool tool = fixture.tools().get("message_query");
        assertEquals(1, tool.calls);
        assertEquals(42L, tool.lastArgs.getUserId());
        assertEquals("未读", tool.lastArgs.getParams().get("keyword"));
        assertFalse(tool.lastArgs.getParams().containsKey("userId"));
        assertFalse(tool.lastArgs.getParams().containsKey("token"));
        assertFalse(tool.lastArgs.getParams().containsKey("authorization"));
    }

    @Test
    void toolCallback_shouldAskForMissingRequiredSlotWithoutExecutingTool() throws Exception {
        TestFixture fixture = newFixture();
        ToolCallback callback = findCallback(fixture.callbacks(), "dorm_repair");

        String output = callback.call("{}", toolContext("trace-2", 42L));
        Map<String, Object> parsed = objectMapper.readValue(output, new TypeReference<>() {
        });
        Map<String, Object> data = objectMapper.convertValue(parsed.get("data"), new TypeReference<>() {
        });

        assertEquals(0, fixture.tools().get("dorm_repair").calls);
        assertEquals(Boolean.TRUE, data.get("clarificationRequired"));
        assertEquals("fault_type", data.get("askFor"));
        assertEquals("fault_type", data.get("missingSlot"));
        assertEquals(1, fixture.recorder().snapshot("trace-2").size());
    }

    @Test
    void knowledgeToolCallback_shouldAskForQuestionWithoutExecutingTool() throws Exception {
        TestFixture fixture = newFixture();
        ToolCallback callback = findCallback(fixture.callbacks(), "campus_knowledge_query");

        String output = callback.call("{}", toolContext("trace-3", 42L));
        Map<String, Object> parsed = objectMapper.readValue(output, new TypeReference<>() {
        });
        Map<String, Object> data = objectMapper.convertValue(parsed.get("data"), new TypeReference<>() {
        });

        assertEquals(0, fixture.tools().get("campus_knowledge_query").calls);
        assertEquals(Boolean.TRUE, data.get("clarificationRequired"));
        assertEquals("question", data.get("askFor"));
        assertEquals("question", data.get("missingSlot"));
        assertEquals(1, fixture.recorder().snapshot("trace-3").size());
    }

    private TestFixture newFixture() {
        Map<String, RecordingTool> tools = new HashMap<>();
        TOOL_NAMES.forEach(name -> tools.put(name, new RecordingTool(name)));
        ToolRegistry registry = new ToolRegistry(tools.values().stream().map(tool -> (AgentTool) tool).toList());
        AgentToolExecutionRecorder recorder = new AgentToolExecutionRecorder();
        CampusAgentToolCallbackConfig config = new CampusAgentToolCallbackConfig(
                objectProvider(registry),
                objectProvider(null),
                objectMapper,
                recorder);
        return new TestFixture(config.campusAgentToolCallbacks(), tools, recorder);
    }

    private <T> ObjectProvider<T> objectProvider(T object) {
        return new ObjectProvider<>() {
            @Override
            public T getObject() {
                return object;
            }

            @Override
            public T getIfAvailable() {
                return object;
            }
        };
    }

    private ToolCallback findCallback(List<ToolCallback> callbacks, String name) {
        return callbacks.stream()
                .filter(callback -> name.equals(callback.getToolDefinition().name()))
                .findFirst()
                .orElseThrow();
    }

    private ToolContext toolContext(String traceId, Long userId) {
        RunnableConfig config = RunnableConfig.builder()
                .addMetadata(CampusAgentContextKeys.TRACE_ID, traceId)
                .addMetadata(CampusAgentContextKeys.USER_ID, userId)
                .build();
        return new ToolContext(Map.of(ToolContextConstants.AGENT_CONFIG_CONTEXT_KEY, config));
    }

    private record TestFixture(
            List<ToolCallback> callbacks,
            Map<String, RecordingTool> tools,
            AgentToolExecutionRecorder recorder
    ) {
    }

    private static final class RecordingTool implements AgentTool {
        private final String name;
        private int calls;
        private ToolArgs lastArgs;

        private RecordingTool(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public ToolResult execute(ToolArgs args) {
            this.calls++;
            this.lastArgs = args;
            assertNotNull(args);
            return ToolResult.ok(name + " ok", Map.of("tool", name));
        }
    }
}
