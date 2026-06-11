package com.caspar.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 失物招领认领记录实体类
 */
@Data
public class LostFoundClaim {
    /**
     * ID
     */
    private Long id;

    /**
     * 失物招领ID
     */
    private Long lostfoundId;

    /**
     * 认领者ID
     */
    private Long claimerId;

    /**
     * 认领说明
     */
    private String message;

    /**
     * 状态: PENDING, APPROVED, REJECTED
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
