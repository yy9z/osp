package com.caspar.entity.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 失物招领VO（用于列表和详情展示）
 */
@Data
public class LostFoundVO {
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
     * 图片URL列表（JSON字符串格式）
     */
    private String images;

    /**
     * 图片URL列表（转换后的List）
     */
    private List<String> imageList;

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
     * 状态: OPEN, RESOLVED, REMOVED
     */
    private String status;

    /**
     * 发布者ID
     */
    private Long publisherId;

    /**
     * 发布者名称
     */
    private String publisherName;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
