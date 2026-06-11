package com.caspar.agent.tool;

import com.caspar.agent.model.ToolArgs;
import com.caspar.agent.model.ToolResult;
import com.caspar.entity.Notification;
import com.caspar.entity.dto.ConversationVO;
import com.caspar.mapper.NotificationMapper;
import com.caspar.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息查询工具：查询当前用户的最近通知、会话和未读消息数。
 */
@Component
@RequiredArgsConstructor
public class MessageTool implements AgentTool {

    private final NotificationMapper notificationMapper;
    private final MessageService messageService;

    @Override
    public String getName() {
        return "message_query";
    }

    @Override
    public ToolResult execute(ToolArgs args) {
        Long userId = args.getUserId();
        if (userId == null) {
            return ToolResult.fail("无法获取当前用户身份，请重新登录后再试。");
        }

        List<Notification> notifications = notificationMapper.selectRecentNotificationsByUserId(userId, 5);
        List<ConversationVO> allConversations = messageService.getConversations(userId);
        List<ConversationVO> conversations = (allConversations == null ? List.<ConversationVO>of() : allConversations).stream()
                .limit(5)
                .toList();
        int unreadCount = messageService.getUnreadCountAll(userId);

        int notificationCount = notifications == null ? 0 : notifications.size();
        int conversationCount = conversations.size();

        Map<String, Object> data = new HashMap<>();
        data.put("type", "MESSAGE_QUERY");
        data.put("total", notificationCount + conversationCount);
        data.put("notificationCount", notificationCount);
        data.put("conversationCount", conversationCount);
        data.put("unreadCount", unreadCount);
        data.put("notifications", notifications == null ? List.of() : notifications);
        data.put("conversations", conversations);
        data.put("messageCenterPath", "/messages");

        String summary = buildSummary(notificationCount, conversationCount, unreadCount);
        data.put("agentSummary", summary);

        return ToolResult.ok(summary, data);
    }

    private String buildSummary(int notificationCount, int conversationCount, int unreadCount) {
        if (notificationCount == 0 && conversationCount == 0) {
            return "您暂无新通知，也没有最近会话。";
        }
        if (unreadCount > 0) {
            return String.format("您有 %d 条未读站内消息，另有 %d 条最新通知。", unreadCount, notificationCount);
        }
        return String.format("为您找到 %d 条最新通知和 %d 个最近会话。", notificationCount, conversationCount);
    }
}
