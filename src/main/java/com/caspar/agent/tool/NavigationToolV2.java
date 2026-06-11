package com.caspar.agent.tool;

import com.caspar.agent.model.NavigationSlots;
import com.caspar.agent.model.NavigationTaskResponse;
import com.caspar.agent.model.ToolArgs;
import com.caspar.agent.model.ToolResult;
import com.caspar.agent.service.*;
import com.caspar.agent.util.SlotValueUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 增强版校园导航工具（编排层）：
 * 负责流程调度，核心能力下沉至 PlaceResolverService / RouteBuilderService / WaypointService。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NavigationToolV2 implements AgentTool {

    private final PlaceResolverService placeResolverService;
    private final RouteBuilderService routeBuilderService;
    private final NavigationActionGenerator actionGenerator;
    private final CampusPoiDictionary poiDictionary;

    @Override
    public String getName() {
        return "navigation_v2";
    }

    @Override
    public ToolResult execute(ToolArgs args) {
        Map<String, Object> params = args.getParams() == null ? Map.of() : args.getParams();

        NavigationSlots slots;
        if (params.get("slots") instanceof NavigationSlots) {
            slots = (NavigationSlots) params.get("slots");
        } else {
            slots = buildSlotsFromParams(params);
        }

        placeResolverService.enhanceWithCampusSemantics(slots, params);

        log.info("execute开始: campus={}, userLat={}, userLng={}",
                slots.getCampus(), slots.getUserLat(), slots.getUserLng());
        placeResolverService.inferCampusIfMissing(slots);

        if (slots.getDestination() == null || slots.getDestination().isBlank()) {
            return ToolResult.fail("请提供目的地信息。");
        }

        if (slots.getDestinationType() != null && slots.getDestinationType().startsWith("nearest_")) {
            return handleNearestQuery(slots);
        }

        ResolvedNavigation resolvedNavigation = placeResolverService.resolveNavigationPlaces(slots);
        ToolResult resolveOutcome = handleResolveOutcome(slots, resolvedNavigation);
        if (resolveOutcome != null) {
            return resolveOutcome;
        }

        return buildNavigationTaskResponse(slots, resolvedNavigation);
    }

    /**
     * 处理“最近XX”类型查询。
     */
    private ToolResult handleNearestQuery(NavigationSlots slots) {
        String type = slots.getDestinationType().replace("nearest_", "");
        List<String> places = poiDictionary.getPlacesByCategory(type);

        if (places.isEmpty()) {
            return ToolResult.fail("未找到相关" + type + "类型的地点");
        }

        slots.setDestination(places.get(0));
        ResolvedNavigation resolvedNavigation = placeResolverService.resolveNavigationPlaces(slots);
        ToolResult resolveOutcome = handleResolveOutcome(slots, resolvedNavigation);
        if (resolveOutcome != null) {
            return resolveOutcome;
        }

        return buildNavigationTaskResponse(slots, resolvedNavigation);
    }

    private ToolResult handleResolveOutcome(NavigationSlots slots, ResolvedNavigation resolvedNavigation) {
        AmapPlaceSearchService.ResolveResult resolveResult = resolvedNavigation.clarificationResult();
        if (resolveResult != null) {
            if (resolveResult.isClarificationRequired()) {
                return placeResolverService.buildClarificationResponse(slots, resolveResult);
            }
            return ToolResult.fail(firstNonBlank(resolveResult.getMessage(), resolvedNavigation.destinationMessage(), "未能解析地点"));
        }
        if (resolvedNavigation.destination() == null) {
            return ToolResult.fail(firstNonBlank(resolvedNavigation.destinationMessage(), "未能解析目的地"));
        }
        return null;
    }

    /**
     * 构建结构化导航任务响应。
     */
    private ToolResult buildNavigationTaskResponse(NavigationSlots slots, ResolvedNavigation resolvedNavigation) {
        NavigationTaskResponse.Understanding understanding = buildUnderstanding(slots, resolvedNavigation);
        NavigationTaskResponse.Route route = routeBuilderService.buildRoute(slots, resolvedNavigation);
        NavigationTaskResponse.Decision decision = buildDecision(slots, resolvedNavigation, route);
        List<NavigationTaskResponse.Action> actions = actionGenerator.generateActions(slots, slots.getTaskScene());

        NavigationTaskResponse response = NavigationTaskResponse.builder()
                .intent("NAVIGATION")
                .taskType(slots.getTaskScene())
                .understanding(understanding)
                .decision(decision)
                .route(route)
                .actions(actions)
                .build();

        Map<String, Object> legacyData = response.toLegacyFormat();
        legacyData.put("destinationOptions", resolvedNavigation.destinationCandidates().stream()
                .map(AmapPlaceSearchService.ResolvedPlace::toMap).toList());
        legacyData.put("candidates", resolvedNavigation.destinationCandidates().stream()
                .map(AmapPlaceSearchService.ResolvedPlace::toMap).toList());

        return ToolResult.ok(decision.getSummary(), legacyData);
    }

    private NavigationTaskResponse.Understanding buildUnderstanding(NavigationSlots slots, ResolvedNavigation resolvedNavigation) {
        NavigationTaskResponse.PlaceInfo destInfo = toPlaceInfo(resolvedNavigation.destination());
        NavigationTaskResponse.PlaceInfo originInfo = toPlaceInfo(resolvedNavigation.origin());

        List<NavigationTaskResponse.PlaceInfo> waypointInfos = resolvedNavigation.waypoints().stream()
                .map(this::toPlaceInfo)
                .toList();

        return NavigationTaskResponse.Understanding.builder()
                .origin(originInfo)
                .destination(destInfo)
                .waypoints(waypointInfos)
                .mode(slots.getTravelMode())
                .preferences(slots.getPreferences())
                .constraints(new ArrayList<>())
                .timeContext(slots.getTimeContext())
                .timeSlot(slots.getTimeSlot())
                .urgencyMinutes(slots.getUrgencyMinutes())
                .build();
    }

    private NavigationTaskResponse.Decision buildDecision(NavigationSlots slots,
                                                          ResolvedNavigation resolvedNavigation,
                                                          NavigationTaskResponse.Route route) {
        List<String> reasoning = new ArrayList<>();

        String scene = slots.getTaskScene();
        if ("class_commute".equals(scene)) {
            reasoning.add("检测到上课赶路场景");
            if (slots.isUrgent()) {
                reasoning.add("时间紧迫，已优先推荐最快路线");
            }
        } else if ("dining".equals(scene)) {
            reasoning.add("检测到就餐场景");
        } else if ("return_dorm".equals(scene)) {
            reasoning.add("检测到返回宿舍场景");
            if (slots.isNightTime()) {
                reasoning.add("当前为夜间，已进行路线安全分析");
            }
        } else if ("express".equals(scene)) {
            reasoning.add("检测到取快递场景");
        }

        appendPlaceReasoning(reasoning, resolvedNavigation.destination());
        resolvedNavigation.waypoints().forEach(place -> appendPlaceReasoning(reasoning, place));

        if (slots.isMultiWaypoint()) {
            reasoning.add("已按您的表达顺序保留途经点，不自动调整先后顺序");
        }

        if (route != null && route.getTotalRoutes() != null && route.getTotalRoutes() > 1) {
            reasoning.add("高德返回" + route.getTotalRoutes() + "条可选路线");
            if (route.getSelectionReason() != null && !route.getSelectionReason().isBlank()) {
                reasoning.add("已选择第" + (route.getSelectedRouteIndex() + 1) + "条路线：" + route.getSelectionReason());
            }
        }

        if (slots.getPreferences().contains("safeNight")) {
            reasoning.add("已启用夜间安全模式，对路线进行了安全分析");
        }
        if ("late_night".equals(slots.getTimeSlot())) {
            reasoning.add("当前为深夜场景，已优先考虑主干道和照明更稳定的路线");
        }
        if (slots.getPreferences().contains("fastest")) {
            reasoning.add("已优先选择最快路线");
        }

        if (route != null && route.getSafetyAnalysis() != null) {
            NavigationTaskResponse.SafetyAnalysis safety = route.getSafetyAnalysis();

            if (safety.getSafetyScore() < 7) {
                reasoning.add("安全评分: " + safety.getSafetyScore() + "/10，存在安全隐患");
            }
            if (safety.hasWarnings()) {
                for (String warning : safety.getWarnings()) {
                    reasoning.add("⚠ " + warning);
                }
            }
            if (safety.hasSuggestions()) {
                for (String suggestion : safety.getSuggestions()) {
                    reasoning.add("💡 " + suggestion);
                }
            }
        }

        if (route != null && route.getOverallAnalysis() != null
                && route.getOverallAnalysis().getSummary() != null
                && !route.getOverallAnalysis().getSummary().isBlank()) {
            reasoning.add("全路径评估：" + route.getOverallAnalysis().getSummary());
        }

        String summary = buildSummary(slots, resolvedNavigation.destination(), resolvedNavigation.destinationMessage(), resolvedNavigation.waypoints());

        return NavigationTaskResponse.Decision.builder()
                .summary(summary)
                .reasoning(reasoning)
                .campusHint(resolvedNavigation.destination().getCampus())
                .build();
    }

    private void appendPlaceReasoning(List<String> reasoning, AmapPlaceSearchService.ResolvedPlace place) {
        if (place == null || place.getSource() == null) {
            return;
        }
        if ("campus_poi_dict".equals(place.getSource())) {
            reasoning.add("已通过校园地点词典识别地点【" + place.getName() + "】");
        } else {
            reasoning.add("已通过高德地图搜索识别地点【" + place.getName() + "】");
        }
    }

    private NavigationTaskResponse.PlaceInfo toPlaceInfo(AmapPlaceSearchService.ResolvedPlace place) {
        if (place == null) {
            return null;
        }
        return NavigationTaskResponse.PlaceInfo.builder()
                .name(place.getName())
                .lat(place.getLat())
                .lng(place.getLng())
                .address(place.getAddress())
                .source(place.getSource())
                .build();
    }

    private String buildSummary(NavigationSlots slots,
                                AmapPlaceSearchService.ResolvedPlace destination,
                                String reasoning,
                                List<AmapPlaceSearchService.ResolvedPlace> waypoints) {
        StringBuilder sb = new StringBuilder();

        if (slots.getOrigin() != null && !slots.getOrigin().isBlank()) {
            sb.append("已为您规划从").append(slots.getOrigin()).append("出发");
        } else {
            sb.append("已为您识别导航任务");
        }

        if (waypoints != null && !waypoints.isEmpty()) {
            for (AmapPlaceSearchService.ResolvedPlace waypoint : waypoints) {
                sb.append("，先途经").append(waypoint.getName());
            }
            sb.append("，再前往").append(destination.getName());
        } else {
            sb.append("，前往").append(destination.getName());
        }

        if (destination.getCampus() != null) {
            sb.append("（").append(destination.getCampus()).append("）");
        }

        if (reasoning != null && !reasoning.isBlank()) {
            sb.append("。").append(reasoning);
        }

        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private NavigationSlots buildSlotsFromParams(Map<String, Object> params) {
        NavigationSlots slots = new NavigationSlots();
        slots.setDestination(SlotValueUtils.getString(params, "destination"));
        slots.setOrigin(SlotValueUtils.getString(params, "origin"));
        slots.setCampus(SlotValueUtils.getString(params, "campus"));
        slots.setTravelMode(normalizeMode(
                SlotValueUtils.getString(params, "travel_mode"),
                SlotValueUtils.getString(params, "mode")));
        slots.setOriginalQuery(SlotValueUtils.getString(params, "originalQuery", "query", "userInput", "message"));
        slots.setUserLat(SlotValueUtils.getDouble(params, "userLat", "user_lat", "currentLat", "lat"));
        slots.setUserLng(SlotValueUtils.getDouble(params, "userLng", "user_lng", "currentLng", "lng"));

        Object rawWaypoints = params.get("waypoints");
        if (rawWaypoints instanceof List<?> list) {
            log.info("从params提取waypoints: {}", list);
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    Object name = map.get("name");
                    if (name != null) {
                        slots.addWaypoint(String.valueOf(name));
                    }
                } else if (item != null) {
                    slots.addWaypoint(String.valueOf(item));
                }
            }
        }

        Object rawPreferences = params.get("preferences");
        if (rawPreferences instanceof List<?> list) {
            for (Object item : list) {
                if (item != null) {
                    slots.addPreference(String.valueOf(item));
                }
            }
        }

        if (params.get("taskScene") != null) {
            slots.setTaskScene(String.valueOf(params.get("taskScene")));
        }
        if (params.get("timeContext") != null) {
            slots.setTimeContext(String.valueOf(params.get("timeContext")));
        }
        if (params.get("timeSlot") != null) {
            slots.setTimeSlot(String.valueOf(params.get("timeSlot")));
        }
        if (params.get("urgencyMinutes") instanceof Number num) {
            slots.setUrgencyMinutes(num.intValue());
        }
        if (params.get("destinationType") != null) {
            slots.setDestinationType(String.valueOf(params.get("destinationType")));
        }

        if (slots.getOrigin() == null && slots.getUserLat() != null) {
            slots.setOrigin("当前位置");
            slots.setOriginSource("location");
        }

        log.info("buildSlotsFromParams完成: destination={}, waypoints={}, campus={}",
                slots.getDestination(), slots.getWaypoints(), slots.getCampus());

        return slots;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String normalizeMode(String travelMode, String mode) {
        String value = travelMode != null ? travelMode : mode;
        if (value == null || value.isBlank()) return "walking";
        String normalized = value.trim().toLowerCase();
        if (normalized.contains("骑") || normalized.contains("cycle") || normalized.contains("bike")) {
            return "cycling";
        }
        return "walking";
    }

}
