package com.caspar.agent.exception;

/**
 * 大模型调用异常：网络超时、API Key 无效、响应解析失败等情况抛出
 */
public class LlmCallException extends RuntimeException {

    public LlmCallException(String message) {
        super(message);
    }

    public LlmCallException(String message, Throwable cause) {
        super(message, cause);
    }
}
