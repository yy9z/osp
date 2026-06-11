package com.caspar.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 附近搜索请求DTO
 */
@Data
@Schema(description = "附近搜索请求")
public class NearbySearchRequest {

    @Schema(description = "纬度", example = "31.8384")
    @NotNull(message = "纬度不能为空")
    private BigDecimal lat;

    @Schema(description = "经度", example = "117.2167")
    @NotNull(message = "经度不能为空")
    private BigDecimal lng;

    @Schema(description = "搜索半径（米）", example = "500")
    private Integer radius = 500;

    @Schema(description = "类别过滤（可选）", example = "TEACHING")
    private String category;
}
