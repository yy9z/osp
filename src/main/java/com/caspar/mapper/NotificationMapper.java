package com.caspar.mapper;

import com.caspar.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 通知Mapper接口
 */
@Mapper
public interface NotificationMapper {

    /**
     * 插入通知
     *
     * @param notification 通知实体
     * @return 影响行数
     */
    int insert(Notification notification);

    /**
     * 查询当前用户最近的通知
     *
     * @param userId 用户ID
     * @param limit 数量限制
     * @return 通知列表
     */
    List<Notification> selectRecentNotificationsByUserId(@Param("userId") Long userId,
                                                         @Param("limit") int limit);

    int countByUserAndRelatedAndTitle(@Param("userId") Long userId,
                                      @Param("relatedId") Long relatedId,
                                      @Param("title") String title);
}
