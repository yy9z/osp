package com.caspar.agent.service;

import com.caspar.agent.registry.AgentToolCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 兼容兜底规划服务：根据意图和槽位，决定需要调用哪些本地函数以及调用顺序。
 * 当前 Agent 主链路已经迁移到 Spring AI Alibaba ReactAgent，本类仅保留给旧逻辑兼容。
 */
@Service
@RequiredArgsConstructor
public class PlannerService {

    private final AgentToolCatalog toolCatalog;

    /**
     * 根据意图返回需要执行的工具列表。
     */
    public List<String> plan(String intent, Map<String, Object> slots) {
        return toolCatalog.getToolNames(intent);
    }
}
