package com.caspar.agent.service;

import com.caspar.agent.model.NavigationSlots;
import com.caspar.agent.model.NavigationTaskResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 单段路线决策服务：负责选路、几何选择与分析回退。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SingleRouteDecisionService {

    private final RouteSelectionService routeSelectionService;
    private final RouteAnalysisService routeAnalysisService;
    private final GeometryAssemblerService geometryAssemblerService;

    public record SingleRouteDecision(
            Map<String, Object> routeResult,
            NavigationTaskResponse.SafetyAnalysis safetyAnalysis,
            NavigationTaskResponse.OverallAnalysis overallAnalysis,
            List<Map<String, Object>> alternativeRoutes,
            int selectedRouteIndex,
            String selectionReason,
            String geometrySource,
            String geometryFallbackReason
    ) {
    }

    public SingleRouteDecision decide(Map<String, Object> routeResult, NavigationSlots slots) {
        List<Map<String, Object>> paths = geometryAssemblerService.castMapList(routeResult.get("paths"));
        int pathCount = routeResult.get("pathCount") != null ? ((Number) routeResult.get("pathCount")).intValue() : 1;
        Map<String, Object> selectedPath = routeResult;
        boolean shouldAnalyzeSingleRoute = routeSelectionService.needsSceneAnalysis(slots);
        List<RouteAnalysisService.RouteScore> scoredRoutes = List.of();
        NavigationTaskResponse.SafetyAnalysis safetyAnalysis = null;
        NavigationTaskResponse.OverallAnalysis overallAnalysis = null;
        List<Map<String, Object>> alternativeRoutes = null;
        int selectedRouteIndex = 0;
        String routeSelectionReason = null;

        if (paths != null && !paths.isEmpty()) {
            RouteSelectionService.SingleRouteSelection selection = routeSelectionService
                    .selectSingleRoute(paths, slots, shouldAnalyzeSingleRoute);
            if (selection.selectedPath() != null) {
                selectedPath = selection.selectedPath();
            }
            selectedRouteIndex = selection.selectedRouteIndex();
            routeSelectionReason = selection.selectionReason();
            alternativeRoutes = selection.alternativeRoutes();
            safetyAnalysis = selection.safetyAnalysis();
            overallAnalysis = selection.overallAnalysis();
            scoredRoutes = selection.scoredRoutes();

            if (paths.size() > 1) {
                log.info("LLM选择路线{} / {}: {}", selectedRouteIndex + 1, pathCount, routeSelectionReason);
            } else {
                log.info("单段路线 v5 决策结果: candidateRoutes={}, selectedRouteIndex={}", pathCount, selectedRouteIndex);
            }
        } else {
            log.info("单段路线 v5 决策结果为空，使用 routeResult 作为默认路线: candidateRoutes={}", pathCount);
        }

        List<List<Double>> selectedGeometry = geometryAssemblerService.castPathList(selectedPath.get("path"));
        String apiVersion = routeResult.get("amapApiVersion") != null
                ? String.valueOf(routeResult.get("amapApiVersion")) : "v5";
        String geometrySource = geometryAssemblerService.geometrySourceFromApiVersion(apiVersion);
        String geometryFallbackReason = null;
        if (selectedGeometry.size() <= 1) {
            log.warn("单段路线 v5 候选 {} 未携带可绘制几何，pathPoints={}",
                    selectedRouteIndex, selectedGeometry.size());
            geometrySource = GeometryAssemblerService.GEOMETRY_SOURCE_UNAVAILABLE;
            geometryFallbackReason = "v5 候选路线未返回可绘制几何";
        }

        Map<String, Object> normalizedRouteResult = new LinkedHashMap<>(routeResult);
        normalizedRouteResult.put("distance", selectedPath.get("distance"));
        normalizedRouteResult.put("duration", selectedPath.get("duration"));
        normalizedRouteResult.put("steps", selectedPath.get("steps"));
        normalizedRouteResult.put("path", selectedGeometry);
        normalizedRouteResult.put("geometrySource", geometrySource);
        normalizedRouteResult.put("geometryFallbackReason", geometryFallbackReason);
        log.info("单段路线使用 v5 候选几何: source={}, pathPoints={}", geometrySource, selectedGeometry.size());

        if (shouldAnalyzeSingleRoute && safetyAnalysis == null) {
            log.info("路线分析触发: scene={}, preferences={}, timeContext={}",
                    slots.getTaskScene(), slots.getPreferences(), slots.getTimeContext());

            RouteAnalysisService.RouteAnalysisResult analysis = routeAnalysisService.analyze(
                    normalizedRouteResult,
                    slots.getTaskScene(),
                    slots.getPreferences(),
                    slots.getTimeContext(),
                    slots.getTimeSlot()
            );

            if (analysis != null) {
                safetyAnalysis = NavigationTaskResponse.SafetyAnalysis.builder()
                        .safetyScore(analysis.getScore())
                        .warnings(analysis.getWarnings())
                        .suggestions(analysis.getSuggestions())
                        .hasAlternative(analysis.isRecommendAlternative())
                        .alternativeSummary(analysis.getAlternativeDescription())
                        .build();

                List<String> keyPoints = new ArrayList<>();
                if (analysis.getWarnings() != null) {
                    keyPoints.addAll(analysis.getWarnings());
                }
                if (analysis.getSuggestions() != null) {
                    keyPoints.addAll(analysis.getSuggestions());
                }

                String overallSummary = analysis.getReason() != null && !analysis.getReason().isBlank()
                        ? analysis.getReason()
                        : "路线已结合当前场景完成评估";
                String recommendation = analysis.getAlternativeDescription();
                if (recommendation == null || recommendation.isBlank()) {
                    recommendation = analysis.getSuggestions() != null && !analysis.getSuggestions().isEmpty()
                            ? analysis.getSuggestions().get(0)
                            : "可按当前路线前往";
                }

                overallAnalysis = NavigationTaskResponse.OverallAnalysis.builder()
                        .overallScore(analysis.getScore())
                        .summary(overallSummary)
                        .keyPoints(keyPoints.stream().limit(3).toList())
                        .recommendation(recommendation)
                        .build();
            }
        }

        if (overallAnalysis == null) {
            final int finalSelectedRouteIndex = selectedRouteIndex;
            RouteAnalysisService.RouteScore selectedScore = scoredRoutes.stream()
                    .filter(score -> score.getIndex() == finalSelectedRouteIndex)
                    .findFirst()
                    .orElse(null);
            overallAnalysis = routeSelectionService.buildFallbackOverallAnalysis(selectedPath, selectedScore);
        }

        return new SingleRouteDecision(
                normalizedRouteResult,
                safetyAnalysis,
                overallAnalysis,
                alternativeRoutes,
                selectedRouteIndex,
                routeSelectionReason,
                geometrySource,
                geometryFallbackReason
        );
    }
}
