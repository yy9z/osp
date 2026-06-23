package com.caspar.agent.service;

/**
 * ReactAgent 调用期间注入 ToolContext / RunnableConfig 的上下文键。
 */
public final class CampusAgentContextKeys {

    public static final String USER_ID = "userId";
    public static final String TRACE_ID = "traceId";

    private CampusAgentContextKeys() {
    }
}
