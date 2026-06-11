package com.caspar.agent.registry;

import com.caspar.agent.tool.AgentTool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 工具注册表：收集所有 AgentTool Bean，按名称索引。
 */
@Component
public class ToolRegistry {

    private final Map<String, AgentTool> toolMap;

    /** Spring 自动注入所有 AgentTool 实现 */
    public ToolRegistry(List<AgentTool> tools) {
        this.toolMap = tools.stream()
                .collect(Collectors.toMap(AgentTool::getName, Function.identity()));
    }

    public AgentTool getTool(String name) {
        return toolMap.get(name);
    }

    public boolean hasTool(String name) {
        return toolMap.containsKey(name);
    }
}
