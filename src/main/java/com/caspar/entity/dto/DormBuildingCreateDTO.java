package com.caspar.entity.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DormBuildingCreateDTO {
    @NotBlank(message = "楼栋名称不能为空")
    @Size(max = 64, message = "楼栋名称长度不能超过64个字符")
    private String name;

    @NotBlank(message = "校区不能为空")
    @Size(max = 32, message = "校区长度不能超过32个字符")
    private String campus;

    @Min(value = 1, message = "楼层数必须大于0")
    private Integer floors;

    @Min(value = 1, message = "每层房间数必须大于0")
    private Integer roomsPerFloor;

    @Min(value = 1, message = "每间房入住人数必须大于0")
    private Integer capacityPerRoom;

    @Size(max = 16, message = "性别字段长度不能超过16个字符")
    private String gender;
    private Long managerId;
}
