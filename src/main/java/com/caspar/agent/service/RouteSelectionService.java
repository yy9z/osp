package com.caspar.agent.service;

import com.caspar.agent.model.NavigationSegmentSlots;
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
 * 选路决策服务：负责多候选路线打分、选择与分析结果归并。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RouteSelectionService {

    private final RouteAnalysisService routeAnalysisService;

    public record SingleRouteSelection(
            Map<String, Object> selectedPath,
            int selectedRouteIndex,
            String selectionReason,
            List<Map<String, Object>> alternativeRoutes,
            NavigationTaskResponse.SafetyAnalysis safetyAnalysis,
            NavigationTaskResponse.OverallAnalysis overallAnalysis,
            List<RouteAnalysisService.RouteScore> scoredRoutes
    ) {
    }

    public record SegmentRouteSelection(
            Map<String, Object> selectedPath,
            int selectedRouteIndex,
            String selectionReason,
            List<Map<String, Object>> alternativeRoutes
    ) {
    }

    public boolean needsSceneAnalysis(NavigationSlots slots) {
        if ("night".equals(slots.getTimeContext())) {
            return true;
        }
        if ("late_night".equals(slots.getTimeSlot())) {
            return true;
        }
        if ("return_dorm".equals(slots.getTaskScene())) {
            return true;
        }
        return slots.getPreferences() != null && slots.getPreferences().contains("safeNight");
    }

    public SingleRouteSelection selectSingleRoute(List<Map<String, Object>> paths,
                                                  NavigationSlots slots,
                                                  boolean shouldAnalyze) {
        if (paths == null || paths.isEmpty()) {
            return new SingleRouteSelection(null, 0, null, null, null, null, List.of());
        }

        int selectedRouteIndex = 0;
        String routeSelectionReason = null;
        List<RouteAnalysisService.RouteScore> scoredRoutes = List.of();
        Map<String, Object> selectedPath = paths.get(0);

        if (paths.size() > 1) {
            RouteAnalysisService.RouteSelectionResult selection = routeAnalysisService.selectBestRoute(paths, slots);
            scoredRoutes = selection != null && selection.getAllScores() != null
                    ? selection.getAllScores() : List.of();
            if (selection != null) {
                selectedRouteIndex = Math.max(0, Math.min(selection.getSelectedIndex(), paths.size() - 1));
                routeSelectionReason = selection.getReason();
            }
            selectedPath = paths.get(selectedRouteIndex);
        }

        List<Map<String, Object>> alternativeRoutes = new ArrayList<>();
        NavigationTaskResponse.SafetyAnalysis selectedSafety = null;
        NavigationTaskResponse.OverallAnalysis selectedOverall = null;

        for (int i = 0; i < paths.size(); i++) {
            int routeIndex = i;
            Map<String, Object> path = paths.get(i);
            RouteAnalysisService.RouteScore matchedScore = scoredRoutes.stream()
                    .filter(score -> score.getIndex() == routeIndex)
                    .findFirst()
                    .orElse(null);

            NavigationTaskResponse.SafetyAnalysis candidateSafety = null;
            NavigationTaskResponse.OverallAnalysis candidateOverall = buildFallbackOverallAnalysis(path, matchedScore);

            if (shouldAnalyze) {
                RouteAnalysisService.RouteAnalysisResult analysis = routeAnalysisService.analyze(
                        path,
                        slots.getTaskScene(),
                        slots.getPreferences(),
                        slots.getTimeContext(),
                        slots.getTimeSlot()
                );
                if (analysis != null) {
                    candidateSafety = buildSafetyAnalysis(analysis);
                    candidateOverall = buildOverallAnalysis(analysis);
                }
            }

            Map<String, Object> altRoute = new LinkedHashMap<>();
            altRoute.put("index", routeIndex);
            altRoute.put("distance", path.get("distance"));
            altRoute.put("duration", path.get("duration"));
            altRoute.put("summary", matchedScore != null && matchedScore.getSummary() != null && !matchedScore.getSummary().isBlank()
                    ? matchedScore.getSummary() : path.get("summary"));
            altRoute.put("reason", matchedScore != null ? matchedScore.getReason() : "");
            altRoute.put("score", matchedScore != null ? matchedScore.getScore() : 7);
            altRoute.put("steps", path.get("steps"));
            altRoute.put("path", path.get("path"));
            altRoute.put("selected", routeIndex == selectedRouteIndex);
            if (candidateSafety != null) {
                altRoute.put("safetyAnalysis", safetyAnalysisToMap(candidateSafety));
            }
            if (candidateOverall != null) {
                altRoute.put("overallAnalysis", overallAnalysisToMap(candidateOverall));
            }
            alternativeRoutes.add(altRoute);

            if (routeIndex == selectedRouteIndex) {
                selectedSafety = candidateSafety;
                selectedOverall = candidateOverall;
            }
        }

        if (paths.size() > 1 && (routeSelectionReason == null || routeSelectionReason.isBlank())) {
            routeSelectionReason = "已按当前场景自动选择最合适路线";
        }

        if (selectedOverall == null) {
            final int finalSelectedRouteIndex = selectedRouteIndex;
            RouteAnalysisService.RouteScore selectedScore = scoredRoutes.stream()
                    .filter(score -> score.getIndex() == finalSelectedRouteIndex)
                    .findFirst()
                    .orElse(null);
            selectedOverall = buildFallbackOverallAnalysis(selectedPath, selectedScore);
        }

        return new SingleRouteSelection(
                selectedPath,
                selectedRouteIndex,
                routeSelectionReason,
                alternativeRoutes.isEmpty() ? null : alternativeRoutes,
                selectedSafety,
                selectedOverall,
                scoredRoutes
        );
    }

    public SegmentRouteSelection selectSegmentRoute(List<Map<String, Object>> paths,
                                                    NavigationSegmentSlots segmentSlots,
                                                    NavigationSlots slots) {
        if (paths == null || paths.isEmpty()) {
            return new SegmentRouteSelection(null, 0, "该段使用默认路线", List.of());
        }

        int selectedRouteIndex = 0;
        String selectionReason = "该段使用默认路线";
        List<Map<String, Object>> alternativeRoutes = List.of();

        if (paths.size() > 1) {
            RouteAnalysisService.RouteSelectionResult selection = segmentSlots != null
                    ? routeAnalysisService.selectBestRoute(paths, segmentSlots)
                    : routeAnalysisService.selectBestRoute(paths, slots);
            List<RouteAnalysisService.RouteScore> scores = selection != null && selection.getAllScores() != null
                    ? selection.getAllScores() : List.of();

            if (selection != null) {
                selectedRouteIndex = Math.max(0, Math.min(selection.getSelectedIndex(), paths.size() - 1));
                selectionReason = selection.getReason() != null && !selection.getReason().isBlank()
                        ? selection.getReason()
                        : selectionReason;
            }

            alternativeRoutes = new ArrayList<>();
            for (int routeIndex = 0; routeIndex < paths.size(); routeIndex++) {
                int candidateRouteIndex = routeIndex;
                Map<String, Object> path = paths.get(routeIndex);
                RouteAnalysisService.RouteScore matchedScore = scores.stream()
                        .filter(score -> score.getIndex() == candidateRouteIndex)
                        .findFirst()
                        .orElse(null);

                Map<String, Object> altRoute = new LinkedHashMap<>();
                altRoute.put("index", candidateRouteIndex);
                altRoute.put("distance", path.get("distance"));
                altRoute.put("duration", path.get("duration"));
                altRoute.put("summary", matchedScore != null && matchedScore.getSummary() != null && !matchedScore.getSummary().isBlank()
                        ? matchedScore.getSummary() : path.get("summary"));
                altRoute.put("reason", matchedScore != null ? matchedScore.getReason() : "");
                altRoute.put("score", matchedScore != null ? matchedScore.getScore() : 7);
                altRoute.put("selected", candidateRouteIndex == selectedRouteIndex);
                alternativeRoutes.add(altRoute);
            }
        }

        Map<String, Object> selectedPath = paths.get(Math.max(0, Math.min(selectedRouteIndex, paths.size() - 1)));
        return new SegmentRouteSelection(selectedPath, selectedRouteIndex, selectionReason, alternativeRoutes);
    }

    public NavigationTaskResponse.OverallAnalysis buildFallbackOverallAnalysis(
            Map<String, Object> path,
            RouteAnalysisService.RouteScore matchedScore
    ) {
        if (path == null) {
            return null;
        }
        String scoreSummary = matchedScore != null ? matchedScore.getSummary() : null;
        String scoreReason = matchedScore != null ? matchedScore.getReason() : null;
        int score = matchedScore != null && matchedScore.getScore() > 0 ? matchedScore.getScore() : 7;

        List<String> keyPoints = new ArrayList<>();
        if (scoreSummary != null && !scoreSummary.isBlank()) {
            keyPoints.add(scoreSummary);
        }
        if (scoreReason != null && !scoreReason.isBlank() && !scoreReason.equals(scoreSummary)) {
            keyPoints.add(scoreReason);
        }

        String pathSummary = path.get("summary") != null ? String.valueOf(path.get("summary")) : null;

        return NavigationTaskResponse.OverallAnalysis.builder()
                .overallScore(score)
                .summary(firstNonBlank(scoreReason, scoreSummary, pathSummary, "路线已准备就绪"))
                .keyPoints(keyPoints.stream().limit(3).toList())
                .recommendation(firstNonBlank(scoreReason, "可按当前路线前往"))
                .build();
    }

    private NavigationTaskResponse.SafetyAnalysis buildSafetyAnalysis(RouteAnalysisService.RouteAnalysisResult analysis) {
        return NavigationTaskResponse.SafetyAnalysis.builder()
                .safetyScore(analysis.getScore())
                .warnings(analysis.getWarnings())
                .suggestions(analysis.getSuggestions())
                .hasAlternative(analysis.isRecommendAlternative())
                .alternativeSummary(analysis.getAlternativeDescription())
                .build();
    }

    private NavigationTaskResponse.OverallAnalysis buildOverallAnalysis(RouteAnalysisService.RouteAnalysisResult analysis) {
        List<String> keyPoints = new ArrayList<>();
        if (analysis.getWarnings() != null) {
            keyPoints.addAll(analysis.getWarnings());
        }
        if (analysis.getSuggestions() != null) {
            keyPoints.addAll(analysis.getSuggestions());
        }

        String summary = firstNonBlank(
                analysis.getReason(),
                "路线已结合当前场景完成评估"
        );
        String recommendation = firstNonBlank(
                analysis.getAlternativeDescription(),
                analysis.getSuggestions() != null && !analysis.getSuggestions().isEmpty() ? analysis.getSuggestions().get(0) : null,
                "可按当前路线前往"
        );

        return NavigationTaskResponse.OverallAnalysis.builder()
                .overallScore(analysis.getScore())
                .summary(summary)
                .keyPoints(keyPoints.stream().limit(3).toList())
                .recommendation(recommendation)
                .build();
    }

    private Map<String, Object> safetyAnalysisToMap(NavigationTaskResponse.SafetyAnalysis safetyAnalysis) {
        Map<String, Object> safetyMap = new LinkedHashMap<>();
        safetyMap.put("safetyScore", safetyAnalysis.getSafetyScore());
        safetyMap.put("warnings", safetyAnalysis.getWarnings() != null ? safetyAnalysis.getWarnings() : List.of());
        safetyMap.put("suggestions", safetyAnalysis.getSuggestions() != null ? safetyAnalysis.getSuggestions() : List.of());
        safetyMap.put("hasAlternative", safetyAnalysis.isHasAlternative());
        safetyMap.put("alternativeSummary", safetyAnalysis.getAlternativeSummary());
        return safetyMap;
    }

    private Map<String, Object> overallAnalysisToMap(NavigationTaskResponse.OverallAnalysis overallAnalysis) {
        Map<String, Object> overallMap = new LinkedHashMap<>();
        overallMap.put("overallScore", overallAnalysis.getOverallScore());
        overallMap.put("summary", overallAnalysis.getSummary());
        overallMap.put("keyPoints", overallAnalysis.getKeyPoints() != null ? overallAnalysis.getKeyPoints() : List.of());
        overallMap.put("recommendation", overallAnalysis.getRecommendation());
        return overallMap;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
