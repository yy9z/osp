package com.caspar.agent.config;

import com.alibaba.cloud.ai.graph.agent.tools.ToolContextHelper;
import com.caspar.agent.mcp.McpToolClient;
import com.caspar.agent.model.ToolResult;
import com.caspar.agent.service.AgentToolExecutionRecorder;
import com.caspar.agent.service.CampusAgentContextKeys;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 将系统内跨模块 MCP 场景注册为 ReactAgent 可调用工具。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "agent.mcp", name = "enabled", havingValue = "true")
public class CampusMcpToolCallbackConfig {

    private static final Set<String> RESERVED_ARGUMENT_KEYS = Set.of("userId", "user_id", "token", "authorization");

    private static final List<McpScenarioSpec> MCP_SCENARIOS = List.of(
            new McpScenarioSpec(
                    "mcp_campus_overview",
                    "系统内 MCP 场景：汇总首页主动提醒、未读消息和报修状态，适合用户询问待办事项或校园事务总览。",
                    Map.of("keyword", "可选，消息或提醒关键词"),
                    List.of(),
                    null
            ),
            new McpScenarioSpec(
                    "mcp_dorm_repair_flow",
                    "系统内 MCP 场景：先检查宿舍档案，再提交宿舍报修工单，适合用户用自然语言描述宿舍故障。",
                    Map.of("fault_type", "故障类型，例如空调/灯/网络/门锁/水管/其他",
                            "description", "故障补充描述，可选"),
                    List.of("fault_type"),
                    "好的，请问是什么故障？（空调/灯/网络/门锁/水管/其他）"
            ),
            new McpScenarioSpec(
                    "mcp_secondhand_meetup_flow",
                    "系统内 MCP 场景：搜索二手商品，并联动校内面交地点与导航规划。",
                    Map.of("keyword", "商品关键词",
                            "category", "商品分类",
                            "max_price", "最高预算",
                            "destination", "期望面交点，可选"),
                    List.of(),
                    null
            ),
            new McpScenarioSpec(
                    "mcp_lostfound_match_flow",
                    "系统内 MCP 场景：发布寻物或招领记录，并返回系统匹配候选。",
                    Map.of("type", "LOST 或 FOUND，默认 LOST",
                            "item_name", "物品名称",
                            "color", "颜色，可选",
                            "location", "地点，可选",
                            "description", "补充描述，可选"),
                    List.of("item_name"),
                    "请描述一下物品名称是什么？"
            )
    );

    private final McpToolClient mcpToolClient;
    private final ObjectMapper objectMapper;
    private final AgentToolExecutionRecorder executionRecorder;

    public List<ToolCallback> campusMcpScenarioToolCallbacks() {
        return MCP_SCENARIOS.stream()
                .map(this::buildCallback)
                .toList();
    }

    private ToolCallback buildCallback(McpScenarioSpec spec) {
        return FunctionToolCallback
                .builder(spec.name(), (Map<String, Object> params, ToolContext toolContext) -> executeScenario(spec, params, toolContext))
                .description(spec.description())
                .inputType(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .inputSchema(spec.inputSchema())
                .build();
    }

    private String executeScenario(McpScenarioSpec spec, Map<String, Object> params, ToolContext toolContext) {
        Map<String, Object> safeParams = sanitize(params);
        Long userId = resolveUserId(toolContext);
        String traceId = resolveTraceId(toolContext);

        ToolResult result;
        List<String> missingSlots = findMissingSlots(spec, safeParams);
        if (!missingSlots.isEmpty()) {
            result = clarificationResult(spec, missingSlots);
            executionRecorder.record(traceId, spec.name(), safeParams, userId, result);
            return toToolOutput(result);
        }

        try {
            result = mcpToolClient.call(spec.name(), safeParams, userId);
        } catch (Exception e) {
            log.error("MCP场景工具执行异常: {}", spec.name(), e);
            result = ToolResult.fail("MCP 场景调用失败: " + e.getMessage());
        }

        executionRecorder.record(traceId, spec.name(), safeParams, userId, result);
        return toToolOutput(result);
    }

    private Map<String, Object> sanitize(Map<String, Object> params) {
        Map<String, Object> sanitized = new LinkedHashMap<>();
        if (params == null || params.isEmpty()) {
            return sanitized;
        }
        params.forEach((key, value) -> {
            if (StringUtils.hasText(key) && !RESERVED_ARGUMENT_KEYS.contains(key)) {
                sanitized.put(key, value);
            }
        });
        return sanitized;
    }

    private List<String> findMissingSlots(McpScenarioSpec spec, Map<String, Object> params) {
        List<String> missing = new ArrayList<>();
        for (String required : spec.required()) {
            Object value = params.get(required);
            if (value == null || String.valueOf(value).isBlank()) {
                missing.add(required);
            }
        }
        return missing;
    }

    private ToolResult clarificationResult(McpScenarioSpec spec, List<String> missingSlots) {
        String firstMissing = missingSlots.get(0);
        String question = StringUtils.hasText(spec.askQuestion())
                ? spec.askQuestion()
                : "请补充必要信息：" + firstMissing;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("clarificationRequired", true);
        data.put("clarificationType", "missing_slot");
        data.put("askFor", firstMissing);
        data.put("missingSlot", firstMissing);
        data.put("missingSlots", missingSlots);
        data.put("agentSummary", question);
        return ToolResult.ok(question, data);
    }

    private Long resolveUserId(ToolContext toolContext) {
        if (toolContext == null) {
            return null;
        }
        Object value = ToolContextHelper.getConfig(toolContext)
                .flatMap(config -> config.metadata(CampusAgentContextKeys.USER_ID))
                .orElse(null);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String resolveTraceId(ToolContext toolContext) {
        if (toolContext == null) {
            return null;
        }
        Object value = ToolContextHelper.getConfig(toolContext)
                .flatMap(config -> config.metadata(CampusAgentContextKeys.TRACE_ID))
                .orElse(null);
        return value == null ? null : String.valueOf(value);
    }

    private String toToolOutput(ToolResult result) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("success", result.isSuccess());
        output.put("summary", result.isSuccess() ? result.getSummary() : result.getErrorMessage());
        output.put("data", result.getData());
        try {
            return objectMapper.writeValueAsString(output);
        } catch (JsonProcessingException e) {
            return result.isSuccess() ? String.valueOf(result.getSummary()) : String.valueOf(result.getErrorMessage());
        }
    }

    private record McpScenarioSpec(
            String name,
            String description,
            Map<String, String> properties,
            List<String> required,
            String askQuestion
    ) {
        String inputSchema() {
            StringBuilder builder = new StringBuilder();
            builder.append("{\"type\":\"object\",\"properties\":{");
            int index = 0;
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                if (index++ > 0) {
                    builder.append(',');
                }
                builder.append('"').append(entry.getKey()).append("\":{\"type\":\"string\",\"description\":\"")
                        .append(entry.getValue()).append("\"}");
            }
            builder.append("},\"required\":[");
            for (int i = 0; i < required.size(); i++) {
                if (i > 0) {
                    builder.append(',');
                }
                builder.append('"').append(required.get(i)).append('"');
            }
            builder.append("]}");
            return builder.toString();
        }
    }
}
