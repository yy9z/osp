package com.caspar.agent.session;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.caspar.agent.model.AgentSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Agent 会话状态管理，优先使用 Redis，Redis 不可用时降级到内存存储。
 * Key 格式：agent:session:{sessionId}，TTL 30 分钟
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentSessionManager {

    private static final String KEY_PREFIX = "agent:session:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${agent.session.ttl-minutes:30}")
    private long sessionTtlMinutes;

    @Value("${agent.session.memory.max-size:10000}")
    private long maxMemorySessions;

    /** Redis 不可用时的内存降级存储 */
    private Cache<String, AgentSession> memoryStore;
    private final ConcurrentHashMap<String, Object> sessionLocks = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        memoryStore = Caffeine.newBuilder()
                .expireAfterAccess(sessionTtlMinutes, TimeUnit.MINUTES)
                .maximumSize(maxMemorySessions)
                .build();
    }

    public String createSession(Long userId) {
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        createSessionWithId(sessionId, userId);
        return sessionId;
    }

    public AgentSession createSessionWithId(String sessionId, Long userId) {
        AgentSession session = new AgentSession();
        session.setSessionId(sessionId);
        session.setUserId(userId);
        save(session);
        return session;
    }

    public AgentSession getSession(String sessionId) {
        try {
            Object raw = redisTemplate.opsForValue().get(KEY_PREFIX + sessionId);
            if (raw != null) {
                return objectMapper.convertValue(raw, AgentSession.class);
            }
        } catch (Exception e) {
            log.warn("Redis 不可用，降级到内存会话: {}", e.getMessage());
        }
        return memoryStore.getIfPresent(sessionId);
    }

    public void save(AgentSession session) {
        try {
            redisTemplate.opsForValue().set(
                    KEY_PREFIX + session.getSessionId(),
                    session,
                    sessionTtlMinutes,
                    TimeUnit.MINUTES
            );
        } catch (Exception e) {
            log.warn("Redis 不可用，会话保存到内存: {}", e.getMessage());
        }
        // 始终在内存中保存一份，确保降级时可用
        memoryStore.put(session.getSessionId(), session);
    }

    public void delete(String sessionId) {
        try {
            redisTemplate.delete(KEY_PREFIX + sessionId);
        } catch (Exception e) {
            log.warn("Redis 不可用，从内存删除会话: {}", e.getMessage());
        }
        memoryStore.invalidate(sessionId);
    }

    /**
     * 如果 sessionId 存在则返回已有会话，否则创建新会话。
     */
    public AgentSession getOrCreate(String sessionId, Long userId) {
        if (sessionId != null) {
            Object lock = sessionLocks.computeIfAbsent(sessionId, ignored -> new Object());
            try {
                synchronized (lock) {
                    AgentSession existing = getSession(sessionId);
                    if (existing != null) {
                        return existing;
                    }
                    return createSessionWithId(sessionId, userId);
                }
            } finally {
                sessionLocks.remove(sessionId, lock);
            }
        }
        String newId = createSession(userId);
        return getSession(newId);
    }
}
