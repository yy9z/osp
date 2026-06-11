package com.caspar.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RepairCreateDTO {
    private Long dormitoryId;

    @NotBlank(message = "报修描述不能为空")
    @Size(max = 2000, message = "报修描述长度不能超过2000个字符")
    private String description;

    @Size(max = 4000, message = "图片字段长度不能超过4000个字符")
    private String images;

    @Size(max = 16, message = "紧急程度字段长度不能超过16个字符")
    private String urgency;
    
    @Size(max = 32, message = "校区长度不能超过32个字符")
    private String campus;

    @Size(max = 64, message = "楼栋长度不能超过64个字符")
    private String building;

    @Size(max = 32, message = "房间号长度不能超过32个字符")
    private String roomNo;
}
