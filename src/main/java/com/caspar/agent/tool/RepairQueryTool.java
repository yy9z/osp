package com.caspar.agent.tool;

import com.caspar.agent.model.ToolArgs;
import com.caspar.agent.model.ToolResult;
import com.caspar.common.PageResult;
import com.caspar.entity.dto.RepairVO;
import com.caspar.mapper.DormitoryRepairMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 报修工单查询工具：查询当前用户最近的报修记录
 */
@Component
@RequiredArgsConstructor
public class RepairQueryTool implements AgentTool {

    private final DormitoryRepairMapper repairMapper;

    @Override
    public String getName() {
        return "repair_query";
    }

    @Override
    public ToolResult execute(ToolArgs args) {
        Long userId = args.getUserId();
        List<RepairVO> list = repairMapper.selectListByUserId(userId, 0, 5);

        if (list == null || list.isEmpty()) {
            return ToolResult.ok("您暂无报修记录。", null);
        }

        RepairVO latest = list.get(0);
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", latest.getId());
        data.put("status", latest.getStatus());
        data.put("description", latest.getDescription());
        data.put("createTime", latest.getCreateTime());
        data.put("recentOrders", list);

        String summary = String.format("您最近一次报修（%s）状态为：%s。",
                latest.getDescription(), translateStatus(latest.getStatus()));
        return ToolResult.ok(summary, data);
    }

    private String translateStatus(String status) {
        return switch (status) {
            case "PENDING" -> "等待处理";
            case "PROCESSING" -> "处理中";
            case "COMPLETED" -> "已完成";
            case "REJECTED" -> "已驳回";
            default -> status;
        };
    }
}
