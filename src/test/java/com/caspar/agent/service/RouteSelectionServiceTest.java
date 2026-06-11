package com.caspar.agent.service;

import com.caspar.agent.model.NavigationSlots;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RouteSelectionServiceTest {

    @Test
    void needsSceneAnalysis_shouldBeTrueForLateNight() {
        RouteSelectionService service = new RouteSelectionService(mock(RouteAnalysisService.class));
        NavigationSlots slots = new NavigationSlots();
        slots.setTimeSlot("late_night");

        assertTrue(service.needsSceneAnalysis(slots));
    }

    @Test
    void selectSingleRoute_shouldUseSelectionResultForFixedPaths() {
        RouteAnalysisService routeAnalysisService = mock(RouteAnalysisService.class);
        RouteSelectionService service = new RouteSelectionService(routeAnalysisService);
        NavigationSlots slots = new NavigationSlots();

        List<Map<String, Object>> paths = List.of(
                Map.of("distance", 1200, "duration", 900, "summary", "路线A"),
                Map.of("distance", 900, "duration", 780, "summary", "路线B")
        );

        RouteAnalysisService.RouteSelectionResult selectionResult = RouteAnalysisService.RouteSelectionResult.builder()
                .selectedIndex(1)
                .reason("路线B更短且更快")
                .allScores(List.of(
                        RouteAnalysisService.RouteScore.builder().index(0).score(6).summary("A").reason("较远").build(),
                        RouteAnalysisService.RouteScore.builder().index(1).score(8).summary("B").reason("更优").build()
                ))
                .build();

        when(routeAnalysisService.selectBestRoute(anyList(), any(NavigationSlots.class))).thenReturn(selectionResult);

        RouteSelectionService.SingleRouteSelection result = service.selectSingleRoute(paths, slots, false);

        assertEquals(1, result.selectedRouteIndex());
        assertEquals(paths.get(1), result.selectedPath());
        assertEquals("路线B更短且更快", result.selectionReason());
        assertNotNull(result.alternativeRoutes());
        assertEquals(2, result.alternativeRoutes().size());
        assertEquals(Boolean.TRUE, result.alternativeRoutes().get(1).get("selected"));
    }

    @Test
    void selectSingleRoute_shouldBuildFallbackOverallWhenNoPerRouteAnalysis() {
        RouteAnalysisService routeAnalysisService = mock(RouteAnalysisService.class);
        RouteSelectionService service = new RouteSelectionService(routeAnalysisService);
        NavigationSlots slots = new NavigationSlots();

        List<Map<String, Object>> paths = List.of(
                Map.of("distance", 1500, "duration", 1200, "summary", "路线A"),
                Map.of("distance", 1000, "duration", 840, "summary", "路线B")
        );

        RouteAnalysisService.RouteSelectionResult selectionResult = RouteAnalysisService.RouteSelectionResult.builder()
                .selectedIndex(1)
                .reason("选择路线B")
                .allScores(List.of(
                        RouteAnalysisService.RouteScore.builder().index(0).score(5).summary("A").reason("一般").build(),
                        RouteAnalysisService.RouteScore.builder().index(1).score(9).summary("B").reason("最优").build()
                ))
                .build();
        when(routeAnalysisService.selectBestRoute(anyList(), any(NavigationSlots.class))).thenReturn(selectionResult);

        RouteSelectionService.SingleRouteSelection result = service.selectSingleRoute(paths, slots, false);

        assertNotNull(result.overallAnalysis());
        assertEquals(9, result.overallAnalysis().getOverallScore());
        assertEquals("最优", result.overallAnalysis().getSummary());
    }
}
