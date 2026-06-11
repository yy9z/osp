package com.caspar.service;

import com.caspar.entity.DormBuilding;
import com.caspar.entity.dto.HomeDashboardDTO;
import com.caspar.entity.dto.LostFoundVO;
import com.caspar.entity.dto.RepairVO;
import com.caspar.mapper.DormBuildingMapper;
import com.caspar.mapper.DormitoryRepairMapper;
import com.caspar.mapper.LostFoundMapper;
import com.caspar.mapper.MessageMapper;
import com.caspar.mapper.SecondhandGoodsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 聚合用户当前最需要处理的校园事务提醒。
 */
@Service
@RequiredArgsConstructor
public class ProactiveTipService {

    private final SecondhandGoodsMapper secondhandGoodsMapper;
    private final LostFoundMapper lostFoundMapper;
    private final DormitoryRepairMapper dormitoryRepairMapper;
    private final DormBuildingMapper dormBuildingMapper;
    private final MessageMapper messageMapper;

    public List<HomeDashboardDTO.ProactiveTipVO> buildProactiveTips(Long userId, String role) {
        List<HomeDashboardDTO.ProactiveTipVO> tips = new ArrayList<>();

        int unreadCount = messageMapper.countUnreadAll(userId);
        if (unreadCount > 0) {
            tips.add(buildTip(
                    "你有未读消息",
                    "当前有 " + unreadCount + " 条未读消息，建议先查看重点会话。",
                    "/messages",
                    "HIGH",
                    100
            ));
        }

        Long pendingAuditCount = secondhandGoodsMapper.countMy(userId, "PENDING");
        if (pendingAuditCount != null && pendingAuditCount > 0) {
            tips.add(buildTip(
                    "二手商品待审核",
                    "你有 " + pendingAuditCount + " 个商品在审核中，建议补充描述和图片提高通过率。",
                    "/secondhand/my",
                    "MEDIUM",
                    80
            ));
        }

        Long rejectedCount = secondhandGoodsMapper.countMy(userId, "REJECTED");
        if (rejectedCount != null && rejectedCount > 0) {
            tips.add(buildTip(
                    "二手商品被驳回",
                    "有 " + rejectedCount + " 个商品被驳回，建议修改后重新上架。",
                    "/secondhand/my",
                    "HIGH",
                    90
            ));
        }

        List<RepairVO> myRepairs = dormitoryRepairMapper.selectListByUserId(userId, 0, 30);
        long myPendingRepairs = myRepairs.stream().filter(item -> "PENDING".equalsIgnoreCase(item.getStatus())).count();
        long myProcessingRepairs = myRepairs.stream().filter(item -> "PROCESSING".equalsIgnoreCase(item.getStatus())).count();
        String myRepairActionPath = "/dormitory/repair";
        if (myPendingRepairs > 0) {
            tips.add(buildTip(
                    "报修工单待处理",
                    "你有 " + myPendingRepairs + " 条报修仍在排队，建议确认联系方式可用。",
                    myRepairActionPath,
                    "MEDIUM",
                    75
            ));
        }
        if (myProcessingRepairs > 0) {
            tips.add(buildTip(
                    "报修正在处理中",
                    "你有 " + myProcessingRepairs + " 条报修正在处理，建议留意处理进度。",
                    myRepairActionPath,
                    "LOW",
                    65
            ));
        }

        List<LostFoundVO> myLost = lostFoundMapper.selectMyList(userId, "LOST", 0, 30);
        long myOpenLost = myLost.stream().filter(item -> "OPEN".equalsIgnoreCase(item.getStatus())).count();
        if (myOpenLost > 0) {
            tips.add(buildTip(
                    "仍有未找回失物",
                    "你有 " + myOpenLost + " 条寻物公告仍在进行中，可查看智能匹配结果。",
                    "/lostfound/my-lost",
                    "MEDIUM",
                    70
            ));
        }

        List<LostFoundVO> myFound = lostFoundMapper.selectMyList(userId, "FOUND", 0, 30);
        long myOpenFound = myFound.stream().filter(item -> "OPEN".equalsIgnoreCase(item.getStatus())).count();
        if (myOpenFound > 0) {
            tips.add(buildTip(
                    "你发布的招领还未归还",
                    "有 " + myOpenFound + " 条招领公告尚未完成，建议同步最新进展。",
                    "/lostfound/my-found",
                    "LOW",
                    60
            ));
        }

        if ("ADMIN".equalsIgnoreCase(role) || "DORM_MANAGER".equalsIgnoreCase(role)) {
            long pendingRepairs = resolvePendingRepairCount(userId, role);
            if (pendingRepairs > 0) {
                String tipContent = "当前共有 " + pendingRepairs + " 条待处理报修，建议优先处理高优先级工单。";
                if ("DORM_MANAGER".equalsIgnoreCase(role)) {
                    tipContent = "你负责楼栋当前共有 " + pendingRepairs + " 条待处理报修，建议优先处理高优先级工单。";
                }
                tips.add(buildTip(
                        "有待处理报修工单",
                        tipContent,
                        "/dormitory/manage",
                        "HIGH",
                        95
                ));
            }
        }

        if ("ADMIN".equalsIgnoreCase(role)) {
            long globalPendingGoods = secondhandGoodsMapper.countPending(null, null);
            if (globalPendingGoods > 0) {
                tips.add(buildTip(
                        "有待审核二手商品",
                        "当前有 " + globalPendingGoods + " 条二手商品待审核，建议及时处理。",
                        "/admin/secondhand",
                        "HIGH",
                        92
                ));
            }
        }

        return tips.stream()
                .sorted(Comparator.comparing(HomeDashboardDTO.ProactiveTipVO::getPriority).reversed())
                .limit(6)
                .collect(Collectors.toList());
    }

    public long resolvePendingRepairCount(Long userId, String role) {
        if (!"DORM_MANAGER".equalsIgnoreCase(role)) {
            return dormitoryRepairMapper.count("PENDING", null, null);
        }

        DormBuilding managedBuilding = dormBuildingMapper.findByManagerId(userId);
        if (managedBuilding == null || managedBuilding.getName() == null || managedBuilding.getName().isBlank()) {
            return 0L;
        }
        return dormitoryRepairMapper.count("PENDING", managedBuilding.getName(), null);
    }

    private HomeDashboardDTO.ProactiveTipVO buildTip(String title,
                                                     String content,
                                                     String actionPath,
                                                     String level,
                                                     int priority) {
        HomeDashboardDTO.ProactiveTipVO tip = new HomeDashboardDTO.ProactiveTipVO();
        tip.setTitle(title);
        tip.setContent(content);
        tip.setActionPath(actionPath);
        tip.setLevel(level);
        tip.setPriority(priority);
        return tip;
    }
}
