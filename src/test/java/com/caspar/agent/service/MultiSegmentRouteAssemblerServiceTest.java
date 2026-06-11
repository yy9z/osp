package com.caspar.agent.service;

import com.caspar.agent.model.NavigationSegmentSlots;
import com.caspar.agent.model.NavigationSlots;
import com.caspar.util.AmapRouteUtil;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MultiSegmentRouteAssemblerServiceTest {

    @Test
    void assemble_shouldAggregateSegmentsAndMergeGeometry() {
        AmapRouteUtil amapRouteUtil = mock(AmapRouteUtil.class);
        RouteSelectionService routeSelectionService = mock(RouteSelectionService.class);
        GeometryAssemblerService geometryAssemblerService = new GeometryAssemblerService();
        SegmentRoutePlannerService segmentRoutePlannerService = new SegmentRoutePlannerService(
                amapRouteUtil, routeSelectionService, geometryAssemblerService
        );
        MultiSegmentRouteAggregationService multiSegmentRouteAggregationService = new MultiSegmentRouteAggregationService(
                geometryAssemblerService
        );
        MultiSegmentRouteAssemblerService service = new MultiSegmentRouteAssemblerService(
                segmentRoutePlannerService, multiSegmentRouteAggregationService
        );

        List<AmapRouteUtil.RoutePoint> itinerary = List.of(
                new AmapRouteUtil.RoutePoint("A", 31.0, 117.0),
                new AmapRouteUtil.RoutePoint("B", 31.1, 117.1),
                new AmapRouteUtil.RoutePoint("C", 31.2, 117.2)
        );
        List<NavigationSegmentSlots> segmentSlots = List.of(
                NavigationSegmentSlots.builder().index(0).fromName("A").toName("B").build(),
                NavigationSegmentSlots.builder().index(1).fromName("B").toName("C").build()
        );
        NavigationSlots slots = new NavigationSlots();

        Map<String, Object> firstPath = path(220, 140,
                List.of(List.of(117.0, 31.0), List.of(117.1, 31.1)));
        Map<String, Object> secondPath = path(200, 120,
                List.of(List.of(117.0, 31.0), List.of(117.08, 31.08), List.of(117.1, 31.1)));
        Map<String, Object> segmentRoute0 = segmentRoute(List.of(firstPath, secondPath), "v5", "alternative_route=3");

        Map<String, Object> thirdPath = path(300, 180,
                List.of(List.of(117.1, 31.1), List.of(117.2, 31.2)));
        Map<String, Object> segmentRoute1 = segmentRoute(List.of(thirdPath), "v3", "amap_default");

        when(amapRouteUtil.walkingRouteForDecision(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(segmentRoute0, segmentRoute1);

        List<Map<String, Object>> alt0 = List.of(Map.of("index", 0, "selected", false), Map.of("index", 1, "selected", true));
        List<Map<String, Object>> alt1 = List.of(Map.of("index", 0, "selected", true));
        when(routeSelectionService.selectSegmentRoute(anyList(), any(), any(NavigationSlots.class)))
                .thenReturn(new RouteSelectionService.SegmentRouteSelection(secondPath, 1, "第1段选更优路线", alt0))
                .thenReturn(new RouteSelectionService.SegmentRouteSelection(thirdPath, 0, "第2段默认路线", alt1));

        Map<String, Object> result = service.assemble(itinerary, segmentSlots, slots);

        assertEquals(Boolean.TRUE, result.get("success"));
        assertEquals(500, ((Number) result.get("distance")).intValue());
        assertEquals(300, ((Number) result.get("duration")).intValue());
        assertEquals("v5+v3", result.get("amapApiVersion"));
        assertEquals("alternative_route=3+amap_default", result.get("routingStrategy"));
        assertEquals(GeometryAssemblerService.GEOMETRY_SOURCE_MIXED, result.get("geometrySource"));

        @SuppressWarnings("unchecked")
        List<List<Double>> mergedPath = (List<List<Double>>) result.get("path");
        assertEquals(4, mergedPath.size());
        assertEquals(List.of(117.0, 31.0), mergedPath.get(0));
        assertEquals(List.of(117.2, 31.2), mergedPath.get(3));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> segments = (List<Map<String, Object>>) result.get("segments");
        assertEquals(2, segments.size());
        assertEquals(1, ((Number) segments.get(0).get("selectedRouteIndex")).intValue());
        assertEquals("第1段选更优路线", segments.get(0).get("selectionReason"));
        assertEquals(0, ((Number) segments.get(1).get("selectedRouteIndex")).intValue());
    }

    @Test
    void assemble_shouldReturnFailureWhenAnySegmentFails() {
        AmapRouteUtil amapRouteUtil = mock(AmapRouteUtil.class);
        RouteSelectionService routeSelectionService = mock(RouteSelectionService.class);
        GeometryAssemblerService geometryAssemblerService = new GeometryAssemblerService();
        SegmentRoutePlannerService segmentRoutePlannerService = new SegmentRoutePlannerService(
                amapRouteUtil, routeSelectionService, geometryAssemblerService
        );
        MultiSegmentRouteAggregationService multiSegmentRouteAggregationService = new MultiSegmentRouteAggregationService(
                geometryAssemblerService
        );
        MultiSegmentRouteAssemblerService service = new MultiSegmentRouteAssemblerService(
                segmentRoutePlannerService, multiSegmentRouteAggregationService
        );

        List<AmapRouteUtil.RoutePoint> itinerary = List.of(
                new AmapRouteUtil.RoutePoint("A", 31.0, 117.0),
                new AmapRouteUtil.RoutePoint("B", 31.1, 117.1)
        );

        when(amapRouteUtil.walkingRouteForDecision(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Map.of("success", false, "error", "mock failed"));

        Map<String, Object> result = service.assemble(itinerary, List.of(), new NavigationSlots());

        assertEquals(Boolean.FALSE, result.get("success"));
        assertEquals(0, ((Number) result.get("failedSegmentIndex")).intValue());
        assertEquals("mock failed", result.get("error"));
        assertTrue(!result.containsKey("segments"));
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

    private Map<String, Object> segmentRoute(List<Map<String, Object>> paths, String apiVersion, String strategy) {
        Map<String, Object> route = new LinkedHashMap<>();
        route.put("success", true);
        route.put("paths", paths);
        route.put("amapApiVersion", apiVersion);
        route.put("routingStrategy", strategy);
        return route;
    }
}
