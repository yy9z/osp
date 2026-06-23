package com.caspar.entity.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 发布者查看的认领申请。
 */
@Data
public class LostFoundClaimVO {

    private Long id;

    private Long lostfoundId;

    private Long claimerId;

    private String claimerName;

    private String claimerPhone;

    private String message;

    private String status;

    private LocalDateTime createTime;
}
