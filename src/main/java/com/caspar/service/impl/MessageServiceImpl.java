package com.caspar.service.impl;

import com.caspar.common.PageResult;
import com.caspar.entity.ConversationSetting;
import com.caspar.entity.Message;
import com.caspar.entity.User;
import com.caspar.entity.dto.ConversationVO;
import com.caspar.entity.dto.MessageSendDTO;
import com.caspar.entity.dto.MessageVO;
import com.caspar.mapper.ConversationSettingMapper;
import com.caspar.mapper.MessageMapper;
import com.caspar.mapper.UserMapper;
import com.caspar.service.MessageService;
import com.caspar.util.PaginationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ConversationSettingMapper conversationSettingMapper;

    @Override
    @Transactional
    public Long sendMessage(Long senderId, MessageSendDTO messageDTO) {
        if (messageDTO.getReceiverId() == null) {
            throw new IllegalArgumentException("接收者ID不能为空");
        }
        if (messageDTO.getContent() == null || messageDTO.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("消息内容不能为空");
        }

        Message message = new Message();
        message.setSenderId(senderId);
        message.setReceiverId(messageDTO.getReceiverId());
        message.setContent(messageDTO.getContent());
        message.setRelatedType(messageDTO.getRelatedType());
        message.setRelatedId(messageDTO.getRelatedId());
        message.setIsRead(0);
        message.setCreateTime(LocalDateTime.now());

        messageMapper.insert(message);
        return message.getId();
    }

    @Override
    public List<ConversationVO> getConversations(Long userId) {
        List<ConversationVO> conversations = messageMapper.selectConversations(userId);

        // Refactor: 移除 conversation.setLastMessageTime(conversation.getLastMessageTime()) 无意义自赋值
        for (ConversationVO conversation : conversations) {
            User user = userMapper.findById(conversation.getUserId());
            if (user != null) {
                conversation.setUsername(user.getUsername());
                conversation.setRealName(user.getRealName());
                conversation.setAvatar(user.getAvatar());
            }
            ConversationSetting setting = conversationSettingMapper.findByUserAndOther(userId, conversation.getUserId());
            if (setting != null) {
                conversation.setIsPinned(setting.getIsPinned());
            }
        }

        return conversations.stream()
                .sorted((a, b) -> {
                    Integer aPinned = a.getIsPinned() != null ? a.getIsPinned() : 0;
                    Integer bPinned = b.getIsPinned() != null ? b.getIsPinned() : 0;
                    if (bPinned.compareTo(aPinned) != 0) {
                        return bPinned.compareTo(aPinned);
                    }
                    if (a.getLastMessageTime() == null) return 1;
                    if (b.getLastMessageTime() == null) return -1;
                    return b.getLastMessageTime().compareTo(a.getLastMessageTime());
                })
                .collect(Collectors.toList());
    }

    @Override
    public PageResult<MessageVO> getHistory(Long currentUserId, Long otherUserId, Integer page, Integer size) {
        int safePage = PaginationUtils.safePage(page);
        int safeSize = PaginationUtils.safeSize(size);
        int offset = PaginationUtils.offset(safePage, safeSize);

        List<MessageVO> records = messageMapper.selectHistory(currentUserId, otherUserId, offset, safeSize);
        Long total = messageMapper.countHistoryMessages(currentUserId, otherUserId);

        return new PageResult<>(records, total, safePage, safeSize);
    }

    @Override
    public List<MessageVO> getHistorySince(Long currentUserId, Long otherUserId, Long sinceId, Integer size) {
        long safeSinceId = sinceId == null ? 0L : Math.max(0L, sinceId);
        int safeSize = PaginationUtils.safeSize(size);
        return messageMapper.selectHistorySince(currentUserId, otherUserId, safeSinceId, safeSize);
    }

    @Override
    @Transactional
    public boolean markAsRead(Long currentUserId, Long otherUserId) {
        messageMapper.markAsRead(currentUserId, otherUserId);
        return true;
    }

    @Override
    public int getUnreadCount(Long receiverId, Long senderId) {
        return messageMapper.countUnread(receiverId, senderId);
    }

    @Override
    public int getUnreadCountAll(Long userId) {
        return messageMapper.countUnreadAll(userId);
    }

    @Override
    public ConversationVO getOrCreateConversation(Long currentUserId, Long otherUserId) {
        ConversationVO conversation = new ConversationVO();
        conversation.setUserId(otherUserId);

        User user = userMapper.findById(otherUserId);
        if (user != null) {
            conversation.setUsername(user.getUsername());
            conversation.setRealName(user.getRealName());
            conversation.setAvatar(user.getAvatar());
        }

        ConversationSetting setting = conversationSettingMapper.findByUserAndOther(currentUserId, otherUserId);
        if (setting != null) {
            conversation.setIsPinned(setting.getIsPinned());
        }

        return conversation;
    }

    @Override
    @Transactional
    public boolean pinConversation(Long userId, Long otherUserId, boolean pinned) {
        ConversationSetting setting = conversationSettingMapper.findByUserAndOther(userId, otherUserId);
        if (setting == null) {
            setting = new ConversationSetting();
            setting.setUserId(userId);
            setting.setOtherUserId(otherUserId);
            setting.setIsPinned(pinned ? 1 : 0);
            conversationSettingMapper.insert(setting);
        } else {
            setting.setIsPinned(pinned ? 1 : 0);
            conversationSettingMapper.update(setting);
        }
        return true;
    }

    @Override
    @Transactional
    public boolean deleteConversation(Long userId, Long otherUserId) {
        messageMapper.deleteByUsers(userId, otherUserId);
        return true;
    }
}
