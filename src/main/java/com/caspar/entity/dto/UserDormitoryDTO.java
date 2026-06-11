package com.caspar.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;

@Data
public class UserDormitoryDTO {
    @NotBlank(message = "校区不能为空")
    @Size(max = 32, message = "校区长度不能超过32个字符")
    private String campus;

    @NotBlank(message = "楼栋不能为空")
    @Size(max = 64, message = "楼栋长度不能超过64个字符")
    private String building;

    @NotBlank(message = "宿舍号不能为空")
    @Size(max = 32, message = "宿舍号长度不能超过32个字符")
    private String room;

    @Size(max = 32, message = "床位长度不能超过32个字符")
    private String bed;
    private LocalDate checkInDate;
}
