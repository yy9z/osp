package com.caspar.agent.service;

import com.caspar.agent.llm.LlmClient;
import com.caspar.agent.model.AgentSession;
import com.caspar.agent.model.LlmMessage;
import com.caspar.agent.model.NavigationSlots;
import com.caspar.agent.util.SlotValueUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 导航槽位提取服务 - 增强版，支持6类槽位
 * 完全由大模型理解和提取，不使用硬编码规则
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NavigationSlotExtractor {

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;
    private final CampusPoiDictionary poiDictionary;
    private final NavigationSceneClassifier sceneClassifier;
    private final UstcCampusResolver campusResolver;

    /**
     * 从对话中提取导航槽位（完全由LLM理解）
     */
    public NavigationSlots extractSlots(String userInput, AgentSession session, Map<String, Object> context) {
        long startTime = System.currentTimeMillis();
        LocalDateTime now = sceneClassifier.getBeijingNow();

        // 1. 调用LLM提取所有槽位（包括多地点、紧急情况、夜间场景等）
        Map<String, Object> llmSlots = extractByLlm(userInput, session, now);
        if (shouldReviewMultiPlaceSlots(userInput, llmSlots)) {
            llmSlots = reviewByLlm(userInput, now, llmSlots);
        }
        log.info("LLM槽位提取结果: {}", llmSlots);

        // 2. 构建NavigationSlots对象
        NavigationSlots slots = new NavigationSlots();
        slots.setOriginalQuery(userInput);

        // A. 目的地与途经点完全由LLM判断
        String destination = SlotValueUtils.getString(llmSlots, "destination");
        List<String> extractedWaypoints = getStringList(llmSlots, "waypoints");

        // B. 起点槽位
        String origin = SlotValueUtils.getString(llmSlots, "origin");
        if (origin != null && !origin.isBlank()) {
            if ("当前位置".equals(origin) || "我的位置".equals(origin)) {
                slots.setOrigin("当前位置");
                slots.setOriginSource("user_input");
            } else {
                CampusPoiDictionary.PoiMatch originMatch = poiDictionary.matchPlace(origin);
                slots.setOrigin(originMatch != null ? originMatch.fullName() : origin);
                slots.setOriginSource("user_input");
            }
        } else if (context != null && context.containsKey("userLat") && context.containsKey("userLng")) {
            slots.setOrigin("当前位置");
            slots.setOriginSource("location");
        }

        if (destination != null) {
            applyDestination(destination, userInput, slots);
        }

        // D. 出行方式槽位
        String travelMode = SlotValueUtils.getString(llmSlots, "travel_mode");
        slots.setTravelMode(normalizeTravelMode(travelMode));

        // E. 多地点途经点槽位（由LLM判断，不做标准化，让高德自动选择最近的）
        if (extractedWaypoints != null) {
            slots.setWaypoints(new ArrayList<>());
            for (String waypoint : extractedWaypoints) {
                // 不做标准化，保留用户原始描述，让高德根据用户位置选择最近的
                slots.addWaypoint(waypoint);
            }
            log.info("LLM提取的途经点: {}", slots.getWaypoints());
        }

        // F. 紧急情况槽位（由LLM直接提取）
        Integer urgencyMinutes = SlotValueUtils.getInteger(llmSlots, "urgency_minutes");
        if (urgencyMinutes == null) {
            urgencyMinutes = SlotValueUtils.getInteger(llmSlots, "urgencyMinutes");
        }
        slots.setUrgencyMinutes(urgencyMinutes);

        // G. 时间上下文槽位（由LLM直接提取）
        String timeContext = SlotValueUtils.getString(llmSlots, "time_context");
        if (timeContext == null) {
            timeContext = SlotValueUtils.getString(llmSlots, "timeContext");
        }

        String timeSlot = SlotValueUtils.getString(llmSlots, "time_slot");
        if (timeSlot == null) {
            timeSlot = SlotValueUtils.getString(llmSlots, "timeSlot");
        }

        if (timeSlot == null && sceneClassifier.isKnownTimeSlot(timeContext)) {
            timeSlot = timeContext;
        }
        if (timeContext == null && sceneClassifier.isKnownTimeContext(timeSlot)) {
            timeContext = timeSlot;
        }

        timeContext = sceneClassifier.normalizeTimeContext(timeContext, userInput, now);
        slots.setTimeContext(timeContext);
        timeSlot = sceneClassifier.normalizeTimeSlot(timeSlot, userInput, now);
        slots.setTimeSlot(timeSlot);

        // H. 偏好槽位（由LLM直接提取）
        List<String> preferences = normalizePreferences(getStringList(llmSlots, "preferences"));
        if (preferences != null) {
            preferences.forEach(slots::addPreference);
        }
        // 补充场景推断的偏好
        List<String> inferredPreferences = sceneClassifier.inferPreferences(userInput, slots, null);
        inferredPreferences.forEach(slots::addPreference);

        // I. 任务场景槽位
        String scene = sceneClassifier.classifyScene(userInput, slots);
        slots.setTaskScene(scene);

        // J. 其他字段
        slots.setCampus(SlotValueUtils.getString(llmSlots, "campus"));
        if (slots.getCampus() == null || slots.getCampus().isBlank()) {
            String campusFromDestination = campusResolver.inferCampusFromText(slots.getDestination());
            if (campusFromDestination != null) {
                slots.setCampus(campusFromDestination);
            } else {
                slots.setCampus(campusResolver.inferCampusFromText(userInput));
            }
        }
        if (context != null) {
            log.info("从context提取用户位置: userLat={}, userLng={}",
                    context.get("userLat"), context.get("userLng"));
            slots.setUserLat(SlotValueUtils.getDouble(context, "userLat"));
            slots.setUserLng(SlotValueUtils.getDouble(context, "userLng"));
        } else {
            log.info("context为空，无法获取用户位置");
        }

        log.info("导航槽位提取完成, destination={}, waypoints={}, urgencyMinutes={}, timeContext={}, timeSlot={}, userLat={}, userLng={}, 耗时={}ms",
            slots.getDestination(), slots.getWaypoints(), slots.getUrgencyMinutes(), slots.getTimeContext(), slots.getTimeSlot(),
            slots.getUserLat(), slots.getUserLng(),
            System.currentTimeMillis() - startTime);

        return slots;
    }

    private void applyDestination(String destination, String userInput, NavigationSlots slots) {
        String campusHint = campusResolver.inferCampusFromText(destination);
        if ((slots.getCampus() == null || slots.getCampus().isBlank()) && campusHint != null) {
            slots.setCampus(campusHint);
        }
        CampusPoiDictionary.PoiMatch match = poiDictionary.matchPlace(destination);
        if (match != null) {
            // 目的地文本显式包含校区时保留原文本，避免丢失“西校区图书馆”这类关键信息
            if (campusHint != null) {
                slots.setDestination(destination);
            } else {
                slots.setDestination(match.fullName());
            }
            slots.setDestinationType(match.category());
            return;
        }

        slots.setDestination(destination);
        String semanticType = poiDictionary.detectSemanticType(destination);
        if (semanticType != null) {
            slots.setDestinationType(semanticType);
            if (poiDictionary.isNearestQuery(userInput)) {
                slots.setDestinationType("nearest_" + semanticType);
            }
        }
    }

    private Map<String, Object> extractByLlm(String userInput, AgentSession session, LocalDateTime now) {
        long startTime = System.currentTimeMillis();
        String systemPrompt = buildSlotPrompt(now);
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

        try {
            String raw = llmClient.chat(messages);
            raw = raw.replaceAll("```json\\s*|```\\s*", "").trim();
            log.info("导航槽位提取完成, 耗时={}ms", System.currentTimeMillis() - startTime);
            return objectMapper.readValue(raw, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("LLM槽位提取失败, 耗时={}ms: {}", System.currentTimeMillis() - startTime, e.getMessage());
            return new java.util.HashMap<>();
        }
    }

    private String buildSlotPrompt(LocalDateTime now) {
        String nowText = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        return """
                你是中国科学技术大学校园导航的槽位提取模块。
                仅返回 JSON，不要输出其他文字，不要添加 markdown 代码块。

                当前时间（北京时间）：%s

                ## 基础规则
                1. 提取 destination（目的地）、origin（出发地）、campus（校区）、travel_mode（出行方式）。
                2. 用户说"我这里""我的位置""当前位置"时，将 origin 提取为"当前位置"。
                3. 用户没有明确提到校区时，campus 返回 null，不要擅自编造校区。
                4. travel_mode 仅识别为 walking 或 cycling，未提及时返回 null。
                5. 如果用户说"最近的XX"，destination提取为"XX"，不要加"最近"。
                6. 你必须根据当前北京时间填写 time_slot，不允许留空。
                7. time_context 是粗粒度语义上下文，只能是 now、night、after_class、before_exam 这类值；不要把 morning、evening_peak 这种时段写进 time_context。
                8. 多段意图里如果后续某一段提到“高峰期”“晚上”，不要把它直接污染成整体 time_context。
                9. 如果句子出现“然后”“再”“之后”“最后”等后续主句，后续主句里的地点优先作为最终 destination，前面主句里的地点通常降为 waypoints。
                10. 不允许因为第一段更紧急、地点更近、或者第一段更像“办事”就把第一段地点错误识别成最终 destination。

                ## 多地点导航规则（重要！）
                在提取前，你必须先判断：
                1. 用户最终真正想去到哪里，这才是 destination。
                2. 用户说的“顺便去”“路过”“途中去一下”这类地点，通常是 waypoints，而不是最终 destination。
                3. 判断完成后，再按用户表达顺序提取 waypoints；不要根据你自己的常识、距离远近或路线便利性调整顺序。
                4. “先去A办事，然后去B吃饭/上课/回宿舍”时，B 是 destination，A 是 waypoint。

                用户可能表达涉及多个地点的需求，你需要识别并提取：

                **识别场景**：
                - "先去A再去B"、"去A然后去B"、"去A之后去B" → A是途经点，B是目的地
                - "去A吃饭，吃完去B"、"去A拿快递再去B" → A是途经点，B是目的地
                - "去A，路过B"、"去A，途经B"、"去A顺路去B" → A是目的地，B是途经点
                - "去A，顺便去B"、"去A吃饭，顺便去B" → 先判断用户主要要去的是 A 还是 B；如果 B 明显是附带地点，则 A 是目的地，B 是途经点
                - "去A路过B再去C" → A是目的地，B、C是途经点（按表达顺序）

                **提取规则**：
                - waypoints: 按用户表达的顺序，提取所有中间要经过的地点名称
                - destination: 最终要到达的目的地
                - 没有途经点时 waypoints 返回空数组 []

                **示例**：
                - 输入："去东区食堂吃饭，吃完去教室"
                  输出：{"destination": "教室", "origin": null, "campus": null, "travel_mode": null, "waypoints": ["东区食堂"]}

                - 输入："先去快递站再去图书馆"
                  输出：{"destination": "图书馆", "origin": null, "campus": null, "travel_mode": null, "waypoints": ["快递站"]}

                - 输入："去图书馆路过食堂"
                  输出：{"destination": "图书馆", "origin": null, "campus": null, "travel_mode": null, "waypoints": ["食堂"]}

                - 输入："我要去食堂吃饭，顺便去软件学院"
                  输出：{"destination": "食堂", "origin": null, "campus": null, "travel_mode": null, "waypoints": ["软件学院"]}

                - 输入："我要去软件学院，顺便去食堂"
                  输出：{"destination": "软件学院", "origin": null, "campus": null, "travel_mode": null, "waypoints": ["食堂"]}

                - 输入："我现在要去软件学院打卡，还有5分钟打卡结束，然后我想去食堂吃饭"
                  输出：{"destination": "食堂", "origin": "当前位置", "campus": null, "travel_mode": null, "waypoints": ["软件学院"], "urgency_minutes": 5}

                - 输入："我现在要去软件学院打卡，还有5分钟打卡结束，帮我找一条路线越快越好，然后我想去食堂吃饭，避开主干道，防止下班高峰期"
                  输出：{"destination": "食堂", "origin": "当前位置", "campus": null, "travel_mode": null, "waypoints": ["软件学院"], "urgency_minutes": 5}

                - 输入："去图书馆"
                  输出：{"destination": "图书馆", "origin": null, "campus": null, "travel_mode": null, "waypoints": []}

                ## 特殊场景规则
                - 如果用户提到"赶时间"、"还有X分钟"、"快点"等，在 urgency_minutes 字段填入分钟数
                - 如果用户提到"晚上"、"夜间"、"天黑"等，在 time_context 字段填入 "night"
                - 如果用户表达"安全路线"、"人多的路"等偏好，在 preferences 数组中添加 "safeNight"、"avoidCrowd" 等
                - 如果用户表达"避开主干道"，在 preferences 里返回 "avoidMainRoad"
                - time_slot 必须结合当前北京时间填写，可选值建议使用：early_morning, morning, noon_heat, afternoon_heat, evening_peak, night, late_night
                - 如果 time_slot 是 "late_night"，要理解为深夜场景，后续路线决策默认主干道优先；不要把深夜理解成适合抄近路或穿偏僻小路
                - 如果用户没有主动补充偏好，也要根据 time_slot 帮助后续判断场景

                ## 返回字段
                返回 JSON 对象，包含以下字段：
                - destination: 目的地名称（字符串或null）
                - origin: 出发地名称（字符串或null）
                - campus: 校区名称（字符串或null）
                - travel_mode: "walking" 或 "cycling" 或 null
                - waypoints: 途经点名称数组（数组，没有则为 []）
                - urgency_minutes: 紧急分钟数（数字或null）
                - time_context: 时间上下文（"night"、"after_class" 等，或null）
                - time_slot: 时间槽位（字符串，必须返回）
                - preferences: 偏好数组（如 ["safeNight", "fastest"]，没有则为 []）
                """.formatted(nowText);
    }

    private boolean shouldReviewMultiPlaceSlots(String userInput, Map<String, Object> llmSlots) {
        String input = userInput == null ? "" : userInput;
        List<String> waypoints = getStringList(llmSlots, "waypoints");
        return (waypoints != null && !waypoints.isEmpty())
                || input.contains("顺便")
                || input.contains("然后")
                || input.contains("再去")
                || input.contains("之后")
                || input.contains("最后")
                || input.contains("先去")
                || input.contains("路过");
    }

    private Map<String, Object> reviewByLlm(String userInput, LocalDateTime now, Map<String, Object> extractedSlots) {
        try {
            List<LlmMessage> messages = List.of(
                    LlmMessage.system(buildSlotReviewPrompt(now)),
                    LlmMessage.user(buildSlotReviewUserPrompt(userInput, extractedSlots))
            );
            String raw = llmClient.chat(messages);
            raw = raw.replaceAll("```json\\s*|```\\s*", "").trim();
            Map<String, Object> reviewed = objectMapper.readValue(raw, new TypeReference<>() {});
            log.info("导航槽位复核结果: {}", reviewed);
            return reviewed;
        } catch (Exception e) {
            log.warn("导航槽位复核失败，沿用初版结果: {}", e.getMessage());
            return extractedSlots;
        }
    }

    private String buildSlotReviewPrompt(LocalDateTime now) {
        return """
                你是中国科学技术大学校园导航槽位复核器。
                你会收到用户原始需求和一版初始槽位，你的任务只是在必要时纠正 destination 与 waypoints 的关系。
                仅返回 JSON，不要输出其他文字。

                当前时间（北京时间）：%s

                复核规则：
                1. destination 必须是整条行程最后主要要去的地点。
                2. 被“顺便”“路过”“途中”“先去再去”“然后再去”“之后去”修饰的前置地点，通常是 waypoints。
                3. 如果后半句由“然后”“再”“之后”“最后”引出新的主要任务地点，后面的地点优先是 destination。
                4. 不允许因为第一段更紧急、第一段像办事、或者第一段距离更近，就把第一段地点错误地当成最终 destination。
                5. 如果初始槽位已经正确，原样返回。
                6. 保留 origin、campus、travel_mode、urgency_minutes、time_context、time_slot、preferences，除非它们与纠正后的 destination/waypoints 明显矛盾。

                关键示例：
                - 用户："我现在要去软件学院打卡，还有5分钟打卡结束，然后我想去食堂吃饭"
                  正确结果：destination=食堂, waypoints=["软件学院"]
                - 用户："我要去食堂吃饭，顺便去软件学院"
                  正确结果：destination=食堂, waypoints=["软件学院"]
                - 用户："先去快递站再去图书馆"
                  正确结果：destination=图书馆, waypoints=["快递站"]
                """.formatted(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
    }

    private String buildSlotReviewUserPrompt(String userInput, Map<String, Object> extractedSlots) throws Exception {
        return """
                用户原始需求：
                %s

                初始槽位：
                %s

                请输出修正后的 JSON。
                """.formatted(userInput, objectMapper.writeValueAsString(extractedSlots));
    }

    private String normalizeTravelMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return null;
        }
        String normalized = mode.trim().toLowerCase();
        if (normalized.contains("骑") || normalized.contains("cycle") || normalized.contains("bike")) {
            return "cycling";
        }
        return "walking";
    }

    @SuppressWarnings("unchecked")
    private List<String> getStringList(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v == null) return null;
        if (v instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
            return result;
        }
        return null;
    }

    private List<String> normalizePreferences(List<String> rawPreferences) {
        if (rawPreferences == null) {
            return null;
        }
        List<String> normalized = new ArrayList<>();
        for (String preference : rawPreferences) {
            String mapped = normalizePreference(preference);
            if (mapped != null && !normalized.contains(mapped)) {
                normalized.add(mapped);
            }
        }
        return normalized;
    }

    private String normalizePreference(String preference) {
        if (preference == null || preference.isBlank()) {
            return null;
        }
        String normalized = preference.trim().replace("-", "_");
        String lower = normalized.toLowerCase();
        return switch (lower) {
            case "fastest", "fast", "quickest" -> "fastest";
            case "shortest", "short" -> "shortest";
            case "safenight", "safe_night" -> "safeNight";
            case "avoidcrowd", "avoid_crowd" -> "avoidCrowd";
            case "avoidsun", "avoid_sun" -> "avoidSun";
            case "luggagefriendly", "luggage_friendly" -> "luggageFriendly";
            case "avoidmainroad", "avoid_main_road" -> "avoidMainRoad";
            default -> preference.trim();
        };
    }

}
