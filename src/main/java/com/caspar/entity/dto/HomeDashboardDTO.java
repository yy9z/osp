package com.caspar.entity.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 首页数据展示DTO
 */
@Data
public class HomeDashboardDTO {
    /**
     * 统计数据
     */
    private StatisticsDTO statistics;

    /**
     * 通知列表
     */
    private List<NotificationVO> notifications;

    /**
     * 二手商品列表
     */
    private List<SecondhandGoodsVO> secondHandItems;

    /**
     * 失物招领列表
     */
    private List<LostFoundVO> lostFoundItems;

    /**
     * 主动提醒
     */
    private List<ProactiveTipVO> proactiveTips;

    /**
     * 统计数据内部类
     */
    @Data
    public static class StatisticsDTO {
        /**
         * 二手商品数量
         */
        private long secondHandCount;

        /**
         * 失物招领数量
         */
        private long lostFoundCount;

        /**
         * 未读消息数量
         */
        private long unreadMsgCount;

        /**
         * 宿舍报修数量
         */
        private long dormRepairCount;
    }

    /**
     * 通知VO内部类
     */
    @Data
    public static class NotificationVO {
        /**
         * 通知ID
         */
        private Long id;

        /**
         * 通知类型
         */
        private String type;

        /**
         * 标题
         */
        private String title;

        /**
         * 内容
         */
        private String content;

        /**
         * 关联ID
         */
        private Long relatedId;

        /**
         * 关联类型
         */
        private String relatedType;

        /**
         * 创建时间
         */
        private LocalDateTime createTime;
    }

    /**
     * 主动提醒 VO
     */
    @Data
    public static class ProactiveTipVO {
        /**
         * 提醒标题
         */
        private String title;

        /**
         * 提醒内容
         */
        private String content;

        /**
         * 前端跳转路径
         */
        private String actionPath;

        /**
         * 优先级标签：HIGH/MEDIUM/LOW
         */
        private String level;

        /**
         * 数值优先级（越高越优先）
         */
        private Integer priority;
    }
}
