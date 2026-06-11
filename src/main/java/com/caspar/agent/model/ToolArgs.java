package com.caspar.agent.model;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ToolArgs {
    private String toolName;
    private Map<String, Object> params;
    /** 当前用户ID，从 SecurityContext 注入，不允许由外部传入 */
    private Long userId;
}
