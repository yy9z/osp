package com.caspar.controller;

import com.caspar.common.Result;
import com.caspar.entity.Notification;
import com.caspar.entity.dto.HomeDashboardDTO;
import com.caspar.entity.dto.LostFoundVO;
import com.caspar.entity.dto.SecondhandGoodsVO;
import com.caspar.mapper.LostFoundMapper;
import com.caspar.mapper.MessageMapper;
import com.caspar.mapper.NotificationMapper;
import com.caspar.mapper.SecondhandGoodsMapper;
import com.caspar.service.ProactiveTipService;
import com.caspar.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 首页控制器
 */
@RestController
@RequestMapping("/api/v1/home")
public class HomeController {

    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    @Autowired
    private SecondhandGoodsMapper secondHandGoodsMapper;

    @Autowired
    private LostFoundMapper lostFoundMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private ProactiveTipService proactiveTipService;

    /**
     * 获取首页数据
     */
    @GetMapping("/data")
    public Result<HomeDashboardDTO> getHomeData() {
        HomeDashboardDTO dashboard = new HomeDashboardDTO();

        // 获取当前用户ID
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            return Result.unauthorized();
        }
        String currentRole = SecurityUtils.getCurrentUserRole();

        // 1. 获取统计数据
        HomeDashboardDTO.StatisticsDTO statistics = new HomeDashboardDTO.StatisticsDTO();
        statistics.setSecondHandCount(secondHandGoodsMapper.count(null, null));
        statistics.setLostFoundCount(lostFoundMapper.count(null, null, null));
        statistics.setDormRepairCount(proactiveTipService.resolvePendingRepairCount(currentUserId, currentRole));
        // 获取当前用户的未读消息数
        statistics.setUnreadMsgCount(messageMapper.countUnreadAll(currentUserId));
        dashboard.setStatistics(statistics);

        // 2. 获取当前用户的通知列表（最近5条）
        try {
            List<Notification> notifications = notificationMapper.selectRecentNotificationsByUserId(currentUserId, 5);
            List<HomeDashboardDTO.NotificationVO> notificationVOs = notifications.stream()
                .map(this::convertToNotificationVO)
                .collect(Collectors.toList());
            dashboard.setNotifications(notificationVOs);
        } catch (Exception exception) {
            log.warn("Failed to load personalized notifications for user {}", currentUserId, exception);
            dashboard.setNotifications(Collections.emptyList());
        }

        // 3. 获取二手商品列表（最新5条）
        List<SecondhandGoodsVO> secondHandItems = secondHandGoodsMapper.selectList(
                null, null, "latest", 0, 5);
        dashboard.setSecondHandItems(secondHandItems);

        // 4. 获取失物招领列表（最新5条）
        List<LostFoundVO> lostFoundItems = lostFoundMapper.selectList(
                null, null, null, 0, 5);
        dashboard.setLostFoundItems(lostFoundItems);
        dashboard.setProactiveTips(proactiveTipService.buildProactiveTips(currentUserId, currentRole));

        return Result.success(dashboard);
    }

    /**
     * 获取主动提醒（用于首页/智能助手）
     */
    @GetMapping("/proactive-tips")
    public Result<List<HomeDashboardDTO.ProactiveTipVO>> getProactiveTips() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            return Result.unauthorized();
        }
        String currentRole = SecurityUtils.getCurrentUserRole();
        return Result.success(proactiveTipService.buildProactiveTips(currentUserId, currentRole));
    }

    /**
     * 将Notification转换为NotificationVO
     */
    private HomeDashboardDTO.NotificationVO convertToNotificationVO(Notification notification) {
        HomeDashboardDTO.NotificationVO vo = new HomeDashboardDTO.NotificationVO();
        vo.setId(notification.getId());
        vo.setType(notification.getType());
        vo.setTitle(notification.getTitle());
        vo.setContent(notification.getContent());
        vo.setRelatedId(notification.getRelatedId());
        vo.setRelatedType(notification.getRelatedType());
        vo.setCreateTime(notification.getCreateTime());
        return vo;
    }

}
