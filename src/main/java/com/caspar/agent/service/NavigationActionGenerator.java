package com.caspar.agent.service;

import com.caspar.agent.model.NavigationSlots;
import com.caspar.agent.model.NavigationTaskResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 导航推荐动作生成器
 */
@Service
public class NavigationActionGenerator {

    /**
     * 生成推荐动作列表
     */
    public List<NavigationTaskResponse.Action> generateActions(NavigationSlots slots, String scene) {
        List<NavigationTaskResponse.Action> actions = new ArrayList<>();

        // 1. 切换出行方式
        if ("walking".equals(slots.getTravelMode())) {
            actions.add(buildAction("replan", "改成骑行路线", Map.of("mode", "cycling")));
        } else {
            actions.add(buildAction("replan", "改成步行路线", Map.of("mode", "walking")));
        }

        // 2. 场景相关动作
        String destination = slots.getDestination();
        if (destination != null) {
            // 返回路线
            actions.add(buildAction("return_route", "返回" + destination + "路线",
                Map.of("origin", destination, "destination", "当前位置")));

            // 附近设施
            if ("dining".equals(scene)) {
                actions.add(buildAction("nearby", "查附近打印店",
                    Map.of("category", "print_shop", "near", destination)));
            } else if ("study".equals(scene)) {
                actions.add(buildAction("nearby", "查附近食堂",
                    Map.of("category", "canteen", "near", destination)));
            } else if ("class_commute".equals(scene)) {
                actions.add(buildAction("nearby", "途经打印店",
                    Map.of("category", "print_shop", "waypoint", true)));
            }
        }

        // 3. 偏好调整动作
        if (!slots.getPreferences().contains("safeNight") && slots.isNightTime()) {
            actions.add(buildAction("replan", "切换夜间安全路线",
                Map.of("preference", "safeNight")));
        }

        if (!slots.getPreferences().contains("fastest") && slots.isUrgent()) {
            actions.add(buildAction("replan", "切换最快路线",
                Map.of("preference", "fastest")));
        }

        // 4. 收藏路线
        if (destination != null && slots.getOrigin() != null) {
            actions.add(buildAction("save_route", "收藏此路线",
                Map.of("origin", slots.getOrigin(), "destination", destination)));
        }

        // 5. 常用地点快捷导航
        if ("return_dorm".equals(scene)) {
            actions.add(buildAction("quick_nav", "去图书馆",
                Map.of("destination", "图书馆")));
        }

        return actions;
    }

    /**
     * 生成追问建议（用于聊天界面）
     */
    public List<String> generateFollowUpSuggestions(NavigationSlots slots, String scene) {
        List<String> suggestions = new ArrayList<>();
        String destination = slots.getDestination();

        if (destination != null) {
            // 基于场景的建议
            if ("dining".equals(scene)) {
                suggestions.add("从" + destination + "去图书馆怎么走");
                suggestions.add(destination + "附近有打印店吗");
            } else if ("study".equals(scene)) {
                suggestions.add("从" + destination + "回宿舍怎么走");
                suggestions.add(destination + "附近哪里可以吃饭");
            } else if ("class_commute".equals(scene)) {
                suggestions.add("下课后去食堂怎么走");
                suggestions.add("顺路去打印店");
            } else {
                suggestions.add("从" + destination + "返回怎么走");
                suggestions.add(destination + "附近有什么设施");
            }
        } else {
            suggestions.add("去图书馆怎么走");
            suggestions.add("最近的食堂在哪");
            suggestions.add("帮我导航到快递点");
        }

        // 出行方式切换建议
        if ("walking".equals(slots.getTravelMode())) {
            suggestions.add("换骑行重新规划");
        } else {
            suggestions.add("换步行重新规划");
        }

        return suggestions;
    }

    private NavigationTaskResponse.Action buildAction(String type, String label, Map<String, Object> payload) {
        return NavigationTaskResponse.Action.builder()
                .type(type)
                .label(label)
                .payload(new HashMap<>(payload))
                .build();
    }
}
