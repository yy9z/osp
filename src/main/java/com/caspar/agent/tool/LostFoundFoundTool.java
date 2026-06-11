package com.caspar.agent.tool;

import com.caspar.agent.model.ToolArgs;
import com.caspar.agent.model.ToolResult;
import org.springframework.stereotype.Component;

/**
 * 拾物登记工具（复用 LostFoundTool 的实现，类型为 FOUND）
 */
@Component
public class LostFoundFoundTool extends LostFoundTool {

    public LostFoundFoundTool(com.caspar.mapper.LostFoundMapper lostFoundMapper) {
        super(lostFoundMapper);
    }

    @Override
    public String getName() {
        return "lostfound_found";
    }

    @Override
    public ToolResult execute(ToolArgs args) {
        return doExecute(args, "FOUND");
    }
}
