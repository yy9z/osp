package com.caspar.agent.config;

import com.alibaba.cloud.ai.graph.agent.tools.ToolContextHelper;
import com.caspar.agent.model.ToolArgs;
import com.caspar.agent.model.ToolResult;
import com.caspar.agent.registry.ToolRegistry;
import com.caspar.agent.service.AgentToolExecutionRecorder;
import com.caspar.agent.service.CampusAgentContextKeys;
import com.caspar.agent.tool.AgentTool;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 将项目已有 AgentTool 适配为 Spring AI Alibaba / Spring AI ToolCallback。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class CampusAgentToolCallbackConfig {

    private static final Set<String> RESERVED_ARGUMENT_KEYS = Set.of("userId", "user_id", "token", "authorization");

    private static final List<ToolSpec> TOOL_SPECS = List.of(
            new ToolSpec(
                    "dorm_repair",
                    "提交宿舍报修工单。用户描述宿舍设施故障时使用，例如空调、灯、网络、门锁、水管等。",
                    Map.of("fault_type", "故障类型，例如空调/灯/网络/门锁/水管/其他",
                            "description", "故障补充描述，可选"),
                    List.of("fault_type"),
                    "好的，请问是什么故障？（空调/灯/网络/门锁/水管/其他）"
            ),
            new ToolSpec(
                    "dorm_query",
                    "查询当前登录用户的宿舍档案、宿舍号和入住信息。",
                    Map.of(),
                    List.of(),
                    null
            ),
            new ToolSpec(
                    "repair_query",
                    "查询当前登录用户最近的宿舍报修工单和处理状态。",
                    Map.of("status", "工单状态，可选", "keyword", "查询关键词，可选"),
                    List.of(),
                    null
            ),
            new ToolSpec(
                    "secondhand_search",
                    "搜索或推荐二手商品。可按关键词、分类、预算和排序偏好查询。",
                    Map.of("keyword", "商品关键词", "category", "商品分类", "max_price", "最高预算", "sort_preference", "排序偏好"),
                    List.of(),
                    null
            ),
            new ToolSpec(
                    "secondhand_publish",
                    "发布二手商品。用户表达想出售、转让、发布商品时使用。",
                    Map.of("title", "商品标题", "category", "商品分类", "price", "价格", "description", "商品描述", "condition", "新旧程度"),
                    List.of("title", "category", "price"),
                    "发布商品还需要标题、分类和价格，请补充缺失信息。"
            ),
            new ToolSpec(
                    "lostfound_lost",
                    "发布寻物信息。用户丢失物品并希望寻找时使用。",
                    Map.of("item_name", "丢失物品名称", "color", "颜色", "location", "丢失地点", "time", "丢失时间", "description", "补充描述"),
                    List.of("item_name"),
                    "请描述一下丢失物品的名称是什么？"
            ),
            new ToolSpec(
                    "lostfound_found",
                    "发布招领信息。用户捡到物品并希望登记招领时使用。",
                    Map.of("item_name", "拾到物品名称", "color", "颜色", "location", "拾到地点", "time", "拾到时间", "description", "补充描述"),
                    List.of("item_name"),
                    "请描述一下拾到物品的名称是什么？"
            ),
            new ToolSpec(
                    "navigation_v2",
                    "校园导航和路线规划。用户询问去哪里、从哪到哪、怎么走时使用。",
                    Map.of("destination", "目的地", "origin", "出发地", "campus", "校区", "travel_mode", "出行方式", "waypoints", "途经点"),
                    List.of("destination"),
                    "请问您要去哪里？"
            ),
            new ToolSpec(
                    "message_query",
                    "查询消息、通知、会话和未读提醒。",
                    Map.of("keyword", "消息关键词", "unread_only", "是否只看未读"),
                    List.of(),
                    null
            ),
            new ToolSpec(
                    "campus_tips",
                    "查询首页待办、主动提醒和当前优先处理的校园事务。",
                    Map.of(),
                    List.of(),
                    null
            ),
            new ToolSpec(
                    "campus_knowledge_query",
                    "检索平台使用说明、校园事务规则和常见问题。用户询问怎么使用平台、流程、规则、注意事项或FAQ时使用。",
                    Map.of("question", "用户想查询的平台说明或规则问题",
                            "module", "模块名称，可选，例如宿舍报修/二手交易/失物招领/消息通知/校园导航",
                            "top_k", "返回知识片段数量，可选，默认3"),
                    List.of("question"),
                    "请告诉我你想查询哪方面的平台使用说明或校园事务规则。"
            )
    );

    private final ObjectProvider<ToolRegistry> toolRegistryProvider;
    private final ObjectProvider<CampusMcpToolCallbackConfig> mcpToolCallbackConfigProvider;
    private final ObjectMapper objectMapper;
    private final AgentToolExecutionRecorder executionRecorder;

    @Bean("campusAgentToolCallbacks")
    public List<ToolCallback> campusAgentToolCallbacks() {
        List<ToolCallback> callbacks = new ArrayList<>(TOOL_SPECS.stream()
                .map(this::buildCallback)
                .toList());
        CampusMcpToolCallbackConfig mcpConfig = mcpToolCallbackConfigProvider.getIfAvailable();
        if (mcpConfig != null) {
            callbacks.addAll(mcpConfig.campusMcpScenarioToolCallbacks());
        }
        return List.copyOf(callbacks);
    }

    private ToolCallback buildCallback(ToolSpec spec) {
        return FunctionToolCallback
                .builder(spec.name(), (Map<String, Object> params, ToolContext toolContext) -> executeTool(spec, params, toolContext))
                .description(spec.description())
                .inputType(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .inputSchema(spec.inputSchema())
                .build();
    }

    private String executeTool(ToolSpec spec, Map<String, Object> params, ToolContext toolContext) {
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

        AgentTool tool = toolRegistryProvider.getObject().getTool(spec.name());
        if (tool == null) {
            result = ToolResult.fail("工具 [" + spec.name() + "] 不存在");
            executionRecorder.record(traceId, spec.name(), safeParams, userId, result);
            return toToolOutput(result);
        }

        try {
            result = tool.execute(ToolArgs.builder()
                    .toolName(spec.name())
                    .params(safeParams)
                    .userId(userId)
                    .build());
        } catch (Exception e) {
            log.error("ReactAgent工具执行异常: {}", spec.name(), e);
            result = ToolResult.fail("工具执行失败: " + e.getMessage());
        }

        executionRecorder.record(traceId, spec.name(), safeParams, userId, result);
        return toToolOutput(result);
    }

    private Map<String, Object> sanitize(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> sanitized = new LinkedHashMap<>();
        params.forEach((key, value) -> {
            if (key != null && !RESERVED_ARGUMENT_KEYS.contains(key)) {
                sanitized.put(key, value);
            }
        });
        return sanitized;
    }

    private List<String> findMissingSlots(ToolSpec spec, Map<String, Object> params) {
        List<String> missing = new ArrayList<>();
        for (String required : spec.required()) {
            Object value = params.get(required);
            if (value == null || String.valueOf(value).isBlank()) {
                missing.add(required);
            }
        }
        return missing;
    }

    private ToolResult clarificationResult(ToolSpec spec, List<String> missingSlots) {
        String firstMissing = missingSlots.get(0);
        String question = spec.askQuestion() == null || spec.askQuestion().isBlank()
                ? "请补充必要信息：" + firstMissing
                : spec.askQuestion();
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

    private record ToolSpec(
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
                builder.append('"').append(entry.getKey()).append('"')
                        .append(":{\"type\":\"string\",\"description\":\"")
                        .append(entry.getValue())
                        .append("\"}");
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
