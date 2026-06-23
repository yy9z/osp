package com.caspar.agent.controller;

import com.caspar.agent.mcp.McpProperties;
import com.caspar.agent.model.ToolArgs;
import com.caspar.agent.model.ToolResult;
import com.caspar.agent.registry.ToolRegistry;
import com.caspar.agent.tool.AgentTool;
import com.caspar.common.Result;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InternalMcpToolControllerTest {

    @Test
    void call_shouldRejectMissingSecret() {
        TestFixture fixture = newFixture();

        ResponseEntity<Result<ToolResult>> response = fixture.controller().call(
                request("message_query", Map.of("keyword", "未读"), 42L),
                null,
                loopbackRequest()
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(0, fixture.tool().calls);
    }

    @Test
    void call_shouldRejectNonLoopbackRequest() {
        TestFixture fixture = newFixture();
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRemoteAddr("8.8.8.8");

        ResponseEntity<Result<ToolResult>> response = fixture.controller().call(
                request("message_query", Map.of("keyword", "未读"), 42L),
                "secret",
                servletRequest
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(0, fixture.tool().calls);
    }

    @Test
    void call_shouldRejectToolOutsideAllowList() {
        TestFixture fixture = newFixture();

        ResponseEntity<Result<ToolResult>> response = fixture.controller().call(
                request("unknown_tool", Map.of(), 42L),
                "secret",
                loopbackRequest()
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(0, fixture.tool().calls);
    }

    @Test
    void call_shouldExecuteAllowedToolAndStripIdentityArguments() {
        TestFixture fixture = newFixture();

        ResponseEntity<Result<ToolResult>> response = fixture.controller().call(
                request("message_query", Map.of(
                        "keyword", "未读",
                        "userId", 99,
                        "token", "bad",
                        "authorization", "Bearer bad"
                ), 42L),
                "secret",
                loopbackRequest()
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getData());
        assertTrue(response.getBody().getData().isSuccess());
        assertEquals(1, fixture.tool().calls);
        assertEquals(42L, fixture.tool().lastArgs.getUserId());
        assertEquals("未读", fixture.tool().lastArgs.getParams().get("keyword"));
        assertFalse(fixture.tool().lastArgs.getParams().containsKey("userId"));
        assertFalse(fixture.tool().lastArgs.getParams().containsKey("token"));
        assertFalse(fixture.tool().lastArgs.getParams().containsKey("authorization"));
    }

    private TestFixture newFixture() {
        RecordingTool tool = new RecordingTool("message_query");
        ToolRegistry registry = new ToolRegistry(List.of(tool));
        McpProperties properties = new McpProperties();
        properties.setInternalSecret("secret");
        return new TestFixture(new InternalMcpToolController(registry, properties), tool);
    }

    private InternalMcpToolController.InternalMcpToolCallRequest request(String toolName, Map<String, Object> arguments, Long userId) {
        InternalMcpToolController.InternalMcpToolCallRequest request = new InternalMcpToolController.InternalMcpToolCallRequest();
        request.setToolName(toolName);
        request.setArguments(arguments);
        request.setUserId(userId);
        return request;
    }

    private MockHttpServletRequest loopbackRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        return request;
    }

    private record TestFixture(InternalMcpToolController controller, RecordingTool tool) {
    }

    private static class RecordingTool implements AgentTool {
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
            calls++;
            lastArgs = args;
            return ToolResult.ok("ok", Map.of("ok", true));
        }
    }
}
