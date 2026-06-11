package com.caspar.entity.dto;

import lombok.Data;

@Data
public class DormitoryAssignableUserVO {
    private Long userId;
    private String username;
    private String realName;
    private String phone;
    private String role;
    private String dormitoryBuilding;
    private String dormitoryRoom;
    private String dormitoryBed;

    public boolean getAssigned() {
        return dormitoryBuilding != null && !dormitoryBuilding.isEmpty()
                && dormitoryRoom != null && !dormitoryRoom.isEmpty();
    }

    public String getDormitoryText() {
        if (!getAssigned()) {
            return "未分配";
        }
        String bedText = dormitoryBed != null && !dormitoryBed.isEmpty() ? " " + dormitoryBed : "";
        return dormitoryBuilding + " " + dormitoryRoom + bedText;
    }
}