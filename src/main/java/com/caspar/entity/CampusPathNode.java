package com.caspar.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 校园路径节点实体类
 * 对应数据库表 campus_path_node
 */
@Data
public class CampusPathNode {

    /**
     * 节点ID
     */
    private Long id;

    /**
     * 节点名称
     */
    private String name;

    /**
     * 纬度
     */
    private BigDecimal latitude;

    /**
     * 经度
     */
    private BigDecimal longitude;

    /**
     * 节点类型: POI(景点), INTERSECTION(交叉路口), WAYPOINT(途径点)
     */
    private String nodeType;

    /**
     * 关联的POI ID（如果是POI类型）
     */
    private Long poiId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
