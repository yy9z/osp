package com.caspar.entity.dto;

import lombok.Data;

import java.util.List;

/**
 * 卖家可信度评分响应。
 */
@Data
public class SellerTrustScoreVO {

    private Long sellerId;

    private Integer score;

    /**
     * HIGH / MEDIUM / LOW
     */
    private String level;

    private Long totalListings;

    private Long approvedCount;

    private Long soldCount;

    private Long rejectedCount;

    private Long removedCount;

    private Double avgReplyMinutes;

    private List<String> highlights;
}

