package com.caspar.agent.service;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 静态后续建议策略（非导航意图）。
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class StaticFollowUpSuggestionStrategy implements FollowUpSuggestionStrategy {

    @Override
    public boolean supports(String intent) {
        return intent != null;
    }

    @Override
    public List<String> suggestions(String intent, Map<String, Object> slots) {
        if (intent == null) {
            return Collections.emptyList();
        }
        return switch (intent) {
            case "DORM_REPAIR" -> Arrays.asList("查看工单状态", "报修其他设备", "联系宿管");
            case "REPAIR_QUERY" -> Arrays.asList("催促处理", "提交新报修", "查看我的报修列表");
            case "LOSTFOUND_LOST" -> Arrays.asList("查看相似招领公告", "补充丢失描述", "前往寻物公告");
            case "LOSTFOUND_FOUND" -> Arrays.asList("查看寻物公告", "修改招领信息", "标记物品已归还");
            case "SECONDHAND_SEARCH" -> Arrays.asList("查看更多商品", "按价格排序", "收藏该商品");
            case "SECONDHAND_PUBLISH" -> Arrays.asList("查看我的在售商品", "修改价格", "下架商品");
            case "DORM_QUERY" -> Arrays.asList("查看宿舍成员", "完善宿舍信息", "发起报修");
            case "MESSAGE_QUERY" -> Arrays.asList("查看未读消息", "按类型筛选通知", "清空已读通知");
            case "CAMPUS_KNOWLEDGE" -> Arrays.asList("查看其他平台流程", "了解校园事务规则", "查询常见问题");
            default -> Collections.emptyList();
        };
    }
}
