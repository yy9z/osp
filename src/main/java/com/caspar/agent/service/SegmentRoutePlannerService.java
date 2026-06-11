package com.caspar.agent.service;

import com.caspar.agent.model.NavigationSegmentSlots;
import com.caspar.agent.model.NavigationSlots;
import com.caspar.util.AmapRouteUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 多段路线中的单段规划服务：负责单段请求、选路与段结果提取。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SegmentRoutePlannerService {

    private final AmapRouteUtil amapRouteUtil;
    private final RouteSelectionService routeSelectionService;
    private final GeometryAssemblerService geometryAssemblerService;

    public record SegmentPlanningResult(
            boolean success,
            SegmentRoutePlan plan,
            String error
    ) {
        public static SegmentPlanningResult success(SegmentRoutePlan plan) {
            return new SegmentPlanningResult(true, plan, null);
        }

        public static SegmentPlanningResult failed(String error) {
            return new SegmentPlanningResult(false, null, error);
        }
    }

    public SegmentPlanningResult planSegment(int index,
                                             AmapRouteUtil.RoutePoint from,
                                             AmapRouteUtil.RoutePoint to,
                                             NavigationSegmentSlots segmentSlots,
                                             NavigationSlots slots) {
        Map<String, Object> segmentRoute = amapRouteUtil.walkingRouteForDecision(from.lng(), from.lat(), to.lng(), to.lat());
        if (!Boolean.TRUE.equals(segmentRoute.get("success"))) {
            return SegmentPlanningResult.failed(String.valueOf(segmentRoute.getOrDefault("error", "多段路线规划失败")));
        }

        List<Map<String, Object>> paths = geometryAssemblerService.castMapList(segmentRoute.get("paths"));
        log.info("多段路线 segment={} v5 决策结果: from={} to={}, candidateRoutes={}",
                index, from.name(), to.name(), paths.isEmpty() ? 1 : paths.size());

        RouteSelectionService.SegmentRouteSelection selection = routeSelectionService
                .selectSegmentRoute(paths, segmentSlots, slots);
        int selectedRouteIndex = selection.selectedRouteIndex();
        String selectionReason = selection.selectionReason();
        List<Map<String, Object>> alternativeRoutes = selection.alternativeRoutes();
        Map<String, Object> selectedPath = selection.selectedPath() != null ? selection.selectedPath() : segmentRoute;

        log.info("多段路线 segment={} v5 选路完成: selectedRouteIndex={}, candidateRoutes={}, reason={}",
                index, selectedRouteIndex, paths.isEmpty() ? 1 : paths.size(), selectionReason);

        int distance = ((Number) selectedPath.getOrDefault("distance", 0)).intValue();
        int duration = ((Number) selectedPath.getOrDefault("duration", 0)).intValue();
        List<Map<String, Object>> segmentSteps = geometryAssemblerService.castStepList(selectedPath.get("steps"));
        List<List<Double>> segmentPath = geometryAssemblerService.castPathList(selectedPath.get("path"));
        String apiVersion = segmentRoute.get("amapApiVersion") != null
                ? String.valueOf(segmentRoute.get("amapApiVersion")) : "v5";
        String segmentGeoSource = geometryAssemblerService.geometrySourceFromApiVersion(apiVersion);
        String geometryFallbackReason = null;
        if (segmentPath.size() <= 1) {
            log.warn("多段路线 segment={} v5 候选 {} 未携带可绘制几何，pathPoints={}",
                    index, selectedRouteIndex, segmentPath.size());
            segmentGeoSource = GeometryAssemblerService.GEOMETRY_SOURCE_UNAVAILABLE;
            geometryFallbackReason = "第" + (index + 1) + "段: v5 候选路线未返回可绘制几何";
        }

        log.info("多段路线原始分段轨迹: segmentIndex={}, routeIndex={}, pathPoints={}, firstPoints={}",
                index, selectedRouteIndex, segmentPath.size(), segmentPath.stream().limit(2).toList());

        SegmentRoutePlan plan = new SegmentRoutePlan(
                index,
                from,
                to,
                distance,
                duration,
                segmentSteps,
                segmentPath,
                segmentGeoSource,
                geometryFallbackReason,
                selectedRouteIndex,
                selectionReason,
                paths.isEmpty() ? 1 : paths.size(),
                alternativeRoutes,
                segmentRoute.get("amapApiVersion") != null ? String.valueOf(segmentRoute.get("amapApiVersion")) : null,
                segmentRoute.get("routingStrategy") != null ? String.valueOf(segmentRoute.get("routingStrategy")) : null
        );
        return SegmentPlanningResult.success(plan);
    }
}
