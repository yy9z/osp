package com.caspar.agent.service;

import com.caspar.agent.model.NavigationSegmentSlots;
import com.caspar.agent.model.NavigationSlots;
import com.caspar.util.AmapRouteUtil;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoutePlanningGatewayServiceTest {

    @Test
    void plan_shouldUseSingleRouteWhenNotMultiWaypoint() {
        AmapRouteUtil amapRouteUtil = mock(AmapRouteUtil.class);
        SegmentNavigationSlotService segmentNavigationSlotService = mock(SegmentNavigationSlotService.class);
        WaypointService waypointService = mock(WaypointService.class);
        MultiSegmentRouteAssemblerService multiSegmentRouteAssemblerService = mock(MultiSegmentRouteAssemblerService.class);
        RoutePlanningGatewayService service = new RoutePlanningGatewayService(
                amapRouteUtil,
                segmentNavigationSlotService,
                waypointService,
                multiSegmentRouteAssemblerService
        );

        AmapPlaceSearchService.ResolvedPlace origin = place("o", "起点", 31.0, 117.0);
        AmapPlaceSearchService.ResolvedPlace destination = place("d", "终点", 31.1, 117.1);
        NavigationSlots slots = new NavigationSlots();
        slots.setDestination("终点");
        ResolvedNavigation resolvedNavigation = new ResolvedNavigation(
                origin,
                destination,
                List.of(),
                null,
                List.of(destination),
                null
        );

        Map<String, Object> routeResult = new LinkedHashMap<>();
        routeResult.put("success", true);
        routeResult.put("distance", 200);
        when(amapRouteUtil.walkingRouteForDecision(117.0, 31.0, 117.1, 31.1)).thenReturn(routeResult);

        RoutePlanningGatewayService.RoutePlanningResult result = service.plan(slots, resolvedNavigation);

        assertSame(routeResult, result.routeResult());
        assertEquals(List.of(), result.segmentSlotProfiles());
        verify(amapRouteUtil).walkingRouteForDecision(117.0, 31.0, 117.1, 31.1);
        verify(waypointService, never()).buildItinerary(any(), any());
        verify(multiSegmentRouteAssemblerService, never()).assemble(any(), any(), any());
    }

    @Test
    void plan_shouldUseMultiWaypointPipelineWhenWaypointsPresent() {
        AmapRouteUtil amapRouteUtil = mock(AmapRouteUtil.class);
        SegmentNavigationSlotService segmentNavigationSlotService = mock(SegmentNavigationSlotService.class);
        WaypointService waypointService = mock(WaypointService.class);
        MultiSegmentRouteAssemblerService multiSegmentRouteAssemblerService = mock(MultiSegmentRouteAssemblerService.class);
        RoutePlanningGatewayService service = new RoutePlanningGatewayService(
                amapRouteUtil,
                segmentNavigationSlotService,
                waypointService,
                multiSegmentRouteAssemblerService
        );

        AmapPlaceSearchService.ResolvedPlace origin = place("o", "起点", 31.0, 117.0);
        AmapPlaceSearchService.ResolvedPlace waypoint = place("w1", "图书馆", 31.05, 117.05);
        AmapPlaceSearchService.ResolvedPlace destination = place("d", "终点", 31.1, 117.1);
        NavigationSlots slots = new NavigationSlots();
        slots.setOriginalQuery("先去图书馆再去终点");
        slots.setDestination("终点");
        slots.addWaypoint("图书馆");
        ResolvedNavigation resolvedNavigation = new ResolvedNavigation(
                origin,
                destination,
                List.of(waypoint),
                null,
                List.of(destination),
                null
        );

        List<AmapRouteUtil.RoutePoint> itinerary = List.of(
                new AmapRouteUtil.RoutePoint("起点", 31.0, 117.0),
                new AmapRouteUtil.RoutePoint("图书馆", 31.05, 117.05),
                new AmapRouteUtil.RoutePoint("终点", 31.1, 117.1)
        );
        List<NavigationSegmentSlots> segmentSlots = List.of(
                NavigationSegmentSlots.builder().index(0).build(),
                NavigationSegmentSlots.builder().index(1).build()
        );
        Map<String, Object> routeResult = Map.of("success", true, "distance", 500);

        when(waypointService.buildItinerary(origin, resolvedNavigation)).thenReturn(itinerary);
        when(segmentNavigationSlotService.extractSegmentSlots(eq("先去图书馆再去终点"), eq(itinerary), eq(slots)))
                .thenReturn(segmentSlots);
        when(multiSegmentRouteAssemblerService.assemble(itinerary, segmentSlots, slots)).thenReturn(routeResult);

        RoutePlanningGatewayService.RoutePlanningResult result = service.plan(slots, resolvedNavigation);

        assertSame(routeResult, result.routeResult());
        assertSame(segmentSlots, result.segmentSlotProfiles());
        verify(waypointService).buildItinerary(origin, resolvedNavigation);
        verify(segmentNavigationSlotService).extractSegmentSlots("先去图书馆再去终点", itinerary, slots);
        verify(multiSegmentRouteAssemblerService).assemble(itinerary, segmentSlots, slots);
        verify(amapRouteUtil, never()).walkingRouteForDecision(anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }

    private AmapPlaceSearchService.ResolvedPlace place(String id, String name, double lat, double lng) {
        return new AmapPlaceSearchService.ResolvedPlace(
                id,
                name,
                name + "地址",
                lat,
                lng,
                "东校区",
                "mock",
                null
        );
    }
}
