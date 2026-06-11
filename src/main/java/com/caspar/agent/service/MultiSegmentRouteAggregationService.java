package com.caspar.agent.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 多段路线聚合服务：负责段结果合并为最终路线响应。
 */
@Service
@RequiredArgsConstructor
public class MultiSegmentRouteAggregationService {

    private final GeometryAssemblerService geometryAssemblerService;

    public Map<String, Object> aggregate(List<SegmentRoutePlan> plans) {
        Map<String, Object> result = new LinkedHashMap<>();
        int totalDistance = 0;
        int totalDuration = 0;
        List<Map<String, Object>> mergedSteps = new ArrayList<>();
        List<List<Double>> mergedPath = new ArrayList<>();
        List<Map<String, Object>> segments = new ArrayList<>();
        List<String> segmentGeometrySources = new ArrayList<>();
        List<String> geometryFallbackReasons = new ArrayList<>();
        List<String> decisionApiVersions = new ArrayList<>();
        List<String> routingStrategies = new ArrayList<>();

        for (SegmentRoutePlan plan : plans) {
            totalDistance += plan.distance();
            totalDuration += plan.duration();
            mergedSteps.addAll(plan.steps());
            geometryAssemblerService.appendPathPoints(mergedPath, plan.path());
            segmentGeometrySources.add(plan.geometrySource());

            if (plan.geometryFallbackReason() != null && !plan.geometryFallbackReason().isBlank()) {
                geometryFallbackReasons.add(plan.geometryFallbackReason());
            }
            if (plan.amapApiVersion() != null && !plan.amapApiVersion().isBlank()) {
                decisionApiVersions.add(plan.amapApiVersion());
            }
            if (plan.routingStrategy() != null && !plan.routingStrategy().isBlank()) {
                routingStrategies.add(plan.routingStrategy());
            }

            Map<String, Object> segmentInfo = new LinkedHashMap<>();
            segmentInfo.put("index", plan.index());
            segmentInfo.put("from", geometryAssemblerService.toPointMap(plan.from()));
            segmentInfo.put("to", geometryAssemblerService.toPointMap(plan.to()));
            segmentInfo.put("distance", plan.distance());
            segmentInfo.put("duration", plan.duration());
            segmentInfo.put("steps", plan.steps());
            segmentInfo.put("path", plan.path());
            segmentInfo.put("geometrySource", plan.geometrySource());
            segmentInfo.put("selectedRouteIndex", plan.selectedRouteIndex());
            segmentInfo.put("selectionReason", plan.selectionReason());
            segmentInfo.put("totalRoutes", plan.totalRoutes());
            segmentInfo.put("alternativeRoutes", plan.alternativeRoutes());
            segments.add(segmentInfo);
        }

        result.put("success", true);
        result.put("distance", totalDistance);
        result.put("duration", totalDuration);
        result.put("steps", mergedSteps);
        result.put("path", mergedPath);
        result.put("segments", segments);
        result.put("amapApiVersion", decisionApiVersions.stream().distinct().reduce((left, right) -> left + "+" + right).orElse(null));
        result.put("routingStrategy", routingStrategies.stream().distinct().reduce((left, right) -> left + "+" + right).orElse(null));
        result.put("geometrySource", geometryAssemblerService.mergeGeometrySource(segmentGeometrySources));
        result.put("geometryFallbackReason", geometryFallbackReasons.isEmpty() ? null : String.join("；", geometryFallbackReasons));
        return result;
    }
}
