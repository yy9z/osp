package com.caspar.config;

import com.caspar.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 认证接口限流过滤器（登录/注册）。
 * 采用基于 IP + 接口维度的固定窗口限流，避免暴力破解。
 */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final Cache<String, WindowCounter> rateLimitStore;

    private final int maxRequests;
    private final long windowSeconds;

    public AuthRateLimitFilter(
            ObjectMapper objectMapper,
            @Value("${app.security.rate-limit.auth.max-requests:20}") int maxRequests,
            @Value("${app.security.rate-limit.auth.window-seconds:60}") long windowSeconds,
            @Value("${app.security.rate-limit.auth.cache-expire-minutes:120}") long cacheExpireMinutes
    ) {
        this.objectMapper = objectMapper;
        this.maxRequests = Math.max(1, maxRequests);
        this.windowSeconds = Math.max(1L, windowSeconds);
        this.rateLimitStore = Caffeine.newBuilder()
                .expireAfterAccess(Math.max(1L, cacheExpireMinutes), TimeUnit.MINUTES)
                .initialCapacity(1024)
                .maximumSize(200_000)
                .build();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        return !"/api/user/login".equals(path) && !"/api/user/register".equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String clientIp = resolveClientIp(request);
        String path = request.getRequestURI();
        String key = path + ":" + clientIp;

        WindowCounter counter = rateLimitStore.get(key, ignored -> new WindowCounter());
        boolean allowed = counter.tryAcquire(maxRequests, windowSeconds);
        if (allowed) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(429);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Retry-After", String.valueOf(Duration.ofSeconds(windowSeconds).toSeconds()));
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(429, "请求过于频繁，请稍后再试")));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private static final class WindowCounter {
        private long currentWindow = -1L;
        private int count = 0;

        synchronized boolean tryAcquire(int maxRequests, long windowSeconds) {
            long nowWindow = System.currentTimeMillis() / (windowSeconds * 1000L);
            if (nowWindow != currentWindow) {
                currentWindow = nowWindow;
                count = 0;
            }
            if (count >= maxRequests) {
                return false;
            }
            count++;
            return true;
        }
    }
}
