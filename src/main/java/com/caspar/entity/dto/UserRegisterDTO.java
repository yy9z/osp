package com.caspar.entity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册请求DTO
 */
@Data
public class UserRegisterDTO {
    @NotBlank(message = "用户名不能为空")
    @Size(max = 32, message = "用户名长度不能超过32个字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度需为6-64位")
    private String password;

    @Size(max = 32, message = "姓名长度不能超过32个字符")
    private String realName;

    @Pattern(
            regexp = "^$|^1\\d{10}$",
            message = "手机号格式不正确"
    )
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 64, message = "邮箱长度不能超过64个字符")
    private String email;

    @Size(max = 20, message = "角色字段长度不能超过20个字符")
    private String role;

    @Size(max = 64, message = "注册密钥长度不能超过64个字符")
    private String registerKey;
}
