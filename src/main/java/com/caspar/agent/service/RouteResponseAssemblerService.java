package com.caspar.agent.service;

import com.caspar.agent.model.NavigationSlots;
import com.caspar.agent.model.NavigationTaskResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 路线响应组装服务：将流程中间结果拼装为最终 Route 响应对象。
 */
@Service
public class RouteResponseAssemblerService {

    public record RouteAssemblyInput(
            NavigationSlots slots,
            Map<String, Object> routeResult,
            List<NavigationTaskResponse.RouteSegment> routeSegments,
            NavigationTaskResponse.SafetyAnalysis safetyAnalysis,
            NavigationTaskResponse.OverallAnalysis overallAnalysis,
            List<Map<String, Object>> alternativeRoutes,
            int selectedRouteIndex,
            String selectionReason,
            String geometrySource,
            String geometryFallbackReason
    ) {
    }

    @SuppressWarnings("unchecked")
    public NavigationTaskResponse.Route assemble(RouteAssemblyInput input) {
        NavigationTaskResponse.Route.RouteBuilder routeBuilder = NavigationTaskResponse.Route.builder()
                .ready(true)
                .routeType(input.slots().isMultiWaypoint() ? "multi_waypoint" : "single")
                .distance(((Number) input.routeResult().getOrDefault("distance", 0)).intValue())
                .duration(((Number) input.routeResult().getOrDefault("duration", 0)).intValue())
                .steps((List<Map<String, Object>>) input.routeResult().get("steps"))
                .path((List<List<Double>>) input.routeResult().get("path"))
                .segments(input.routeSegments() == null || input.routeSegments().isEmpty() ? List.of() : input.routeSegments())
                .safetyAnalysis(input.safetyAnalysis())
                .overallAnalysis(input.overallAnalysis())
                .amapApiVersion(input.routeResult().get("amapApiVersion") != null ? String.valueOf(input.routeResult().get("amapApiVersion")) : null)
                .routingStrategy(input.routeResult().get("routingStrategy") != null ? String.valueOf(input.routeResult().get("routingStrategy")) : null)
                .geometrySource(input.geometrySource())
                .geometryFallbackReason(input.geometryFallbackReason());

        if (input.alternativeRoutes() != null && !input.alternativeRoutes().isEmpty()) {
            routeBuilder.selectedRouteIndex(input.selectedRouteIndex())
                    .totalRoutes(input.alternativeRoutes().size())
                    .selectionReason(input.selectionReason())
                    .alternativeRoutes(input.alternativeRoutes());
        }

        return routeBuilder.build();
    }
}
