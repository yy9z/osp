package com.caspar.entity.dto;

import com.caspar.entity.DormitoryRepair;
import lombok.Data;

@Data
public class RepairVO extends DormitoryRepair {
    private String building;
    private String roomNo;
    private String username;
    private String realName;
    private String phone;
    private String handlerName;
}
