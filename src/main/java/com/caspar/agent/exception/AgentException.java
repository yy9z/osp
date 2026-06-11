package com.caspar.agent.exception;

/**
 * Agent 业务异常：意图识别失败、工具调用业务错误等场景抛出
 */
public class AgentException extends RuntimeException {

    public AgentException(String message) {
        super(message);
    }

    public AgentException(String message, Throwable cause) {
        super(message, cause);
    }
}
