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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SingleRouteDecisionServiceTest {

    @Test
    void decide_shouldApplySelectionAndKeepGeometryFromSelectedPath() {
        RouteSelectionService routeSelectionService = mock(RouteSelectionService.class);
        RouteAnalysisService routeAnalysisService = mock(RouteAnalysisService.class);
        SingleRouteDecisionService service = new SingleRouteDecisionService(
                routeSelectionService,
                routeAnalysisService,
                new GeometryAssemblerService()
        );

        NavigationSlots slots = new NavigationSlots();
        Map<String, Object> candidateA = path(220, 140, List.of(List.of(117.0, 31.0), List.of(117.1, 31.1)));
        Map<String, Object> candidateB = path(180, 110, List.of(List.of(117.0, 31.0), List.of(117.05, 31.05), List.of(117.1, 31.1)));

        Map<String, Object> routeResult = new LinkedHashMap<>();
        routeResult.put("success", true);
        routeResult.put("pathCount", 2);
        routeResult.put("paths", List.of(candidateA, candidateB));
        routeResult.put("amapApiVersion", "v5");
        routeResult.put("routingStrategy", "alternative_route=3");

        NavigationTaskResponse.SafetyAnalysis safety = NavigationTaskResponse.SafetyAnalysis.builder()
                .safetyScore(8)
                .warnings(List.of())
                .suggestions(List.of("可按当前路线前往"))
                .hasAlternative(false)
                .build();
        NavigationTaskResponse.OverallAnalysis overall = NavigationTaskResponse.OverallAnalysis.builder()
                .overallScore(8)
                .summary("路线B更优")
                .keyPoints(List.of("更短"))
                .recommendation("按路线B前往")
                .build();
        RouteAnalysisService.RouteScore selectedScore = RouteAnalysisService.RouteScore.builder()
                .index(1).score(8).summary("B").reason("更短").build();

        when(routeSelectionService.needsSceneAnalysis(slots)).thenReturn(false);
        when(routeSelectionService.selectSingleRoute(any(), eq(slots), eq(false)))
                .thenReturn(new RouteSelectionService.SingleRouteSelection(
                        candidateB,
                        1,
                        "路线B更短",
                        List.of(Map.of("index", 0, "selected", false), Map.of("index", 1, "selected", true)),
                        safety,
                        overall,
                        List.of(selectedScore)
                ));

        SingleRouteDecisionService.SingleRouteDecision decision = service.decide(routeResult, slots);

        assertEquals(1, decision.selectedRouteIndex());
        assertEquals("路线B更短", decision.selectionReason());
        assertEquals(8, decision.safetyAnalysis().getSafetyScore());
        assertEquals(8, decision.overallAnalysis().getOverallScore());
        assertEquals(2, decision.alternativeRoutes().size());
        assertEquals(180, ((Number) decision.routeResult().get("distance")).intValue());
        assertEquals(110, ((Number) decision.routeResult().get("duration")).intValue());
        assertEquals(GeometryAssemblerService.GEOMETRY_SOURCE_AMAP_V5, decision.geometrySource());
        assertNull(decision.geometryFallbackReason());
        verify(routeAnalysisService, never()).analyze(any(), any(), any(), any(), any());
    }

    @Test
    void decide_shouldUseFallbackOverallWhenNoSelectionAndAnalysisFails() {
        RouteSelectionService routeSelectionService = mock(RouteSelectionService.class);
        RouteAnalysisService routeAnalysisService = mock(RouteAnalysisService.class);
        SingleRouteDecisionService service = new SingleRouteDecisionService(
                routeSelectionService,
                routeAnalysisService,
                new GeometryAssemblerService()
        );

        NavigationSlots slots = new NavigationSlots();
        slots.setTimeSlot("late_night");
        Map<String, Object> routeResult = new LinkedHashMap<>();
        routeResult.put("success", true);
        routeResult.put("distance", 120);
        routeResult.put("duration", 90);
        routeResult.put("steps", List.of(Map.of("instruction", "walk")));
        routeResult.put("path", List.of(List.of(117.0, 31.0)));
        routeResult.put("amapApiVersion", "v5");

        NavigationTaskResponse.OverallAnalysis fallbackOverall = NavigationTaskResponse.OverallAnalysis.builder()
                .overallScore(6)
                .summary("fallback")
                .keyPoints(List.of("默认评估"))
                .recommendation("按当前路线前往")
                .build();

        when(routeSelectionService.needsSceneAnalysis(slots)).thenReturn(true);
        when(routeAnalysisService.analyze(any(), any(), any(), any(), any())).thenReturn(null);
        when(routeSelectionService.buildFallbackOverallAnalysis(any(), eq(null))).thenReturn(fallbackOverall);

        SingleRouteDecisionService.SingleRouteDecision decision = service.decide(routeResult, slots);

        assertEquals(0, decision.selectedRouteIndex());
        assertNull(decision.selectionReason());
        assertNull(decision.safetyAnalysis());
        assertNotNull(decision.overallAnalysis());
        assertEquals(6, decision.overallAnalysis().getOverallScore());
        assertEquals(GeometryAssemblerService.GEOMETRY_SOURCE_UNAVAILABLE, decision.geometrySource());
        assertEquals("v5 候选路线未返回可绘制几何", decision.geometryFallbackReason());
        assertEquals(GeometryAssemblerService.GEOMETRY_SOURCE_UNAVAILABLE, decision.routeResult().get("geometrySource"));
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
