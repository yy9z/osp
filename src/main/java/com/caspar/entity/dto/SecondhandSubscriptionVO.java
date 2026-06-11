package com.caspar.entity.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 求购订阅展示对象。
 */
@Data
public class SecondhandSubscriptionVO {

    private Long id;

    private String keyword;

    private String category;

    private BigDecimal maxPrice;

    private String campus;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime lastNotifiedAt;
}

