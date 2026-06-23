package com.caspar.agent.config;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants;
import com.caspar.agent.mcp.McpToolClient;
import com.caspar.agent.model.ToolResult;
import com.caspar.agent.service.AgentToolExecutionRecorder;
import com.caspar.agent.service.CampusAgentContextKeys;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CampusMcpToolCallbackConfigTest {

    private static final List<String> MCP_TOOL_NAMES = List.of(
            "mcp_campus_overview",
            "mcp_dorm_repair_flow",
            "mcp_secondhand_meetup_flow",
            "mcp_lostfound_match_flow"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void campusMcpScenarioToolCallbacks_shouldRegisterFourScenarioTools() {
        TestFixture fixture = newFixture();

        List<String> names = fixture.callbacks().stream()
                .map(callback -> callback.getToolDefinition().name())
                .toList();

        assertEquals(MCP_TOOL_NAMES, names);
    }

    @Test
    void mcpCallback_shouldPassUserIdAndStripIdentityArguments() {
        TestFixture fixture = newFixture();
        when(fixture.mcpToolClient().call(eq("mcp_campus_overview"), org.mockito.ArgumentMatchers.anyMap(), eq(42L)))
                .thenReturn(ToolResult.ok("ok", Map.of("done", true)));

        ToolCallback callback = findCallback(fixture.callbacks(), "mcp_campus_overview");
        callback.call("""
                {
                  "keyword": "未读",
                  "userId": 99,
                  "token": "bad",
                  "authorization": "Bearer bad"
                }
                """, toolContext("trace-1", 42L));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(fixture.mcpToolClient()).call(eq("mcp_campus_overview"), paramsCaptor.capture(), eq(42L));
        Map<String, Object> params = paramsCaptor.getValue();
        assertEquals("未读", params.get("keyword"));
        assertFalse(params.containsKey("userId"));
        assertFalse(params.containsKey("token"));
        assertFalse(params.containsKey("authorization"));
        assertEquals(1, fixture.recorder().snapshot("trace-1").size());
    }

    @Test
    void mcpCallback_shouldAskForMissingRequiredSlotWithoutCallingMcp() throws Exception {
        TestFixture fixture = newFixture();
        ToolCallback callback = findCallback(fixture.callbacks(), "mcp_dorm_repair_flow");

        String output = callback.call("{}", toolContext("trace-2", 42L));
        Map<String, Object> parsed = objectMapper.readValue(output, new TypeReference<>() {
        });
        Map<String, Object> data = objectMapper.convertValue(parsed.get("data"), new TypeReference<>() {
        });

        verify(fixture.mcpToolClient(), never()).call(eq("mcp_dorm_repair_flow"), org.mockito.ArgumentMatchers.anyMap(), eq(42L));
        assertEquals("fault_type", data.get("askFor"));
        assertEquals("fault_type", data.get("missingSlot"));
        assertEquals(1, fixture.recorder().snapshot("trace-2").size());
    }

    private TestFixture newFixture() {
        McpToolClient mcpToolClient = mock(McpToolClient.class);
        AgentToolExecutionRecorder recorder = new AgentToolExecutionRecorder();
        CampusMcpToolCallbackConfig config = new CampusMcpToolCallbackConfig(mcpToolClient, objectMapper, recorder);
        return new TestFixture(config.campusMcpScenarioToolCallbacks(), mcpToolClient, recorder);
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
            McpToolClient mcpToolClient,
            AgentToolExecutionRecorder recorder
    ) {
    }
}
