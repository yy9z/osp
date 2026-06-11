package com.caspar.agent.service;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 导航意图后续建议策略（动态生成）。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class NavigationFollowUpSuggestionStrategy implements FollowUpSuggestionStrategy {

    @Override
    public boolean supports(String intent) {
        return "NAVIGATION".equals(intent);
    }

    @Override
    public List<String> suggestions(String intent, Map<String, Object> slots) {
        String dest = slots != null ? String.valueOf(slots.getOrDefault("destination", "")) : "";
        String mode = slots != null
                ? String.valueOf(slots.getOrDefault("travel_mode", slots.getOrDefault("mode", "walking")))
                : "walking";
        List<String> suggestions = new ArrayList<>();
        if (!dest.isBlank()) {
            suggestions.add("从" + dest + "出发去图书馆怎么走");
            suggestions.add(dest + "附近有哪些餐厅");
        } else {
            suggestions.add("去图书馆怎么走");
            suggestions.add("附近有什么餐厅");
        }
        suggestions.add("cycling".equals(mode) ? "换步行重新规划" : "换骑行重新规划");
        return suggestions;
    }
}
