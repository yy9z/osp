package com.caspar.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Long userId;

    private String username;

    private String password;

    private String realName;

    private String studentId;

    private String phone;

    private String email;

    private String role;

    private String avatar;

    private String bio;

    private String status;

    private String managedBuildingName;

    private LocalDateTime createTime;
}
