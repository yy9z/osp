package com.caspar.agent.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ToolResult {
    private boolean success;
    /** 供 ResponseGenerator 使用的简洁摘要 */
    private String summary;
    /** 结构化数据（卡片数据） */
    private Object data;
    /** 失败时的原因 */
    private String errorMessage;

    public static ToolResult ok(String summary, Object data) {
        return ToolResult.builder().success(true).summary(summary).data(data).build();
    }

    public static ToolResult fail(String errorMessage) {
        return ToolResult.builder().success(false).errorMessage(errorMessage).build();
    }
}
