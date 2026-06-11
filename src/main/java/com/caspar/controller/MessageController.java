package com.caspar.controller;

import com.caspar.common.PageResult;
import com.caspar.common.Result;
import com.caspar.entity.dto.ConversationVO;
import com.caspar.entity.dto.MessageSendDTO;
import com.caspar.entity.dto.MessageVO;
import com.caspar.service.MessageService;
import com.caspar.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息控制器
 */
@RestController
@RequestMapping("/api/message")
@Slf4j
public class MessageController {

    @Autowired
    private MessageService messageService;

    /**
     * 发送消息
     */
    @PostMapping("/send")
    public Result<Map<String, Long>> sendMessage(@Valid @RequestBody MessageSendDTO messageDTO) {
        Long senderId = SecurityUtils.getCurrentUserId();
        if (senderId == null) {
            return Result.unauthorized();
        }

        // Refactor: 入参校验已在 Service 层执行，全局异常处理器主动返回 badRequest
        try {
            Long messageId = messageService.sendMessage(senderId, messageDTO);
            Map<String, Long> data = new HashMap<>();
            data.put("messageId", messageId);
            return Result.success("消息发送成功", data);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("发送消息失败, senderId={}, receiverId={}", senderId, messageDTO.getReceiverId(), e);
            return Result.error("发送消息失败");
        }
    }

    /**
     * 获取会话列表
     */
    @GetMapping("/conversations")
    public Result<List<ConversationVO>> getConversations() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }

        try {
            List<ConversationVO> conversations = messageService.getConversations(userId);
            return Result.success(conversations);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("获取会话列表失败, userId={}", userId, e);
            return Result.error("获取会话列表失败");
        }
    }

    /**
     * 获取消息详情（与指定用户的聊天记录）
     */
    @GetMapping("/history/{userId}")
    public Result<PageResult<MessageVO>> getHistory(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {

        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            return Result.unauthorized();
        }

        try {
            PageResult<MessageVO> result = messageService.getHistory(currentUserId, userId, page, size);
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("获取消息记录失败, currentUserId={}, otherUserId={}, page={}, size={}",
                    currentUserId, userId, page, size, e);
            return Result.error("获取消息记录失败");
        }
    }

    /**
     * 获取增量消息（ID > sinceId），用于轮询场景减少全量拉取开销。
     */
    @GetMapping("/history/{userId}/since")
    public Result<List<MessageVO>> getHistorySince(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") Long sinceId,
            @RequestParam(defaultValue = "20") Integer size) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            return Result.unauthorized();
        }

        try {
            List<MessageVO> records = messageService.getHistorySince(currentUserId, userId, sinceId, size);
            return Result.success(records);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("获取增量消息失败, currentUserId={}, otherUserId={}, sinceId={}, size={}",
                    currentUserId, userId, sinceId, size, e);
            return Result.error("获取增量消息失败");
        }
    }

    /**
     * 标记消息为已读
     */
    @PutMapping("/read/{userId}")
    public Result<Void> markAsRead(@PathVariable Long userId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            return Result.unauthorized();
        }

        try {
            messageService.markAsRead(currentUserId, userId);
            // 无论是否有未读消息需要更新，都返回成功
            return Result.success("标记已读成功", null);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("标记消息已读失败, currentUserId={}, otherUserId={}", currentUserId, userId, e);
            return Result.error("标记已读失败");
        }
    }

    /**
     * 获取当前用户未读消息总数
     */
    @GetMapping("/unread-count")
    public Result<Integer> getUnreadCount() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }

        try {
            int count = messageService.getUnreadCountAll(userId);
            return Result.success(count);
        } catch (Exception e) {
            log.error("获取未读消息总数失败, userId={}", userId, e);
            return Result.error("获取未读消息数失败");
        }
    }

    /**
     * 获取或创建会话（幂等接口）
     * 返回与指定用户的会话信息，如果不存在历史消息则返回用户信息用于创建新会话
     */
    @GetMapping("/conversation/{otherUserId}")
    public Result<ConversationVO> getOrCreateConversation(@PathVariable Long otherUserId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            return Result.unauthorized();
        }

        try {
            ConversationVO conversation = messageService.getOrCreateConversation(currentUserId, otherUserId);
            return Result.success(conversation);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("获取或创建会话失败, currentUserId={}, otherUserId={}", currentUserId, otherUserId, e);
            return Result.error("获取会话失败");
        }
    }

    /**
     * 置顶/取消置顶会话
     */
    @PutMapping("/conversation/{otherUserId}/pin")
    public Result<Void> pinConversation(
            @PathVariable Long otherUserId,
            @RequestParam(defaultValue = "true") boolean pinned) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }

        try {
            messageService.pinConversation(userId, otherUserId, pinned);
            return Result.success(pinned ? "置顶成功" : "取消置顶成功", null);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("置顶会话操作失败, userId={}, otherUserId={}, pinned={}", userId, otherUserId, pinned, e);
            return Result.error("操作失败");
        }
    }

    /**
     * 删除会话（逻辑删除）
     */
    @DeleteMapping("/conversation/{otherUserId}")
    public Result<Void> deleteConversation(@PathVariable Long otherUserId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }

        try {
            messageService.deleteConversation(userId, otherUserId);
            return Result.success("删除成功", null);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("删除会话失败, userId={}, otherUserId={}", userId, otherUserId, e);
            return Result.error("删除失败");
        }
    }

}
