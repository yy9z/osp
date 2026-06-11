package com.caspar.entity.dto;

import com.caspar.entity.enums.GoodsStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 二手商品VO（用于列表展示）
 */
@Data
public class SecondhandGoodsVO {
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
     * 分类
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
     * 新旧程度
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
     * 卖家名称
     */
    private String sellerName;

    /**
     * 卖家电话
     */
    private String sellerPhone;

    /**
     * 浏览次数
     */
    private Integer viewCount;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    private String rejectReason;

    private String removeReason;

    private Long handlerId;

    private LocalDateTime handleTime;

    private Boolean deleted;

    private Long deletedBy;

    private LocalDateTime deletedAt;

    private String deleteReason;

    /**
     * Agent 推荐理由（非数据库字段，由工具层动态生成）
     */
    private String recommendReason;
}
