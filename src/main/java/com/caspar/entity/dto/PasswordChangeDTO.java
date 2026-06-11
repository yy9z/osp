package com.caspar.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码请求DTO
 */
@Data
public class PasswordChangeDTO {
    @NotBlank(message = "原密码不能为空")
    @Size(max = 64, message = "原密码长度不能超过64位")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 64, message = "新密码长度需为6-64位")
    private String newPassword;
}
