package com.caspar.agent.service;

import com.caspar.agent.model.NavigationSlots;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 多地点导航服务 - 支持途经点场景
 *
 * 场景示例：
 * - "去食堂，但是要路过快递站拿快递"
 * - "先去打印店再去图书馆"
 * - "从宿舍去教学楼，顺路去食堂"
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultiWaypointNavigationService {

    private final CampusPoiDictionary poiDictionary;

    private static final List<PatternHandler> PATTERN_HANDLERS = List.of(
        // "先去A再去B" - A是第一站，B是最终目的地
        new PatternHandler(Pattern.compile("先去(.{1,20}?)再去(.{1,20})"), "sequential_two_step"),
        new PatternHandler(Pattern.compile("先去(.{1,20}?)然后去(.{1,20})"), "sequential_then"),
        new PatternHandler(Pattern.compile("先去(.{1,20}?)接着去(.{1,20})"), "sequential_then"),
        new PatternHandler(Pattern.compile("先去(.{1,20}?)之后去(.{1,20})"), "sequential_then"),

        // "去A，路过B" / "去A，途经B" - A是目的地，B是途经点
        new PatternHandler(Pattern.compile("去(.{1,20}?)，?(?:但是)?(?:要)?路过(.{1,20})"), "via_waypoint"),
        new PatternHandler(Pattern.compile("去(.{1,20}?)，?途经(.{1,20})"), "via_waypoint"),
        new PatternHandler(Pattern.compile("去(.{1,20}?)，?顺路去(.{1,20})"), "via_waypoint"),
        new PatternHandler(Pattern.compile("去(.{1,20}?)顺便(?:去|拿|取)?(.{1,20})"), "via_waypoint"),
        new PatternHandler(Pattern.compile("我想去(.{1,20}?)顺便(?:去|拿|取)?(.{1,20})"), "via_waypoint"),

        // "去A吃饭/干嘛，吃完/之后去B" - A是第一站，B是最终目的地
        new PatternHandler(Pattern.compile("去(.{1,20}?)(?:吃饭|就餐|用餐)，?(?:吃完|之后|然后)?去(.{1,20})"), "stopover_then_destination"),
        new PatternHandler(Pattern.compile("想去(.{1,20}?)(?:吃饭|就餐|用餐)，?(?:吃完|之后|然后)?去(.{1,20})"), "stopover_then_destination"),
        new PatternHandler(Pattern.compile("去(.{1,20}?)(?:拿|取|买).{0,5}，?(?:拿完|取完|买完|之后|然后)?去(.{1,20})"), "stopover_then_destination"),
        new PatternHandler(Pattern.compile("想去(.{1,20}?)(?:拿|取|买).{0,5}，?(?:拿完|取完|买完|之后|然后)?去(.{1,20})"), "stopover_then_destination"),

        // "去A，再去B" / "去A，然后去B" - A是第一站，B是最终目的地
        new PatternHandler(Pattern.compile("去(.{1,20}?)，?再去(.{1,20})"), "stopover_then_destination"),
        new PatternHandler(Pattern.compile("去(.{1,20}?)，?然后去(.{1,20})"), "stopover_then_destination"),
        new PatternHandler(Pattern.compile("想去(.{1,20}?)，?(?:再|然后)?去(.{1,20})"), "stopover_then_destination"),

        // "从A去B，顺路去C" - A是起点，B是途经点，C是目的地
        new PatternHandler(Pattern.compile("从(.{1,20}?)去(.{1,20}?)，?顺路去(.{1,20})"), "origin_then_via")
    );

    /**
     * 检测是否为多地点导航需求
     */
    public boolean isMultiWaypointQuery(String userInput) {
        if (userInput == null || userInput.isBlank()) {
            return false;
        }

        String input = userInput.toLowerCase();
        return input.contains("路过")
            || input.contains("顺路")
            || input.contains("途经")
            || input.contains("顺便")
            || (input.contains("先去") && (input.contains("再去") || input.contains("然后去") || input.contains("接着去") || input.contains("之后去")))
            || (input.contains("去") && input.contains("再去"))
            || (input.contains("去") && input.contains("然后去"))
            || (input.contains("吃饭") && input.contains("吃完") && input.contains("去"))
            || (input.contains("想去") && input.matches(".*想去.{1,20}，.{0,5}去.{1,20}.*"));
    }

    /**
     * 提取多地点信息
     */
    public MultiWaypointTask extractWaypoints(String userInput, NavigationSlots baseSlots) {
        MultiWaypointTask task = new MultiWaypointTask();
        task.setOriginalQuery(userInput);
        task.setOrigin(baseSlots.getOrigin());

        if (userInput == null || userInput.isBlank()) {
            return task;
        }

        for (PatternHandler handler : PATTERN_HANDLERS) {
            Matcher matcher = handler.pattern().matcher(userInput);
            if (!matcher.find()) {
                continue;
            }

            switch (handler.type()) {
                case "sequential_two_step", "sequential_then", "stopover_then_destination" -> {
                    // "先去A再去B" 或 "去A吃饭，吃完去B" - A是第一站，B是最终目的地
                    String first = normalizePlaceName(matcher.group(1));
                    String second = normalizePlaceName(matcher.group(2));
                    task.addWaypoint(first);
                    task.setDestination(second);
                    task.setTaskType("sequential");
                    task.setSummary("已识别：先前往【" + first + "】，再前往【" + second + "】");
                    log.info("多地点导航识别: type={}, waypoint={}, destination={}", handler.type(), first, second);
                    return task;
                }
                case "via_waypoint" -> {
                    // "去A，路过B" - A是目的地，B是途经点
                    String destination = normalizePlaceName(matcher.group(1));
                    String waypoint = normalizePlaceName(trimWaypointSuffix(matcher.group(2)));
                    task.setDestination(destination);
                    task.addWaypoint(waypoint);
                    task.setTaskType("via_waypoint");
                    task.setSummary("已识别：前往【" + destination + "】，途中经过【" + waypoint + "】");
                    log.info("多地点导航识别: type={}, destination={}, waypoint={}", handler.type(), destination, waypoint);
                    return task;
                }
                case "origin_then_via" -> {
                    String origin = normalizePlaceName(matcher.group(1));
                    String destination = normalizePlaceName(matcher.group(2));
                    String waypoint = normalizePlaceName(trimWaypointSuffix(matcher.group(3)));
                    task.setOrigin(origin);
                    task.setDestination(destination);
                    task.addWaypoint(waypoint);
                    task.setTaskType("origin_then_via");
                    task.setSummary("已识别：从【" + origin + "】出发，途中经过【" + waypoint + "】，最终到达【" + destination + "】");
                    log.info("多地点导航识别: type={}, origin={}, waypoint={}, destination={}", handler.type(), origin, waypoint, destination);
                    return task;
                }
                default -> {
                }
            }
        }

        if (baseSlots.getDestination() != null) {
            task.setDestination(baseSlots.getDestination());
        }
        return task;
    }

    /**
     * 生成多地点导航建议
     */
    public String generateMultiWaypointSuggestion(MultiWaypointTask task) {
        StringBuilder sb = new StringBuilder();
        sb.append(task.getSummary());

        if (!task.getWaypoints().isEmpty()) {
            sb.append("\n\n推荐路线顺序：");
            if (task.getOrigin() != null) {
                sb.append("\n1. 起点：").append(task.getOrigin());
            }
            int index = task.getOrigin() != null ? 2 : 1;
            for (String waypoint : task.getWaypoints()) {
                sb.append("\n").append(index++).append(". 途经：").append(waypoint);
            }
            sb.append("\n").append(index).append(". 终点：").append(task.getDestination());
        }

        return sb.toString();
    }

    private String normalizePlaceName(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim().replaceAll("^[，。；、\s]+|[，。；、\s]+$", "");

        // 移除常见的动作词后缀
        trimmed = trimmed.replaceAll("(上课|下课|开会|吃饭|就餐|拿快递|取快递|买东西|自习|学习)$", "");

        CampusPoiDictionary.PoiMatch match = poiDictionary.matchPlace(trimmed);
        return match != null ? match.fullName() : trimmed;
    }

    private String trimWaypointSuffix(String raw) {
        if (raw == null) {
            return null;
        }
        return raw.replaceAll("(拿快递|取快递|吃饭|买东西|去一下)$", "").trim();
    }

    private record PatternHandler(Pattern pattern, String type) {}

    /**
     * 多地点导航任务
     */
    @Data
    public static class MultiWaypointTask {
        private String originalQuery;
        private String origin;
        private String destination;
        private List<String> waypoints = new ArrayList<>();
        private String taskType;  // via_waypoint, sequential, convenient_via
        private String summary;

        public void addWaypoint(String waypoint) {
            if (waypoint != null && !waypoint.isBlank() && !waypoints.contains(waypoint)) {
                waypoints.add(waypoint);
            }
        }

        public boolean hasWaypoints() {
            return !waypoints.isEmpty();
        }
    }
}
