package com.caspar.agent.model;

import lombok.Data;

import java.util.Map;

@Data
public class AgentRequest {
    /** 会话ID，首次为null由系统分配 */
    private String sessionId;
    /** 用户输入的自然语言 */
    private String message;
    /** 可选上下文（如当前位置） */
    private Map<String, Object> context;
    /** 待确认写操作的唯一标识 */
    private String confirmationId;
    /** APPROVE / REJECT；也兼容用户直接输入“确认执行”或“取消” */
    private String confirmationDecision;
}
