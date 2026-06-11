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
 * 路线规划入口服务：负责单段/多段规划分支调度与规划结果返回。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoutePlanningGatewayService {

    private final AmapRouteUtil amapRouteUtil;
    private final SegmentNavigationSlotService segmentNavigationSlotService;
    private final WaypointService waypointService;
    private final MultiSegmentRouteAssemblerService multiSegmentRouteAssemblerService;

    public record RoutePlanningResult(
            Map<String, Object> routeResult,
            List<NavigationSegmentSlots> segmentSlotProfiles
    ) {
    }

    public RoutePlanningResult plan(NavigationSlots slots, ResolvedNavigation resolvedNavigation) {
        AmapPlaceSearchService.ResolvedPlace origin = resolvedNavigation.origin();
        if (slots.isMultiWaypoint()) {
            log.info("执行多途经点路径规划，途经点数量: {}", slots.getWaypoints().size());
            List<AmapRouteUtil.RoutePoint> itinerary = waypointService.buildItinerary(origin, resolvedNavigation);
            log.info("多途经点行程: {}", itinerary.stream().map(AmapRouteUtil.RoutePoint::name).toList());

            List<NavigationSegmentSlots> segmentSlotProfiles = segmentNavigationSlotService.extractSegmentSlots(
                    slots.getOriginalQuery() != null ? slots.getOriginalQuery() : slots.getDestination(),
                    itinerary,
                    slots
            );
            Map<String, Object> routeResult = multiSegmentRouteAssemblerService.assemble(itinerary, segmentSlotProfiles, slots);
            return new RoutePlanningResult(routeResult, segmentSlotProfiles);
        }

        log.info("执行单段路径规划");
        AmapPlaceSearchService.ResolvedPlace destination = resolvedNavigation.destination();
        Map<String, Object> routeResult = amapRouteUtil.walkingRouteForDecision(
                origin.getLng(),
                origin.getLat(),
                destination.getLng(),
                destination.getLat()
        );
        return new RoutePlanningResult(routeResult, List.of());
    }
}
