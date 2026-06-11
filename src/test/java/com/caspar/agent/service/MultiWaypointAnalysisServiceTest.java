package com.caspar.agent.service;

import com.caspar.agent.model.NavigationSegmentSlots;
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

class MultiWaypointAnalysisServiceTest {

    @Test
    void analyze_shouldBuildSegmentsAndOverallAnalysis() {
        SegmentAnalysisService segmentAnalysisService = mock(SegmentAnalysisService.class);
        RouteAnalysisService routeAnalysisService = mock(RouteAnalysisService.class);
        MultiWaypointAnalysisService service = new MultiWaypointAnalysisService(
                segmentAnalysisService,
                routeAnalysisService,
                new GeometryAssemblerService()
        );

        NavigationSlots slots = new NavigationSlots();
        List<NavigationSegmentSlots> segmentSlotProfiles = List.of(
                NavigationSegmentSlots.builder().index(0).build(),
                NavigationSegmentSlots.builder().index(1).build()
        );

        NavigationTaskResponse.RouteSegment segment0 = NavigationTaskResponse.RouteSegment.builder()
                .index(0)
                .path(List.of(List.of(117.0, 31.0), List.of(117.1, 31.1)))
                .analysis(NavigationTaskResponse.SegmentAnalysis.builder()
                        .score(7).reason("段0稳定").warnings(List.of()).suggestions(List.of("可通行")).build())
                .build();
        NavigationTaskResponse.RouteSegment segment1 = NavigationTaskResponse.RouteSegment.builder()
                .index(1)
                .path(List.of(List.of(117.1, 31.1), List.of(117.2, 31.2)))
                .analysis(NavigationTaskResponse.SegmentAnalysis.builder()
                        .score(8).reason("段1更优").warnings(List.of()).suggestions(List.of("建议优先")).build())
                .build();

        when(segmentAnalysisService.buildRouteSegmentsWithAnalysis(any(), eq(slots), eq(segmentSlotProfiles)))
                .thenReturn(List.of(segment0, segment1));
        when(routeAnalysisService.analyzeOverall(any(), eq(slots)))
                .thenReturn(RouteAnalysisService.OverallAnalysis.builder()
                        .overallScore(8)
                        .summary("总体较优")
                        .keyPoints(List.of("路径连续"))
                        .recommendation("按当前路线前往")
                        .build());

        Map<String, Object> routeResult = new LinkedHashMap<>();
        routeResult.put("segments", List.of(Map.of("index", 0), Map.of("index", 1)));
        routeResult.put("path", List.of(
                List.of(117.0, 31.0),
                List.of(117.1, 31.1),
                List.of(117.2, 31.2)
        ));

        MultiWaypointAnalysisService.MultiWaypointAnalysisResult result =
                service.analyze(routeResult, slots, segmentSlotProfiles);

        assertEquals(2, result.routeSegments().size());
        assertNotNull(result.overallAnalysis());
        assertEquals(8, result.overallAnalysis().getOverallScore());
        assertEquals("总体较优", result.overallAnalysis().getSummary());
    }

    @Test
    void analyze_shouldSkipOverallWhenNoValidSegmentAnalysis() {
        SegmentAnalysisService segmentAnalysisService = mock(SegmentAnalysisService.class);
        RouteAnalysisService routeAnalysisService = mock(RouteAnalysisService.class);
        MultiWaypointAnalysisService service = new MultiWaypointAnalysisService(
                segmentAnalysisService,
                routeAnalysisService,
                new GeometryAssemblerService()
        );

        NavigationSlots slots = new NavigationSlots();
        when(segmentAnalysisService.buildRouteSegmentsWithAnalysis(any(), eq(slots), eq(List.of())))
                .thenReturn(List.of(
                        NavigationTaskResponse.RouteSegment.builder()
                                .index(0)
                                .path(List.of(List.of(117.0, 31.0)))
                                .analysis(null)
                                .build()
                ));

        Map<String, Object> routeResult = new LinkedHashMap<>();
        routeResult.put("segments", List.of(Map.of("index", 0)));
        routeResult.put("path", List.of(List.of(117.0, 31.0), List.of(117.1, 31.1)));

        MultiWaypointAnalysisService.MultiWaypointAnalysisResult result =
                service.analyze(routeResult, slots, List.of());

        assertEquals(1, result.routeSegments().size());
        assertNull(result.overallAnalysis());
        verify(routeAnalysisService, never()).analyzeOverall(any(), eq(slots));
    }
}
