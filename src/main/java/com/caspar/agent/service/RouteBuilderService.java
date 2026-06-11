package com.caspar.agent.service;

import com.caspar.agent.model.NavigationSegmentSlots;
import com.caspar.agent.model.NavigationSlots;
import com.caspar.agent.model.NavigationTaskResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 路线构建服务：负责路线规划、分段评估与路线响应组装。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RouteBuilderService {

    private final RoutePlanningGatewayService routePlanningGatewayService;
    private final SingleRouteDecisionService singleRouteDecisionService;
    private final MultiWaypointAnalysisService multiWaypointAnalysisService;
    private final RouteResponseAssemblerService routeResponseAssemblerService;

    public NavigationTaskResponse.Route buildRoute(NavigationSlots slots, ResolvedNavigation resolvedNavigation) {
        log.info("buildRoute开始: isMultiWaypoint={}, waypoints={}, userLat={}, userLng={}",
                slots.isMultiWaypoint(), slots.getWaypoints(), slots.getUserLat(), slots.getUserLng());

        AmapPlaceSearchService.ResolvedPlace origin = resolvedNavigation.origin();
        if (origin == null) {
            log.warn("无法规划路线：缺少可用起点，origin={}", slots.getOrigin());
            return NavigationTaskResponse.Route.builder()
                    .ready(false)
                    .routeType(slots.isMultiWaypoint() ? "multi_waypoint" : "single")
                    .build();
        }

        try {
            RoutePlanningGatewayService.RoutePlanningResult planningResult =
                    routePlanningGatewayService.plan(slots, resolvedNavigation);
            Map<String, Object> routeResult = planningResult.routeResult();
            List<NavigationSegmentSlots> segmentSlotProfiles = planningResult.segmentSlotProfiles();

            if (!Boolean.TRUE.equals(routeResult.get("success"))) {
                return NavigationTaskResponse.Route.builder()
                        .ready(false)
                        .routeType(slots.isMultiWaypoint() ? "multi_waypoint" : "single")
                        .build();
            }

            NavigationTaskResponse.SafetyAnalysis safetyAnalysis = null;
            NavigationTaskResponse.OverallAnalysis overallAnalysis = null;
            List<NavigationTaskResponse.RouteSegment> routeSegments = List.of();
            List<Map<String, Object>> alternativeRoutes = null;
            int selectedRouteIndex = 0;
            String routeSelectionReason = null;
            String geometrySource = routeResult.get("geometrySource") != null ? String.valueOf(routeResult.get("geometrySource")) : null;
            String geometryFallbackReason = routeResult.get("geometryFallbackReason") != null
                    ? String.valueOf(routeResult.get("geometryFallbackReason"))
                    : null;

            if (slots.isMultiWaypoint()) {
                MultiWaypointAnalysisService.MultiWaypointAnalysisResult analysisResult =
                        multiWaypointAnalysisService.analyze(routeResult, slots, segmentSlotProfiles);
                routeSegments = analysisResult.routeSegments();
                overallAnalysis = analysisResult.overallAnalysis();
            } else {
                SingleRouteDecisionService.SingleRouteDecision decision =
                        singleRouteDecisionService.decide(routeResult, slots);
                routeResult = decision.routeResult();
                selectedRouteIndex = decision.selectedRouteIndex();
                routeSelectionReason = decision.selectionReason();
                alternativeRoutes = decision.alternativeRoutes();
                safetyAnalysis = decision.safetyAnalysis();
                overallAnalysis = decision.overallAnalysis();
                geometrySource = decision.geometrySource();
                geometryFallbackReason = decision.geometryFallbackReason();
            }

            return routeResponseAssemblerService.assemble(
                    new RouteResponseAssemblerService.RouteAssemblyInput(
                            slots,
                            routeResult,
                            routeSegments,
                            safetyAnalysis,
                            overallAnalysis,
                            alternativeRoutes,
                            selectedRouteIndex,
                            routeSelectionReason,
                            geometrySource,
                            geometryFallbackReason
                    )
            );
        } catch (Exception e) {
            log.error("路线规划失败", e);
            return NavigationTaskResponse.Route.builder()
                    .ready(false)
                    .routeType(slots.isMultiWaypoint() ? "multi_waypoint" : "single")
                    .build();
        }
    }
}
