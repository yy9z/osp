package com.caspar.service;

import com.caspar.common.PageResult;
import com.caspar.entity.dto.ConversationVO;
import com.caspar.entity.dto.MessageSendDTO;
import com.caspar.entity.dto.MessageVO;

import java.util.List;

public interface MessageService {

    Long sendMessage(Long senderId, MessageSendDTO messageDTO);

    List<ConversationVO> getConversations(Long userId);

    PageResult<MessageVO> getHistory(Long currentUserId, Long otherUserId, Integer page, Integer size);

    List<MessageVO> getHistorySince(Long currentUserId, Long otherUserId, Long sinceId, Integer size);

    boolean markAsRead(Long currentUserId, Long otherUserId);

    int getUnreadCount(Long receiverId, Long senderId);

    int getUnreadCountAll(Long userId);

    ConversationVO getOrCreateConversation(Long currentUserId, Long otherUserId);

    boolean pinConversation(Long userId, Long otherUserId, boolean pinned);

    boolean deleteConversation(Long userId, Long otherUserId);
}
