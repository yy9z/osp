package com.caspar.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserDormitory {
    private Long id;
    private Long userId;
    private String campus;
    private String building;
    private String room;
    private String bed;
    private LocalDateTime checkInDate;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
