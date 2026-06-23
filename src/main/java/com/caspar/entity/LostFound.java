package com.caspar.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 失物招领实体类
 */
@Data
public class LostFound {
    /**
     * ID
     */
    private Long id;

    /**
     * 类型: LOST(失物), FOUND(招领)
     */
    private String type;

    /**
     * 标题
     */
    private String title;

    /**
     * 描述
     */
    private String description;

    /**
     * 分类: ELECTRONICS, DOCUMENTS, KEY, CLOTHING, OTHER
     */
    private String category;

    /**
     * 图片URL列表，JSON数组格式
     */
    private String images;

    /**
     * 丢失/拾取地点
     */
    private String location;

    /**
     * 丢失/拾取时间
     */
    private LocalDateTime lostTime;

    /**
     * 悬赏金额
     */
    private BigDecimal reward;

    /**
     * 发布者为本条信息提供的联系方式
     */
    private String contact;

    /**
     * 状态: OPEN, RESOLVED, REMOVED
     */
    private String status;

    /**
     * 发布者ID
     */
    private Long publisherId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
