package com.caspar.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 失物招领认领请求DTO
 */
@Data
public class LostFoundClaimDTO {
    /**
     * 认领说明
     */
    @NotBlank(message = "认领说明不能为空")
    @Size(max = 500, message = "认领说明长度不能超过500个字符")
    private String message;
}
