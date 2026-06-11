package com.caspar.controller;

import com.caspar.common.Result;
import com.caspar.entity.Message;
import com.caspar.mapper.MessageMapper;
import com.caspar.util.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/message")
public class MessageAdminController {

    @Autowired
    private MessageMapper messageMapper;

    @PostMapping("/broadcast")
    public Result<Void> broadcast(@Valid @RequestBody BroadcastRequest request) {
        String userRole = SecurityUtils.getCurrentUserRole();
        if (!"ADMIN".equals(userRole)) {
            return Result.forbidden();
        }

        Long senderId = SecurityUtils.getCurrentUserId();
        if (senderId == null) {
            return Result.unauthorized();
        }

        Message message = new Message();
        message.setType("BROADCAST");
        message.setTitle(request.getTitle());
        message.setContent(request.getContent());
        message.setSenderId(senderId);
        message.setCreateTime(LocalDateTime.now());

        messageMapper.insertBroadcast(message);
        return Result.success("公告发布成功", null);
    }

    @Data
    public static class BroadcastRequest {
        @NotBlank(message = "标题不能为空")
        @Size(max = 100, message = "标题长度不能超过100个字符")
        private String title;

        @NotBlank(message = "内容不能为空")
        @Size(max = 2000, message = "内容长度不能超过2000个字符")
        private String content;
    }
}
