package com.caspar.agent.tool;

import com.caspar.agent.model.ToolArgs;
import com.caspar.agent.model.ToolResult;
import com.caspar.entity.DormitoryRepair;
import com.caspar.entity.UserDormitory;
import com.caspar.mapper.DormitoryRepairMapper;
import com.caspar.mapper.UserDormitoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 宿舍报修工具：提交报修工单
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DormRepairTool implements AgentTool {

    private final DormitoryRepairMapper repairMapper;
    private final UserDormitoryMapper userDormitoryMapper;

    @Override
    public String getName() {
        return "dorm_repair";
    }

    @Override
    public ToolResult execute(ToolArgs args) {
        Map<String, Object> params = args.getParams();
        Long userId = args.getUserId();

        String faultType = getString(params, "fault_type");
        String description = getString(params, "description");

        // 自动从用户宿舍档案补充宿舍信息
        UserDormitory userDorm = userDormitoryMapper.findByUserId(userId);
        if (userDorm == null || isBlank(userDorm.getBuilding()) || isBlank(userDorm.getRoom())) {
            Map<String, Object> clarifyData = new HashMap<>();
            clarifyData.put("clarificationRequired", true);
            clarifyData.put("clarificationType", "dorm_info_required");
            clarifyData.put("clarificationOptions", java.util.List.of("去宿舍管理完善宿舍信息", "告诉我宿舍号后重试"));
            clarifyData.put("agentSummary", "我暂时没有拿到你的宿舍信息，无法直接提交报修。请先完善宿舍信息，或补充宿舍号后再试。");
            return ToolResult.ok("缺少宿舍信息，暂未提交报修。", clarifyData);
        }

        DormitoryRepair repair = new DormitoryRepair();
        repair.setUserId(userId);
        repair.setDescription(faultType + (description != null ? "：" + description : ""));
        repair.setUrgency("NORMAL");
        repair.setStatus("PENDING");
        repair.setCreateTime(LocalDateTime.now());
        repair.setUpdateTime(LocalDateTime.now());

        repair.setCampus(userDorm.getCampus());
        repair.setBuilding(userDorm.getBuilding());
        repair.setRoomNo(userDorm.getRoom());

        try {
            repairMapper.insert(repair);

            Map<String, Object> data = new HashMap<>();
            data.put("orderId", repair.getId());
            data.put("status", "PENDING");
            data.put("faultType", faultType);
            data.put("building", repair.getBuilding());
            data.put("roomNo", repair.getRoomNo());
            data.put("submitTime", LocalDateTime.now().toString());

            String summary = String.format("已成功提交报修工单（%s），工单号：%d，请等待宿管处理。",
                    faultType, repair.getId());
            return ToolResult.ok(summary, data);
        } catch (Exception e) {
            log.error("报修工单提交失败", e);
            return ToolResult.fail("报修提交失败：" + e.getMessage());
        }
    }

    private String getString(Map<String, Object> params, String key) {
        Object v = params.get(key);
        return v == null ? null : v.toString();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
