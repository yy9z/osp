# Spring Security 配置 - 快速参考

## 权限规则速查表

### 📌 当前配置的所有放行规则

| 请求 | 端点 | 放行 | 原因 |
|------|------|------|------|
| **访问首页** | `GET /` | ✅ | 前端入口 |
| | `GET /index.html` | ✅ | 前端入口 |
| **静态资源** | `GET /assets/**` | ✅ | 前端 CSS、JS 等 |
| | `GET /images/**` | ✅ | 前端图片资源 |
| | `GET /favicon.ico` | ✅ | 网站图标 |
| **用户认证** | `POST /api/user/register` | ✅ | 用户注册 |
| | `POST /api/user/login` | ✅ | 用户登录 |
| **二手交易** | `GET /api/secondhand/list` | ✅ | 列表查看（无需登录） |
| | `GET /api/secondhand/{id}` | ✅ | 详情查看（无需登录） |
| | `POST /api/secondhand` | 🔐 | 发布商品（需登录） |
| **失物招领** | `GET /api/lostfound/list` | ✅ | 列表查看（无需登录） |
| | `GET /api/lostfound/{id}` | ✅ | 详情查看（无需登录） |
| **校园导航** | `GET /api/navigation/**` | ✅ | 所有导航功能（无需登录） |
| **宿舍管理** | `GET /api/dormitory/list` | ✅ | 列表查看（无需登录） |
| | `GET /api/dormitory/{id}` | ✅ | 详情查看（无需登录） |
| **所有其他 API** | 任意其他路径 | 🔐 | 需要有效的 JWT Token |

---

## 如何添加新的放行规则？

### 场景 1：放行某个公开 API 列表

**需求：** 放行 `/api/message/news` 列表（显示公告）

```java
// 在 filterChain 方法的 authorizeHttpRequests 中添加：
.requestMatchers("/api/message/news").permitAll()
```

**修改位置：** [SecurityConfig.java](../../src/main/java/com/caspar/config/SecurityConfig.java#L105)

```java
// 4.3 放行公开 API 端点
// ...
.requestMatchers("/api/dormitory/list", "/api/dormitory/{id}").permitAll()

// 👇 在这里添加新规则
.requestMatchers("/api/message/news").permitAll()  // 新增：公开公告
// ...
```

### 场景 2：放行某个需要认证的资源

**需求：** `/api/user/profile` 需要先登录

```java
// ❌ 不要添加到 permitAll()
// ✅ 默认就会被 anyRequest().authenticated() 捕获
// 无需任何配置
```

### 场景 3：对某个 API 限制特定角色

**需求：** 只有管理员能访问 `/api/admin/users`

```java
// 方式 1：在配置中添加角色检查
.requestMatchers("/api/admin/users").hasRole("ADMIN")
```

**修改位置：** [SecurityConfig.java](../../src/main/java/com/caspar/config/SecurityConfig.java) - 在 `anyRequest().authenticated()` 之前

```java
.authorizeHttpRequests(auth -> auth
    // 其他规则...
    
    // 🔐 添加角色限制规则
    .requestMatchers("/api/admin/users").hasRole("ADMIN")
    .requestMatchers("/api/admin/**").hasRole("ADMIN")
    
    // 默认规则
    .anyRequest().authenticated()
)
```

**方式 2：使用 @PreAuthorize 注解（更灵活）**

```java
@GetMapping("/users")
@PreAuthorize("hasRole('ADMIN')")  // 只有 ADMIN 角色可访问
public Result<?> listUsers() {
    // ...
}
```

---

## 配置修改示例

### 示例 1：添加新的静态资源路径

**需求：** 需要放行 `/docs` 目录下的文档

```java
// 修改前：
.requestMatchers("/images/**").permitAll()
.requestMatchers("/favicon.ico", "/manifest.json").permitAll()

// 修改后：
.requestMatchers("/images/**").permitAll()
.requestMatchers("/docs/**").permitAll()  // ✅ 新增
.requestMatchers("/favicon.ico", "/manifest.json").permitAll()
```

### 示例 2：生产环境限制 CORS

**需求：** 只允许特定域名访问

```java
// 修改函数：corsConfigurationSource()

// 修改前：
configuration.setAllowedOrigins(List.of("*"));  // 所有来源

// 修改后：
// 开发环境
List<String> origins = Arrays.asList(
    "http://localhost:3000",
    "http://localhost:5173"
);

// 生产环境
// List<String> origins = List.of("https://campus.ustc.edu.cn");

configuration.setAllowedOrigins(origins);
```

### 示例 3：支持 HTTP 方法细粒度控制

**需求：** `GET /api/items` 公开，但 `POST /api/items` 需要认证

```java
import org.springframework.http.HttpMethod;

.authorizeHttpRequests(auth -> auth
    // 其他规则...
    
    // 📍 GET 请求公开放行
    .requestMatchers(HttpMethod.GET, "/api/items").permitAll()
    
    // 📍 POST 请求需要认证
    .requestMatchers(HttpMethod.POST, "/api/items").authenticated()
    
    // 默认规则
    .anyRequest().authenticated()
)
```

---

## 常见规则模式速查

### 1. 完全公开（无需登录）

```java
.requestMatchers("/path/**").permitAll()
```

### 2. 需要认证（任何登录用户）

```java
.requestMatchers("/api/user/**").authenticated()
// 或默认：.anyRequest().authenticated()
```

### 3. 需要特定角色

```java
.requestMatchers("/api/admin/**").hasRole("ADMIN")
```

### 4. 需要任意一个角色

```java
.requestMatchers("/api/sensitive/**").hasAnyRole("ADMIN", "MANAGER")
```

### 5. 需要特定权限

```java
.requestMatchers("/api/edit/**").hasAuthority("WRITE_PERMISSION")
```

### 6. HTTP 方法级别控制

```java
.requestMatchers(HttpMethod.GET, "/api/items").permitAll()
.requestMatchers(HttpMethod.DELETE, "/api/items/**").hasRole("ADMIN")
```

---

## Token 验证流程图

```
HTTP 请求
  │
  ├─ 请求头包含 Authorization: Bearer token？
  │   │
  │   ├─ 是 → JwtAuthenticationFilter 拦截
  │   │       │
  │   │       ├─ 解析 Token
  │   │       ├─ 验证签名和过期时间
  │   │       │
  │   │       ├─ Token 有效？
  │   │       │   ├─ 是 → 提取用户信息，设置 SecurityContext
  │   │       │   └─ 否 → 保持未认证状态
  │   │       │
  │   │       └─ 继续过滤链
  │   │
  │   └─ 否 → 继续（可能需要根据请求路径处理）
  │
  ├─ AuthorizationFilter 检查权限规则
  │   │
  │   ├─ 请求路径在 permitAll() 中？
  │   │   ├─ 是 → ✅ 放行
  │   │   └─ 否 → 需要 authenticated()
  │   │
  │   ├─ 用户已认证？
  │   │   ├─ 是 → ✅ 放行
  │   │   └─ 否 → 🚫 返回 401 Unauthorized
  │   │
  │   └─ 用户有所需角色？
  │       ├─ 是 → ✅ 放行
  │       └─ 否 → 🚫 返回 403 Forbidden
  │
  └─ Controller 处理请求
      └─ 可以通过 SecurityUtils.getCurrentUserId() 获取用户 ID
         或 @AuthenticationPrincipal Long userId 获取用户
```

---

## 与 JwtAuthenticationFilter 的关系

```
SecurityConfig.java
    │
    ├─ 定义权限规则（哪些路径需要认证）
    │
    └─ 注入 JwtAuthenticationFilter
        │
        └─ 配置：.addFilterBefore(jwtAuthenticationFilter, ...)
            │
            ├─ JwtAuthenticationFilter 在 UsernamePasswordAuthenticationFilter 之前执行
            │
            └─ 作用：验证 Token 并设置 SecurityContext
                │
                └─ 后续过滤器根据 SecurityContext 判断是否有权限
```

**关键关系：**
- `SecurityConfig` = "规则定义者"（定义哪些路径需要认证、哪些公开）
- `JwtAuthenticationFilter` = "验证执行者"（验证 Token 是否有效）

---

## 测试命令

### 1. 获取 Token

```bash
# 登录
TOKEN=$(curl -s -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"123456"}' \
  | jq -r '.data.token' 2>/dev/null || echo "获取失败")

echo "Token: $TOKEN"
```

### 2. 测试公开接口（无需 Token）

```bash
# 访问首页
curl -i http://localhost:8080/

# 访问列表
curl -i http://localhost:8080/api/secondhand/list
```

### 3. 测试受保护接口（需要 Token）

```bash
# 需要 Token
curl -i http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer $TOKEN"

# 如果没有 Token
curl -i http://localhost:8080/api/user/profile
# 预期：401 Unauthorized
```

### 4. CORS 预检测试

```bash
curl -i -X OPTIONS http://localhost:8080/api/user/profile \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: GET" \
  -H "Access-Control-Request-Headers: Content-Type,Authorization"
```

---

## 故障排除

### 问题 1：访问公开接口仍返回 401

**原因：** 端点配置遗漏或拼写错误

**解决：**
```java
// ❌ 错误：路径拼写错误
.requestMatchers("/api/secondhand/list").permitAll()

// ✅ 正确：检查实际端点是否匹配
@GetMapping("/list")  // 实际端点
public List<?> list() { ... }
```

### 问题 2：跨域请求被拒绝

**原因：** CORS 配置或 Origin 不匹配

**解决：**
```java
// 调试 CORS 配置
configuration.setAllowedOrigins(List.of("*"));  // 临时允许所有（仅调试）
// 生产环境必须改为具体域名
configuration.setAllowedOrigins(List.of("https://yourdomain.com"));
```

### 问题 3：Token 过期仍能访问

**原因：** JWT 过滤器未正确验证过期时间

**解决：** 检查 [JwtUtil.java](../../src/main/java/com/caspar/util/JwtUtil.java) 中的 `validateToken()` 方法

---

## 更新日志

| 日期 | 版本 | 变更 |
|------|------|------|
| 2026-03-08 | 1.0 | 初始版本（Spring Boot 4.0.3 + Spring Security 6） |

---

**提示：** 本文档配合 [SPRING_SECURITY_CONFIG.md](./SPRING_SECURITY_CONFIG.md) 使用效果更佳。
