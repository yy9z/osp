package com.caspar.entity.dto;

import lombok.Data;

import java.util.List;

/**
 * 校区面交建议响应。
 */
@Data
public class SecondhandMeetupRecommendationVO {

    private String buyerCampus;

    private String sellerCampus;

    private String recommendedCampus;

    private String reason;

    private List<String> suggestedSpots;

    private List<String> suggestedTimeSlots;

    private List<String> safetyTips;
}

