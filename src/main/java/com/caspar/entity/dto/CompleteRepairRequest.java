package com.caspar.entity.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CompleteRepairRequest {
    @Size(max = 1000, message = "处理备注长度不能超过1000个字符")
    private String remark;

    @Size(max = 4000, message = "处理图片字段长度不能超过4000个字符")
    private String handleImages;
}
