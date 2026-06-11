package com.caspar.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 路径规划响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "路径规划响应")
public class RouteResponse {

    @Schema(description = "起点纬度")
    private BigDecimal fromLat;

    @Schema(description = "起点经度")
    private BigDecimal fromLng;

    @Schema(description = "终点纬度")
    private BigDecimal toLat;

    @Schema(description = "终点经度")
    private BigDecimal toLng;

    @Schema(description = "路径总距离（米）")
    private Double totalDistance;

    @Schema(description = "预计步行时间（秒）")
    private Integer estimatedTime;

    @Schema(description = "路径点序列")
    private List<RoutePoint> points;

    @Schema(description = "路径描述")
    private String description;

    /**
     * 路径点
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "路径点")
    public static class RoutePoint {

        @Schema(description = "节点ID")
        private Long nodeId;

        @Schema(description = "节点名称")
        private String nodeName;

        @Schema(description = "纬度")
        private BigDecimal latitude;

        @Schema(description = "经度")
        private BigDecimal longitude;

        @Schema(description = "节点类型: POI, INTERSECTION, WAYPOINT")
        private String nodeType;

        @Schema(description = "从该点出发的累计距离（米）")
        private Double cumulativeDistance;
    }
}
