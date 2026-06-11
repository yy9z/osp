package com.caspar.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Agent 异步执行器配置。
 */
@Configuration
public class AgentExecutorConfig {

    @Bean(name = "toolExecutor")
    public Executor toolExecutor(
            @Value("${agent.tool-executor.core-pool-size:8}") int corePoolSize,
            @Value("${agent.tool-executor.max-pool-size:32}") int maxPoolSize,
            @Value("${agent.tool-executor.queue-capacity:500}") int queueCapacity,
            @Value("${agent.tool-executor.keep-alive-seconds:60}") int keepAliveSeconds
    ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("agent-tool-");
        executor.setCorePoolSize(Math.max(1, corePoolSize));
        executor.setMaxPoolSize(Math.max(Math.max(1, corePoolSize), maxPoolSize));
        executor.setQueueCapacity(Math.max(0, queueCapacity));
        executor.setKeepAliveSeconds(Math.max(1, keepAliveSeconds));
        executor.setAllowCoreThreadTimeOut(true);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
