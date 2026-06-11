# Spring Security 6 / Spring Boot 3 配置说明

## 文档版本
- Spring Boot 版本：4.0.3（包含 Spring Security 6）
- 配置文件位置：[src/main/java/com/caspar/config/SecurityConfig.java](../src/main/java/com/caspar/config/SecurityConfig.java)
- 更新日期：2026-03-08

---

## 目录
1. [配置概述](#配置概述)
2. [关键配置解析](#关键配置解析)
3. [权限放行规则](#权限放行规则)
4. [核心概念](#核心概念)
5. [常见问题解答](#常见问题解答)
6. [配置检查清单](#配置检查清单)

---

## 配置概述

### 核心设计原则

```
前后端分离项目 → JWT Token 认证 → 无状态会话 → 禁用 CSRF
```

本配置采用 **Spring Security 6 推荐的 Lambda DSL 写法**，相比旧版本具有以下优势：

| 特性 | 旧版本（注解方式）| 新版本（Lambda DSL） |
|------|---|---|
| 代码格式 | `antMatchers()` | `requestMatchers()` |
| 类型安全 | 弱 | 强 |
| IDE 支持 | 一般 | 优秀 |
| 执行效率 | 较慢 | 快速 |
| 维护性 | 难 | 易 |

---

## 关键配置解析

### 1. CSRF 禁用 ❌

```java
.csrf(csrf -> csrf.disable())
```

**为什么禁用？**
- 前后端分离项目使用 JWT Token（请求头中）
- 不依赖 Session 和 Cookie
- CSRF 攻击主要针对基于 Cookie 的认证方式
- 禁用 CSRF 后，自动全局禁用跨站请求防护

**风险管理：**
- ✅ JWT Token 具有时间有限性和签名验证
- ✅ 前端需要正确的 Token 才能请求
- ✅ 通常配合 HTTPOnly Cookie 存储 Token

---

### 2. CORS 启用 ✅

```java
.cors(cors -> cors.configurationSource(corsConfigurationSource()))
```

**CORS 配置详解：**

```java
CorsConfiguration configuration = new CorsConfiguration();

// 允许的来源（前端地址）
configuration.setAllowedOrigins(List.of("*"));  
// ⚠️ 生产环境应改为：List.of("https://campus.example.com")

// 允许的 HTTP 方法
configuration.setAllowedMethods(Arrays.asList(
    "GET",      // 获取数据
    "POST",     // 提交数据
    "PUT",      // 更新整个资源
    "DELETE",   // 删除数据
    "OPTIONS",  // 预检请求
    "PATCH"     // 部分更新
));

// 允许的请求头（客户端请求头）
configuration.setAllowedHeaders(List.of("*"));
// 包括：Authorization, Content-Type, Accept 等

// 暴露的响应头（客户端可访问的响应头）
configuration.setExposedHeaders(List.of(
    "Authorization",  // 返回 Token
    "Content-Type"    // 内容类型
));

// 预检请求缓存时间（秒）
configuration.setMaxAge(3600L);  // 1 小时
```

**跨域请求流程：**

```
浏览器
  ↓
1. 发送 OPTIONS 预检请求
   ↓
Spring Security CORS 验证
  ↓
2. 如果预检通过，发送真实请求（GET/POST 等）
  ↓
返回响应
```

---

### 3. 会话管理 🔐

```java
.sessionManagement(session -> 
    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
)
```

**STATELESS 的含义：**

| 配置值 | 含义 | 适用场景 |
|------|------|--------|
| `STATELESS` | 不创建或维护 Session | ✅ JWT/Token 认证 |
| `IF_REQUIRED` | 需要时创建 Session | ❌ 不推荐混用 |
| `NEVER` | 不创建但接受 Session | 传统应用 |
| `ALWAYS` | 总是创建 Session | 基于 Session 认证 |

**为什么使用 STATELESS？**
- JWT Token 本身包含用户信息
- 无需服务器存储 Session
- 便于水平扩展（任何服务器都能验证 Token）

---

### 4. 请求授权配置（权限规则）

```java
.authorizeHttpRequests(auth -> auth
    // 规则 1：首页
    .requestMatchers("/", "/index.html").permitAll()
    
    // 规则 2：静态资源
    .requestMatchers("/assets/**").permitAll()
    .requestMatchers("/images/**").permitAll()
    
    // 规则 3：公开 API
    .requestMatchers("/api/user/register", "/api/user/login").permitAll()
    
    // 规则 4：默认规则
    .anyRequest().authenticated()
)
```

**匹配规则说明：**

```java
.requestMatchers(pattern)     // 精确匹配和通配符
.permitAll()                  // 允许所有人访问（无需登录）
.authenticated()              // 需要认证（必须有效的 Token）
.hasRole("ADMIN")             // 需要特定角色
.hasAnyRole("ADMIN", "USER")  // 需要任意一个角色
```

**通配符模式：**

| 模式 | 匹配例子 | 不匹配例子 |
|------|--------|---------|
| `/api/user/**` | `/api/user/1`, `/api/user/profile/info` | `/api/admin/**` |
| `/assets/**` | `/assets/css/style.css` | `/images/logo.png` |
| `/**` | 任何路径 | （都匹配） |

---

### 5. JWT 过滤器集成 🔑

```java
.addFilterBefore(
    jwtAuthenticationFilter, 
    UsernamePasswordAuthenticationFilter.class
)
```

**过滤链执行顺序：**

```
请求到达
  ↓
JwtAuthenticationFilter（自定义）
  ├─ 解析 Authorization 请求头
  ├─ 提取 Token
  ├─ 验证 Token 签名和过期时间
  └─ 如果有效，设置 SecurityContext
  ↓
UsernamePasswordAuthenticationFilter（Spring Security 内置）
  └─ 处理账密认证（本项目不使用）
  ↓
其他过滤器链
  ↓
请求处理器（Controller）
```

**JwtAuthenticationFilter 职责：**

```java
// 典型的 JWT 过滤器逻辑
protected void doFilterInternal(
    HttpServletRequest request,
    HttpServletResponse response,
    FilterChain filterChain
) throws ServletException, IOException {
    try {
        // 1. 从请求头提取 Token
        String token = getTokenFromRequest(request);
        
        // 2. 验证 Token
        if (jwtUtil.validateToken(token)) {
            // 3. 解析用户信息
            Long userId = jwtUtil.getUserId(token);
            String role = jwtUtil.getRole(token);
            
            // 4. 创建认证对象
            UsernamePasswordAuthenticationToken auth = 
                new UsernamePasswordAuthenticationToken(
                    userId,              // Principal（用户标识）
                    null,                // Credentials（密码，JWT 用不到）
                    authorities          // Authorities（权限）
                );
            
            // 5. 设置到安全上下文
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
    } catch (Exception e) {
        logger.error("Token 验证失败", e);
    }
    
    // 6. 继续过滤链
    filterChain.doFilter(request, response);
}
```

---

## 权限放行规则

### 前端资源放行

#### 📄 首页资源
```java
.requestMatchers("/", "/index.html").permitAll()
```
- `GET /` → 访问首页
- `GET /index.html` → 直接访问 HTML 文件

#### 🎨 静态资源
```java
.requestMatchers("/assets/**").permitAll()     // CSS、JS、字体等
.requestMatchers("/images/**").permitAll()     // 图片资源
.requestMatchers("/favicon.ico").permitAll()   // 网站图标
.requestMatchers("/manifest.json").permitAll() // PWA 配置
```

**资源路径对应关系：**
```
校园前端文件结构              放行规则
campus-frontend/
├── public/
│   ├── images/             ← /images/**
│   └── favicon.ico         ← /favicon.ico
├── src/
│   └── assets/             ← /assets/**
│       ├── styles/
│       └── js/
└── index.html              ← /index.html
```

### 用户认证 API（公开）

```java
.requestMatchers("/api/user/register").permitAll()  // 用户注册
.requestMatchers("/api/user/login").permitAll()     // 用户登录
```

### 列表查看 API（公开）

```java
.requestMatchers("/api/secondhand/list").permitAll()      // 二手交易列表
.requestMatchers("/api/secondhand/{id}").permitAll()      // 二手交易详情
.requestMatchers("/api/lostfound/list").permitAll()       // 失物招领列表
.requestMatchers("/api/lostfound/{id}").permitAll()       // 失物招领详情
.requestMatchers("/api/dormitory/list").permitAll()       // 宿舍管理列表
```

### 校园导航 API（完全开放）

```java
.requestMatchers("/api/navigation/**").permitAll()        // 所有导航 API
.requestMatchers("/api/user/navigation/**").permitAll()   // 用户导航
```

### 文档和监控端点（可选）

```java
.requestMatchers("/swagger-ui.html").permitAll()    // Swagger UI
.requestMatchers("/swagger-ui/**").permitAll()      // Swagger 资源
.requestMatchers("/v3/api-docs/**").permitAll()     // OpenAPI JSON
.requestMatchers("/actuator/**").permitAll()        // 监控端点
```

### 默认规则：其他所有请求需要认证

```java
.anyRequest().authenticated()
```

**含义：**
- 任何其他路径的请求都需要:
  1. ✅ 提供有效的 JWT Token
  2. ✅ Token 未过期
  3. ✅ Token 签名有效

---

## 核心概念

### SecurityContext vs SecurityContextHolder

```java
// 获取当前被认证用户信息
Authentication auth = SecurityContextHolder
    .getContext()
    .getAuthentication();

if (auth != null && auth.isAuthenticated()) {
    Object principal = auth.getPrincipal();     // 用户标识（userId）
    String credentials = auth.getCredentials(); // 凭证（JWT 中为 null）
    Collection<?> authorities = auth.getAuthorities();  // 权限列表
}
```

### requestMatchers 常见用法

```java
// 1. 精确匹配
.requestMatchers("/api/user/login").permitAll()

// 2. 路径变量（冰星号 ** 匹配多级）
.requestMatchers("/api/navigation/**").permitAll()

// 3. 多个路径
.requestMatchers("/login", "/register", "/help").permitAll()

// 4. HTTP 方法匹配
.requestMatchers(HttpMethod.GET, "/api/items").permitAll()
.requestMatchers(HttpMethod.POST, "/api/items").authenticated()

// 5. 正则表达式（MvcRequestMatcher）
.requestMatchers(RegexRequestMatcher.regexMatcher("^/api/v\\d+/.*")).permitAll()
```

### 认证流程完整示例

```
HTTP 请求：GET /api/user/profile
请求头：Authorization: Bearer eyJhbGc...（JWT Token）
          ↓
JwtAuthenticationFilter.doFilterInternal()
  ├─ 步骤 1：提取 Token
  │   从 Authorization 头中获取 "Bearer " 后的部分
  │   
  ├─ 步骤 2：验证 Token
  │   ├─ 检查签名是否有效
  │   ├─ 检查是否过期
  │   └─ 检查格式是否正确
  │
  ├─ 步骤 3：成功 → 解析信息
  │   userId: 123
  │   role: ADMIN
  │   username: zhangsan
  │
  ├─ 步骤 4：创建 Authentication 对象
  │   UsernamePasswordAuthenticationToken(
  │       principal: 123L,
  │       credentials: null,
  │       authorities: [ROLE_ADMIN]
  │   )
  │
  └─ 步骤 5：存储到 SecurityContext
      SecurityContextHolder.getContext().setAuthentication(...)
      ↓
AuthorizationFilter
  ├─ 检查请求路径权限规则
  ├─ /api/user/profile 需要 authenticated()？✅ 是
  ├─ 当前用户已认证？✅ 是（Token 有效）
  └─ 通过放行
      ↓
UserController.getUserProfile()
  └─ 调用 SecurityUtils.getCurrentUserId()
      返回：123L  ✅
      ↓
返回响应 200 OK
```

---

## 常见问题解答

### Q1：为什么要禁用 CSRF？

**A：** 前后端分离项目中，前端是独立的 JavaScript 应用，无法发起 CSRF 攻击，因为：

1. **不依赖 Cookie**：前端用 Token（localStorage 或内存中），浏览器无法自动附加
2. **Token 需主动添加**：每个 API 请求都需要在请求头中明确携带 Authorization
3. **同源策略限制**：跨域请求受 CORS 限制

```javascript
// 前端必须主动添加 Token
fetch('/api/user/profile', {
    headers: {
        'Authorization': `Bearer ${token}`  // 必须手动设置
    }
});
```

### Q2：CORS 中 allowCredentials 为什么是 false？

**A：** 因为项目使用 JWT 认证，不依赖 Cookie 或 Session：

```java
// JWT 认证流程
Authorization: Bearer token  // 请求头中的 Token
↓
不涉及 Cookie  // allowCredentials = false
↓
无需跨域发送 Cookie
```

但如果项目后续需要支持 Cookie（如用于存储 JWT），需要：

```java
configuration.setAllowCredentials(true);
// 必须改为具体的前端 URL，不能用 "*"
configuration.setAllowedOrigins(
    List.of("https://campus.frontend.com")
);
```

### Q3：requestMatchers 和 antMatchers 有什么区别？

**A：**

| 特性 | antMatchers | requestMatchers |
|------|---|---|
| 弃用状态 | ⚠️ Spring Security 5 后弃用 | ✅ Spring Security 6+ 推荐 |
| 写法 | `.antMatchers("/api/**")` | `.requestMatchers("/api/**")` |
| 性能 | 较慢 | 更快 |
| 功能 | 基础 | 支持 HTTP 方法、正则等 |

```java
// ❌ 旧写法（与本配置不兼容）
.antMatchers("/api/**").permitAll()

// ✅ 新写法（本配置使用）
.requestMatchers("/api/**").permitAll()
```

### Q4：如何限制 CORS 的来源？

**A：** 修改 `corsConfigurationSource()` 方法：

```java
// 开发环境（允许多个来源）
configuration.setAllowedOrigins(Arrays.asList(
    "http://localhost:3000",      // 前端开发服务器
    "http://localhost:5173",       // Vite 开发服务器
    "http://192.168.1.100:8080"   // 内网地址
));

// 生产环境（只允许特定来源）
configuration.setAllowedOrigins(List.of(
    "https://campus.example.com"  // 只允许生产域名
));
```

### Q5：如何为某个 API 添加角色限制？

**A：** 在 `authorizeHttpRequests` 中使用 `hasRole()`：

```java
.authorizeHttpRequests(auth -> auth
    // 管理员操作
    .requestMatchers("/api/admin/**").hasRole("ADMIN")
    
    // 宿管员操作
    .requestMatchers("/api/dorm/**").hasAnyRole("ADMIN", "DORM_MANAGER")
    
    // 已认证用户
    .requestMatchers("/api/user/**").authenticated()
    
    // 公开接口
    .requestMatchers("/api/items/list").permitAll()
    
    // 默认：其他请求需要认证
    .anyRequest().authenticated()
)
```

**注意：** `hasRole()` 会自动添加 `ROLE_` 前缀，无需手动添加。

### Q6：Token 过期了怎么办？

**A：** 前端需要实现以下逻辑：

```javascript
// 前端拦截器
axios.interceptors.response.use(
    response => response,
    error => {
        if (error.response?.status === 401) {
            // Token 过期或无效
            localStorage.removeItem('token');
            router.push('/login');  // 重定向到登录页
        }
        return Promise.reject(error);
    }
);
```

后端返回 401 Unauthorized 时，前端应清除 Token 并重定向。

---

## 配置检查清单

### 部署前检查

- [ ] **CSRF 禁用** - ✅ 确认已 `csrf.disable()`
- [ ] **CORS 配置** - ⚠️ 确认生产环境已限制 `allowedOrigins`
- [ ] **静态资源** - ✅ 确认前端资源路径放行
- [ ] **首页放行** - ✅ 确认 `/` 和 `/index.html` 已放行
- [ ] **认证 API** - ✅ 确认 `/api/user/login` 和 `/register` 已放行
- [ ] **JWT 过滤器** - ✅ 确认过滤器已注入和配置
- [ ] **会话管理** - ✅ 确认已设置 `STATELESS`
- [ ] **密码编码器** - ✅ 确认使用 BCryptPasswordEncoder

### 测试场景

#### 场景 1：访问首页（不登录）
```bash
curl -i http://localhost:8080/
# 预期：200 OK
```

#### 场景 2：访问公开列表（不登录）
```bash
curl -i http://localhost:8080/api/secondhand/list
# 预期：200 OK（数据列表）
```

#### 场景 3：访问受保护资源（不登录）
```bash
curl -i http://localhost:8080/api/user/profile
# 预期：401 Unauthorized
```

#### 场景 4：访问受保护资源（有效 Token）
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456"}' \
  | jq -r '.data.token')

curl -i http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer $TOKEN"
# 预期：200 OK（用户信息）
```

#### 场景 5：CORS 预检请求
```bash
curl -i -X OPTIONS http://localhost:8080/api/user/profile \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: GET"
# 预期：200 OK，CORS 响应头正确
```

---

## 配置文件关键部分速查

### 立即修改项（生产部署）

```java
// ⚠️ 1. CORS 来源限制
configuration.setAllowedOrigins(List.of(
    "https://your-domain.com"  // 改为实际域名
));

// ⚠️ 2. Token 过期时间
.jwtUtil.expiration = 3600000;  // 改为合适的过期时间
```

### 可选扩展

```java
// 加入防护：Rate Limiting（请求限流）
// 加入防护：HTTPS 强制跳转
// 加入防护：权限细化（基于"方法"而不仅是"路由"）
```

---

## 相关链接

- 📖 [Spring Security 官方文档](https://spring.io/projects/spring-security)
- 📖 [Spring Security 6 迁移指南](https://docs.spring.io/spring-security/reference/migration/index.html)
- 📖 [JWT Token 标准](https://tools.ietf.org/html/rfc7519)
- 🔧 [本项目 JWT 工具类](../src/main/java/com/caspar/util/JwtUtil.java)
- 🔧 [本项目 JWT 过滤器](../src/main/java/com/caspar/config/JwtAuthenticationFilter.java)

---

**最后更新：2026-03-08 | 作者：GitHub Copilot**
