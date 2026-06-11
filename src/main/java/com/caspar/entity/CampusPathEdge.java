package com.caspar.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 校园路径边实体类（邻接表）
 * 对应数据库表 campus_path_edge
 */
@Data
public class CampusPathEdge {

    /**
     * 边ID
     */
    private Long id;

    /**
     * 起点节点ID
     */
    private Long fromNodeId;

    /**
     * 终点节点ID
     */
    private Long toNodeId;

    /**
     * 距离（米）
     */
    private Double distance;

    /**
     * 道路类型: WALK(步行), CYCLE(骑行)
     */
    private String wayType;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
