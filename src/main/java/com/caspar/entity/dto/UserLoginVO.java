package com.caspar.entity.dto;

import lombok.Data;

@Data
public class UserLoginVO {
    private String token;
    private Long userId;
    private String username;
    private String realName;
    private String studentId;
    private String phone;
    private String email;
    private String role;
    private String avatar;
    private String bio;
}
