package com.caspar.agent.service;

import com.caspar.agent.model.ToolResult;

import java.util.Map;

/**
 * 单次 ReactAgent 调用期间的本地工具执行记录。
 */
public record AgentToolExecutionRecord(
        String toolName,
        Map<String, Object> params,
        Long userId,
        ToolResult result
) {
}
