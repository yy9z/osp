package com.caspar.agent.tool;

import com.caspar.agent.model.ToolArgs;
import com.caspar.agent.model.ToolResult;
import com.caspar.entity.UserDormitory;
import com.caspar.mapper.DormitoryRepairMapper;
import com.caspar.mapper.UserDormitoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 宿舍信息查询工具
 */
@Component
@RequiredArgsConstructor
public class DormQueryTool implements AgentTool {

    private final UserDormitoryMapper userDormitoryMapper;
    private final DormitoryRepairMapper repairMapper;

    @Override
    public String getName() {
        return "dorm_query";
    }

    @Override
    public ToolResult execute(ToolArgs args) {
        Long userId = args.getUserId();
        UserDormitory dorm = userDormitoryMapper.findByUserId(userId);
        if (dorm == null) {
            return ToolResult.fail("未找到您的宿舍分配信息，请联系管理员。");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("campus", dorm.getCampus());
        data.put("building", dorm.getBuilding());
        data.put("room", dorm.getRoom());
        data.put("bed", dorm.getBed());

        String summary = String.format("您当前住在 %s %s %s 室 %s 床。",
                dorm.getCampus() != null ? dorm.getCampus() : "",
                dorm.getBuilding() != null ? dorm.getBuilding() : "",
                dorm.getRoom() != null ? dorm.getRoom() : "",
                dorm.getBed() != null ? dorm.getBed() : "");
        return ToolResult.ok(summary, data);
    }
}
