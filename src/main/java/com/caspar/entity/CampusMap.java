package com.caspar.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 校园地图实体类
 */
@Data
public class CampusMap {
    /**
     * 地图ID
     */
    private Long id;

    /**
     * 校园名称
     */
    private String name;

    /**
     * 地图中心纬度
     */
    private BigDecimal centerLat;

    /**
     * 地图中心经度
     */
    private BigDecimal centerLng;

    /**
     * 默认缩放级别
     */
    private Integer zoom;

    /**
     * 校园地图图片URL
     */
    private String imageUrl;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
