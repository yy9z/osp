package com.caspar.entity;

import lombok.Data;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 校园POI地点实体类
 * 对应数据库表 campus_poi
 */
@Data
public class CampusPoi {

    /**
     * POI ID
     */
    private Long id;

    /**
     * 地点名称
     */
    private String name;

    /**
     * 类别: TEACHING(教学楼), DINING(食堂), LIBRARY(图书馆),
     * DORMITORY(宿舍), SPORTS(运动场), ADMIN(行政楼),
     * SCENIC(景点), ENTRANCE(校门), ROAD(道路)
     */
    private String category;

    /**
     * 地点描述
     */
    private String description;

    /**
     * 纬度
     */
    private BigDecimal latitude;

    /**
     * 经度
     */
    private BigDecimal longitude;

    /**
     * 空间位置（POINT类型）
     */
    private Point location;

    /**
     * 楼层信息，如"1-6层"
     */
    private String floor;

    /**
     * 图片URL
     */
    private String imageUrl;

    /**
     * 开放时间
     */
    private String openTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
