package com.caspar.entity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DormBuildingManagerAssignDTO {
    @NotNull(message = "宿管ID不能为空")
    private Long managerId;
}
