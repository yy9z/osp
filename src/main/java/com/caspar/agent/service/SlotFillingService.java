package com.caspar.agent.service;

import com.caspar.agent.llm.LlmClient;
import com.caspar.agent.model.AgentSession;
import com.caspar.agent.model.LlmMessage;
import com.caspar.agent.model.NavigationSlots;
import com.caspar.agent.model.SlotResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 槽位填充服务：调用大模型从对话历史中提取槽位参数，并判断缺失项。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SlotFillingService {

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;
    private final NavigationSlotExtractor navigationSlotExtractor;

    /** 各意图的必填槽位定义 */
    private static final Map<String, List<String>> REQUIRED_SLOTS = Map.of(
            "DORM_REPAIR",       List.of("fault_type"),
            "DORM_QUERY",        List.of(),
            "REPAIR_QUERY",      List.of(),
            "SECONDHAND_SEARCH", List.of(),
            "SECONDHAND_PUBLISH", List.of("title", "category", "price"),
            "LOSTFOUND_LOST",    List.of("item_name"),
            "LOSTFOUND_FOUND",   List.of("item_name"),
            "NAVIGATION",        List.of("destination"),
            "MESSAGE_QUERY",     List.of()
    );

    /** 各槽位对应的追问文本 */
    private static final Map<String, String> SLOT_QUESTIONS = Map.of(
            "fault_type",   "好的，请问是什么故障？（空调/灯/网络/门锁/水管/其他）",
            "dorm_no",      "请问您的宿舍号是多少？",
            "item_name",    "请描述一下丢失的物品名称是什么？",
            "destination",  "请问您要去哪里？",
            "campus",       "中科大有多个校区，请问您想去哪个校区？（东校区/西校区/南校区/中校区/高新校区）",
            "title",        "请输入商品的标题：",
            "category",     "请选择分类：数码/书籍/生活/服装/其他",
            "price",        "请输入出售价格（元）："
    );

    /**
     * 从对话上下文中提取槽位，判断缺失参数。
     */
    public SlotResult extractSlots(String intent, String userInput, AgentSession session) {
        long startTime = System.currentTimeMillis();
        // 导航意图使用增强版槽位提取器
        if ("NAVIGATION".equals(intent)) {
            return extractNavigationSlots(userInput, session);
        }

        // 其他意图使用原有逻辑
        String systemPrompt = buildSlotPrompt(intent);
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(LlmMessage.system(systemPrompt));
        // 加入历史对话
        for (Map<String, String> h : session.getHistory()) {
            if ("user".equals(h.get("role"))) {
                messages.add(LlmMessage.user(h.get("content")));
            } else {
                messages.add(LlmMessage.assistant(h.get("content")));
            }
        }
        messages.add(LlmMessage.user(userInput));

        Map<String, Object> extracted = new HashMap<>();
        try {
            String raw = llmClient.chat(messages);
            String normalized = normalizeJsonContent(raw);
            Map<String, Object> parsed = objectMapper.readValue(normalized, new TypeReference<>() {});
            // 合并已有槽位
            extracted.putAll(session.getSlots());
            for (Map.Entry<String, Object> e : parsed.entrySet()) {
                if (e.getValue() != null && !e.getValue().toString().isBlank()) {
                    extracted.put(e.getKey(), e.getValue());
                }
            }
            log.info("槽位提取完成, intent={}, 耗时={}ms", intent, System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            log.warn("槽位提取失败, 耗时={}ms: {}", System.currentTimeMillis() - startTime, e.getMessage());
            extracted.putAll(session.getSlots());
        }

        // 判断缺失的必填槽位
        List<String> required = REQUIRED_SLOTS.getOrDefault(intent, Collections.emptyList());
        List<String> missing = new ArrayList<>();
        for (String slot : required) {
            Object val = extracted.get(slot);
            if (val == null || val.toString().isBlank()) {
                missing.add(slot);
            }
        }

        SlotResult result = new SlotResult();
        result.setSlots(extracted);
        result.setMissingSlots(missing);
        if (!missing.isEmpty()) {
            result.setAskQuestion(SLOT_QUESTIONS.getOrDefault(missing.get(0), "请提供更多信息："));
        }
        return result;
    }

    /**
     * 导航专用槽位提取（使用增强版）
     */
    private SlotResult extractNavigationSlots(String userInput, AgentSession session) {
        try {
            NavigationSlots navSlots = navigationSlotExtractor.extractSlots(
                userInput, session, session.getSlots()
            );
            NavigationSlots mergedSlots = mergeNavigationSlots(navSlots, session.getSlots());

            // 转换为通用槽位格式
            Map<String, Object> extracted = new HashMap<>();
            if (session.getSlots() != null) {
                extracted.putAll(session.getSlots());
            }
            extracted.put("destination", mergedSlots.getDestination());
            extracted.put("origin", mergedSlots.getOrigin());
            extracted.put("campus", mergedSlots.getCampus());
            extracted.put("travel_mode", mergedSlots.getTravelMode());
            extracted.put("preferences", mergedSlots.getPreferences());
            extracted.put("taskScene", mergedSlots.getTaskScene());
            extracted.put("timeContext", mergedSlots.getTimeContext());
            extracted.put("timeSlot", mergedSlots.getTimeSlot());
            extracted.put("urgencyMinutes", mergedSlots.getUrgencyMinutes());
            extracted.put("destinationType", mergedSlots.getDestinationType());
            extracted.put("waypoints", mergedSlots.getWaypoints());
            extracted.put("userLat", mergedSlots.getUserLat());
            extracted.put("userLng", mergedSlots.getUserLng());
            extracted.put("originalQuery", mergedSlots.getOriginalQuery());
            extracted.put("slots", mergedSlots); // 保存完整对象，key改为slots以匹配NavigationToolV2

            // 判断缺失槽位
            List<String> missing = new ArrayList<>();
            if (mergedSlots.getDestination() == null || mergedSlots.getDestination().isBlank()) {
                missing.add("destination");
            }

            SlotResult result = new SlotResult();
            result.setSlots(extracted);
            result.setMissingSlots(missing);
            if (!missing.isEmpty()) {
                result.setAskQuestion(SLOT_QUESTIONS.getOrDefault(missing.get(0), "请提供更多信息："));
            }
            return result;
        } catch (Exception e) {
            log.error("导航槽位提取失败", e);
            // 降级到原有逻辑
            Map<String, Object> extracted = new HashMap<>();
            extracted.putAll(session.getSlots());
            SlotResult result = new SlotResult();
            result.setSlots(extracted);
            result.setMissingSlots(List.of("destination"));
            result.setAskQuestion("请问您要去哪里？");
            return result;
        }
    }

    private NavigationSlots mergeNavigationSlots(NavigationSlots current, Map<String, Object> sessionSlots) {
        NavigationSlots merged = current == null ? new NavigationSlots() : current;
        NavigationSlots previous = toNavigationSlots(sessionSlots);
        if (previous == null) {
            if (merged.getTravelMode() == null || merged.getTravelMode().isBlank()) {
                merged.setTravelMode("walking");
            }
            return merged;
        }

        if (isBlank(merged.getDestination())) {
            merged.setDestination(previous.getDestination());
        }
        if (isBlank(merged.getDestinationType())) {
            merged.setDestinationType(previous.getDestinationType());
        }
        if (isBlank(merged.getOrigin())) {
            merged.setOrigin(previous.getOrigin());
            if (isBlank(merged.getOriginSource())) {
                merged.setOriginSource(previous.getOriginSource());
            }
        }
        if (isBlank(merged.getCampus())) {
            merged.setCampus(previous.getCampus());
        }
        if (isBlank(merged.getTravelMode())) {
            merged.setTravelMode(previous.getTravelMode());
        }
        if (merged.getPreferences() == null || merged.getPreferences().isEmpty()) {
            merged.setPreferences(previous.getPreferences() == null ? new ArrayList<>() : new ArrayList<>(previous.getPreferences()));
        }
        if (merged.getWaypoints() == null) {
            merged.setWaypoints(previous.getWaypoints() == null ? null : new ArrayList<>(previous.getWaypoints()));
        }
        if (isBlank(merged.getTaskScene())) {
            merged.setTaskScene(previous.getTaskScene());
        }
        if (isBlank(merged.getTimeContext())) {
            merged.setTimeContext(previous.getTimeContext());
        }
        if (isBlank(merged.getTimeSlot())) {
            merged.setTimeSlot(previous.getTimeSlot());
        }
        if (merged.getUrgencyMinutes() == null) {
            merged.setUrgencyMinutes(previous.getUrgencyMinutes());
        }
        if (merged.getUserLat() == null) {
            merged.setUserLat(previous.getUserLat());
        }
        if (merged.getUserLng() == null) {
            merged.setUserLng(previous.getUserLng());
        }
        if (isBlank(merged.getOriginalQuery())) {
            merged.setOriginalQuery(previous.getOriginalQuery());
        }

        if (merged.getTravelMode() == null || merged.getTravelMode().isBlank()) {
            merged.setTravelMode("walking");
        }
        return merged;
    }

    private NavigationSlots toNavigationSlots(Map<String, Object> sessionSlots) {
        if (sessionSlots == null || sessionSlots.isEmpty()) {
            return null;
        }

        Object raw = sessionSlots.get("slots");
        if (raw instanceof NavigationSlots navigationSlots) {
            return navigationSlots;
        }

        if (raw != null) {
            try {
                return objectMapper.convertValue(raw, NavigationSlots.class);
            } catch (IllegalArgumentException e) {
                log.warn("历史导航槽位对象转换失败，将尝试使用扁平槽位回填: {}", e.getMessage());
            }
        }

        if (!sessionSlots.containsKey("destination")
                && !sessionSlots.containsKey("origin")
                && !sessionSlots.containsKey("campus")
                && !sessionSlots.containsKey("travel_mode")) {
            return null;
        }

        NavigationSlots fallback = new NavigationSlots();
        fallback.setDestination(asString(sessionSlots.get("destination")));
        fallback.setOrigin(asString(sessionSlots.get("origin")));
        fallback.setCampus(asString(sessionSlots.get("campus")));
        fallback.setTravelMode(asString(sessionSlots.get("travel_mode")));
        fallback.setTaskScene(asString(sessionSlots.get("taskScene")));
        fallback.setTimeContext(asString(sessionSlots.get("timeContext")));
        fallback.setTimeSlot(asString(sessionSlots.get("timeSlot")));
        fallback.setDestinationType(asString(sessionSlots.get("destinationType")));
        fallback.setOriginalQuery(asString(sessionSlots.get("originalQuery")));
        fallback.setUrgencyMinutes(asInteger(sessionSlots.get("urgencyMinutes")));
        fallback.setUserLat(asDouble(sessionSlots.get("userLat")));
        fallback.setUserLng(asDouble(sessionSlots.get("userLng")));

        Object preferences = sessionSlots.get("preferences");
        if (preferences instanceof List<?> list) {
            for (Object item : list) {
                if (item != null && !item.toString().isBlank()) {
                    fallback.addPreference(item.toString());
                }
            }
        }

        Object waypoints = sessionSlots.get("waypoints");
        if (waypoints instanceof List<?> list) {
            for (Object item : list) {
                if (item != null && !item.toString().isBlank()) {
                    fallback.addWaypoint(item.toString());
                }
            }
        }
        return fallback;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private Integer asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double asDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
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

    private String buildSlotPrompt(String intent) {
        if ("NAVIGATION".equals(intent)) {
            return """
                    你是中国科学技术大学校园导航的槽位提取模块。
                    仅返回 JSON，不要输出其他文字，不要添加 markdown 代码块。

                    规则：
                    1. 提取 destination（目的地）、origin（出发地）、campus（校区）、travel_mode（出行方式）。
                    2. 用户说“我这里”“我的位置”“当前位置”时，将 origin 提取为“当前位置”。
                    3. 用户没有明确提到校区时，campus 返回 null，不要擅自编造校区。
                    4. travel_mode 仅识别为 walking 或 cycling，未提及时返回 null。
                    5. 只提取用户明确表达的信息，不要根据常识补充目的地。

                    返回字段：destination, origin, campus, travel_mode
                    未提及字段必须返回 null。
                    """;
        }
        return """
                你是一个槽位提取模块，从用户的对话中提取关键参数。
                仅返回 JSON，不要输出其他文字，不要添加 markdown 代码块。
                意图：%s

                提取以下字段（未提及则值为 null）：
                %s
                """.formatted(intent, getSlotFields(intent));
    }

    private String getSlotFields(String intent) {
        return switch (intent) {
            case "DORM_REPAIR"       -> "fault_type（故障类型）, dorm_no（宿舍号）, description（描述）";
            case "SECONDHAND_SEARCH" -> "keyword（关键词）, category（分类）, max_price（最高价格，数字）, sort_preference（排序偏好）";
            case "SECONDHAND_PUBLISH"-> "title（标题）, category（分类）, price（价格，数字）, description（描述）, condition（成色）";
            case "LOSTFOUND_LOST",
                 "LOSTFOUND_FOUND"   -> "item_name（物品名称）, color（颜色）, location（地点）, time（时间）, description（描述）";
            case "NAVIGATION"        -> "destination（目的地）, origin（出发地）, campus（校区）, travel_mode（出行方式：walking/cycling）, preference（偏好）";
            default                  -> "无特殊字段";
        };
    }
}
