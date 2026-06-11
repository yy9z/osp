package com.caspar.mapper;

import com.caspar.entity.Message;
import com.caspar.entity.dto.ConversationVO;
import com.caspar.entity.dto.MessageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 消息Mapper接口
 */
@Mapper
public interface MessageMapper {

    /**
     * 插入消息
     *
     * @param message 消息实体
     * @return 影响行数
     */
    int insert(Message message);

    /**
     * 查询与指定用户的聊天记录
     *
     * @param userId1 用户1ID
     * @param userId2 用户2ID
     * @param offset  偏移量
     * @param limit   数量限制
     * @return 消息列表
     */
    List<MessageVO> selectHistory(@Param("userId1") Long userId1,
                                   @Param("userId2") Long userId2,
                                   @Param("offset") Integer offset,
                                   @Param("limit") Integer limit);

    /**
     * 查询增量消息（ID 大于 sinceId 的新消息）。
     *
     * @param userId1  当前用户ID
     * @param userId2  对方用户ID
     * @param sinceId  起始消息ID（不包含）
     * @param limit    数量限制
     * @return 新消息列表（按 ID 升序）
     */
    List<MessageVO> selectHistorySince(@Param("userId1") Long userId1,
                                       @Param("userId2") Long userId2,
                                       @Param("sinceId") Long sinceId,
                                       @Param("limit") Integer limit);

    /**
     * 查询会话列表
     *
     * @param userId 用户ID
     * @return 会话列表
     */
    List<ConversationVO> selectConversations(@Param("userId") Long userId);

    /**
     * 标记消息为已读
     *
     * @param receiverId 接收者ID
     * @param senderId   发送者ID
     * @return 影响行数
     */
    int markAsRead(@Param("receiverId") Long receiverId, @Param("senderId") Long senderId);

    /**
     * 获取与指定用户的未读消息数
     *
     * @param receiverId 接收者ID
     * @param senderId   发送者ID
     * @return 未读消息数
     */
    int countUnread(@Param("receiverId") Long receiverId, @Param("senderId") Long senderId);

    /**
     * 获取当前用户发送的消息总数（用于分页）
     *
     * @param userId 用户ID
     * @return 消息总数
     */
    Long countSentMessages(@Param("userId") Long userId);

    /**
     * 获取与指定用户的历史消息总数（用于分页）
     *
     * @param currentUserId 当前用户ID
     * @param otherUserId    对方用户ID
     * @return 消息总数
     */
    Long countHistoryMessages(@Param("userId1") Long currentUserId, @Param("userId2") Long otherUserId);

    /**
     * 获取当前用户的全部未读消息数
     *
     * @param userId 用户ID
     * @return 未读消息总数
     */
    int countUnreadAll(@Param("userId") Long userId);

    /**
     * 删除两个用户之间的所有消息
     *
     * @param userId1 用户1ID
     * @param userId2 用户2ID
     * @return 影响行数
     */
    int deleteByUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    int insertBroadcast(Message message);

    Double averageFirstReplyMinutes(@Param("userId") Long userId);
}
