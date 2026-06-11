package com.caspar.agent.tool;

import com.caspar.agent.model.ToolArgs;
import com.caspar.agent.model.ToolResult;
import com.caspar.entity.User;
import com.caspar.entity.dto.HomeDashboardDTO;
import com.caspar.mapper.UserMapper;
import com.caspar.service.ProactiveTipService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 主动提醒工具：把首页智能提醒接入 Agent 对话。
 */
@Component
@RequiredArgsConstructor
public class CampusTipsTool implements AgentTool {

    private final ProactiveTipService proactiveTipService;
    private final UserMapper userMapper;

    @Override
    public String getName() {
        return "campus_tips";
    }

    @Override
    public ToolResult execute(ToolArgs args) {
        Long userId = args.getUserId();
        if (userId == null) {
            return ToolResult.fail("无法获取当前用户身份，请重新登录后再试。");
        }

        User user = userMapper.findById(userId);
        String role = user == null ? null : user.getRole();
        List<HomeDashboardDTO.ProactiveTipVO> tips = proactiveTipService.buildProactiveTips(userId, role);

        long highPriorityCount = tips.stream()
                .filter(tip -> "HIGH".equalsIgnoreCase(tip.getLevel()))
                .count();

        Map<String, Object> data = new HashMap<>();
        data.put("type", "CAMPUS_TIPS");
        data.put("tips", tips);
        data.put("total", tips.size());
        data.put("highPriorityCount", highPriorityCount);
        data.put("agentSummary", buildSummary(tips.size(), highPriorityCount));

        return ToolResult.ok(buildSummary(tips.size(), highPriorityCount), data);
    }

    private String buildSummary(int total, long highPriorityCount) {
        if (total == 0) {
            return "当前没有需要优先处理的校园事务。";
        }
        if (highPriorityCount > 0) {
            return String.format("为你整理了 %d 条待处理提醒，其中 %d 条优先级较高。", total, highPriorityCount);
        }
        return String.format("为你整理了 %d 条待处理提醒。", total);
    }
}
