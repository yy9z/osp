package com.caspar.agent.service;

import com.caspar.agent.model.NavigationSegmentSlots;
import com.caspar.agent.model.NavigationSlots;
import com.caspar.agent.model.NavigationTaskResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 多途经点分析服务：负责段分析组装、综合分析与几何诊断日志。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultiWaypointAnalysisService {

    private final SegmentAnalysisService segmentAnalysisService;
    private final RouteAnalysisService routeAnalysisService;
    private final GeometryAssemblerService geometryAssemblerService;

    public record MultiWaypointAnalysisResult(
            List<NavigationTaskResponse.RouteSegment> routeSegments,
            NavigationTaskResponse.OverallAnalysis overallAnalysis
    ) {
    }

    public MultiWaypointAnalysisResult analyze(Map<String, Object> routeResult,
                                               NavigationSlots slots,
                                               List<NavigationSegmentSlots> segmentSlotProfiles) {
        List<Map<String, Object>> rawSegments = geometryAssemblerService.castMapList(routeResult.get("segments"));
        List<NavigationTaskResponse.RouteSegment> routeSegments = segmentAnalysisService
                .buildRouteSegmentsWithAnalysis(rawSegments, slots, segmentSlotProfiles);

        List<RouteAnalysisService.SegmentAnalysis> segmentAnalyses = routeSegments.stream()
                .map(NavigationTaskResponse.RouteSegment::getAnalysis)
                .map(analysis -> analysis != null ? RouteAnalysisService.SegmentAnalysis.builder()
                        .score(analysis.getScore())
                        .reason(analysis.getReason())
                        .warnings(analysis.getWarnings())
                        .suggestions(analysis.getSuggestions())
                        .build() : null)
                .toList();

        List<RouteAnalysisService.SegmentAnalysis> validAnalyses = segmentAnalyses.stream()
                .filter(Objects::nonNull)
                .toList();

        NavigationTaskResponse.OverallAnalysis overallAnalysis = null;
        if (!validAnalyses.isEmpty()) {
            RouteAnalysisService.OverallAnalysis overall = routeAnalysisService.analyzeOverall(validAnalyses, slots);
            if (overall != null) {
                overallAnalysis = NavigationTaskResponse.OverallAnalysis.builder()
                        .overallScore(overall.getOverallScore())
                        .summary(overall.getSummary())
                        .keyPoints(overall.getKeyPoints())
                        .recommendation(overall.getRecommendation())
                        .build();
                log.info("综合分析结果: overallScore={}, summary={}", overall.getOverallScore(), overall.getSummary());
            }
        }

        int totalPathPoints = geometryAssemblerService.castPathList(routeResult.get("path")).size();
        List<Integer> segmentPathPoints = routeSegments.stream()
                .map(segment -> segment.getPath() == null ? 0 : segment.getPath().size())
                .toList();
        log.info("多段路线几何诊断: totalPathPoints={}, segmentPathPoints={}",
                totalPathPoints, segmentPathPoints);
        log.info("发送前端的多段路线几何样本: totalPathSample={}, segmentPathSample={}",
                geometryAssemblerService.castPathList(routeResult.get("path")).stream().limit(2).toList(),
                routeSegments.stream()
                        .limit(2)
                        .map(segment -> segment.getPath() == null ? List.of() : segment.getPath().stream().limit(2).toList())
                        .toList());
        if (totalPathPoints > 1 && segmentPathPoints.stream().anyMatch(size -> size <= 1)) {
            log.warn("检测到分段路径缺失，但总路径可用，前端应回退到总路径展示");
        }

        return new MultiWaypointAnalysisResult(routeSegments, overallAnalysis);
    }
}
