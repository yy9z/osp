package com.caspar.agent.tool;

import com.caspar.agent.model.ToolArgs;
import com.caspar.agent.model.ToolResult;

/**
 * Agent 工具接口，所有工具均实现此接口。
 */
public interface AgentTool {
    /** 工具名称，全局唯一 */
    String getName();

    /** 执行工具并返回结果 */
    ToolResult execute(ToolArgs args);
}
