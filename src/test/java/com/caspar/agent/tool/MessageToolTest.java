package com.caspar.agent.tool;

import com.caspar.agent.model.ToolArgs;
import com.caspar.agent.model.ToolResult;
import com.caspar.entity.Notification;
import com.caspar.entity.dto.ConversationVO;
import com.caspar.mapper.NotificationMapper;
import com.caspar.service.MessageService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MessageToolTest {

    @Test
    void execute_shouldReturnNotificationsConversationsAndUnreadCount() {
        NotificationMapper notificationMapper = mock(NotificationMapper.class);
        MessageService messageService = mock(MessageService.class);
        MessageTool tool = new MessageTool(notificationMapper, messageService);

        Notification notification = new Notification();
        notification.setId(1L);
        notification.setTitle("审核通过");
        notification.setContent("你的二手商品已通过审核");
        notification.setCreateTime(LocalDateTime.now());

        ConversationVO conversation = new ConversationVO();
        conversation.setUserId(2L);
        conversation.setRealName("测试同学");
        conversation.setLastMessage("还在吗？");
        conversation.setUnreadCount(3);

        when(notificationMapper.selectRecentNotificationsByUserId(1L, 5)).thenReturn(List.of(notification));
        when(messageService.getConversations(1L)).thenReturn(List.of(conversation));
        when(messageService.getUnreadCountAll(1L)).thenReturn(3);

        ToolResult result = tool.execute(ToolArgs.builder().userId(1L).params(Map.of()).build());

        assertTrue(result.isSuccess());
        Map<?, ?> data = (Map<?, ?>) result.getData();
        assertEquals(1, data.get("notificationCount"));
        assertEquals(1, data.get("conversationCount"));
        assertEquals(3, data.get("unreadCount"));
        assertFalse(((List<?>) data.get("notifications")).isEmpty());
        assertFalse(((List<?>) data.get("conversations")).isEmpty());
    }
}
