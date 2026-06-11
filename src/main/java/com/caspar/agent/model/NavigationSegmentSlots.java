package com.caspar.agent.model;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 单段导航槽位：用于多段行程中对每一段单独建模和评估。
 */
@Data
@Builder
public class NavigationSegmentSlots {

    private Integer index;
    private String fromName;
    private String toName;
    private String destination;
    private String segmentGoal;
    private String taskScene;
    @Builder.Default
    private List<String> preferences = new ArrayList<>();
    @Builder.Default
    private List<String> constraints = new ArrayList<>();
    private String timeContext;
    private String timeSlot;
    private Integer urgencyMinutes;
    private String notes;

    public boolean hasPreferences() {
        return preferences != null && !preferences.isEmpty();
    }

    public boolean hasConstraints() {
        return constraints != null && !constraints.isEmpty();
    }

    public boolean isUrgent() {
        return urgencyMinutes != null && urgencyMinutes > 0 && urgencyMinutes <= 15;
    }
}
