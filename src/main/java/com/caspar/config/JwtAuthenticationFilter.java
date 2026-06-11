package com.caspar.config;

import com.caspar.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT认证过滤器
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // Refactor: 显式定义 Logger，替代继承自 OncePerRequestFilter 的隐式 logger
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // 获取JWT Token
            String token = getTokenFromRequest(request);
            // Refactor: 降为 DEBUG 级别，避免生产日志过多且欢欢 Token 内容
            logger.debug("请求: {}, Token: {}", request.getRequestURI(), token != null ? "present" : "absent");

            if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
                // 一次解析 Token，避免重复解码
                JwtUtil.TokenPayload payload = jwtUtil.parseTokenPayload(token);
                Long userId = payload.userId();
                String username = payload.username();
                String role = payload.role();

                // Refactor: Token 验证成功日志降为 DEBUG
                logger.debug("Token验证成功, userId: {}, role: {}", userId, role);

                // 创建认证对象
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,  // principal: userId
                                null,    // credentials
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                        );

                // 设置到安全上下文中
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                logger.debug("Token无效或为空");
            }
        } catch (Exception e) {
            logger.error("无法设置用户认证", e);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中获取Token
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
