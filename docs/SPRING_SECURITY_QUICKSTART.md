# Spring Security 配置 - 快速开始

## 🚀 5 分钟快速理解

### 本配置的作用

你的应用现在支持以下权限管理：

```
┌─ 公开内容 ──────────────────────────────────┐
│  • 首页 (/) 和 HTML 文件                    │
│  • 前端静态资源 (/assets/**, /images/**)   │
│  • 用户认证 (登录、注册)                    │
│  • 列表查看 (二手、失物、宿舍等)            │
│  • 校园导航功能                            │
└──────────────────────────────────────────────┘

┌─ 受保护内容 ──────────────────────────────────┐
│  需要 JWT Token 认证                         │
│  • 个人资料                                 │
│  • 用户操作（发布、编辑、删除等）          │
│  • 管理员工作                              │
└──────────────────────────────────────────────┘
```

---

## ✅ 已完成的工作

| 内容 | 状态 | 说明 |
|------|------|------|
| 修改 SecurityConfig.java | ✅ 完成 | 追加了前端资源放行规则 |
| 详细代码注释 | ✅ 完成 | 每个配置块都有说明 |
| 禁用 CSRF | ✅ 完成 | 因为使用 JWT Token 认证 |
| 启用 CORS | ✅ 完成 | 允许前端跨域访问 |
| 创建文档说明 | ✅ 完成 | 3 份详细文档 |
| 创建测试脚本 | ✅ 完成 | 自动化验证配置 |

---

## 📁 生成的文档

| 文件 | 用途 | 何时查看 |
|------|------|--------|
| [SecurityConfig.java](../../src/main/java/com/caspar/config/SecurityConfig.java) | 完整的配置代码 | 需要修改配置时 |
| [SPRING_SECURITY_CONFIG.md](./SPRING_SECURITY_CONFIG.md) | 完整说明文档（20+ 页） | 深入学习时 |
| [SPRING_SECURITY_QUICK_REFERENCE.md](./SPRING_SECURITY_QUICK_REFERENCE.md) | 快速参考表 | 查询规则时 |
| [SPRING_SECURITY_IMPROVEMENTS.md](./SPRING_SECURITY_IMPROVEMENTS.md) | 改进对比 | 了解改动时 |
| [test_security_config.sh](./test_security_config.sh) | 自动化测试脚本 | 验证配置时 |

---

## 🧪 快速验证配置

### 方法 1：手动测试（3 个命令）

```bash
# 1️⃣ 测试首页访问（应该成功）
curl -i http://localhost:8080/

# 2️⃣ 测试列表 API（应该成功）
curl -i http://localhost:8080/api/secondhand/list

# 3️⃣ 测试受保护接口（应该返回 401）
curl -i http://localhost:8080/api/user/profile
```

**预期结果：**
- ✅ 命令 1 和 2：返回 `200 OK`
- ✅ 命令 3：返回 `401 Unauthorized`

### 方法 2：自动化测试（使用脚本）

```bash
# 给脚本执行权限
chmod +x docs/test_security_config.sh

# 运行测试
bash docs/test_security_config.sh
```

---

## 📝 常见配置修改

### 场景 1：需要放行新的 API

**需求：** 放行 `/api/notice/list`（公告列表）

**修改步骤：**

1. 打开 [SecurityConfig.java](../../src/main/java/com/caspar/config/SecurityConfig.java)
2. 找到 `filterChain` 方法
3. 在 `// 4.3 放行公开 API 端点` 部分添加：

```java
// 公告相关
.requestMatchers("/api/notice/list").permitAll()
```

4. 保存并重启应用

### 场景 2：只允许管理员访问某个接口

**需求：** 只有管理员能访问 `/api/admin/users` 

有两种方式：

**方式 A：在 SecurityConfig 中配置**

```java
.requestMatchers("/api/admin/users").hasRole("ADMIN")
```

**方式 B：在 Controller 中使用注解（推荐）**

```java
@GetMapping("/users")
@PreAuthorize("hasRole('ADMIN')")  // 只有 ADMIN 角色可访问
public List<User> listUsers() {
    // ...
}
```

### 场景 3：修改前端跨域来源（生产环境）

**目前配置：** 允许所有来源（`*`）- 仅用于开发

**生产环境修改：**

1. 打开 [SecurityConfig.java](../../src/main/java/com/caspar/config/SecurityConfig.java)
2. 找到 `corsConfigurationSource` 方法
3. 修改来源限制：

```java
// 修改前（开发环境）
configuration.setAllowedOrigins(List.of("*"));  

// 修改后（生产环境）
configuration.setAllowedOrigins(List.of(
    "https://campus.ustc.edu.cn"  // 替换为实际域名
));
```

---

## 🔑 核心概念速记

### JWT Token 认证流程

```
用户登录
  ↓
后端生成 JWT Token（包含用户信息）
  ↓
返回 Token 给前端
  ↓
前端存储 Token（localStorage 或内存中）
  ↓
前端每次请求时在请求头中携带：Authorization: Bearer token
  ↓
后端 JwtAuthenticationFilter 验证 Token
  ↓
Token 有效 → 解析用户信息 → 继续处理请求
Token 无效 → 返回 401 Unauthorized
```

### 权限检查顺序

```
1. 请求到达
   ↓
2. 检查路径是否在 permitAll() 中
   ├─ 是 → ✅ 直接放行（无需登录）
   └─ 否 → 继续
   ↓
3. 检查是否需要认证
   ├─ 需要 → 检查用户是否已认证
   │   ├─ 已认证 → ✅ 放行
   │   └─ 未认证 → 🚫 返回 401
   └─ 无需 → ✅ 放行
```

### 三个关键概念

| 概念 | 含义 | 例子 |
|------|------|------|
| **permitAll()** | 所有人都可以访问 | 登录页、列表页 |
| **authenticated()** | 任何已登录用户 | 个人资料、编辑功能 |
| **hasRole('X')** | 只有特定角色 | 管理员功能 |

---

## 🐛 常见问题

### Q: 前端访问一片空白？

**原因：** `/` 和 `/assets/**` 可能未放行

**解决：** 检查 SecurityConfig 中是否有这两行：
```java
.requestMatchers("/", "/index.html").permitAll()
.requestMatchers("/assets/**").permitAll()
```

---

### Q: 登录后仍不能访问受保护的 API？

**可能原因：**
1. Token 格式错误
2. Token 已过期
3. Token 签名验证失败

**调试步骤：**
```java
// 打开 logs：
// 查看 JwtAuthenticationFilter 的日志
logger.debug("Token验证结果: ...");
```

---

### Q: 如何测试角色权限？

**方法：** 用不同角色的用户登录进行测试

```bash
# 使用管理员账号登录
curl -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'

# 获取 Token，然后访问管理员接口
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/admin/users
```

---

### Q: CORS 错误怎么办？

**错误表现：** 浏览器报 "Cross-Origin Request Blocked"

**解决：**
1. 检查 CORS 配置中的 `allowedOrigins` 是否正确
2. 检查允许的方法是否包含你的请求方法（GET/POST 等）
3. 开发时可临时改为 `List.of("*")` 进行调试

---

## 📋 检查清单

### 部署前验证

- [ ] **代码编译无错误**
  ```bash
  mvn clean compile
  ```

- [ ] **首页可访问**
  ```bash
  curl -i http://localhost:8080/
  ```

- [ ] **静态资源可访问**
  ```bash
  curl -i http://localhost:8080/assets/style.css
  ```

- [ ] **列表 API 可访问（无需登录）**
  ```bash
  curl -i http://localhost:8080/api/secondhand/list
  ```

- [ ] **登录功能正常**
  ```bash
  curl -X POST http://localhost:8080/api/user/login \
    -H "Content-Type: application/json" \
    -d '{"username":"test","password":"123456"}'
  ```

- [ ] **受保护接口需要 Token**
  ```bash
  # 无 Token - 应该 401
  curl -i http://localhost:8080/api/user/profile
  ```

---

## 🚦 流量控制示例

如果需要添加请求限流，可以在配置中增加：

```java
// 导入 Spring Cloud CircuitBreaker（可选）
// 或使用 Bucket4j 库进行限流
```

---

## 📞 技术支持

### 查看完整文档

- **详细配置说明** → [SPRING_SECURITY_CONFIG.md](./SPRING_SECURITY_CONFIG.md)
- **快速查询表** → [SPRING_SECURITY_QUICK_REFERENCE.md](./SPRING_SECURITY_QUICK_REFERENCE.md)
- **改进对比** → [SPRING_SECURITY_IMPROVEMENTS.md](./SPRING_SECURITY_IMPROVEMENTS.md)

### 查看源代码

- **配置类** → [SecurityConfig.java](../../src/main/java/com/caspar/config/SecurityConfig.java)
- **JWT 工具** → [JwtUtil.java](../../src/main/java/com/caspar/util/JwtUtil.java)
- **JWT 过滤器** → [JwtAuthenticationFilter.java](../../src/main/java/com/caspar/config/JwtAuthenticationFilter.java)
- **安全实用工具** → [SecurityUtils.java](../../src/main/java/com/caspar/util/SecurityUtils.java)

---

## 🎯 下一步

### 推荐操作顺序

1. **🧪 运行自动化测试**
   ```bash
   bash docs/test_security_config.sh
   ```

2. **📚 阅读详细文档**（花 15 分钟）
   - 了解 CSRF、CORS、JWT 概念

3. **🔧 按需修改配置**
   - 基于实际业务需求调整权限规则

4. **📝 编写自定义规则**
   - 为新功能添加权限配置

5. **🚀 部署到生产环境**
   - 不要忘记修改 CORS 源和其他敏感配置

---

## 💼 项目特定信息

### JWT Token 工具类位置
[src/main/java/com/caspar/util/JwtUtil.java](../../src/main/java/com/caspar/util/JwtUtil.java)

**关键方法：**
- `generateToken()` - 生成 Token
- `validateToken()` - 验证 Token
- `getUserId()` - 从 Token 提取用户 ID
- `getRole()` - 从 Token 提取角色

### Spring Security 版本

```
Spring Boot 版本: 4.0.3
Spring Security 版本: 6.x（包含在 Spring Boot 中）
Java 版本: 17
```

---

## 🎓 推荐学习资源

- [Spring Security 官方文档](https://spring.io/projects/spring-security)
- [JWT 标准 (RFC 7519)](https://tools.ietf.org/html/rfc7519)
- [CORS 详解](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS)
- [Spring Boot 3 迁移指南](https://spring.io/projects/spring-boot)

---

**最后更新：2026-03-08** | **作者：GitHub Copilot**
