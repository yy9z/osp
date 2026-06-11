package com.caspar.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DormBuilding {
    private Long id;
    private String name;
    private String code;
    private String campus;
    private Integer floors;
    private Integer roomsPerFloor;
    private Integer capacityPerRoom;
    private String gender;
    private Long managerId;
    private String managerName;
    private LocalDateTime createTime;
}
