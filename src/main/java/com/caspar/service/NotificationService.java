package com.caspar.service;

import com.caspar.entity.Notification;

/**
 * 通知服务接口
 */
public interface NotificationService {

    /**
     * 保存通知
     *
     * @param notification 通知实体
     */
    void saveNotification(Notification notification);
}
