package com.caspar.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DormitoryRepair {
    private Long id;
    private Long dormitoryId;
    private Long userId;
    private String description;
    private String images;
    private String urgency;
    private String status;
    private String remark;
    private String handleImages;
    private Long handlerId;
    private LocalDateTime handleTime;
    private LocalDateTime processingStartedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    private String campus;
    private String building;
    private String roomNo;
}
