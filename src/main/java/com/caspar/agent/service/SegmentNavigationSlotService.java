package com.caspar.agent.service;

import com.caspar.agent.llm.LlmClient;
import com.caspar.agent.model.LlmMessage;
import com.caspar.agent.model.NavigationSegmentSlots;
import com.caspar.agent.model.NavigationSlots;
import com.caspar.util.AmapRouteUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 分段导航槽位提取服务：将多地点行程拆成多段，为每一段单独生成槽位。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SegmentNavigationSlotService {

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;
    private final NavigationSceneClassifier sceneClassifier;

    public List<NavigationSegmentSlots> extractSegmentSlots(String userInput,
                                                            List<AmapRouteUtil.RoutePoint> itinerary,
                                                            NavigationSlots overallSlots) {
        if (itinerary == null || itinerary.size() < 2) {
            return List.of();
        }

        LocalDateTime now = sceneClassifier.getBeijingNow();
        try {
            List<LlmMessage> messages = List.of(
                    LlmMessage.system(buildSystemPrompt()),
                    LlmMessage.user(buildUserPrompt(userInput, itinerary, overallSlots, now))
            );
            String llmResponse = llmClient.chat(messages);
            List<NavigationSegmentSlots> parsed = parseSegmentSlots(llmResponse, itinerary.size() - 1);
            if (parsed.size() == itinerary.size() - 1) {
                List<NavigationSegmentSlots> normalized = normalizeSegmentSlots(parsed, itinerary, overallSlots, now);
                log.info("分段槽位提取结果: {}", summarizeSegmentSlots(normalized));
                return normalized;
            }
        } catch (Exception e) {
            log.warn("分段槽位提取失败，使用降级逻辑", e);
        }

        List<NavigationSegmentSlots> fallback = buildFallbackSlots(itinerary, overallSlots, now);
        log.info("分段槽位降级结果: {}", summarizeSegmentSlots(fallback));
        return fallback;
    }

    private String buildSystemPrompt() {
        return """
                你是一个校园导航多段任务拆解专家。
                你的任务是把一条多段校园导航路线拆成若干段，并为每一段提取独立槽位。

                规则：
                1. 输出必须是纯 JSON 数组，数组长度必须等于给定的段数。
                2. 先判断用户对每一段真正的目标和约束，再填写字段。
                3. 某段没有明确补充偏好时，需要根据当前北京时间填写 time_slot，并用它作为默认判断依据。
                4. 不要改变行程顺序；每一项只能对应给定的第 N 段。
                5. 如果用户提到“还有5分钟”“打卡快结束”，应只落在相关那一段，不要错误传播到后续所有段。
                6. 如果用户提到“避开主干道”“防止高峰期”，优先落在与就餐/返程等后续段更相关的那一段。
                7. time_context 是粗粒度语义上下文，只能填写 now、night、after_class、before_exam 这类值；不要把 morning、evening_peak 这种时段写进 time_context。
                8. 如果某一段的 time_slot 是 late_night，且用户没有明确要求避开主干道，则默认在 constraints 中加入 "prefer_main_road"，表示深夜主干道优先，宁可略绕也不要走偏僻小路。

                每段返回字段：
                - index: 段索引，从0开始
                - destination: 该段的终点
                - segment_goal: 该段的目标，如“赶去打卡”“去食堂吃饭”
                - task_scene: 该段场景，如 class_commute, dining, study, express, daily_convenience
                - preferences: 偏好数组，如 ["fastest", "avoidCrowd"]
                - constraints: 约束数组，如 ["avoid_main_road"]、["prefer_main_road"]
                - time_context: 时间上下文，如 now, night, after_class
                - time_slot: 时间槽位，如 early_morning, morning, noon_heat, afternoon_heat, evening_peak, night, late_night
                - urgency_minutes: 整数或 null
                - notes: 一句话说明为什么这样分配
                """;
    }

    private String buildUserPrompt(String userInput,
                                   List<AmapRouteUtil.RoutePoint> itinerary,
                                   NavigationSlots overallSlots,
                                   LocalDateTime now) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("当前时间（北京时间）：")
                .append(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .append("\n");
        prompt.append("用户原始需求：").append(userInput).append("\n");
        prompt.append("整体导航槽位：\n");
        prompt.append("- taskScene: ").append(nullToDefault(overallSlots.getTaskScene(), "daily_convenience")).append("\n");
        prompt.append("- preferences: ").append(overallSlots.getPreferences() == null ? List.of() : overallSlots.getPreferences()).append("\n");
        prompt.append("- timeContext: ").append(sceneClassifier.normalizeTimeContext(overallSlots.getTimeContext(), userInput, now)).append("\n");
        String normalizedTimeSlot = sceneClassifier.normalizeTimeSlot(overallSlots.getTimeSlot(), userInput, now);
        prompt.append("- timeSlot: ").append(normalizedTimeSlot).append("\n");
        if ("late_night".equals(normalizedTimeSlot)) {
            prompt.append("- extraConstraint: prefer_main_road（深夜主干道优先，除非用户明确要求避开主干道）\n");
        }
        prompt.append("- urgencyMinutes: ").append(overallSlots.getUrgencyMinutes()).append("\n\n");
        prompt.append("请按以下固定路线段拆分：\n");
        for (int i = 0; i < itinerary.size() - 1; i++) {
            prompt.append(i)
                    .append(": ")
                    .append(itinerary.get(i).name())
                    .append(" -> ")
                    .append(itinerary.get(i + 1).name())
                    .append("\n");
        }
        prompt.append("""

                示例：
                用户原始需求：我要去软件学院打卡，还有5分钟打卡结束，帮我找一条路径越快越好，然后我想去食堂吃饭，避开主干道，防止下班高峰期
                路线段：
                0: 当前位置 -> 软件学院
                1: 软件学院 -> 食堂

                输出示例：
                [
                  {
                    "index": 0,
                    "destination": "软件学院",
                    "segment_goal": "尽快赶去软件学院打卡",
                    "task_scene": "class_commute",
                    "preferences": ["fastest"],
                    "constraints": [],
                    "time_context": "now",
                    "time_slot": "evening_peak",
                    "urgency_minutes": 5,
                    "notes": "这一段对应打卡任务，时间紧迫，应优先速度"
                  },
                  {
                    "index": 1,
                    "destination": "食堂",
                    "segment_goal": "去食堂吃饭并避开下班高峰影响",
                    "task_scene": "dining",
                    "preferences": ["avoidCrowd"],
                    "constraints": ["avoid_main_road"],
                    "time_context": "now",
                    "time_slot": "evening_peak",
                    "urgency_minutes": null,
                    "notes": "这一段对应就餐任务，需要避开主干道和高峰流线"
                  }
                ]
                """);
        return prompt.toString();
    }

    private List<NavigationSegmentSlots> parseSegmentSlots(String llmResponse, int expectedSize) throws Exception {
        String json = extractJsonArray(llmResponse);
        JsonNode root = objectMapper.readTree(json);
        if (!root.isArray()) {
            return List.of();
        }
        List<NavigationSegmentSlots> result = new ArrayList<>();
        for (JsonNode node : root) {
            Integer urgencyMinutes = parseNullableInteger(node, "urgency_minutes", "urgencyMinutes");
            result.add(NavigationSegmentSlots.builder()
                    .index(node.path("index").asInt(result.size()))
                    .destination(asText(node, "destination"))
                    .segmentGoal(asText(node, "segment_goal", "segmentGoal"))
                    .taskScene(asText(node, "task_scene", "taskScene"))
                    .preferences(parseStringList(node.path("preferences")))
                    .constraints(parseStringList(node.path("constraints")))
                    .timeContext(asText(node, "time_context", "timeContext"))
                    .timeSlot(asText(node, "time_slot", "timeSlot"))
                    .urgencyMinutes(urgencyMinutes)
                    .notes(asText(node, "notes"))
                    .build());
        }
        return result.size() == expectedSize ? result : List.of();
    }

    private List<NavigationSegmentSlots> normalizeSegmentSlots(List<NavigationSegmentSlots> parsed,
                                                               List<AmapRouteUtil.RoutePoint> itinerary,
                                                               NavigationSlots overallSlots,
                                                               LocalDateTime now) {
        List<NavigationSegmentSlots> normalized = new ArrayList<>();
        for (int i = 0; i < parsed.size(); i++) {
            NavigationSegmentSlots raw = parsed.get(i);
            NavigationSegmentSlots fallback = buildFallbackSlot(i, itinerary, overallSlots, now);
            String rawTimeContext = raw.getTimeContext();
            String rawTimeSlot = raw.getTimeSlot();
            if (rawTimeSlot == null && sceneClassifier.isKnownTimeSlot(rawTimeContext)) {
                rawTimeSlot = rawTimeContext;
            }
            if (rawTimeContext == null && sceneClassifier.isKnownTimeContext(rawTimeSlot)) {
                rawTimeContext = rawTimeSlot;
            }
            normalized.add(NavigationSegmentSlots.builder()
                    .index(i)
                    .fromName(itinerary.get(i).name())
                    .toName(itinerary.get(i + 1).name())
                    .destination(nullToDefault(raw.getDestination(), fallback.getDestination()))
                    .segmentGoal(nullToDefault(raw.getSegmentGoal(), fallback.getSegmentGoal()))
                    .taskScene(nullToDefault(raw.getTaskScene(), fallback.getTaskScene()))
                    .preferences(raw.getPreferences() == null || raw.getPreferences().isEmpty() ? fallback.getPreferences() : raw.getPreferences())
                    .constraints(raw.getConstraints() == null || raw.getConstraints().isEmpty() ? fallback.getConstraints() : raw.getConstraints())
                    .timeContext(sceneClassifier.normalizeTimeContext(rawTimeContext, buildSegmentTimeHint(overallSlots, itinerary, i), now))
                    .timeSlot(sceneClassifier.normalizeTimeSlot(rawTimeSlot, buildSegmentTimeHint(overallSlots, itinerary, i), now))
                    .urgencyMinutes(raw.getUrgencyMinutes() != null ? raw.getUrgencyMinutes() : fallback.getUrgencyMinutes())
                    .notes(nullToDefault(raw.getNotes(), fallback.getNotes()))
                    .build());
        }
        return normalized;
    }

    private List<NavigationSegmentSlots> buildFallbackSlots(List<AmapRouteUtil.RoutePoint> itinerary,
                                                            NavigationSlots overallSlots,
                                                            LocalDateTime now) {
        List<NavigationSegmentSlots> fallback = new ArrayList<>();
        for (int i = 0; i < itinerary.size() - 1; i++) {
            fallback.add(buildFallbackSlot(i, itinerary, overallSlots, now));
        }
        return fallback;
    }

    private NavigationSegmentSlots buildFallbackSlot(int index,
                                                     List<AmapRouteUtil.RoutePoint> itinerary,
                                                     NavigationSlots overallSlots,
                                                     LocalDateTime now) {
        String fromName = itinerary.get(index).name();
        String toName = itinerary.get(index + 1).name();
        String timeHint = buildSegmentTimeHint(overallSlots, itinerary, index);
        String timeContext = sceneClassifier.normalizeTimeContext(overallSlots.getTimeContext(), timeHint, now);
        String timeSlot = sceneClassifier.normalizeTimeSlot(overallSlots.getTimeSlot(), timeHint, now);

        List<String> preferences = new ArrayList<>();
        if (index == 0 && overallSlots.getPreferences() != null) {
            preferences.addAll(overallSlots.getPreferences());
        }
        if (preferences.isEmpty()) {
            switch (timeSlot) {
                case "noon_heat", "afternoon_heat" -> preferences.add("avoidSun");
                case "evening_peak" -> preferences.add("avoidCrowd");
                case "night", "late_night" -> preferences.add("safeNight");
                default -> {
                }
            }
        }

        List<String> constraints = new ArrayList<>();
        String query = nullToDefault(overallSlots.getOriginalQuery(), "");
        boolean userAvoidMainRoad = query.contains("避开主干道");
        if ("late_night".equals(timeSlot) && !userAvoidMainRoad) {
            constraints.add("prefer_main_road");
        }
        if (userAvoidMainRoad) {
            constraints.add("avoid_main_road");
        }

        String taskScene = overallSlots.getTaskScene();
        if (index == itinerary.size() - 2) {
            taskScene = nullToDefault(taskScene, "daily_convenience");
        } else if (overallSlots.isUrgent()) {
            taskScene = "class_commute";
        }

        String notes = "该段未提取到更细槽位，使用整体需求和时间槽位补全";
        if (overallSlots.isUrgent() && index == 0) {
            notes = "首段存在时效要求，默认优先时效";
        } else if ("late_night".equals(timeSlot) && !userAvoidMainRoad) {
            notes = "深夜场景默认主干道优先，避免偏僻小路";
        }

        return NavigationSegmentSlots.builder()
                .index(index)
                .fromName(fromName)
                .toName(toName)
                .destination(toName)
                .segmentGoal("前往" + toName)
                .taskScene(nullToDefault(taskScene, "daily_convenience"))
                .preferences(preferences)
                .constraints(constraints)
                .timeContext(timeContext)
                .timeSlot(timeSlot)
                .urgencyMinutes(index == 0 ? overallSlots.getUrgencyMinutes() : null)
                .notes(notes)
                .build();
    }

    private String extractJsonArray(String response) {
        if (response == null || response.isBlank()) return "[]";
        String trimmed = response.trim();
        int start = trimmed.indexOf('[');
        int end = trimmed.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return "[]";
    }

    private String asText(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            String value = node.path(fieldName).asText("");
            if (!value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private List<String> parseStringList(JsonNode node) {
        List<String> result = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode item : node) {
                String text = item.asText("");
                if (!text.isBlank()) {
                    result.add(text);
                }
            }
        }
        return result;
    }

    private Integer parseNullableInteger(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode valueNode = node.path(fieldName);
            if (valueNode.isNumber()) {
                return valueNode.asInt();
            }
            if (valueNode.isTextual()) {
                String text = valueNode.asText("").trim();
                if (!text.isEmpty() && !"null".equalsIgnoreCase(text)) {
                    try {
                        return Integer.parseInt(text);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return null;
    }

    private String nullToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String summarizeSegmentSlots(List<NavigationSegmentSlots> segmentSlotsList) {
        if (segmentSlotsList == null || segmentSlotsList.isEmpty()) {
            return "[]";
        }
        StringBuilder summary = new StringBuilder("[");
        for (int i = 0; i < segmentSlotsList.size(); i++) {
            NavigationSegmentSlots slot = segmentSlotsList.get(i);
            if (i > 0) {
                summary.append(", ");
            }
            summary.append("{index=").append(slot.getIndex())
                    .append(", from=").append(slot.getFromName())
                    .append(", to=").append(slot.getToName())
                    .append(", destination=").append(slot.getDestination())
                    .append(", goal=").append(slot.getSegmentGoal())
                    .append(", scene=").append(slot.getTaskScene())
                    .append(", timeContext=").append(slot.getTimeContext())
                    .append(", timeSlot=").append(slot.getTimeSlot())
                    .append(", urgencyMinutes=").append(slot.getUrgencyMinutes())
                    .append(", preferences=").append(slot.getPreferences())
                    .append(", constraints=").append(slot.getConstraints())
                    .append("}");
        }
        summary.append("]");
        return summary.toString();
    }

    private String buildSegmentTimeHint(NavigationSlots overallSlots,
                                        List<AmapRouteUtil.RoutePoint> itinerary,
                                        int index) {
        StringBuilder hint = new StringBuilder();
        if (overallSlots != null && overallSlots.getOriginalQuery() != null) {
            hint.append(overallSlots.getOriginalQuery());
        }
        if (itinerary != null && itinerary.size() > index + 1) {
            if (!hint.isEmpty()) {
                hint.append(" ");
            }
            hint.append(itinerary.get(index).name()).append(" -> ").append(itinerary.get(index + 1).name());
        }
        return hint.toString();
    }
}
