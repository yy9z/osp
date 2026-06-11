package com.caspar.agent.service;

import com.caspar.agent.model.NavigationSegmentSlots;
import com.caspar.agent.model.NavigationSlots;
import com.caspar.util.AmapRouteUtil;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SegmentRoutePlannerServiceTest {

    @Test
    void planSegment_shouldReturnFailureWhenRouteRequestFails() {
        AmapRouteUtil amapRouteUtil = mock(AmapRouteUtil.class);
        RouteSelectionService routeSelectionService = mock(RouteSelectionService.class);
        SegmentRoutePlannerService service = new SegmentRoutePlannerService(
                amapRouteUtil,
                routeSelectionService,
                new GeometryAssemblerService()
        );

        when(amapRouteUtil.walkingRouteForDecision(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Map.of("success", false, "error", "route failed"));

        SegmentRoutePlannerService.SegmentPlanningResult result = service.planSegment(
                0,
                new AmapRouteUtil.RoutePoint("A", 31.0, 117.0),
                new AmapRouteUtil.RoutePoint("B", 31.1, 117.1),
                NavigationSegmentSlots.builder().index(0).build(),
                new NavigationSlots()
        );

        assertFalse(result.success());
        assertEquals("route failed", result.error());
    }

    @Test
    void planSegment_shouldBuildSegmentPlanFromSelection() {
        AmapRouteUtil amapRouteUtil = mock(AmapRouteUtil.class);
        RouteSelectionService routeSelectionService = mock(RouteSelectionService.class);
        SegmentRoutePlannerService service = new SegmentRoutePlannerService(
                amapRouteUtil,
                routeSelectionService,
                new GeometryAssemblerService()
        );

        Map<String, Object> candidateA = path(220, 140, List.of(List.of(117.0, 31.0), List.of(117.1, 31.1)));
        Map<String, Object> candidateB = path(180, 110, List.of(List.of(117.0, 31.0), List.of(117.08, 31.08), List.of(117.1, 31.1)));
        Map<String, Object> segmentRoute = new LinkedHashMap<>();
        segmentRoute.put("success", true);
        segmentRoute.put("paths", List.of(candidateA, candidateB));
        segmentRoute.put("amapApiVersion", "v5");
        segmentRoute.put("routingStrategy", "alternative_route=3");
        when(amapRouteUtil.walkingRouteForDecision(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(segmentRoute);

        when(routeSelectionService.selectSegmentRoute(anyList(), any(), any(NavigationSlots.class)))
                .thenReturn(new RouteSelectionService.SegmentRouteSelection(
                        candidateB,
                        1,
                        "选择更短路线",
                        List.of(Map.of("index", 0, "selected", false), Map.of("index", 1, "selected", true))
                ));

        SegmentRoutePlannerService.SegmentPlanningResult result = service.planSegment(
                0,
                new AmapRouteUtil.RoutePoint("A", 31.0, 117.0),
                new AmapRouteUtil.RoutePoint("B", 31.1, 117.1),
                NavigationSegmentSlots.builder().index(0).build(),
                new NavigationSlots()
        );

        assertTrue(result.success());
        assertEquals(180, result.plan().distance());
        assertEquals(110, result.plan().duration());
        assertEquals(1, result.plan().selectedRouteIndex());
        assertEquals("选择更短路线", result.plan().selectionReason());
        assertEquals("v5", result.plan().amapApiVersion());
        assertEquals("alternative_route=3", result.plan().routingStrategy());
        assertEquals(GeometryAssemblerService.GEOMETRY_SOURCE_AMAP_V5, result.plan().geometrySource());
    }

    private Map<String, Object> path(int distance, int duration, List<List<Double>> points) {
        Map<String, Object> path = new LinkedHashMap<>();
        path.put("distance", distance);
        path.put("duration", duration);
        path.put("summary", "mock");
        path.put("steps", List.of(Map.of("instruction", "walk", "distance", distance, "duration", duration)));
        path.put("path", points);
        return path;
    }
}
