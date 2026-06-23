package com.caspar.agent.controller;

import com.caspar.agent.mcp.McpProperties;
import com.caspar.agent.model.ToolArgs;
import com.caspar.agent.model.ToolResult;
import com.caspar.agent.registry.ToolRegistry;
import com.caspar.agent.tool.AgentTool;
import com.caspar.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 系统内 MCP server 调用已有 AgentTool 的桥接入口。
 * 该接口只允许本机进程携带共享密钥访问，避免暴露为公网工具调用后门。
 */
@Slf4j
@RestController
@RequestMapping("/api/internal/mcp")
@RequiredArgsConstructor
public class InternalMcpToolController {

    private static final String MCP_SECRET_HEADER = "X-Campus-MCP-Secret";

    private static final Set<String> ALLOWED_TOOLS = Set.of(
            "dorm_repair",
            "dorm_query",
            "repair_query",
            "secondhand_search",
            "secondhand_publish",
            "lostfound_lost",
            "lostfound_found",
            "navigation_v2",
            "message_query",
            "campus_tips"
    );

    private static final Set<String> RESERVED_ARGUMENT_KEYS = Set.of(
            "userId",
            "user_id",
            "token",
            "authorization"
    );

    private final ToolRegistry toolRegistry;
    private final McpProperties mcpProperties;

    @PostMapping("/tools/call")
    public ResponseEntity<Result<ToolResult>> call(@RequestBody InternalMcpToolCallRequest request,
                                                   @RequestHeader(value = MCP_SECRET_HEADER, required = false) String secret,
                                                   HttpServletRequest servletRequest) {
        if (!isLoopbackRequest(servletRequest) || !isValidSecret(secret)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Result.forbidden());
        }
        if (request == null || !StringUtils.hasText(request.getToolName())) {
            return ResponseEntity.badRequest().body(Result.badRequest("缺少工具名称"));
        }
        String toolName = request.getToolName().trim();
        if (!ALLOWED_TOOLS.contains(toolName)) {
            return ResponseEntity.badRequest().body(Result.badRequest("MCP 工具不在白名单内: " + toolName));
        }
        if (request.getUserId() == null) {
            return ResponseEntity.badRequest().body(Result.badRequest("缺少当前用户身份"));
        }

        AgentTool tool = toolRegistry.getTool(toolName);
        if (tool == null) {
            return ResponseEntity.badRequest().body(Result.badRequest("工具不存在: " + toolName));
        }

        try {
            ToolResult result = tool.execute(ToolArgs.builder()
                    .toolName(toolName)
                    .params(sanitizeArguments(request.getArguments()))
                    .userId(request.getUserId())
                    .build());
            return ResponseEntity.ok(Result.success(result));
        } catch (Exception e) {
            log.error("系统内 MCP 工具桥接执行异常: toolName={}", toolName, e);
            return ResponseEntity.ok(Result.success(ToolResult.fail("工具执行失败: " + e.getMessage())));
        }
    }

    private boolean isValidSecret(String providedSecret) {
        String expectedSecret = mcpProperties.getInternalSecret();
        if (!StringUtils.hasText(expectedSecret) || !StringUtils.hasText(providedSecret)) {
            return false;
        }
        byte[] expected = expectedSecret.getBytes(StandardCharsets.UTF_8);
        byte[] provided = providedSecret.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, provided);
    }

    private boolean isLoopbackRequest(HttpServletRequest request) {
        if (request == null || !StringUtils.hasText(request.getRemoteAddr())) {
            return false;
        }
        try {
            InetAddress address = InetAddress.getByName(request.getRemoteAddr());
            return address.isLoopbackAddress();
        } catch (Exception ignored) {
            return false;
        }
    }

    private Map<String, Object> sanitizeArguments(Map<String, Object> arguments) {
        Map<String, Object> sanitized = new LinkedHashMap<>();
        if (arguments == null || arguments.isEmpty()) {
            return sanitized;
        }
        arguments.forEach((key, value) -> {
            if (StringUtils.hasText(key) && !RESERVED_ARGUMENT_KEYS.contains(key)) {
                sanitized.put(key, value);
            }
        });
        return sanitized;
    }

    @Data
    public static class InternalMcpToolCallRequest {
        private String toolName;
        private Map<String, Object> arguments;
        private Long userId;
    }
}
