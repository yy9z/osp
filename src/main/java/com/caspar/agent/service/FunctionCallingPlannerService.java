package com.caspar.agent.service;

import com.caspar.agent.llm.LlmClient;
import com.caspar.agent.model.AgentSession;
import com.caspar.agent.model.FunctionCallPlan;
import com.caspar.agent.model.IntentResult;
import com.caspar.agent.model.LlmMessage;
import com.caspar.agent.model.SlotResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Legacy Function Calling 规划服务。
 * <p>
 * 当前 Agent 主链路已经迁移到 Spring AI Alibaba ReactAgent，本类仅保留给旧测试、
 * 回归对比和必要的兼容兜底使用，不再作为 /api/agent/chat 的主规划入口。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FunctionCallingPlannerService {

    private static final int MAX_CONTEXT_MESSAGES = 8;

    private static final Map<String, String> INTENT_TO_TOOL = Map.of(
            "DORM_REPAIR", "dorm_repair",
            "DORM_QUERY", "dorm_query",
            "REPAIR_QUERY", "repair_query",
            "SECONDHAND_SEARCH", "secondhand_search",
            "SECONDHAND_PUBLISH", "secondhand_publish",
            "LOSTFOUND_LOST", "lostfound_lost",
            "LOSTFOUND_FOUND", "lostfound_found",
            "NAVIGATION", "navigation_v2",
            "MESSAGE_QUERY", "message_query",
            "CAMPUS_TIPS", "campus_tips"
    );

    private static final Map<String, List<String>> REQUIRED_SLOTS = Map.of(
            "DORM_REPAIR", List.of("fault_type"),
            "DORM_QUERY", List.of(),
            "REPAIR_QUERY", List.of(),
            "SECONDHAND_SEARCH", List.of(),
            "SECONDHAND_PUBLISH", List.of("title", "category", "price"),
            "LOSTFOUND_LOST", List.of("item_name"),
            "LOSTFOUND_FOUND", List.of("item_name"),
            "NAVIGATION", List.of("destination"),
            "MESSAGE_QUERY", List.of(),
            "CAMPUS_TIPS", List.of()
    );

    private static final Map<String, String> SLOT_QUESTIONS = Map.of(
            "fault_type", "好的，请问是什么故障？（空调/灯/网络/门锁/水管/其他）",
            "dorm_no", "请问您的宿舍号是多少？",
            "item_name", "请描述一下物品名称是什么？",
            "destination", "请问您要去哪里？",
            "campus", "中科大有多个校区，请问您想去哪个校区？（东校区/西校区/南校区/中校区/高新校区）",
            "title", "请输入商品的标题：",
            "category", "请选择分类：数码/书籍/生活/服装/其他",
            "price", "请输入价格（元）："
    );

    private static final Set<String> RESERVED_ARGUMENT_KEYS = Set.of("userId", "user_id", "token", "authorization");

    private static final String SYSTEM_PROMPT = """
            你是高校校园智能事务平台的 Function Calling 规划器。
            你必须一次性完成三件事：识别意图、抽取函数参数、选择要调用的本地函数。
            仅返回 JSON，不要输出其他文字，不要添加 markdown 代码块。

            可用函数：
            1. dorm_repair：宿舍报修
               arguments: fault_type, dorm_no, description
               required: fault_type
            2. dorm_query：查询我的宿舍信息
               arguments: {}
            3. repair_query：查询报修工单
               arguments: status, keyword
            4. secondhand_search：搜索/推荐二手商品
               arguments: keyword, category, max_price, sort_preference
            5. secondhand_publish：发布二手商品
               arguments: title, category, price, description, condition
               required: title, category, price
            6. lostfound_lost：发布寻物
               arguments: item_name, color, location, time, description
               required: item_name
            7. lostfound_found：发布招领
               arguments: item_name, color, location, time, description
               required: item_name
            8. navigation_v2：校园导航
               arguments: destination, origin, campus, travel_mode, preferences, waypoints,
                          taskScene, timeContext, timeSlot, urgencyMinutes, destinationType,
                          userLat, userLng, originalQuery
               required: destination
            9. message_query：查看消息/通知
               arguments: keyword, unread_only
            10. campus_tips：查看当前待办、主动提醒、需要优先处理的校园事务
               arguments: {}

            意图名称必须是：
            DORM_REPAIR, DORM_QUERY, REPAIR_QUERY, SECONDHAND_SEARCH, SECONDHAND_PUBLISH,
            LOSTFOUND_LOST, LOSTFOUND_FOUND, NAVIGATION, MESSAGE_QUERY, CAMPUS_TIPS, UNKNOWN。

            输出 JSON 格式：
            {
              "intent": "SECONDHAND_SEARCH",
              "confidence": 0.95,
              "tool_calls": [
                {
                  "name": "secondhand_search",
                  "arguments": {
                    "keyword": "台灯",
                    "max_price": 50
                  }
                }
              ],
              "missing_slots": [],
              "ask_question": null
            }

            规则：
            - 如果是普通问答或无法归入校园业务，intent 返回 UNKNOWN，tool_calls 返回 []。
            - 如果用户是在回答上一轮追问，要优先沿用会话状态中的 existing_intent 和 existing_slots。
            - 缺少 required 参数时，在 missing_slots 中列出缺失字段，并给出 ask_question。
            - dorm_repair 的宿舍信息由系统根据当前用户档案自动补充，不要把 dorm_no 或 description 列为缺失参数。
            - 不要把 userId、token、authorization 等身份字段放进 arguments。
            - 只抽取用户明确表达或会话状态中已经存在的参数，不要编造价格、地点、宿舍号。
            - 导航相关表达，如图书馆、食堂、教学楼、校区、怎么走、从哪到哪，优先使用 NAVIGATION / navigation_v2。
            - “有什么待办/提醒/要处理/优先事项/今天需要看什么”等表达，使用 CAMPUS_TIPS / campus_tips。
            - 二手商品搜索中，“50元以内的台灯”应输出 keyword=台灯, max_price=50。
            """;

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;
    private final IntentService intentService;
    private final SlotFillingService slotFillingService;
    private final PlannerService plannerService;

    public FunctionCallPlan plan(String userInput, AgentSession session) {
        long startTime = System.currentTimeMillis();
        try {
            List<LlmMessage> messages = new ArrayList<>();
            messages.add(LlmMessage.system(SYSTEM_PROMPT));
            appendRecentHistory(messages, userInput, session == null ? List.of() : session.getHistory());
            messages.add(LlmMessage.user(buildStatePayload(userInput, session)));

            String raw = llmClient.chat(messages);
            FunctionCallPlan plan = parsePlan(raw, session);
            log.info("Function Calling规划完成, intent={}, tools={}, missing={}, 耗时={}ms",
                    plan.getIntent(), plan.getToolNames(), plan.getMissingSlots(),
                    System.currentTimeMillis() - startTime);
            return plan;
        } catch (Exception e) {
            log.warn("Function Calling规划失败，降级到旧三段式链路, 耗时={}ms: {}",
                    System.currentTimeMillis() - startTime, e.getMessage());
            return fallbackPlan(userInput, session);
        }
    }

    private FunctionCallPlan parsePlan(String raw, AgentSession session) throws Exception {
        Map<String, Object> parsed = objectMapper.readValue(normalizeJsonContent(raw), new TypeReference<>() {});
        FunctionCallPlan plan = new FunctionCallPlan();

        String intent = normalizeIntent(asString(parsed.get("intent")));
        plan.setIntent(intent);
        plan.setConfidence(asDouble(parsed.get("confidence"), 0.0));

        Map<String, Object> mergedSlots = new HashMap<>();
        if (session != null && session.getSlots() != null) {
            mergedSlots.putAll(session.getSlots());
        }

        List<String> toolNames = extractToolNames(parsed);
        Map<String, Object> arguments = extractArguments(parsed);
        normalizeArgumentAliases(arguments);
        sanitizeArguments(arguments);
        mergedSlots.putAll(arguments);

        if (!"UNKNOWN".equals(intent) && toolNames.isEmpty()) {
            String defaultTool = INTENT_TO_TOOL.get(intent);
            if (defaultTool != null) {
                toolNames = List.of(defaultTool);
            }
        }

        // 必填参数必须由服务端定义校验，不能直接信任模型返回的 missing_slots。
        List<String> missingSlots = findMissingRequiredSlots(intent, mergedSlots);

        plan.setToolNames(toolNames);
        plan.setSlots(mergedSlots);
        plan.setMissingSlots(missingSlots);
        plan.setAskQuestion(resolveAskQuestion(missingSlots));
        return plan;
    }

    private FunctionCallPlan fallbackPlan(String userInput, AgentSession session) {
        FunctionCallPlan plan = new FunctionCallPlan();
        AgentSession safeSession = session == null ? new AgentSession() : session;
        try {
            IntentResult intentResult = intentService.identify(userInput, safeSession.getHistory());
            String intent = normalizeIntent(intentResult.getIntent());
            plan.setIntent(intent);
            plan.setConfidence(intentResult.getConfidence());
            plan.setFallbackUsed(true);

            if ("UNKNOWN".equals(intent)) {
                return plan;
            }

            SlotResult slotResult = slotFillingService.extractSlots(intent, userInput, safeSession);
            Map<String, Object> slots = slotResult.getSlots() == null ? Map.of() : slotResult.getSlots();
            plan.setSlots(new HashMap<>(slots));
            plan.setMissingSlots(slotResult.getMissingSlots() == null ? List.of() : slotResult.getMissingSlots());
            plan.setAskQuestion(slotResult.getAskQuestion());
            plan.setToolNames(plannerService.plan(intent, slots));
            return plan;
        } catch (Exception e) {
            log.warn("旧三段式兜底也失败: {}", e.getMessage());
            plan.setIntent("UNKNOWN");
            plan.setFallbackUsed(true);
            return plan;
        }
    }

    private void appendRecentHistory(List<LlmMessage> messages, String userInput, List<Map<String, String>> history) {
        if (history == null || history.isEmpty()) {
            return;
        }
        int start = Math.max(0, history.size() - MAX_CONTEXT_MESSAGES);
        for (int i = start; i < history.size(); i++) {
            Map<String, String> item = history.get(i);
            if (item == null) {
                continue;
            }
            String role = item.get("role");
            String content = item.get("content");
            if (content == null || content.isBlank()) {
                continue;
            }
            if (i == history.size() - 1 && "user".equals(role)
                    && userInput != null && content.trim().equals(userInput.trim())) {
                continue;
            }
            if ("assistant".equals(role)) {
                messages.add(LlmMessage.assistant(content));
            } else {
                messages.add(LlmMessage.user(content));
            }
        }
    }

    private String buildStatePayload(String userInput, AgentSession session) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("current_user_input", userInput);
        payload.put("existing_intent", session == null ? null : session.getIntent());
        payload.put("existing_slots", session == null ? Map.of() : session.getSlots());
        payload.put("pending_slots", session == null ? List.of() : session.getPendingSlots());
        return "请基于当前用户输入和会话状态输出 Function Calling JSON：\n" + toJson(payload);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private List<String> extractToolNames(Map<String, Object> parsed) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        Object toolCalls = parsed.get("tool_calls");
        if (toolCalls instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    addKnownToolName(names, asString(map.get("name")));
                    addKnownToolName(names, asString(map.get("tool_name")));
                    Object function = map.get("function");
                    if (function instanceof Map<?, ?> functionMap) {
                        addKnownToolName(names, asString(functionMap.get("name")));
                    } else {
                        addKnownToolName(names, asString(function));
                    }
                }
            }
        }
        addKnownToolName(names, asString(parsed.get("tool_name")));
        Object functionCall = parsed.get("function_call");
        if (functionCall instanceof Map<?, ?> functionMap) {
            addKnownToolName(names, asString(functionMap.get("name")));
        }
        return new ArrayList<>(names);
    }

    private Map<String, Object> extractArguments(Map<String, Object> parsed) {
        Map<String, Object> arguments = new HashMap<>();
        Object toolCalls = parsed.get("tool_calls");
        if (toolCalls instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    Object rawArgs = map.get("arguments");
                    arguments.putAll(asArgumentMap(rawArgs));
                    Object function = map.get("function");
                    if (function instanceof Map<?, ?> functionMap) {
                        arguments.putAll(asArgumentMap(functionMap.get("arguments")));
                    }
                }
            }
        }
        if (parsed.get("function_call") instanceof Map<?, ?> functionMap) {
            arguments.putAll(asArgumentMap(functionMap.get("arguments")));
        }
        if (parsed.get("arguments") != null) {
            arguments.putAll(asArgumentMap(parsed.get("arguments")));
        }
        if (parsed.get("slots") instanceof Map<?, ?> slots) {
            arguments.putAll(objectMapper.convertValue(slots, new TypeReference<Map<String, Object>>() {}));
        }
        return arguments;
    }

    private Map<String, Object> asArgumentMap(Object rawArgs) {
        if (rawArgs == null) {
            return Map.of();
        }
        if (rawArgs instanceof Map<?, ?> map) {
            return objectMapper.convertValue(map, new TypeReference<Map<String, Object>>() {});
        }
        if (rawArgs instanceof String text && !text.isBlank()) {
            try {
                return objectMapper.readValue(text, new TypeReference<>() {});
            } catch (Exception e) {
                log.warn("arguments 字符串解析失败: {}", e.getMessage());
            }
        }
        return Map.of();
    }

    private void addKnownToolName(Set<String> names, String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        if (INTENT_TO_TOOL.containsValue(name)) {
            names.add(name);
        }
    }

    private void sanitizeArguments(Map<String, Object> arguments) {
        for (String key : RESERVED_ARGUMENT_KEYS) {
            arguments.remove(key);
        }
        arguments.entrySet().removeIf(e -> e.getValue() == null || String.valueOf(e.getValue()).isBlank());
    }

    private void normalizeArgumentAliases(Map<String, Object> arguments) {
        moveIfPresent(arguments, "time_context", "timeContext");
        moveIfPresent(arguments, "time_slot", "timeSlot");
        moveIfPresent(arguments, "urgency_minutes", "urgencyMinutes");
        moveIfPresent(arguments, "destination_type", "destinationType");
        moveIfPresent(arguments, "user_lat", "userLat");
        moveIfPresent(arguments, "user_lng", "userLng");
        moveIfPresent(arguments, "original_query", "originalQuery");
    }

    private void moveIfPresent(Map<String, Object> arguments, String from, String to) {
        if (arguments.containsKey(from) && !arguments.containsKey(to)) {
            arguments.put(to, arguments.get(from));
        }
        arguments.remove(from);
    }

    private List<String> findMissingRequiredSlots(String intent, Map<String, Object> slots) {
        List<String> required = REQUIRED_SLOTS.getOrDefault(intent, List.of());
        List<String> missing = new ArrayList<>();
        for (String slot : required) {
            Object value = slots.get(slot);
            if (value == null || String.valueOf(value).isBlank()) {
                missing.add(slot);
            }
        }
        return missing;
    }

    private String resolveAskQuestion(List<String> missingSlots) {
        if (missingSlots == null || missingSlots.isEmpty()) {
            return null;
        }
        return SLOT_QUESTIONS.getOrDefault(missingSlots.get(0), "请提供更多信息：");
    }

    private String normalizeJsonContent(String raw) {
        String stripped = stripMarkdownCodeBlock(raw);
        int start = stripped.indexOf("{");
        int end = stripped.lastIndexOf("}");
        if (start >= 0 && end > start) {
            return stripped.substring(start, end + 1).trim();
        }
        return stripped;
    }

    private String stripMarkdownCodeBlock(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        int firstNewline = trimmed.indexOf('\n');
        int lastFence = trimmed.lastIndexOf("```");
        if (firstNewline > 0 && lastFence > firstNewline) {
            return trimmed.substring(firstNewline + 1, lastFence).trim();
        }
        return trimmed.replace("```", "").trim();
    }

    private String normalizeIntent(String intent) {
        if (intent == null || intent.isBlank()) {
            return "UNKNOWN";
        }
        String normalized = intent.trim().toUpperCase(Locale.ROOT);
        if (INTENT_TO_TOOL.containsKey(normalized) || "UNKNOWN".equals(normalized)) {
            return normalized;
        }
        return "UNKNOWN";
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value).trim();
    }

    private double asDouble(Object value, double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
