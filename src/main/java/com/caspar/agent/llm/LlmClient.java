package com.caspar.agent.llm;

import com.caspar.agent.exception.LlmCallException;
import com.caspar.agent.model.LlmMessage;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 大模型调用封装。
 * 底层使用 Spring AI Alibaba + DashScope 自动配置，业务层继续通过 chat(messages) 调用。
 */
@Slf4j
@Service
public class LlmClient {

    private final ChatClient.Builder chatClientBuilder;

    private ChatClient chatClient;
    private ExecutorService llmExecutor;

    @Value("${spring.ai.dashscope.chat.options.model:unknown}")
    private String model;

    @Value("${agent.llm.timeout-ms:30000}")
    private long timeoutMs;

    @Value("${agent.llm.retry.max-attempts:2}")
    private int maxAttempts;

    @Value("${agent.llm.retry.backoff-ms:300}")
    private long retryBackoffMs;

    public LlmClient(ChatClient.Builder chatClientBuilder) {
        this.chatClientBuilder = chatClientBuilder;
    }

    @PostConstruct
    void init() {
        this.chatClient = chatClientBuilder.build();
        this.llmExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @PreDestroy
    void shutdown() {
        if (llmExecutor != null) {
            llmExecutor.shutdownNow();
        }
    }

    /**
     * 调用 Spring AI ChatClient，返回模型回复文本。
     *
     * @param messages 对话消息列表
     * @return 模型输出文本
     * @throws LlmCallException 调用失败时抛出
     */
    public String chat(List<LlmMessage> messages) {
        long startTime = System.currentTimeMillis();
        try {
            if (messages == null || messages.isEmpty()) {
                throw new LlmCallException("LLM 调用失败: 消息列表不能为空");
            }

            Prompt prompt = new Prompt(toSpringMessages(messages));
            int attempts = Math.max(1, maxAttempts);
            LlmCallException lastError = null;

            for (int attempt = 1; attempt <= attempts; attempt++) {
                try {
                    String result = callWithTimeout(prompt);
                    if (result == null || result.isBlank()) {
                        throw new LlmCallException("LLM 调用失败: 模型返回空内容");
                    }
                    long totalTime = System.currentTimeMillis() - startTime;
                    log.info("LLM 调用完成: provider=dashscope, model={}, 消息数={}, 尝试次数={}, 耗时={}ms",
                            model, messages.size(), attempt, totalTime);
                    return result;
                } catch (LlmCallException e) {
                    lastError = e;
                    boolean canRetry = attempt < attempts && isRetryable(e);
                    if (!canRetry) {
                        throw e;
                    }
                    log.warn("LLM 调用第 {}/{} 次失败，准备重试: model={}, error={}",
                            attempt, attempts, model, e.getMessage());
                    sleepBeforeRetry();
                }
            }
            throw lastError != null ? lastError : new LlmCallException("LLM 调用失败: 未知错误");
        } catch (LlmCallException e) {
            long totalTime = System.currentTimeMillis() - startTime;
            log.error("LLM 调用失败, model={}, 耗时={}ms, 错误={}", model, totalTime, e.getMessage());
            throw e;
        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - startTime;
            log.error("LLM 调用异常, model={}, 耗时={}ms", model, totalTime, e);
            throw new LlmCallException("LLM 调用异常: " + e.getMessage(), e);
        }
    }

    private String callWithTimeout(Prompt prompt) {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(
                () -> chatClient.prompt(prompt).call().content(),
                llmExecutor
        );
        try {
            return future.get(Math.max(timeoutMs, 1000L), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new LlmCallException("LLM 调用超时(" + timeoutMs + "ms)", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmCallException("LLM 调用被中断", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new LlmCallException("LLM 调用异常: " + cause.getMessage(), cause);
        }
    }

    private boolean isRetryable(LlmCallException e) {
        String message = e.getMessage();
        if (message == null) {
            return true;
        }
        return !message.contains("消息列表不能为空")
                && !message.contains("没有可发送的有效消息");
    }

    private void sleepBeforeRetry() {
        if (retryBackoffMs <= 0) {
            return;
        }
        try {
            Thread.sleep(retryBackoffMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private List<Message> toSpringMessages(List<LlmMessage> messages) {
        List<Message> springMessages = new ArrayList<>(messages.size());

        for (LlmMessage message : messages) {
            if (message == null || message.getContent() == null || message.getContent().isBlank()) {
                continue;
            }

            String role = message.getRole();
            String content = message.getContent();

            switch (role) {
                case "system" -> springMessages.add(new SystemMessage(content));
                case "assistant" -> springMessages.add(new AssistantMessage(content));
                case "user" -> springMessages.add(new UserMessage(content));
                default -> {
                    log.warn("检测到未知消息角色 '{}', 按 user 处理", role);
                    springMessages.add(new UserMessage(content));
                }
            }
        }

        if (springMessages.isEmpty()) {
            throw new LlmCallException("LLM 调用失败: 没有可发送的有效消息");
        }

        return springMessages;
    }
}
