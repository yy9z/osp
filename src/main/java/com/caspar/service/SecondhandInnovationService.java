package com.caspar.service;

import com.caspar.entity.Notification;
import com.caspar.entity.SecondhandGoods;
import com.caspar.entity.SecondhandSubscription;
import com.caspar.entity.User;
import com.caspar.entity.UserDormitory;
import com.caspar.entity.dto.*;
import com.caspar.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 二手交易增强能力：订阅提醒、面交建议、议价模板、卖家可信度。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecondhandInnovationService {

    private static final String SUBSCRIPTION_HIT_TITLE = "求购订阅命中";

    private final SecondhandSubscriptionMapper subscriptionMapper;
    private final SecondhandGoodsMapper secondhandGoodsMapper;
    private final NotificationMapper notificationMapper;
    private final NotificationService notificationService;
    private final UserDormitoryMapper userDormitoryMapper;
    private final UserMapper userMapper;
    private final MessageMapper messageMapper;

    public Long createSubscription(Long userId, SecondhandSubscriptionCreateDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getKeyword())) {
            throw new IllegalArgumentException("订阅关键词不能为空");
        }
        if (dto.getMaxPrice() != null && dto.getMaxPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("预算上限必须大于0");
        }

        String keyword = dto.getKeyword().trim();
        if (keyword.length() > 40) {
            throw new IllegalArgumentException("关键词长度不能超过40个字符");
        }

        String category = normalizeBlank(dto.getCategory());
        String campus = normalizeCampus(normalizeBlank(dto.getCampus()));

        int sameRuleCount = subscriptionMapper.countSameActiveRule(
                userId, keyword, category, dto.getMaxPrice(), campus
        );
        if (sameRuleCount > 0) {
            throw new IllegalArgumentException("已存在相同订阅规则");
        }

        SecondhandSubscription subscription = new SecondhandSubscription();
        subscription.setUserId(userId);
        subscription.setKeyword(keyword);
        subscription.setCategory(category);
        subscription.setMaxPrice(dto.getMaxPrice());
        subscription.setCampus(campus);
        subscription.setStatus("ACTIVE");
        subscription.setCreateTime(LocalDateTime.now());
        subscription.setUpdateTime(LocalDateTime.now());
        subscriptionMapper.insert(subscription);
        return subscription.getId();
    }

    public List<SecondhandSubscriptionVO> listSubscriptions(Long userId) {
        List<SecondhandSubscription> list = subscriptionMapper.selectByUserId(userId);
        List<SecondhandSubscriptionVO> result = new ArrayList<>();
        for (SecondhandSubscription s : list) {
            SecondhandSubscriptionVO vo = new SecondhandSubscriptionVO();
            vo.setId(s.getId());
            vo.setKeyword(s.getKeyword());
            vo.setCategory(s.getCategory());
            vo.setMaxPrice(s.getMaxPrice());
            vo.setCampus(s.getCampus());
            vo.setStatus(s.getStatus());
            vo.setCreateTime(s.getCreateTime());
            vo.setLastNotifiedAt(s.getLastNotifiedAt());
            result.add(vo);
        }
        return result;
    }

    public boolean deleteSubscription(Long userId, Long subscriptionId) {
        return subscriptionMapper.deleteByIdAndUserId(subscriptionId, userId) > 0;
    }

    /**
     * 商品上架时触发“求购订阅命中提醒”。
     */
    public void notifySubscribersForGoods(SecondhandGoods goods) {
        if (goods == null || goods.getId() == null || goods.getSellerId() == null) {
            return;
        }
        if (goods.getPrice() == null || !StringUtils.hasText(goods.getTitle())) {
            return;
        }

        String sellerCampus = resolveUserCampus(goods.getSellerId());
        String description = goods.getDescription() == null ? "" : goods.getDescription();

        List<SecondhandSubscription> matched = subscriptionMapper.selectMatchedSubscriptions(
                goods.getSellerId(),
                goods.getCategory(),
                goods.getPrice(),
                sellerCampus,
                goods.getTitle(),
                description
        );

        for (SecondhandSubscription subscription : matched) {
            if (notificationMapper.countByUserAndRelatedAndTitle(
                    subscription.getUserId(), goods.getId(), SUBSCRIPTION_HIT_TITLE
            ) > 0) {
                continue;
            }

            Notification notification = new Notification();
            notification.setUserId(subscription.getUserId());
            notification.setType("SECONDHAND");
            notification.setTitle(SUBSCRIPTION_HIT_TITLE);
            notification.setContent(buildSubscriptionHitContent(subscription, goods, sellerCampus));
            notification.setRelatedId(goods.getId());
            notification.setRelatedType("SECONDHAND");
            notification.setCreateTime(LocalDateTime.now());
            notificationService.saveNotification(notification);

            subscriptionMapper.updateLastNotifiedAt(subscription.getId());
        }

        if (!matched.isEmpty()) {
            log.info("订阅提醒触发: goodsId={}, 命中订阅数={}", goods.getId(), matched.size());
        }
    }

    public SecondhandMeetupRecommendationVO getMeetupRecommendation(Long buyerId, Long goodsId) {
        SecondhandGoods goods = secondhandGoodsMapper.findById(goodsId);
        if (goods == null) {
            throw new IllegalArgumentException("商品不存在");
        }

        String buyerCampus = resolveUserCampus(buyerId);
        String sellerCampus = resolveUserCampus(goods.getSellerId());
        String recommendedCampus = chooseMeetupCampus(buyerCampus, sellerCampus);

        SecondhandMeetupRecommendationVO vo = new SecondhandMeetupRecommendationVO();
        vo.setBuyerCampus(buyerCampus == null ? "未填写" : buyerCampus);
        vo.setSellerCampus(sellerCampus == null ? "未填写" : sellerCampus);
        vo.setRecommendedCampus(recommendedCampus);
        vo.setReason(buildMeetupReason(buyerCampus, sellerCampus, recommendedCampus));
        vo.setSuggestedSpots(suggestSpots(recommendedCampus));
        vo.setSuggestedTimeSlots(suggestTimeSlots());
        vo.setSafetyTips(List.of(
                "优先选择校内公共区域，尽量白天交易",
                "当面验货后再付款，不要提前转账",
                "保留聊天记录与转账凭证，出现异常及时联系保卫处"
        ));
        return vo;
    }

    public SecondhandBargainTemplateVO getBargainTemplate(Long buyerId, Long goodsId, BigDecimal targetPrice) {
        SecondhandGoodsVO goods = secondhandGoodsMapper.findDetailById(goodsId);
        if (goods == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        if (goods.getPrice() == null || goods.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("商品价格异常，暂时无法生成议价模板");
        }

        BigDecimal suggestedPrice = normalizeTargetPrice(goods.getPrice(), targetPrice);
        String meetupCampus = chooseMeetupCampus(resolveUserCampus(buyerId), resolveUserCampus(goods.getSellerId()));

        String line1 = String.format(
                "你好，我对你发布的【%s】很感兴趣，标价是¥%s。",
                goods.getTitle(), toPrice(goods.getPrice())
        );
        String line2 = String.format(
                "如果方便的话，¥%s可以吗？我可以在%s面交，当面验货付款。",
                toPrice(suggestedPrice), meetupCampus
        );

        List<String> templates = List.of(
                line1 + line2 + "谢谢！",
                line1 + "想礼貌问下价格能否小调到¥" + toPrice(suggestedPrice) + "？我今天就可以到校内面交。",
                "同学你好，" + goods.getTitle() + "还在吗？我诚心想要，预算大概¥" + toPrice(suggestedPrice) + "，可以的话我配合你的时间地点。"
        );

        SecondhandBargainTemplateVO vo = new SecondhandBargainTemplateVO();
        vo.setSuggestedPrice(suggestedPrice);
        vo.setQuickCopy(templates.get(0));
        vo.setTemplates(templates);
        return vo;
    }

    public SellerTrustScoreVO getSellerTrustScore(Long sellerId) {
        long total = nz(secondhandGoodsMapper.countBySeller(sellerId));
        long approved = nz(secondhandGoodsMapper.countBySellerAndStatus(sellerId, "APPROVED"));
        long sold = nz(secondhandGoodsMapper.countBySellerAndStatus(sellerId, "SOLD"));
        long rejected = nz(secondhandGoodsMapper.countBySellerAndStatus(sellerId, "REJECTED"));
        long removed = nz(secondhandGoodsMapper.countBySellerAndStatus(sellerId, "REMOVED"));

        long validSuccess = approved + sold;
        double approvalRate = total == 0 ? 0 : (double) validSuccess / total;
        double soldRate = validSuccess == 0 ? 0 : (double) sold / validSuccess;
        double rejectRate = total == 0 ? 0 : (double) rejected / total;
        double removeRate = total == 0 ? 0 : (double) removed / total;

        Double avgReplyMinutes = messageMapper.averageFirstReplyMinutes(sellerId);
        int replyScore = scoreByReply(avgReplyMinutes);
        int ageScore = scoreByAccountAge(sellerId);

        int score = (int) Math.round(
                40
                        + approvalRate * 25
                        + soldRate * 20
                        + replyScore
                        + ageScore
                        - rejectRate * 15
                        - removeRate * 10
        );
        score = Math.max(0, Math.min(100, score));

        SellerTrustScoreVO vo = new SellerTrustScoreVO();
        vo.setSellerId(sellerId);
        vo.setScore(score);
        vo.setLevel(score >= 85 ? "HIGH" : score >= 70 ? "MEDIUM" : "LOW");
        vo.setTotalListings(total);
        vo.setApprovedCount(approved);
        vo.setSoldCount(sold);
        vo.setRejectedCount(rejected);
        vo.setRemovedCount(removed);
        vo.setAvgReplyMinutes(avgReplyMinutes == null ? null : round2(avgReplyMinutes));
        vo.setHighlights(buildHighlights(total, soldRate, rejectRate, avgReplyMinutes));
        return vo;
    }

    private String buildSubscriptionHitContent(SecondhandSubscription subscription,
                                               SecondhandGoods goods,
                                               String sellerCampus) {
        String campusText = StringUtils.hasText(sellerCampus) ? sellerCampus : "校内";
        return String.format(
                "你订阅的“%s”有新商品：%s（¥%s），建议尽快联系卖家，推荐面交校区：%s。",
                subscription.getKeyword(),
                goods.getTitle(),
                toPrice(goods.getPrice()),
                campusText
        );
    }

    private String resolveUserCampus(Long userId) {
        if (userId == null) {
            return null;
        }
        UserDormitory userDormitory = userDormitoryMapper.findByUserId(userId);
        if (userDormitory == null || !StringUtils.hasText(userDormitory.getCampus())) {
            return null;
        }
        return normalizeCampus(userDormitory.getCampus());
    }

    private String normalizeCampus(String campus) {
        if (!StringUtils.hasText(campus)) {
            return null;
        }
        String value = campus.trim();
        if (value.contains("高新")) return "高新校区";
        if (value.contains("东")) return "东校区";
        if (value.contains("西")) return "西校区";
        if (value.contains("中")) return "中校区";
        if (value.contains("南")) return "南校区";
        return value;
    }

    private String chooseMeetupCampus(String buyerCampus, String sellerCampus) {
        if (!StringUtils.hasText(buyerCampus) && !StringUtils.hasText(sellerCampus)) {
            return "东校区";
        }
        if (!StringUtils.hasText(buyerCampus)) {
            return sellerCampus;
        }
        if (!StringUtils.hasText(sellerCampus)) {
            return buyerCampus;
        }
        if (buyerCampus.equals(sellerCampus)) {
            return buyerCampus;
        }
        return "中校区";
    }

    private String buildMeetupReason(String buyerCampus, String sellerCampus, String recommendedCampus) {
        if (StringUtils.hasText(buyerCampus) && buyerCampus.equals(sellerCampus)) {
            return "你们双方都在同一校区，优先推荐就近面交点。";
        }
        if (!StringUtils.hasText(buyerCampus) || !StringUtils.hasText(sellerCampus)) {
            return "已根据已知校区信息推荐最稳妥的面交点。";
        }
        return "你们位于不同校区，推荐在" + recommendedCampus + "会合，兼顾双方通勤成本。";
    }

    private List<String> suggestSpots(String campus) {
        return switch (campus) {
            case "东校区" -> List.of("郭沫若广场东侧", "东区学生食堂门口", "东校区操场看台下");
            case "西校区" -> List.of("西区图书馆南门", "西区活动中心大厅", "西区操场北门");
            case "南校区" -> List.of("南校区东正门门卫处", "南校区图书角", "南校区生活区便利店门口");
            case "高新校区" -> List.of("高新园区图文中心一层", "高新园区门卫岗亭旁", "高新园区食堂入口");
            default -> List.of("中校区北门", "中区连廊口", "中校区生活服务点");
        };
    }

    private List<String> suggestTimeSlots() {
        LocalTime now = LocalTime.now();
        if (now.isBefore(LocalTime.of(12, 0))) {
            return List.of("今天12:00-13:30", "今天18:00-20:00", "明天12:00-13:30");
        }
        if (now.isBefore(LocalTime.of(18, 0))) {
            return List.of("今天18:00-20:00", "明天12:00-13:30", "明天18:00-20:00");
        }
        return List.of("明天12:00-13:30", "明天18:00-20:00", "后天12:00-13:30");
    }

    private BigDecimal normalizeTargetPrice(BigDecimal originPrice, BigDecimal targetPrice) {
        BigDecimal minPrice = BigDecimal.ONE;
        if (targetPrice != null && targetPrice.compareTo(BigDecimal.ZERO) > 0) {
            return targetPrice.min(originPrice).max(minPrice).setScale(2, RoundingMode.HALF_UP);
        }
        return originPrice.multiply(new BigDecimal("0.90"))
                .max(minPrice)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private int scoreByReply(Double avgReplyMinutes) {
        if (avgReplyMinutes == null) return 5;
        if (avgReplyMinutes <= 15) return 10;
        if (avgReplyMinutes <= 60) return 8;
        if (avgReplyMinutes <= 180) return 6;
        if (avgReplyMinutes <= 720) return 4;
        if (avgReplyMinutes <= 1440) return 2;
        return 1;
    }

    private int scoreByAccountAge(Long sellerId) {
        User user = userMapper.findById(sellerId);
        if (user == null || user.getCreateTime() == null) {
            return 2;
        }
        long days = ChronoUnit.DAYS.between(user.getCreateTime().toLocalDate(), LocalDate.now());
        if (days >= 365) return 8;
        if (days >= 180) return 6;
        if (days >= 90) return 4;
        if (days >= 30) return 2;
        return 1;
    }

    private List<String> buildHighlights(long total,
                                         double soldRate,
                                         double rejectRate,
                                         Double avgReplyMinutes) {
        List<String> highlights = new ArrayList<>();
        if (total == 0) {
            highlights.add("新卖家，建议优先当面验货再交易");
            return highlights;
        }
        if (soldRate >= 0.5) {
            highlights.add("历史成交率较高");
        }
        if (avgReplyMinutes != null && avgReplyMinutes <= 60) {
            highlights.add("消息响应较快");
        }
        if (rejectRate >= 0.3) {
            highlights.add("历史审核拒绝偏多，建议详细沟通后交易");
        }
        if (highlights.isEmpty()) {
            highlights.add("交易记录稳定，建议继续保持平台内沟通");
        }
        return highlights;
    }

    private String toPrice(BigDecimal price) {
        return price == null ? "0" : price.stripTrailingZeros().toPlainString();
    }

    private String normalizeBlank(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private long nz(Long value) {
        return value == null ? 0L : value;
    }

    private double round2(double value) {
        return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}

