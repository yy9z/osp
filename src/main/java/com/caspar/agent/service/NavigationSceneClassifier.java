package com.caspar.agent.service;

import com.caspar.agent.model.NavigationSlots;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 导航场景分类服务
 */
@Service
@RequiredArgsConstructor
public class NavigationSceneClassifier {

    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");
    private static final Set<String> KNOWN_TIME_CONTEXTS = Set.of("now", "night", "after_class", "before_exam");
    private static final Set<String> KNOWN_TIME_SLOTS = Set.of(
            "early_morning", "morning", "noon_heat", "afternoon_heat", "evening_peak", "night", "late_night"
    );

    private final CampusPoiDictionary poiDictionary;

    public LocalDateTime getBeijingNow() {
        return LocalDateTime.now(BEIJING_ZONE);
    }

    /**
     * 识别任务场景
     */
    public String classifyScene(String userInput, NavigationSlots slots) {
        String input = userInput.toLowerCase();

        // 上课赶路
        if (input.contains("上课") || input.contains("赶课") || input.contains("去上课") ||
            (slots.getUrgencyMinutes() != null && slots.getDestinationType() != null &&
             slots.getDestinationType().contains("teaching"))) {
            return "class_commute";
        }

        // 就餐
        if (input.contains("吃饭") || input.contains("食堂") || input.contains("就餐") ||
            "canteen".equals(poiDictionary.detectSemanticType(input))) {
            return "dining";
        }

        // 取快递
        if (input.contains("快递") || input.contains("取件") || input.contains("菜鸟") ||
            "express".equals(poiDictionary.detectSemanticType(input))) {
            return "express";
        }

        // 学习
        if (input.contains("自习") || input.contains("学习") || input.contains("图书馆") ||
            "study".equals(poiDictionary.detectSemanticType(input)) ||
            "library".equals(poiDictionary.detectSemanticType(input))) {
            return "study";
        }

        // 就医
        if (input.contains("看病") || input.contains("医院") || input.contains("就医") ||
            "medical".equals(poiDictionary.detectSemanticType(input))) {
            return "medical";
        }

        // 回宿舍
        if (input.contains("回宿舍") || input.contains("回去") || input.contains("回寝")) {
            return "return_dorm";
        }

        // 新生导览
        if (input.contains("熟悉校园") || input.contains("新生") || input.contains("导览") || input.contains("参观")) {
            return "campus_tour";
        }

        // 多地点导航
        if (slots.isMultiWaypoint()) {
            return "multi_waypoint";
        }

        // 日常便利
        return "daily_convenience";
    }

    /**
     * 推断偏好
     */
    public List<String> inferPreferences(String userInput, NavigationSlots slots, String scene) {
        List<String> preferences = new ArrayList<>();
        String input = userInput.toLowerCase();

        // 显式偏好
        if (input.contains("最快") || input.contains("快点") || input.contains("赶时间")) {
            preferences.add("fastest");
        }
        if (input.contains("最短") || input.contains("近一点")) {
            preferences.add("shortest");
        }
        if (input.contains("安全") || input.contains("晚上") || input.contains("夜间")) {
            preferences.add("safeNight");
        }
        if (input.contains("避开人多") || input.contains("人少") || input.contains("不要太挤")) {
            preferences.add("avoidCrowd");
        }
        if (input.contains("搬行李") || input.contains("拿东西") || input.contains("不要台阶")) {
            preferences.add("luggageFriendly");
        }

        // 场景隐式偏好
        if ("class_commute".equals(scene)) {
            if (slots.isUrgent()) {
                preferences.add("fastest");
            }
        }

        if ("return_dorm".equals(scene)) {
            LocalTime now = LocalTime.now(BEIJING_ZONE);
            if (now.isAfter(LocalTime.of(20, 0)) || now.isBefore(LocalTime.of(6, 0))) {
                preferences.add("safeNight");
            }
        }

        if ("dining".equals(scene)) {
            LocalTime now = LocalTime.now(BEIJING_ZONE);
            // 下课高峰期
            if (isClassEndTime(now)) {
                preferences.add("avoidCrowd");
            }
        }

        // 如果用户没有补充更具体偏好，默认由时间槽位驱动
        String timeSlot = slots != null ? slots.getTimeSlot() : null;
        if (timeSlot != null && !timeSlot.isBlank()) {
            switch (timeSlot) {
                case "noon_heat", "afternoon_heat" -> preferences.add("avoidSun");
                case "evening_peak" -> preferences.add("avoidCrowd");
                case "night", "late_night" -> preferences.add("safeNight");
                default -> {
                }
            }
        }

        return preferences;
    }

    /**
     * 推断时间上下文
     */
    public String inferTimeContext(String userInput) {
        return inferTimeContext(userInput, getBeijingNow());
    }

    public String inferTimeContext(String userInput, LocalDateTime now) {
        String input = userInput == null ? "" : userInput.toLowerCase();

        if (input.contains("晚上") || input.contains("夜间") || input.contains("天黑")) {
            return "night";
        }
        if (input.contains("下课") || input.contains("下课后")) {
            return "after_class";
        }
        if (input.contains("考试前") || input.contains("考试")) {
            return "before_exam";
        }

        LocalTime currentTime = now.toLocalTime();
        if (currentTime.isAfter(LocalTime.of(20, 0)) || currentTime.isBefore(LocalTime.of(6, 0))) {
            return "night";
        }

        return "now";
    }

    /**
     * 推断时间槽位。用于更细粒度地描述当前时间带来的导航特征。
     */
    public String inferTimeSlot(String userInput, LocalDateTime now) {
        String input = userInput == null ? "" : userInput.toLowerCase();
        if (input.contains("中午") || input.contains("烈日") || input.contains("大太阳")) {
            return "noon_heat";
        }
        if (input.contains("傍晚高峰") || input.contains("下班高峰")) {
            return "evening_peak";
        }
        if (input.contains("晚上") || input.contains("夜里") || input.contains("夜间")) {
            return now.getHour() >= 22 ? "late_night" : "night";
        }

        int hour = now.getHour();
        if (hour >= 5 && hour < 8) {
            return "early_morning";
        }
        if (hour < 11) {
            return "morning";
        }
        if (hour < 14) {
            return "noon_heat";
        }
        if (hour < 17) {
            return "afternoon_heat";
        }
        if (hour < 19) {
            return "evening_peak";
        }
        if (hour < 22) {
            return "night";
        }
        return "late_night";
    }

    public String normalizeTimeContext(String candidate, String userInput, LocalDateTime now) {
        String normalized = normalizeToken(candidate);
        if (normalized == null) {
            return inferTimeContext(userInput, now);
        }
        if (KNOWN_TIME_SLOTS.contains(normalized)) {
            return inferTimeContext(userInput, now);
        }
        if (KNOWN_TIME_CONTEXTS.contains(normalized)) {
            return normalized;
        }
        return inferTimeContext(userInput, now);
    }

    public String normalizeTimeSlot(String candidate, String userInput, LocalDateTime now) {
        String normalized = normalizeToken(candidate);
        if (normalized == null) {
            return inferTimeSlot(userInput, now);
        }
        if (KNOWN_TIME_SLOTS.contains(normalized)) {
            return normalized;
        }
        return inferTimeSlot(userInput, now);
    }

    public boolean isKnownTimeSlot(String value) {
        return value != null && KNOWN_TIME_SLOTS.contains(normalizeToken(value));
    }

    public boolean isKnownTimeContext(String value) {
        return value != null && KNOWN_TIME_CONTEXTS.contains(normalizeToken(value));
    }

    /**
     * 提取紧急程度（分钟数）
     */
    public Integer extractUrgencyMinutes(String userInput) {
        String input = userInput.toLowerCase();

        // 匹配 "还有X分钟"
        if (input.matches(".*还有\\s*(\\d+)\\s*分钟.*")) {
            try {
                String minutes = input.replaceAll(".*还有\\s*(\\d+)\\s*分钟.*", "$1");
                return Integer.parseInt(minutes);
            } catch (Exception ignored) {}
        }

        // 匹配 "X分钟后"
        if (input.matches(".*(\\d+)\\s*分钟后.*")) {
            try {
                String minutes = input.replaceAll(".*(\\d+)\\s*分钟后.*", "$1");
                return Integer.parseInt(minutes);
            } catch (Exception ignored) {}
        }

        return null;
    }

    private boolean isClassEndTime(LocalTime time) {
        // 简化判断：整点前后10分钟算下课高峰
        int minute = time.getMinute();
        return (minute >= 50 || minute <= 10) &&
               (time.getHour() >= 8 && time.getHour() <= 18);
    }

    private String normalizeToken(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase().replace('-', '_');
    }
}
