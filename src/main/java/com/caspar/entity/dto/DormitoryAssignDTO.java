package com.caspar.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DormitoryAssignDTO {
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "宿舍ID不能为空")
    private Long dormitoryId;

    @Size(max = 32, message = "校区长度不能超过32个字符")
    private String campus;

    @NotBlank(message = "床位不能为空")
    @Size(max = 32, message = "床位长度不能超过32个字符")
    private String bed;
    private LocalDate checkInDate;
}
