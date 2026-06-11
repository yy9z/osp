package com.caspar.entity;

import com.caspar.entity.enums.GoodsStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 二手商品实体类
 */
@Data
public class SecondhandGoods {
    /**
     * 商品ID
     */
    private Long id;

    /**
     * 商品标题
     */
    private String title;

    /**
     * 商品描述
     */
    private String description;

    /**
     * 商品价格
     */
    private BigDecimal price;

    /**
     * 分类: ELECTRONICS, BOOKS, CLOTHING, SPORTS, DAILY, OTHER
     */
    private String category;

    /**
     * 图片URL列表，JSON数组格式
     */
    private String images;

    /**
     * 新旧程度: NEW, LIKE_NEW, GOOD, FAIR
     */
    private String condition;

    /**
     * 状态: AUDITING(审核中), ACTIVE(在售), SOLD(已售出)
     */
    private GoodsStatus status;

    /**
     * 卖家ID
     */
    private Long sellerId;

    /**
     * 浏览次数
     */
    private Integer viewCount;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    private String rejectReason;

    private String removeReason;

    private Long handlerId;

    private LocalDateTime handleTime;

    private Boolean deleted;

    private Long deletedBy;

    private LocalDateTime deletedAt;

    private String deleteReason;
}
