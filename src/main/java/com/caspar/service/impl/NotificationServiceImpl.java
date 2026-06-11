package com.caspar.service.impl;

import com.caspar.entity.Notification;
import com.caspar.mapper.NotificationMapper;
import com.caspar.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 通知服务实现类
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationMapper notificationMapper;

    @Override
    public void saveNotification(Notification notification) {
        if (notification == null) {
            return;
        }
        notificationMapper.insert(notification);
    }
}
