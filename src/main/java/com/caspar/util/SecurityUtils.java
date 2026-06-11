package com.caspar.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Spring Security 上下文工具类
 * 集中提取当前用户信息，消除各 Controller 中重复实现的相同逻辑
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    /**
     * 获取当前登录用户的 ID
     *
     * @return 用户 ID，未登录或 Token 无效时返回 null
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long) {
            return (Long) principal;
        }
        return null;
    }

    /**
     * 获取当前登录用户的角色
     *
     * @return 角色字符串（不含 ROLE_ 前缀），未登录时返回 null
     */
    public static String getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        if (authentication.getAuthorities() == null) {
            return null;
        }
        return authentication.getAuthorities().stream()
                .findFirst()
                .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                .orElse(null);
    }
}
