package com.caspar.entity.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建求购订阅请求。
 */
@Data
public class SecondhandSubscriptionCreateDTO {

    @NotBlank(message = "订阅关键词不能为空")
    @Size(max = 40, message = "订阅关键词长度不能超过40个字符")
    private String keyword;

    @Size(max = 32, message = "分类长度不能超过32个字符")
    private String category;

    @DecimalMin(value = "0.01", message = "预算上限必须大于0")
    private BigDecimal maxPrice;

    @Size(max = 32, message = "校区长度不能超过32个字符")
    private String campus;
}
