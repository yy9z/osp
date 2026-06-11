package com.caspar.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 商品收藏实体类
 */
@Data
public class GoodsFavorite {
    /**
     * 收藏ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 商品ID
     */
    private Long goodsId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
