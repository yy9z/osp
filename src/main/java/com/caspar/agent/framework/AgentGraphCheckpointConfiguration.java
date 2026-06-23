package com.caspar.agent.framework;

import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.checkpoint.savers.redis.RedisSaver;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Checkpoint infrastructure used by the long-running Agent graph.
 */
@Configuration(proxyBeanMethods = false)
@Slf4j
public class AgentGraphCheckpointConfiguration {

    private RedissonClient redissonClient;

    @Bean
    BaseCheckpointSaver agentGraphCheckpointSaver(
            RedisProperties properties,
            @Value("${agent.graph.checkpoint.redis.enabled:true}") boolean redisEnabled) {
        if (!redisEnabled) {
            return new MemorySaver();
        }
        Config config = new Config();
        String address = "redis://" + properties.getHost() + ":" + properties.getPort();
        SingleServerConfig server = config.useSingleServer()
                .setAddress(address)
                .setDatabase(properties.getDatabase())
                .setConnectTimeout(3000)
                .setTimeout(3000)
                .setConnectionMinimumIdleSize(1)
                .setConnectionPoolSize(4)
                .setSubscriptionConnectionMinimumIdleSize(1)
                .setSubscriptionConnectionPoolSize(2)
                .setRetryAttempts(1);
        if (properties.getUsername() != null && !properties.getUsername().isBlank()) {
            server.setUsername(properties.getUsername());
        }
        if (properties.getPassword() != null && !properties.getPassword().isBlank()) {
            server.setPassword(properties.getPassword());
        }
        try {
            redissonClient = Redisson.create(config);
            RedisSaver redisSaver = RedisSaver.builder().redisson(redissonClient).build();
            return new ResilientCheckpointSaver(redisSaver, new MemorySaver());
        } catch (Exception e) {
            redissonClient = null;
            log.warn("Redis checkpoint 初始化失败，Agent Graph 使用内存模式: {}", e.getMessage());
            return new MemorySaver();
        }
    }

    @PreDestroy
    void shutdownRedisson() {
        if (redissonClient != null && !redissonClient.isShutdown()) {
            redissonClient.shutdown();
        }
    }
}
