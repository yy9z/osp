package com.caspar.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 消息发送请求DTO
 */
@Data
public class MessageSendDTO {
    /**
     * 接收者ID
     */
    @NotNull(message = "接收者ID不能为空")
    private Long receiverId;

    /**
     * 消息内容
     */
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息内容长度不能超过2000个字符")
    private String content;

    /**
     * 关联类型: SECONDHAND, LOSTFOUND
     */
    @Size(max = 32, message = "关联类型长度不能超过32个字符")
    private String relatedType;

    /**
     * 关联ID
     */
    private Long relatedId;
}
