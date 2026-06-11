package com.caspar.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 二手求购订阅实体。
 */
@Data
public class SecondhandSubscription {

    private Long id;

    private Long userId;

    /**
     * 关键词（必填）。
     */
    private String keyword;

    /**
     * 分类（可选）。
     */
    private String category;

    /**
     * 预算上限（可选）。
     */
    private BigDecimal maxPrice;

    /**
     * 校区偏好（可选）。
     */
    private String campus;

    /**
     * ACTIVE / INACTIVE
     */
    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private LocalDateTime lastNotifiedAt;
}

