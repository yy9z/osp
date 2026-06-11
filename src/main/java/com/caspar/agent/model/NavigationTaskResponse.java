package com.caspar.agent.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 结构化导航任务响应 - 四段式结构
 */
@Data
@Builder
public class NavigationTaskResponse {

    private String intent;                    // NAVIGATION
    private String taskType;                  // class_commute, dining, express, etc.

    private Understanding understanding;      // 识别结果
    private Decision decision;                // 决策依据
    private Route route;                      // 路线信息
    private List<Action> actions;             // 可执行动作

    /**
     * 理解层：Agent识别了什么
     */
    @Data
    @Builder
    public static class Understanding {
        private PlaceInfo origin;
        private PlaceInfo destination;
        private List<PlaceInfo> waypoints;
        private String mode;                  // walking, cycling
        private List<String> preferences;     // fastest, safeNight, etc.
        private List<String> constraints;     // avoid_construction, etc.
        private String timeContext;           // now, night, after_class
        private String timeSlot;              // morning, noon_heat, evening_peak, night
        private Integer urgencyMinutes;
    }

    /**
     * 决策层：Agent为什么这样规划
     */
    @Data
    @Builder
    public static class Decision {
        private String summary;               // 一句话摘要
        private List<String> reasoning;       // 决策依据列表
        private String campusHint;            // 校区提示
    }

    /**
     * 路线层：具体路线信息
     */
    @Data
    @Builder
    public static class Route {
        private boolean ready;
        private String routeType;             // single, multi_waypoint
        private Integer distance;             // 米
        private Integer duration;             // 秒
        private List<Map<String, Object>> steps;
        private List<List<Double>> path;
        private List<RouteSegment> segments;
        private SafetyAnalysis safetyAnalysis;  // 路线安全分析结果
        private OverallAnalysis overallAnalysis; // 综合分析结果（多段路线）
        // 多路线选择
        private Integer selectedRouteIndex;      // 选中的路线索引
        private Integer totalRoutes;             // 总路线数
        private String selectionReason;          // 选择理由
        private List<Map<String, Object>> alternativeRoutes; // 备选路线列表
        private String amapApiVersion;           // 高德接口版本，如 v5
        private String routingStrategy;          // 高德请求策略，如 alternative_route=3
        private String geometrySource;           // 绘制几何来源，如 amap_v5 / amap_v3_recomputed
        private String geometryFallbackReason;   // 几何回填原因说明
    }

    /**
     * 路线安全分析结果
     */
    @Data
    @Builder
    public static class SafetyAnalysis {
        private int safetyScore;              // 安全评分 1-10
        private List<String> warnings;        // 警告信息列表
        private List<String> suggestions;     // 建议列表
        private boolean hasAlternative;       // 是否有替代路线
        private String alternativeSummary;    // 替代路线摘要

        public boolean hasWarnings() {
            return warnings != null && !warnings.isEmpty();
        }

        public boolean hasSuggestions() {
            return suggestions != null && !suggestions.isEmpty();
        }
    }

    /**
     * 单段路径分析结果
     */
    @Data
    @Builder
    public static class SegmentAnalysis {
        private int score;                      // 该段评分 1-10
        private String reason;                  // 评估理由
        private List<String> warnings;          // 该段警告
        private List<String> suggestions;       // 该段建议

        public boolean hasWarnings() {
            return warnings != null && !warnings.isEmpty();
        }

        public boolean hasSuggestions() {
            return suggestions != null && !suggestions.isEmpty();
        }
    }

    /**
     * 综合分析结果
     */
    @Data
    @Builder
    public static class OverallAnalysis {
        private int overallScore;               // 总体评分
        private String summary;                 // 综合评估摘要
        private List<String> keyPoints;         // 关键提示
        private String recommendation;          // 总体建议
    }

    @Data
    @Builder
    public static class RouteSegment {
        private Integer index;
        private PlaceInfo from;
        private PlaceInfo to;
        private Integer distance;
        private Integer duration;
        private List<Map<String, Object>> steps;
        private List<List<Double>> path;
        private String geometrySource;
        private Integer selectedRouteIndex;
        private Integer totalRoutes;
        private String selectionReason;
        private List<Map<String, Object>> alternativeRoutes;
        private SegmentSlotProfile slotProfile;
        private SegmentAnalysis analysis;      // 该段分析结果
    }

    @Data
    @Builder
    public static class SegmentSlotProfile {
        private Integer index;
        private String destination;
        private String segmentGoal;
        private String taskScene;
        private List<String> preferences;
        private List<String> constraints;
        private String timeContext;
        private String timeSlot;
        private Integer urgencyMinutes;
        private String notes;
    }

    /**
     * 动作层：用户可以做什么
     */
    @Data
    @Builder
    public static class Action {
        private String type;                  // replan, nearby, return_route, save_route
        private String label;                 // 按钮文字
        private Map<String, Object> payload;  // 动作参数
    }

    /**
     * 地点信息
     */
    @Data
    @Builder
    public static class PlaceInfo {
        private String name;
        private Double lat;
        private Double lng;
        private String source;                // device_location, campus_poi_dict, amap_search, user_input
        private String address;
    }

    /**
     * 转换为旧格式（兼容前端）
     */
    public Map<String, Object> toLegacyFormat() {
        Map<String, Object> data = new java.util.HashMap<>();

        // 基础信息
        data.put("intent", intent);
        data.put("taskType", taskType);

        // Understanding
        if (understanding != null) {
            if (understanding.origin != null) {
                data.put("origin", understanding.origin.name);
                data.put("originLat", understanding.origin.lat);
                data.put("originLng", understanding.origin.lng);
                data.put("originSource", understanding.origin.source);
            }
            if (understanding.destination != null) {
                data.put("destination", understanding.destination.name);
                data.put("destLat", understanding.destination.lat);
                data.put("destLng", understanding.destination.lng);
                data.put("destinationAddress", understanding.destination.address);
            }
            data.put("mode", understanding.mode);
            data.put("preferences", understanding.preferences);
            data.put("timeContext", understanding.timeContext);
            data.put("timeSlot", understanding.timeSlot);
            data.put("urgencyMinutes", understanding.urgencyMinutes);
            data.put("waypoints", understanding.waypoints == null ? List.of() : understanding.waypoints.stream()
                .map(this::placeInfoToMap)
                .toList());
        }

        // Decision
        if (decision != null) {
            data.put("agentSummary", decision.summary);
            data.put("reasoning", decision.reasoning != null ? String.join("；", decision.reasoning) : "");
            data.put("campus", decision.campusHint);
        }

        // Route
        if (route != null) {
            data.put("routeReady", route.ready);
            data.put("distance", route.distance);
            data.put("duration", route.duration);
            data.put("steps", route.steps);
            data.put("path", route.path);
            data.put("routeType", route.routeType);
            data.put("amapApiVersion", route.amapApiVersion);
            data.put("routingStrategy", route.routingStrategy);
            data.put("geometrySource", route.geometrySource);
            data.put("geometryFallbackReason", route.geometryFallbackReason);
            data.put("segments", route.segments == null ? List.of() : route.segments.stream()
                .map(this::segmentToMap)
                .toList());
            // 安全分析结果
            if (route.safetyAnalysis != null) {
                Map<String, Object> safetyMap = new java.util.LinkedHashMap<>();
                safetyMap.put("safetyScore", route.safetyAnalysis.safetyScore);
                safetyMap.put("warnings", route.safetyAnalysis.warnings != null ? route.safetyAnalysis.warnings : List.of());
                safetyMap.put("suggestions", route.safetyAnalysis.suggestions != null ? route.safetyAnalysis.suggestions : List.of());
                safetyMap.put("hasAlternative", route.safetyAnalysis.hasAlternative);
                safetyMap.put("alternativeSummary", route.safetyAnalysis.alternativeSummary);
                data.put("safetyAnalysis", safetyMap);
            }
            // 综合分析结果（多段路线）
            if (route.overallAnalysis != null) {
                Map<String, Object> overallMap = new java.util.LinkedHashMap<>();
                overallMap.put("overallScore", route.overallAnalysis.overallScore);
                overallMap.put("summary", route.overallAnalysis.summary);
                overallMap.put("keyPoints", route.overallAnalysis.keyPoints != null ? route.overallAnalysis.keyPoints : List.of());
                overallMap.put("recommendation", route.overallAnalysis.recommendation);
                data.put("overallAnalysis", overallMap);
            }
            // 多路线选择信息
            if (route.totalRoutes != null && route.totalRoutes > 1) {
                data.put("selectedRouteIndex", route.selectedRouteIndex);
                data.put("totalRoutes", route.totalRoutes);
                data.put("selectionReason", route.selectionReason);
                data.put("alternativeRoutes", route.alternativeRoutes != null ? route.alternativeRoutes : List.of());
            }
        }

        // Actions
        if (actions != null && !actions.isEmpty()) {
            data.put("actions", actions);
        }

        return data;
    }

    private Map<String, Object> placeInfoToMap(PlaceInfo placeInfo) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("name", placeInfo.getName());
        map.put("lat", placeInfo.getLat());
        map.put("lng", placeInfo.getLng());
        map.put("source", placeInfo.getSource());
        map.put("address", placeInfo.getAddress());
        return map;
    }

    private Map<String, Object> segmentToMap(RouteSegment segment) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("index", segment.getIndex());
        map.put("from", segment.getFrom() == null ? null : placeInfoToMap(segment.getFrom()));
        map.put("to", segment.getTo() == null ? null : placeInfoToMap(segment.getTo()));
        map.put("distance", segment.getDistance());
        map.put("duration", segment.getDuration());
        map.put("steps", segment.getSteps());
        map.put("path", segment.getPath());
        map.put("geometrySource", segment.getGeometrySource());
        map.put("selectedRouteIndex", segment.getSelectedRouteIndex());
        map.put("totalRoutes", segment.getTotalRoutes());
        map.put("selectionReason", segment.getSelectionReason());
        map.put("alternativeRoutes", segment.getAlternativeRoutes() != null ? segment.getAlternativeRoutes() : List.of());
        if (segment.getSlotProfile() != null) {
            Map<String, Object> slotMap = new java.util.LinkedHashMap<>();
            slotMap.put("index", segment.getSlotProfile().getIndex());
            slotMap.put("destination", segment.getSlotProfile().getDestination());
            slotMap.put("segmentGoal", segment.getSlotProfile().getSegmentGoal());
            slotMap.put("taskScene", segment.getSlotProfile().getTaskScene());
            slotMap.put("preferences", segment.getSlotProfile().getPreferences() != null ? segment.getSlotProfile().getPreferences() : List.of());
            slotMap.put("constraints", segment.getSlotProfile().getConstraints() != null ? segment.getSlotProfile().getConstraints() : List.of());
            slotMap.put("timeContext", segment.getSlotProfile().getTimeContext());
            slotMap.put("timeSlot", segment.getSlotProfile().getTimeSlot());
            slotMap.put("urgencyMinutes", segment.getSlotProfile().getUrgencyMinutes());
            slotMap.put("notes", segment.getSlotProfile().getNotes());
            map.put("slotProfile", slotMap);
        }
        // 添加段分析结果
        if (segment.getAnalysis() != null) {
            Map<String, Object> analysisMap = new java.util.LinkedHashMap<>();
            analysisMap.put("score", segment.getAnalysis().getScore());
            analysisMap.put("reason", segment.getAnalysis().getReason());
            analysisMap.put("warnings", segment.getAnalysis().getWarnings() != null ? segment.getAnalysis().getWarnings() : List.of());
            analysisMap.put("suggestions", segment.getAnalysis().getSuggestions() != null ? segment.getAnalysis().getSuggestions() : List.of());
            map.put("analysis", analysisMap);
        }
        return map;
    }
}
