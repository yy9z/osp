package com.caspar.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 路径规划请求DTO
 */
@Data
@Schema(description = "路径规划请求")
public class RouteRequest {

    @Schema(description = "起点纬度", example = "31.8384")
    @NotNull(message = "起点纬度不能为空")
    private BigDecimal fromLat;

    @Schema(description = "起点经度", example = "117.2167")
    @NotNull(message = "起点经度不能为空")
    private BigDecimal fromLng;

    @Schema(description = "终点纬度", example = "31.8395")
    @NotNull(message = "终点纬度不能为空")
    private BigDecimal toLat;

    @Schema(description = "终点经度", example = "117.2155")
    @NotNull(message = "终点经度不能为空")
    private BigDecimal toLng;

    @Schema(description = "出行方式: WALK(步行), CYCLE(骑行)", example = "WALK")
    private String wayType = "WALK";
}
