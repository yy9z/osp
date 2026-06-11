package com.caspar.agent.tool;

import com.caspar.agent.model.ToolArgs;
import com.caspar.agent.model.ToolResult;
import com.caspar.agent.service.AmapPlaceSearchService;
import com.caspar.agent.service.UstcCampusResolver;
import com.caspar.util.AmapRouteUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 校园导航工具：基于高德 API 解析中国科学技术大学校园地点，并返回结构化导航任务数据。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NavigationTool implements AgentTool {

    private final AmapPlaceSearchService placeSearchService;
    private final UstcCampusResolver campusResolver;
    private final AmapRouteUtil amapRouteUtil;

    @Override
    public String getName() {
        return "navigation";
    }

    @Override
    public ToolResult execute(ToolArgs args) {
        Map<String, Object> params = args.getParams();
        String destination = getString(params, "destination");
        String origin = getString(params, "origin");
        String campus = getString(params, "campus");
        String mode = normalizeMode(getString(params, "travel_mode"), getString(params, "mode"));
        Double userLat = getDouble(params, "userLat", "user_lat", "currentLat", "lat");
        Double userLng = getDouble(params, "userLng", "user_lng", "currentLng", "lng");

        if (destination == null || destination.isBlank()) {
            return ToolResult.fail("请提供目的地信息。");
        }

        AmapPlaceSearchService.ResolveResult destResult = placeSearchService.resolvePlace(destination, campus, userLat, userLng);
        if (destResult.isClarificationRequired()) {
            return ToolResult.ok(destResult.getMessage(), buildClarificationData(destination, origin, mode, campus, destResult));
        }
        if (!destResult.isSuccess() || destResult.getPlace() == null) {
            return ToolResult.fail(destResult.getMessage());
        }

        AmapPlaceSearchService.ResolvedPlace destinationPlace = destResult.getPlace();
        Map<String, Object> data = new HashMap<>();
        data.put("destination", destinationPlace.getName());
        data.put("destinationText", destination);
        data.put("destinationAddress", destinationPlace.getAddress());
        data.put("destLat", destinationPlace.getLat());
        data.put("destLng", destinationPlace.getLng());
        data.put("campus", destinationPlace.getCampus());
        data.put("resolutionSource", destinationPlace.getSource());
        data.put("mode", mode);
        data.put("clarificationRequired", false);
        data.put("routeReady", false);
        data.put("candidates", destResult.getCandidates().stream().map(AmapPlaceSearchService.ResolvedPlace::toMap).toList());
        data.put("destinationOptions", destResult.getCandidates().stream().map(AmapPlaceSearchService.ResolvedPlace::toMap).toList());

        String campusHint = destinationPlace.getCampus() != null ? destinationPlace.getCampus() : campusResolver.normalizeCampus(campus);
        String reasoning = destResult.getMessage();
        if (reasoning != null && !reasoning.isBlank()) {
            data.put("reasoning", reasoning);
        }

        boolean useCurrentLocation = origin == null || origin.isBlank() || "当前位置".equals(origin) || "我的位置".equals(origin);
        if (useCurrentLocation && userLat != null && userLng != null) {
            data.put("origin", "当前位置");
            data.put("originLat", userLat);
            data.put("originLng", userLng);
            data.put("originSource", "USER_LOCATION");
            attachRoute(data, userLng, userLat, destinationPlace.getLng(), destinationPlace.getLat(), mode);
        } else if (origin != null && !origin.isBlank()) {
            AmapPlaceSearchService.ResolveResult originResult = placeSearchService.resolvePlace(origin, campusHint, userLat, userLng);
            if (originResult.isClarificationRequired()) {
                Map<String, Object> clarification = buildClarificationData(destination, origin, mode, campusHint, originResult);
                clarification.putAll(data);
                clarification.put("clarificationType", "origin_campus_disambiguation");
                clarification.put("agentSummary", originResult.getMessage());
                return ToolResult.ok(originResult.getMessage(), clarification);
            }
            if (originResult.isSuccess() && originResult.getPlace() != null) {
                AmapPlaceSearchService.ResolvedPlace originPlace = originResult.getPlace();
                data.put("origin", originPlace.getName());
                data.put("originAddress", originPlace.getAddress());
                data.put("originLat", originPlace.getLat());
                data.put("originLng", originPlace.getLng());
                data.put("originSource", originPlace.getSource());
                attachRoute(data, originPlace.getLng(), originPlace.getLat(), destinationPlace.getLng(), destinationPlace.getLat(), mode);
            }
        }

        String agentSummary = buildSummary(data, reasoning);
        data.put("agentSummary", agentSummary);
        return ToolResult.ok(agentSummary, data);
    }

    private Map<String, Object> buildClarificationData(String destination, String origin, String mode, String campus,
                                                       AmapPlaceSearchService.ResolveResult result) {
        Map<String, Object> data = new HashMap<>();
        data.put("destinationText", destination);
        data.put("originText", origin);
        data.put("mode", mode);
        data.put("campus", campus);
        data.put("clarificationRequired", true);
        data.put("clarificationType", "campus_disambiguation");
        data.put("clarificationOptions", result.getClarificationOptions());
        data.put("agentSummary", result.getMessage());
        return data;
    }

    private void attachRoute(Map<String, Object> data, double originLng, double originLat,
                             double destLng, double destLat, String mode) {
        try {
            Map<String, Object> routeResult = amapRouteUtil.walkingRoute(originLng, originLat, destLng, destLat);
            if (Boolean.TRUE.equals(routeResult.get("success"))) {
                data.put("distance", routeResult.getOrDefault("distance", 0));
                data.put("duration", routeResult.getOrDefault("duration", 0));
                data.put("steps", routeResult.get("steps"));
                data.put("path", routeResult.get("path"));
                data.put("routeReady", true);
            } else {
                data.put("routeReady", false);
                log.warn("高德路线规划失败: {}", routeResult.get("error"));
            }
        } catch (Exception e) {
            log.error("附加路线规划失败", e);
            data.put("routeReady", false);
        }
    }

    private String buildSummary(Map<String, Object> data, String reasoning) {
        String origin = getString(data, "origin");
        String destination = getString(data, "destination");
        String campus = getString(data, "campus");
        Object duration = data.get("duration");
        Object distance = data.get("distance");

        StringBuilder sb = new StringBuilder();
        if (origin != null && !origin.isBlank()) {
            sb.append("已为您识别从【").append(origin).append("】前往【").append(destination).append("】");
        } else {
            sb.append("已为您识别目的地【").append(destination).append("】");
        }
        if (campus != null && !campus.isBlank()) {
            sb.append("（").append(campus).append("）");
        }
        if (distance instanceof Number && duration instanceof Number && ((Number) distance).doubleValue() > 0) {
            double km = ((Number) distance).doubleValue() / 1000.0;
            int minutes = Math.max(1, ((Number) duration).intValue() / 60);
            sb.append("，全程约 ").append(String.format("%.1f", km)).append(" 公里，预计 ").append(minutes).append(" 分钟");
        } else if (origin == null || origin.isBlank()) {
            sb.append("，进入导航页后可继续使用当前位置完成路线规划");
        }
        if (reasoning != null && !reasoning.isBlank()) {
            sb.append("。").append(reasoning);
        }
        return sb.toString();
    }

    private String normalizeMode(String travelMode, String mode) {
        String value = travelMode != null ? travelMode : mode;
        if (value == null || value.isBlank()) {
            return "walking";
        }
        String normalized = value.trim().toLowerCase();
        if (normalized.contains("骑") || normalized.contains("cycle") || normalized.contains("bike")) {
            return "cycling";
        }
        return "walking";
    }

    private String getString(Map<String, Object> params, String key) {
        Object v = params.get(key);
        return v == null || v.toString().isBlank() ? null : v.toString();
    }

    private Double getDouble(Map<String, Object> params, String... keys) {
        for (String key : keys) {
            Object value = params.get(key);
            if (value == null) {
                continue;
            }
            try {
                return Double.parseDouble(value.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }
}
