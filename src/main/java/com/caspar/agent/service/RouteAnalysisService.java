package com.caspar.agent.service;

import com.caspar.agent.llm.LlmClient;
import com.caspar.agent.model.LlmMessage;
import com.caspar.agent.model.NavigationSegmentSlots;
import com.caspar.agent.model.NavigationSlots;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 路线分析服务
 * 使用LLM根据用户场景智能评估和选择路线
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RouteAnalysisService {

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 路线分析结果
     */
    @Data
    @Builder
    public static class RouteAnalysisResult {
        private int score;                      // 路线评分 1-10
        private List<String> warnings;          // 警告信息
        private List<String> suggestions;       // 建议列表
        private String reason;                  // 选择/评估理由
        private boolean recommendAlternative;   // 是否推荐替代路线
        private String alternativeDescription;  // 替代路线描述

        public boolean hasWarnings() {
            return warnings != null && !warnings.isEmpty();
        }

        public boolean hasSuggestions() {
            return suggestions != null && !suggestions.isEmpty();
        }
    }

    /**
     * 单段路径分析结果
     */
    @Data
    @Builder
    public static class SegmentAnalysis {
        private int score;                      // 该段评分 1-10
        private String reason;                  // 评估理由
        private List<String> warnings;          // 该段警告
        private List<String> suggestions;       // 该段建议

        public boolean hasWarnings() {
            return warnings != null && !warnings.isEmpty();
        }

        public boolean hasSuggestions() {
            return suggestions != null && !suggestions.isEmpty();
        }
    }

    /**
     * 综合分析结果
     */
    @Data
    @Builder
    public static class OverallAnalysis {
        private int overallScore;               // 总体评分
        private String summary;                 // 综合评估摘要
        private List<String> keyPoints;         // 关键提示
        private String recommendation;          // 总体建议
    }

    /**
     * 根据用户场景评估路线
     *
     * @param routeResult  路线数据（来自高德API）
     * @param scene        场景（return_dorm, class_commute等）
     * @param preferences  用户偏好列表
     * @param timeContext  时间上下文（night, now等）
     * @param timeSlot     时间槽位（night, late_night等）
     * @return 分析结果
     */
    public RouteAnalysisResult analyze(
            Map<String, Object> routeResult,
            String scene,
            List<String> preferences,
            String timeContext,
            String timeSlot) {

        long startTime = System.currentTimeMillis();

        // 判断是否需要分析（有场景或偏好时都需要）
        if (!needsAnalysis(scene, preferences, timeContext, timeSlot)) {
            log.debug("路线分析: 无需分析, scene={}, preferences={}", scene, preferences);
            return null;
        }

        try {
            // 构建分析提示
            String analysisPrompt = buildAnalysisPrompt(routeResult, scene, preferences, timeContext, timeSlot);

            // 调用LLM分析
            List<LlmMessage> messages = new ArrayList<>();
            messages.add(LlmMessage.system(getSystemPrompt()));
            messages.add(LlmMessage.user(analysisPrompt));

            String llmResponse = llmClient.chat(messages);

            // 解析LLM响应
            RouteAnalysisResult result = parseAnalysisResult(llmResponse);

            long duration = System.currentTimeMillis() - startTime;
            log.info("路线分析完成: score={}, warnings={}, 耗时={}ms",
                    result.getScore(),
                    result.getWarnings(),
                    duration);

            return result;

        } catch (Exception e) {
            log.error("路线分析失败", e);
            return null;
        }
    }

    /**
     * 判断是否需要进行路线分析
     * 只要有场景、偏好或时间上下文，就需要分析
     */
    private boolean needsAnalysis(String scene, List<String> preferences, String timeContext, String timeSlot) {
        // 有场景
        if (scene != null && !scene.isBlank() && !"daily_convenience".equals(scene)) {
            return true;
        }
        // 有偏好
        if (preferences != null && !preferences.isEmpty()) {
            return true;
        }
        // 有时间上下文
        if (timeContext != null && !"now".equals(timeContext)) {
            return true;
        }
        // 深夜时间槽位也需要分析
        if (isLateNight(timeSlot)) {
            return true;
        }
        return false;
    }

    /**
     * 构建分析提示
     */
    private String buildAnalysisPrompt(
            Map<String, Object> routeResult,
            String scene,
            List<String> preferences,
            String timeContext,
            String timeSlot) {

        StringBuilder prompt = new StringBuilder();

        prompt.append("请根据用户场景评估以下步行路线是否合适：\n\n");

        // 路线基本信息
        prompt.append("【路线信息】\n");
        Object distanceObj = routeResult.get("distance");
        int distance = distanceObj instanceof Number ? ((Number) distanceObj).intValue() : 0;
        prompt.append("- 距离: ").append(distance).append("米\n");

        Object durationObj = routeResult.get("duration");
        int durationMinutes = durationObj instanceof Number ? ((Number) durationObj).intValue() / 60 : 0;
        prompt.append("- 预计时间: ").append(durationMinutes).append("分钟\n\n");

        // 路线步骤
        prompt.append("【路线步骤】\n");
        Object stepsObj = routeResult.get("steps");
        if (stepsObj instanceof List<?> steps) {
            int stepNum = 1;
            for (Object step : steps) {
                if (step instanceof Map<?, ?> stepMap) {
                    Object instructionObj = stepMap.get("instruction");
                    String instruction = instructionObj != null ? instructionObj.toString() : "";
                    prompt.append(stepNum++).append(". ").append(instruction).append("\n");
                }
            }
        }
        prompt.append("\n");

        // 用户场景
        prompt.append("【用户场景】\n");
        if (scene != null && !scene.isBlank()) {
            String sceneDesc = getSceneDescription(scene);
            prompt.append("- 场景: ").append(sceneDesc).append("\n");
        }
        if (timeContext != null && !"now".equals(timeContext)) {
            prompt.append("- 时间: ").append(getTimeDescription(timeContext)).append("\n");
        }
        if (timeSlot != null && !timeSlot.isBlank()) {
            prompt.append("- 时间槽位: ").append(timeSlot).append("\n");
        }
        if (preferences != null && !preferences.isEmpty()) {
            prompt.append("- 用户偏好: ").append(String.join("、", preferences)).append("\n");
        }
        if (isLateNight(timeSlot)) {
            prompt.append("- 深夜附加约束: 主干道优先，宁可稍远也不要走偏僻小路\n");
        }

        return prompt.toString();
    }

    /**
     * 场景描述映射
     */
    private String getSceneDescription(String scene) {
        return switch (scene) {
            case "return_dorm" -> "返回宿舍";
            case "class_commute" -> "上课赶路";
            case "dining" -> "就餐";
            case "express" -> "取快递";
            case "study" -> "学习/去图书馆";
            case "medical" -> "就医";
            case "campus_tour" -> "校园导览";
            case "multi_waypoint" -> "多地点途经";
            default -> scene;
        };
    }

    /**
     * 时间上下文描述
     */
    private String getTimeDescription(String timeContext) {
        return switch (timeContext) {
            case "night" -> "夜间";
            case "after_class" -> "下课后";
            case "before_exam" -> "考试前";
            default -> timeContext;
        };
    }

    /**
     * 获取系统提示
     */
    private String getSystemPrompt() {
        return """
            你是一个校园导航路线评估专家。你的任务是根据用户的具体场景和偏好，评估路线是否合适。

            【评估原则】
            1. 理解用户的真实需求：赶时间？安全？舒适？便捷？
            2. 分析路线特点：距离、时间、道路类型、是否有楼梯/台阶等
            3. 给出合理的评估和建议

            【常见场景考量】
            - 夜间出行：关注照明、人流、是否偏僻
            - 深夜出行：主干道优先，重点关注持续照明、监控覆盖和稳定人流，避免偏僻近路
            - 赶时间：关注是否最短最快
            - 搬行李/不想走楼梯：关注是否有台阶、坡道
            - 避开人群：关注是否经过拥挤区域
            - 多地点途经：关注路线是否合理

            【道路类型提示】
            - 主干道/大道：照明好、人流多、安全
            - 小路/小径：可能偏僻、照明不足
            - 有"楼梯"、"台阶"、"阶梯"的路段：需要爬楼梯
            - 有"坡道"、"斜坡"的路段：无台阶

            【输出要求】
            必须输出JSON格式，包含以下字段：
            - score: 整数1-10，路线适合度评分
            - warnings: 字符串数组，需要注意的问题
            - suggestions: 字符串数组，改进建议
            - reason: 字符串，评估理由（一句话说明为什么给这个评分）
            - recommendAlternative: 布尔值，是否建议选择其他路线
            - alternativeDescription: 字符串，如有更好选择请描述

            【示例输出】
            {
              "score": 8,
              "warnings": [],
              "suggestions": ["路线经过主干道，夜间行走较安全"],
              "reason": "路线全程为主干道，距离适中，适合夜间返回宿舍",
              "recommendAlternative": false,
              "alternativeDescription": null
            }

            或

            {
              "score": 5,
              "warnings": ["路线包含一段楼梯，不适合搬行李"],
              "suggestions": ["建议绕行无障碍通道，增加约100米"],
              "reason": "用户需搬行李，但路线包含台阶，不太合适",
              "recommendAlternative": true,
              "alternativeDescription": "绕行图书馆前广场，全程平路，增加约100米"
            }

            注意：输出必须是纯JSON格式，不要包含其他文字。
            """;
    }

    /**
     * 解析LLM响应
     */
    private RouteAnalysisResult parseAnalysisResult(String llmResponse) {
        try {
            String json = extractJson(llmResponse);
            JsonNode root = objectMapper.readTree(json);

            int score = root.path("score").asInt(7);
            List<String> warnings = parseStringArray(root.path("warnings"));
            List<String> suggestions = parseStringArray(root.path("suggestions"));
            String reason = root.path("reason").asText("");
            boolean recommendAlternative = root.path("recommendAlternative").asBoolean(false);
            String alternativeDescription = root.path("alternativeDescription").asText(null);

            return RouteAnalysisResult.builder()
                    .score(score)
                    .warnings(warnings)
                    .suggestions(suggestions)
                    .reason(reason)
                    .recommendAlternative(recommendAlternative)
                    .alternativeDescription(alternativeDescription)
                    .build();

        } catch (Exception e) {
            log.warn("解析LLM响应失败: {}", e.getMessage());
            return RouteAnalysisResult.builder()
                    .score(7)
                    .warnings(List.of())
                    .suggestions(List.of())
                    .reason("路线评估完成")
                    .recommendAlternative(false)
                    .build();
        }
    }

    private String extractJson(String response) {
        if (response == null || response.isBlank()) return "{}";
        String trimmed = response.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) return trimmed.substring(start, end + 1);
        return "{}";
    }

    private List<String> parseStringArray(JsonNode node) {
        List<String> result = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode item : node) {
                String text = item.asText();
                if (text != null && !text.isBlank()) result.add(text);
            }
        }
        return result;
    }

    // ==================== 分段路径评估方法 ====================

    /**
     * 评估单段路径
     *
     * @param segmentRoute  单段路径数据
     * @param fromName      起点名称
     * @param toName        终点名称
     * @param segmentIndex  段索引（从0开始）
     * @param slots         槽位信息
     * @return 该段路径的分析结果
     */
    public SegmentAnalysis analyzeSegment(
            Map<String, Object> segmentRoute,
            String fromName,
            String toName,
            int segmentIndex,
            NavigationSlots slots) {

        long startTime = System.currentTimeMillis();

        // 判断是否需要分析
        if (!needsSegmentAnalysis(slots)) {
            log.debug("段{}分析: 无需分析", segmentIndex);
            return buildDefaultSegmentAnalysis(segmentRoute, fromName, toName);
        }

        try {
            // 构建分段分析提示
            String analysisPrompt = buildSegmentAnalysisPrompt(segmentRoute, fromName, toName, segmentIndex, slots);

            // 调用LLM分析
            List<LlmMessage> messages = new ArrayList<>();
            messages.add(LlmMessage.system(getSegmentAnalysisSystemPrompt()));
            messages.add(LlmMessage.user(analysisPrompt));

            String llmResponse = llmClient.chat(messages);

            // 解析响应
            SegmentAnalysis result = parseSegmentAnalysisResult(llmResponse);

            long duration = System.currentTimeMillis() - startTime;
            log.info("段{}分析完成: score={}, reason={}, 耗时={}ms",
                    segmentIndex, result.getScore(), result.getReason(), duration);

            return result;

        } catch (Exception e) {
            log.error("段{}分析失败", segmentIndex, e);
            return buildDefaultSegmentAnalysis(segmentRoute, fromName, toName);
        }
    }

    public SegmentAnalysis analyzeSegment(
            Map<String, Object> segmentRoute,
            String fromName,
            String toName,
            int segmentIndex,
            NavigationSegmentSlots segmentSlots) {

        long startTime = System.currentTimeMillis();

        if (!needsSegmentAnalysis(segmentSlots)) {
            log.debug("段{}分析: 分段槽位无需进一步分析", segmentIndex);
            return buildDefaultSegmentAnalysis(segmentRoute, fromName, toName);
        }

        try {
            String analysisPrompt = buildSegmentAnalysisPrompt(segmentRoute, fromName, toName, segmentIndex, segmentSlots);
            List<LlmMessage> messages = new ArrayList<>();
            messages.add(LlmMessage.system(getSegmentAnalysisSystemPrompt()));
            messages.add(LlmMessage.user(analysisPrompt));

            String llmResponse = llmClient.chat(messages);
            SegmentAnalysis result = parseSegmentAnalysisResult(llmResponse);

            long duration = System.currentTimeMillis() - startTime;
            log.info("段{}分段槽位分析完成: score={}, reason={}, 耗时={}ms",
                    segmentIndex, result.getScore(), result.getReason(), duration);

            return result;
        } catch (Exception e) {
            log.error("段{}分段槽位分析失败", segmentIndex, e);
            return buildDefaultSegmentAnalysis(segmentRoute, fromName, toName);
        }
    }

    /**
     * 综合评估所有路径段
     *
     * @param segmentAnalyses  所有段评估结果
     * @param slots            槽位信息
     * @return 综合评估结果
     */
    public OverallAnalysis analyzeOverall(
            List<SegmentAnalysis> segmentAnalyses,
            NavigationSlots slots) {

        if (segmentAnalyses == null || segmentAnalyses.isEmpty()) {
            return null;
        }

        long startTime = System.currentTimeMillis();

        try {
            // 构建综合分析提示
            String analysisPrompt = buildOverallAnalysisPrompt(segmentAnalyses, slots);

            // 调用LLM分析
            List<LlmMessage> messages = new ArrayList<>();
            messages.add(LlmMessage.system(getOverallAnalysisSystemPrompt()));
            messages.add(LlmMessage.user(analysisPrompt));

            String llmResponse = llmClient.chat(messages);

            // 解析响应
            OverallAnalysis result = parseOverallAnalysisResult(llmResponse);

            long duration = System.currentTimeMillis() - startTime;
            log.info("综合分析完成: overallScore={}, 耗时={}ms", result.getOverallScore(), duration);

            return result;

        } catch (Exception e) {
            log.error("综合分析失败", e);
            // 返回基于分段结果的默认综合分析
            return buildDefaultOverallAnalysis(segmentAnalyses);
        }
    }

    /**
     * 判断是否需要分段分析
     */
    private boolean needsSegmentAnalysis(NavigationSlots slots) {
        if (slots == null) return false;

        // 有场景
        if (slots.getTaskScene() != null && !slots.getTaskScene().isBlank()
                && !"daily_convenience".equals(slots.getTaskScene())) {
            return true;
        }
        // 有偏好
        if (slots.getPreferences() != null && !slots.getPreferences().isEmpty()) {
            return true;
        }
        // 有时间上下文
        if (slots.getTimeContext() != null && !"now".equals(slots.getTimeContext())) {
            return true;
        }
        // 多途经点
        if (slots.isMultiWaypoint()) {
            return true;
        }
        return false;
    }

    private boolean needsSegmentAnalysis(NavigationSegmentSlots segmentSlots) {
        if (segmentSlots == null) return false;
        if (segmentSlots.getTaskScene() != null && !segmentSlots.getTaskScene().isBlank()
                && !"daily_convenience".equals(segmentSlots.getTaskScene())) {
            return true;
        }
        if (segmentSlots.hasPreferences() || segmentSlots.hasConstraints()) {
            return true;
        }
        if (segmentSlots.getTimeContext() != null && !"now".equals(segmentSlots.getTimeContext())) {
            return true;
        }
        return segmentSlots.getTimeSlot() != null && !segmentSlots.getTimeSlot().isBlank();
    }

    /**
     * 构建默认的单段分析结果
     */
    private SegmentAnalysis buildDefaultSegmentAnalysis(Map<String, Object> segmentRoute, String fromName, String toName) {
        int distance = 0;
        if (segmentRoute != null && segmentRoute.get("distance") instanceof Number) {
            distance = ((Number) segmentRoute.get("distance")).intValue();
        }
        return SegmentAnalysis.builder()
                .score(7)
                .reason(String.format("从%s到%s，距离%d米，路线正常", fromName, toName, distance))
                .warnings(List.of())
                .suggestions(List.of())
                .build();
    }

    /**
     * 构建默认的综合分析结果
     */
    private OverallAnalysis buildDefaultOverallAnalysis(List<SegmentAnalysis> segmentAnalyses) {
        double avgScore = segmentAnalyses.stream()
                .mapToInt(SegmentAnalysis::getScore)
                .average()
                .orElse(7.0);

        return OverallAnalysis.builder()
                .overallScore((int) Math.round(avgScore))
                .summary("路线整体合理，可按规划前往")
                .keyPoints(List.of())
                .recommendation("建议按顺序依次前往各地点")
                .build();
    }

    /**
     * 构建分段分析提示
     */
    private String buildSegmentAnalysisPrompt(
            Map<String, Object> segmentRoute,
            String fromName,
            String toName,
            int segmentIndex,
            NavigationSlots slots) {

        StringBuilder prompt = new StringBuilder();

        prompt.append("请评估以下步行路线段：\n\n");

        // 该段路线基本信息
        prompt.append("【该段路线信息】\n");
        prompt.append("- 从: ").append(fromName).append("\n");
        prompt.append("- 到: ").append(toName).append("\n");

        Object distanceObj = segmentRoute.get("distance");
        int distance = distanceObj instanceof Number ? ((Number) distanceObj).intValue() : 0;
        prompt.append("- 距离: ").append(distance).append("米\n");

        Object durationObj = segmentRoute.get("duration");
        int durationMinutes = durationObj instanceof Number ? ((Number) durationObj).intValue() / 60 : 0;
        prompt.append("- 预计时间: ").append(durationMinutes).append("分钟\n\n");

        // 路线步骤
        prompt.append("【路线步骤】\n");
        Object stepsObj = segmentRoute.get("steps");
        if (stepsObj instanceof List<?> steps) {
            int stepNum = 1;
            for (Object step : steps) {
                if (step instanceof Map<?, ?> stepMap) {
                    Object instructionObj = stepMap.get("instruction");
                    String instruction = instructionObj != null ? instructionObj.toString() : "";
                    prompt.append(stepNum++).append(". ").append(instruction).append("\n");
                }
            }
        }
        prompt.append("\n");

        // 用户场景信息
        prompt.append("【用户场景】\n");
        if (slots.getTaskScene() != null && !slots.getTaskScene().isBlank()) {
            prompt.append("- 任务场景: ").append(getSceneDescription(slots.getTaskScene())).append("\n");
        }
        if (slots.getTimeContext() != null && !"now".equals(slots.getTimeContext())) {
            prompt.append("- 时间上下文: ").append(getTimeDescription(slots.getTimeContext())).append("\n");
        }
        if (slots.getTimeSlot() != null && !slots.getTimeSlot().isBlank()) {
            prompt.append("- 时间槽位: ").append(slots.getTimeSlot()).append("\n");
        }
        if (slots.getPreferences() != null && !slots.getPreferences().isEmpty()) {
            prompt.append("- 用户偏好: ").append(String.join("、", slots.getPreferences())).append("\n");
        }
        if (isLateNight(slots.getTimeSlot())) {
            prompt.append("- 深夜附加约束: 主干道优先，宁可稍远也不要走偏僻小路\n");
        }
        if (slots.isUrgent()) {
            prompt.append("- 时间紧迫: 还剩").append(slots.getUrgencyMinutes()).append("分钟\n");
        }

        return prompt.toString();
    }

    private String buildSegmentAnalysisPrompt(
            Map<String, Object> segmentRoute,
            String fromName,
            String toName,
            int segmentIndex,
            NavigationSegmentSlots segmentSlots) {

        StringBuilder prompt = new StringBuilder();
        prompt.append("请评估以下步行路线段：\n\n");
        prompt.append("【该段路线信息】\n");
        prompt.append("- 段索引: ").append(segmentIndex).append("\n");
        prompt.append("- 从: ").append(fromName).append("\n");
        prompt.append("- 到: ").append(toName).append("\n");
        prompt.append("- 该段目标: ").append(segmentSlots.getSegmentGoal()).append("\n");

        Object distanceObj = segmentRoute.get("distance");
        int distance = distanceObj instanceof Number ? ((Number) distanceObj).intValue() : 0;
        prompt.append("- 距离: ").append(distance).append("米\n");

        Object durationObj = segmentRoute.get("duration");
        int durationMinutes = durationObj instanceof Number ? ((Number) durationObj).intValue() / 60 : 0;
        prompt.append("- 预计时间: ").append(durationMinutes).append("分钟\n\n");

        prompt.append("【路线步骤】\n");
        Object stepsObj = segmentRoute.get("steps");
        if (stepsObj instanceof List<?> steps) {
            int stepNum = 1;
            for (Object step : steps) {
                if (step instanceof Map<?, ?> stepMap) {
                    Object instructionObj = stepMap.get("instruction");
                    String instruction = instructionObj != null ? instructionObj.toString() : "";
                    prompt.append(stepNum++).append(". ").append(instruction).append("\n");
                }
            }
        }
        prompt.append("\n");

        prompt.append("【该段槽位】\n");
        if (segmentSlots.getTaskScene() != null && !segmentSlots.getTaskScene().isBlank()) {
            prompt.append("- 任务场景: ").append(getSceneDescription(segmentSlots.getTaskScene())).append("\n");
        }
        if (segmentSlots.getTimeContext() != null && !segmentSlots.getTimeContext().isBlank()) {
            prompt.append("- 时间上下文: ").append(getTimeDescription(segmentSlots.getTimeContext())).append("\n");
        }
        if (segmentSlots.getTimeSlot() != null && !segmentSlots.getTimeSlot().isBlank()) {
            prompt.append("- 时间槽位: ").append(segmentSlots.getTimeSlot()).append("\n");
        }
        if (segmentSlots.hasPreferences()) {
            prompt.append("- 偏好: ").append(String.join("、", segmentSlots.getPreferences())).append("\n");
        }
        if (segmentSlots.hasConstraints()) {
            prompt.append("- 约束: ").append(String.join("、", segmentSlots.getConstraints())).append("\n");
        }
        if (shouldPreferMainRoad(segmentSlots.getTimeSlot(), segmentSlots.getConstraints())) {
            prompt.append("- 深夜附加约束: 主干道优先，宁可稍远也不要走偏僻小路\n");
        }
        if (segmentSlots.isUrgent()) {
            prompt.append("- 紧急程度: 还剩").append(segmentSlots.getUrgencyMinutes()).append("分钟\n");
        }
        if (segmentSlots.getNotes() != null && !segmentSlots.getNotes().isBlank()) {
            prompt.append("- 补充说明: ").append(segmentSlots.getNotes()).append("\n");
        }
        return prompt.toString();
    }

    /**
     * 构建综合分析提示
     */
    private String buildOverallAnalysisPrompt(List<SegmentAnalysis> segmentAnalyses, NavigationSlots slots) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("请综合评估以下多段路线：\n\n");

        // 各段评估摘要
        prompt.append("【各段评估结果】\n");
        for (int i = 0; i < segmentAnalyses.size(); i++) {
            SegmentAnalysis sa = segmentAnalyses.get(i);
            prompt.append(String.format("段%d: 评分%d/10, %s\n", i + 1, sa.getScore(), sa.getReason()));
            if (sa.hasWarnings()) {
                prompt.append("  警告: ").append(String.join("; ", sa.getWarnings())).append("\n");
            }
        }
        prompt.append("\n");

        // 用户场景
        prompt.append("【用户场景】\n");
        if (slots.getTaskScene() != null && !slots.getTaskScene().isBlank()) {
            prompt.append("- 任务场景: ").append(getSceneDescription(slots.getTaskScene())).append("\n");
        }
        if (slots.getTimeContext() != null && !"now".equals(slots.getTimeContext())) {
            prompt.append("- 时间上下文: ").append(getTimeDescription(slots.getTimeContext())).append("\n");
        }
        if (slots.getPreferences() != null && !slots.getPreferences().isEmpty()) {
            prompt.append("- 用户偏好: ").append(String.join("、", slots.getPreferences())).append("\n");
        }
        if (isLateNight(slots.getTimeSlot())) {
            prompt.append("- 深夜附加约束: 主干道优先，宁可稍远也不要走偏僻小路\n");
        }

        // 途经点信息
        if (slots.isMultiWaypoint() && slots.getWaypoints() != null) {
            prompt.append("- 途经点: ").append(String.join(" → ", slots.getWaypoints())).append("\n");
        }

        return prompt.toString();
    }

    /**
     * 获取分段分析系统提示
     */
    private String getSegmentAnalysisSystemPrompt() {
        return """
            你是一个校园导航路线评估专家。

            当前任务：评估单段路径是否适合用户的场景和需求。

            【评估原则】
            1. 根据用户场景判断该段路线是否合适
            2. 关注该段道路特点：是否偏僻、是否有台阶、照明情况等
            3. 给出针对性的评估

            【常见场景考量】
            - 夜间出行：关注照明、人流、是否偏僻
            - 深夜出行：主干道优先，重点关注持续照明、监控覆盖和稳定人流，避免偏僻近路
            - 赶时间：关注是否最短最快
            - 搬行李：关注是否有台阶
            - 多地点途经：关注该段与整体行程的关系
            - 正午/烈日：关注暴晒和无遮阴路段
            - 晚高峰：关注人流密集和主干道拥堵

            【约束理解】
            - avoid_main_road：尽量避开主干道、人流最密集路段
            - prefer_main_road：深夜场景优先主干道、照明更稳定且有人流的路线
            - avoidCrowd：优先减少高峰拥挤
            - avoidSun：优先减少暴晒

            【输出要求】
            必须输出JSON格式，包含以下字段：
            - score: 整数1-10，该段路线适合度评分
            - reason: 字符串，一句话说明评估理由
            - warnings: 字符串数组，该段需要注意的问题
            - suggestions: 字符串数组，该段改进建议

            【示例输出】
            {
              "score": 8,
              "reason": "该段为主干道，照明良好，适合夜间行走",
              "warnings": [],
              "suggestions": ["路线平坦，便于快速通行"]
            }

            注意：输出必须是纯JSON格式，不要包含其他文字。
            """;
    }

    /**
     * 获取综合分析系统提示
     */
    private String getOverallAnalysisSystemPrompt() {
        return """
            你是一个校园导航路线评估专家。

            当前任务：综合评估多段路径，给出整体建议。

            【评估原则】
            1. 综合各段评估结果，给出整体评分
            2. 找出路线中的关键问题或亮点
            3. 给出实用的出行建议

            【输出要求】
            必须输出JSON格式，包含以下字段：
            - overallScore: 整数1-10，整体路线评分
            - summary: 字符串，综合评估摘要（一句话）
            - keyPoints: 字符串数组，关键提示（2-3条）
            - recommendation: 字符串，总体建议

            【示例输出】
            {
              "overallScore": 8,
              "summary": "路线整体合理，按此顺序可高效完成行程",
              "keyPoints": [
                "第一段经过小路，注意照明",
                "最后一段为主干道，安全可靠"
              ],
              "recommendation": "建议按规划顺序依次前往，夜间注意第一段小路的照明"
            }

            注意：输出必须是纯JSON格式，不要包含其他文字。
            """;
    }

    /**
     * 解析分段分析LLM响应
     */
    private SegmentAnalysis parseSegmentAnalysisResult(String llmResponse) {
        try {
            String json = extractJson(llmResponse);
            JsonNode root = objectMapper.readTree(json);

            int score = root.path("score").asInt(7);
            String reason = root.path("reason").asText("");
            List<String> warnings = parseStringArray(root.path("warnings"));
            List<String> suggestions = parseStringArray(root.path("suggestions"));

            return SegmentAnalysis.builder()
                    .score(score)
                    .reason(reason)
                    .warnings(warnings)
                    .suggestions(suggestions)
                    .build();

        } catch (Exception e) {
            log.warn("解析分段分析LLM响应失败: {}", e.getMessage());
            return SegmentAnalysis.builder()
                    .score(7)
                    .reason("路线评估完成")
                    .warnings(List.of())
                    .suggestions(List.of())
                    .build();
        }
    }

    /**
     * 解析综合分析LLM响应
     */
    private OverallAnalysis parseOverallAnalysisResult(String llmResponse) {
        try {
            String json = extractJson(llmResponse);
            JsonNode root = objectMapper.readTree(json);

            int overallScore = root.path("overallScore").asInt(7);
            String summary = root.path("summary").asText("");
            List<String> keyPoints = parseStringArray(root.path("keyPoints"));
            String recommendation = root.path("recommendation").asText("");

            return OverallAnalysis.builder()
                    .overallScore(overallScore)
                    .summary(summary)
                    .keyPoints(keyPoints)
                    .recommendation(recommendation)
                    .build();

        } catch (Exception e) {
            log.warn("解析综合分析LLM响应失败: {}", e.getMessage());
            return OverallAnalysis.builder()
                    .overallScore(7)
                    .summary("路线评估完成")
                    .keyPoints(List.of())
                    .recommendation("建议按规划前往")
                    .build();
        }
    }

    // ==================== 多路线选择方法 ====================

    /**
     * 多路线选择结果
     */
    @Data
    @Builder
    public static class RouteSelectionResult {
        private int selectedIndex;              // 选中的路线索引
        private String reason;                  // 选择理由
        private List<RouteScore> allScores;     // 所有路线评分
    }

    /**
     * 单条路线评分
     */
    @Data
    @Builder
    public static class RouteScore {
        private int index;                      // 路线索引
        private int score;                      // 评分 1-10
        private String summary;                 // 路线摘要
        private String reason;                  // 评分理由
    }

    /**
     * 从多条路线中选择最优路线
     *
     * @param paths      所有路线列表
     * @param slots      槽位信息
     * @return 选择结果
     */
    public RouteSelectionResult selectBestRoute(
            List<Map<String, Object>> paths,
            NavigationSlots slots) {

        if (paths == null || paths.isEmpty()) {
            return null;
        }

        // 只有一条路线时直接返回
        if (paths.size() == 1) {
            return RouteSelectionResult.builder()
                    .selectedIndex(0)
                    .reason("只有一条可选路线")
                    .allScores(List.of(RouteScore.builder()
                            .index(0)
                            .score(7)
                            .summary((String) paths.get(0).getOrDefault("summary", ""))
                            .reason("唯一路线")
                            .build()))
                    .build();
        }

        long startTime = System.currentTimeMillis();

        try {
            // 构建选择提示
            String selectionPrompt = buildRouteSelectionPrompt(paths, slots);

            // 调用LLM选择
            List<LlmMessage> messages = new ArrayList<>();
            messages.add(LlmMessage.system(getRouteSelectionSystemPrompt()));
            messages.add(LlmMessage.user(selectionPrompt));

            String llmResponse = llmClient.chat(messages);

            // 解析响应
            RouteSelectionResult result = parseRouteSelectionResult(llmResponse, paths);

            long duration = System.currentTimeMillis() - startTime;
            log.info("路线选择完成: 选中路线{}, 耗时={}ms", result.getSelectedIndex(), duration);

            return result;

        } catch (Exception e) {
            log.error("路线选择失败", e);
            // 默认选择第一条
            return RouteSelectionResult.builder()
                    .selectedIndex(0)
                    .reason("选择失败，使用默认路线")
                    .allScores(buildDefaultScores(paths))
                    .build();
        }
    }

    public RouteSelectionResult selectBestRoute(
            List<Map<String, Object>> paths,
            NavigationSegmentSlots segmentSlots) {

        if (paths == null || paths.isEmpty()) {
            return null;
        }

        if (paths.size() == 1) {
            return RouteSelectionResult.builder()
                    .selectedIndex(0)
                    .reason("该段只有一条可选路线")
                    .allScores(List.of(RouteScore.builder()
                            .index(0)
                            .score(7)
                            .summary((String) paths.get(0).getOrDefault("summary", ""))
                            .reason("唯一路线")
                            .build()))
                    .build();
        }

        long startTime = System.currentTimeMillis();
        try {
            String selectionPrompt = buildRouteSelectionPrompt(paths, segmentSlots);
            List<LlmMessage> messages = new ArrayList<>();
            messages.add(LlmMessage.system(getRouteSelectionSystemPrompt()));
            messages.add(LlmMessage.user(selectionPrompt));

            String llmResponse = llmClient.chat(messages);
            RouteSelectionResult result = parseRouteSelectionResult(llmResponse, paths);

            long duration = System.currentTimeMillis() - startTime;
            log.info("分段路线选择完成: 段{}选中路线{}, 耗时={}ms",
                    segmentSlots.getIndex(), result.getSelectedIndex(), duration);

            return result;
        } catch (Exception e) {
            log.error("分段路线选择失败", e);
            return RouteSelectionResult.builder()
                    .selectedIndex(0)
                    .reason("该段选择失败，使用默认路线")
                    .allScores(buildDefaultScores(paths))
                    .build();
        }
    }

    /**
     * 构建路线选择提示
     */
    private String buildRouteSelectionPrompt(List<Map<String, Object>> paths, NavigationSlots slots) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("请从以下路线中选择最适合的一条：\n\n");

        // 列出所有路线
        for (int i = 0; i < paths.size(); i++) {
            Map<String, Object> path = paths.get(i);
            prompt.append("【路线").append(i + 1).append("】\n");
            prompt.append("- 距离: ").append(path.get("distance")).append("米\n");
            prompt.append("- 时间: ").append(((Number) path.get("duration")).intValue() / 60).append("分钟\n");

            String summary = (String) path.getOrDefault("summary", "");
            if (summary != null && !summary.isBlank()) {
                prompt.append("- 特点: ").append(summary).append("\n");
            }

            // 步骤摘要
            List<Map<String, Object>> steps = (List<Map<String, Object>>) path.get("steps");
            if (steps != null && !steps.isEmpty()) {
                prompt.append("- 步骤: 共").append(steps.size()).append("步\n");
            }
            prompt.append("\n");
        }

        // 用户场景
        prompt.append("【用户场景】\n");
        if (slots.getTaskScene() != null && !slots.getTaskScene().isBlank()) {
            prompt.append("- 场景: ").append(getSceneDescription(slots.getTaskScene())).append("\n");
        }
        if (slots.getTimeContext() != null && !"now".equals(slots.getTimeContext())) {
            prompt.append("- 时间: ").append(getTimeDescription(slots.getTimeContext())).append("\n");
        }
        if (slots.getTimeSlot() != null && !slots.getTimeSlot().isBlank()) {
            prompt.append("- 时间槽位: ").append(slots.getTimeSlot()).append("\n");
        }
        if (slots.getPreferences() != null && !slots.getPreferences().isEmpty()) {
            prompt.append("- 偏好: ").append(String.join("、", slots.getPreferences())).append("\n");
        }
        if (isLateNight(slots.getTimeSlot())) {
            prompt.append("- 额外约束: 深夜主干道优先，宁可略绕也不要走偏僻小路\n");
        }
        if (slots.isUrgent()) {
            prompt.append("- 紧急: 还剩").append(slots.getUrgencyMinutes()).append("分钟\n");
        }

        return prompt.toString();
    }

    private String buildRouteSelectionPrompt(List<Map<String, Object>> paths, NavigationSegmentSlots segmentSlots) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("请从以下路线中选择最适合该路线段的一条：\n\n");
        for (int i = 0; i < paths.size(); i++) {
            Map<String, Object> path = paths.get(i);
            prompt.append("【路线").append(i + 1).append("】\n");
            prompt.append("- 距离: ").append(path.get("distance")).append("米\n");
            prompt.append("- 时间: ").append(((Number) path.get("duration")).intValue() / 60).append("分钟\n");
            String summary = (String) path.getOrDefault("summary", "");
            if (summary != null && !summary.isBlank()) {
                prompt.append("- 特点: ").append(summary).append("\n");
            }
            List<Map<String, Object>> steps = (List<Map<String, Object>>) path.get("steps");
            if (steps != null && !steps.isEmpty()) {
                prompt.append("- 步骤: 共").append(steps.size()).append("步\n");
            }
            prompt.append("\n");
        }

        prompt.append("【该段槽位】\n");
        prompt.append("- 终点: ").append(segmentSlots.getDestination()).append("\n");
        if (segmentSlots.getSegmentGoal() != null && !segmentSlots.getSegmentGoal().isBlank()) {
            prompt.append("- 目标: ").append(segmentSlots.getSegmentGoal()).append("\n");
        }
        if (segmentSlots.getTaskScene() != null && !segmentSlots.getTaskScene().isBlank()) {
            prompt.append("- 场景: ").append(getSceneDescription(segmentSlots.getTaskScene())).append("\n");
        }
        if (segmentSlots.getTimeContext() != null && !segmentSlots.getTimeContext().isBlank()) {
            prompt.append("- 时间: ").append(getTimeDescription(segmentSlots.getTimeContext())).append("\n");
        }
        if (segmentSlots.getTimeSlot() != null && !segmentSlots.getTimeSlot().isBlank()) {
            prompt.append("- 时间槽位: ").append(segmentSlots.getTimeSlot()).append("\n");
        }
        if (segmentSlots.hasPreferences()) {
            prompt.append("- 偏好: ").append(String.join("、", segmentSlots.getPreferences())).append("\n");
        }
        if (segmentSlots.hasConstraints()) {
            prompt.append("- 约束: ").append(String.join("、", segmentSlots.getConstraints())).append("\n");
        }
        if (shouldPreferMainRoad(segmentSlots.getTimeSlot(), segmentSlots.getConstraints())) {
            prompt.append("- 额外约束: 深夜主干道优先，宁可略绕也不要走偏僻小路\n");
        }
        if (segmentSlots.isUrgent()) {
            prompt.append("- 紧急: 还剩").append(segmentSlots.getUrgencyMinutes()).append("分钟\n");
        }

        return prompt.toString();
    }

    /**
     * 获取路线选择系统提示
     */
    private String getRouteSelectionSystemPrompt() {
        return """
            你是一个校园导航路线选择专家。

            当前任务：从多条路线中选择最适合用户场景的路线。

            【选择原则】
            1. 赶时间：优先选择时间最短的
            2. 夜间出行：优先选择安全、照明好的路线
            3. 深夜出行或 prefer_main_road 约束：主干道优先，宁可稍远也不要穿偏僻小路
            4. 搬行李：避开有台阶的路线
            5. 晚高峰或避开主干道：尽量减少主干道和人流最密集路段
            6. 大太阳时段：优先减少暴晒路段
            7. 日常出行：综合考虑距离和时间

            【输出要求】
            必须输出JSON格式，包含以下字段：
            - selectedIndex: 整数，选中的路线编号（从0开始）
            - reason: 字符串，选择理由（一句话）
            - scores: 数组，每条路线的评分信息

            【示例输出】
            {
              "selectedIndex": 0,
              "reason": "路线1时间最短，适合赶时间场景",
              "scores": [
                {"index": 0, "score": 9, "summary": "距离800米，预计8分钟", "reason": "最短最快"},
                {"index": 1, "score": 7, "summary": "距离1000米，预计10分钟", "reason": "稍远但更平坦"},
                {"index": 2, "score": 5, "summary": "距离1200米，预计12分钟", "reason": "绕行较多"}
              ]
            }

            注意：输出必须是纯JSON格式，不要包含其他文字。
            """;
    }

    private boolean isLateNight(String timeSlot) {
        return "late_night".equals(timeSlot);
    }

    private boolean shouldPreferMainRoad(String timeSlot, List<String> constraints) {
        return isLateNight(timeSlot) || (constraints != null && constraints.contains("prefer_main_road"));
    }

    /**
     * 解析路线选择结果
     */
    private RouteSelectionResult parseRouteSelectionResult(String llmResponse, List<Map<String, Object>> paths) {
        try {
            String json = extractJson(llmResponse);
            JsonNode root = objectMapper.readTree(json);

            int selectedIndex = root.path("selectedIndex").asInt(0);
            if (selectedIndex < 0 || selectedIndex >= paths.size()) {
                selectedIndex = 0;
            }
            String reason = root.path("reason").asText("");

            // 解析所有评分
            List<RouteScore> allScores = new ArrayList<>();
            JsonNode scoresNode = root.path("scores");
            if (scoresNode.isArray()) {
                for (JsonNode scoreNode : scoresNode) {
                    allScores.add(RouteScore.builder()
                            .index(scoreNode.path("index").asInt(0))
                            .score(scoreNode.path("score").asInt(7))
                            .summary(scoreNode.path("summary").asText(""))
                            .reason(scoreNode.path("reason").asText(""))
                            .build());
                }
            }

            // 如果没有评分信息，构建默认的
            if (allScores.isEmpty()) {
                allScores = buildDefaultScores(paths);
            }

            return RouteSelectionResult.builder()
                    .selectedIndex(selectedIndex)
                    .reason(reason)
                    .allScores(allScores)
                    .build();

        } catch (Exception e) {
            log.warn("解析路线选择结果失败: {}", e.getMessage());
            return RouteSelectionResult.builder()
                    .selectedIndex(0)
                    .reason("解析失败，使用默认路线")
                    .allScores(buildDefaultScores(paths))
                    .build();
        }
    }

    /**
     * 构建默认评分
     */
    private List<RouteScore> buildDefaultScores(List<Map<String, Object>> paths) {
        List<RouteScore> scores = new ArrayList<>();
        for (int i = 0; i < paths.size(); i++) {
            Map<String, Object> path = paths.get(i);
            int distance = path.get("distance") instanceof Number number ? number.intValue() : 0;
            int durationMinutes = path.get("duration") instanceof Number number ? number.intValue() / 60 : 0;
            scores.add(RouteScore.builder()
                    .index(i)
                    .score(7)
                    .summary((String) path.getOrDefault("summary", "距离" + distance + "米，预计" + durationMinutes + "分钟"))
                    .reason("默认选择逻辑，优先保持路线稳定")
                    .build());
        }
        return scores;
    }
}
