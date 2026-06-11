package com.caspar.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Spring Security 配置类（Spring Boot 3 / Spring Security 6 推荐写法）
 * 
 * 配置策略：
 * 1. 禁用 CSRF（前后端分离项目使用 JWT 认证）
 * 2. 启用 CORS（允许前端跨域请求）
 * 3. 无状态会话（SessionCreationPolicy.STATELESS）
 * 4. JWT 过滤器集成（验证 Token）
 * 5. 基于 requestMatchers 的细粒度权限控制
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private AuthRateLimitFilter authRateLimitFilter;

    @Value("${app.security.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}")
    private String corsAllowedOrigins;

    @Value("${springdoc.swagger-ui.enabled:false}")
    private boolean swaggerUiEnabled;

    @Value("${springdoc.api-docs.enabled:false}")
    private boolean apiDocsEnabled;

    /**
     * 密码编码器（BCrypt）
     * 用于密码加密存储和验证
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 认证管理器
     * 由 Spring Security 自动配置，用于处理认证请求
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * 安全过滤链配置（SecurityFilterChain Bean）
     * 
     * Spring Security 6+ 推荐：使用 Lambda DSL + requestMatchers 进行权限配置
     * 替代旧的 antMatchers 和 mvcMatchers
     * 
     * @param http HttpSecurity 对象
     * @return SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // ============ 1. 禁用 CSRF ============
                // 原因：前后端分离项目使用 JWT Token 认证，不需要 CSRF 保护
                .csrf(csrf -> csrf.disable())

                // ============ 2. 启用 CORS ============
                // 允许前端跨域请求，配置源、方法、请求头等
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ============ 3. 会话管理 ============
                // 无状态会话（SessionCreationPolicy.STATELESS）
                // 原因：JWT Token 认证无需 Session 存储
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "未登录或token已过期"))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                response.sendError(HttpServletResponse.SC_FORBIDDEN, "无权限"))
                )

                // ============ 3.1 安全响应头 ============
                .headers(headers -> headers
                        .contentTypeOptions(withDefaults())
                        .frameOptions(frame -> frame.deny())
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; " +
                                        "base-uri 'self'; " +
                                        "frame-ancestors 'none'; " +
                                        "object-src 'none'; " +
                                        "form-action 'self'; " +
                                        "img-src 'self' data: blob: https:; " +
                                        "style-src 'self' 'unsafe-inline'; " +
                                        "script-src 'self'; " +
                                        "connect-src 'self' https:"
                        ))
                )

                // ============ 4. 请求授权配置 ============
                .authorizeHttpRequests(auth -> {
                    // --- 公开访问（无需 Token）---
                    // 前端首页
                    auth.requestMatchers("/", "/index.html").permitAll();
                    auth.requestMatchers("/error", "/favicon.ico").permitAll();
                    // 前端静态资源
                    auth.requestMatchers("/assets/**", "/images/**").permitAll();
                    // 用户认证
                    auth.requestMatchers("/api/user/register", "/api/user/login").permitAll();
                    // 公开查询接口（无需登录）
                    auth.requestMatchers(HttpMethod.GET, "/api/secondhand/list").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/lostfound/list").permitAll();
                    auth.requestMatchers("/api/user/navigation/**").permitAll();
                    auth.requestMatchers(new RegexRequestMatcher("^/api/secondhand/\\d+$", "GET")).permitAll();
                    auth.requestMatchers(new RegexRequestMatcher("^/api/lostfound/\\d+$", "GET")).permitAll();
                    // Swagger 文档资源（仅在显式开启时放行）
                    if (swaggerUiEnabled || apiDocsEnabled) {
                        auth.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/webjars/**").permitAll();
                    }

                    // --- Agent 接口：需要已登录（ROLE_USER 及以上）---
                    auth.requestMatchers("/api/agent/**").authenticated();

                    // --- 其他所有请求需要认证 ---
                    auth.anyRequest().authenticated();
                })

                // ============ 5. JWT 过滤器集成 ============
                // 将自定义 JWT 过滤器添加到 UsernamePasswordAuthenticationFilter 之前
                // 作用：每个请求都会先经过 JWT 过滤器进行 Token 验证
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // ============ 6. 认证接口限流 ============
                // 登录/注册接口在 JWT 前执行基于 IP 的限流防护
                .addFilterBefore(authRateLimitFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS 跨域资源共享配置
     * 
     * 用于允许前端跨域请求访问后端 API
     * 配置项：
     * - 允许的来源：* 表示所有来源（生产环境应限制具体域名）
     * - 允许的方法：GET, POST, PUT, DELETE, OPTIONS
     * - 允许的请求头：所有请求头
     * - 暴露的响应头：所有响应头
     * 
     * @return CorsConfigurationSource CORS 配置源
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 允许的来源（前端 URL）
        // 通过 app.security.cors.allowed-origins 配置，逗号分隔
        configuration.setAllowedOrigins(parseAllowedOrigins(corsAllowedOrigins));

        // 允许的 HTTP 方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // 允许的请求头
        configuration.setAllowedHeaders(List.of("*"));

        // 暴露的响应头（客户端可以访问这些头）
        configuration.setExposedHeaders(List.of("Authorization", "Content-Type"));

        // 是否允许发送凭证（Cookie、Session 等）
        // 注意：当 allowCredentials 为 true 时，allowedOrigins 不能为 "*"
        // 这里使用 false 是因为前后端分离项目使用 JWT Token 认证，不依赖 Session
        configuration.setAllowCredentials(false);

        // 预检请求的缓存时间（秒）
        configuration.setMaxAge(3600L);

        // 注册 CORS 配置
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    private List<String> parseAllowedOrigins(String origins) {
        if (origins == null || origins.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }
}
