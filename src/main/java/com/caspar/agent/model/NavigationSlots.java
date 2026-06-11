package com.caspar.agent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 导航槽位模型 - 扩展为6类槽位
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NavigationSlots {

    // A. 目的地槽位
    private String destination;              // 显式地点名
    private String destinationType;          // 语义目标类型：nearest_canteen, nearest_print_shop, etc.
    private List<String> waypoints;          // 途经点列表（多地点导航）

    // B. 起点槽位
    private String origin;                   // 起点
    private String originSource;             // 起点来源：user_input, context, location, default

    // C. 出行方式槽位
    private String travelMode;               // walking, cycling, mixed, accessible

    // D. 偏好槽位
    private List<String> preferences = new ArrayList<>();  // fastest, shortest, safeNight, avoidCrowd, etc.

    // E. 任务场景槽位
    private String taskScene;                // class_commute, dining, express, study, etc.

    // F. 时间/上下文槽位
    private String timeContext;              // now, night, after_class, before_exam
    private String timeSlot;                 // morning, noon_heat, evening_peak, night, etc.
    private Integer urgencyMinutes;          // 紧急程度（还有几分钟）

    // 其他辅助字段
    private String originalQuery;            // 用户原始请求，用于分段槽位分析
    private String campus;                   // 校区
    private Double userLat;
    private Double userLng;

    /**
     * 添加偏好
     */
    public void addPreference(String preference) {
        if (preference != null && !preference.isBlank() && !preferences.contains(preference)) {
            preferences.add(preference);
        }
    }

    /**
     * 判断是否有紧急时间要求
     */
    public boolean isUrgent() {
        return urgencyMinutes != null && urgencyMinutes > 0 && urgencyMinutes <= 15;
    }

    /**
     * 判断是否夜间场景
     */
    public boolean isNightTime() {
        return "night".equals(timeContext) || preferences.contains("safeNight");
    }

    /**
     * 判断是否多地点导航
     */
    public boolean isMultiWaypoint() {
        return waypoints != null && !waypoints.isEmpty();
    }

    /**
     * 添加途经点
     */
    public void addWaypoint(String waypoint) {
        if (waypoints == null) {
            waypoints = new ArrayList<>();
        }
        if (waypoint != null && !waypoint.isBlank() && !waypoints.contains(waypoint)) {
            waypoints.add(waypoint);
        }
    }
}
