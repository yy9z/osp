package com.caspar.agent.service;

import com.caspar.agent.model.NavigationSlots;
import com.caspar.agent.model.NavigationTaskResponse;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteResponseAssemblerServiceTest {

    private final RouteResponseAssemblerService service = new RouteResponseAssemblerService();

    @Test
    void assemble_shouldIncludeAlternativeRoutesWhenProvided() {
        NavigationSlots slots = new NavigationSlots();
        Map<String, Object> routeResult = new LinkedHashMap<>();
        routeResult.put("distance", 180);
        routeResult.put("duration", 110);
        routeResult.put("steps", List.of(Map.of("instruction", "walk")));
        routeResult.put("path", List.of(List.of(117.0, 31.0), List.of(117.1, 31.1)));
        routeResult.put("amapApiVersion", "v5");
        routeResult.put("routingStrategy", "alternative_route=3");

        NavigationTaskResponse.OverallAnalysis overall = NavigationTaskResponse.OverallAnalysis.builder()
                .overallScore(8)
                .summary("路线较优")
                .keyPoints(List.of("更快"))
                .recommendation("按当前路线前往")
                .build();

        NavigationTaskResponse.Route route = service.assemble(
                new RouteResponseAssemblerService.RouteAssemblyInput(
                        slots,
                        routeResult,
                        List.of(),
                        null,
                        overall,
                        List.of(Map.of("index", 0, "selected", false), Map.of("index", 1, "selected", true)),
                        1,
                        "路线2更优",
                        "amap_v5",
                        null
                )
        );

        assertTrue(route.isReady());
        assertEquals("single", route.getRouteType());
        assertEquals(180, route.getDistance());
        assertEquals(110, route.getDuration());
        assertEquals(1, route.getSelectedRouteIndex());
        assertEquals(2, route.getTotalRoutes());
        assertEquals("路线2更优", route.getSelectionReason());
        assertNotNull(route.getAlternativeRoutes());
        assertEquals(2, route.getAlternativeRoutes().size());
        assertEquals("amap_v5", route.getGeometrySource());
    }

    @Test
    void assemble_shouldHandleMultiWaypointWithoutAlternatives() {
        NavigationSlots slots = new NavigationSlots();
        slots.addWaypoint("图书馆");

        Map<String, Object> routeResult = new LinkedHashMap<>();
        routeResult.put("distance", 500);
        routeResult.put("duration", 300);
        routeResult.put("steps", List.of());
        routeResult.put("path", List.of(List.of(117.0, 31.0)));

        List<NavigationTaskResponse.RouteSegment> segments = List.of(
                NavigationTaskResponse.RouteSegment.builder().index(0).build()
        );

        NavigationTaskResponse.Route route = service.assemble(
                new RouteResponseAssemblerService.RouteAssemblyInput(
                        slots,
                        routeResult,
                        segments,
                        null,
                        null,
                        null,
                        0,
                        null,
                        "unavailable",
                        "test fallback"
                )
        );

        assertEquals("multi_waypoint", route.getRouteType());
        assertNotNull(route.getSegments());
        assertEquals(1, route.getSegments().size());
        assertNull(route.getAlternativeRoutes());
        assertNull(route.getSelectedRouteIndex());
        assertEquals("test fallback", route.getGeometryFallbackReason());
    }
}
