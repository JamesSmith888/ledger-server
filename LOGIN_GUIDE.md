# 登录认证系统使用说明

## 概述

本项目采用 **JWT (JSON Web Token)** 实现用户认证和授权。登录后的用户信息存储方案如下：

### 存储方案

1. **客户端存储**：JWT token 存储在客户端（浏览器的 localStorage/sessionStorage 或移动端的本地存储）
2. **服务端存储**：使用 `ThreadLocal` 在请求处理期间临时存储当前用户信息（`UserContext`）
3. **无状态设计**：服务端不保存 session，所有用户身份信息都包含在 JWT token 中

### 优点

- ✅ 支持分布式部署，无需 session 共享
- ✅ 无状态，易于横向扩展
- ✅ 适合前后端分离架构
- ✅ 性能好，不需要频繁查询数据库或 Redis

## 核心组件

### 1. JwtUtil - JWT 工具类
位置：`org.jim.ledgerserver.common.util.JwtUtil`

功能：
- 生成 JWT token
- 验证 token 有效性
- 解析 token 获取用户信息

### 2. UserContext - 用户上下文
位置：`org.jim.ledgerserver.common.util.UserContext`

功能：
- 使用 `ThreadLocal` 存储当前请求的用户信息
- 提供静态方法获取当前登录用户

使用示例：
```java
// 获取当前登录用户
UserEntity currentUser = UserContext.getCurrentUser();

// 获取当前用户 ID
Long userId = UserContext.getCurrentUserId();

// 检查是否已登录
boolean isLoggedIn = UserContext.isLoggedIn();
```

### 3. AuthInterceptor - 认证拦截器
位置：`org.jim.ledgerserver.common.interceptor.AuthInterceptor`

功能：
- 自动从请求中提取 token
- 验证 token 并设置用户上下文
- 请求结束后清理 ThreadLocal

## API 使用

### 1. 注册
```http
POST /user/register
Content-Type: application/json

{
  "username": "testuser",
  "password": "password123"
}
```

### 2. 登录
```http
POST /user/login
Content-Type: application/json

{
  "username": "testuser",
  "password": "password123"
}
```

响应：
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresAt": "2025-10-17T12:00:00",
  "userId": 1,
  "username": "testuser",
  "nickname": "Test User",
  "avatarUrl": "https://example.com/avatar.jpg"
}
```

### 3. 使用 Token 访问受保护的接口

**方式 1：使用 Authorization Header（推荐）**
```http
GET /api/protected-resource
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**方式 2：使用 Query Parameter**
```http
GET /api/protected-resource?token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 4. 登出
```http
POST /user/logout
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**注意**：由于 JWT 是无状态的，服务端登出只是清除 `UserContext`。客户端需要自行删除本地存储的 token。

### 5. 刷新 Token（可选）
```http
POST /user/refresh-token
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

## 在业务代码中使用

### 获取当前登录用户

```java
@Service
public class SomeService {
    
    public void doSomething() {
        // 获取当前用户
        UserEntity currentUser = UserContext.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException("请先登录");
        }
        
        // 使用用户信息
        Long userId = currentUser.getId();
        String username = currentUser.getUsername();
        
        // 或者直接获取 ID
        Long userId2 = UserContext.getCurrentUserId();
    }
}
```

### 在 Controller 中使用

```java
@RestController
@RequestMapping("/api/ledger")
public class LedgerController {
    
    @GetMapping("/my-ledgers")
    public JSONResult<List<Ledger>> getMyLedgers() {
        // 自动从 UserContext 获取当前用户 ID
        Long currentUserId = UserContext.getCurrentUserId();
        
        if (currentUserId == null) {
            return JSONResult.fail("请先登录");
        }
        
        // 查询当前用户的账本
        List<Ledger> ledgers = ledgerService.findByUserId(currentUserId);
        return JSONResult.ok(ledgers);
    }
}
```

## 配置说明

在 `application.yml` 中配置 JWT 参数：

```yaml
jwt:
  # JWT 密钥（生产环境请使用环境变量）
  secret: your-very-secure-secret-key-that-is-at-least-256-bits-long
  # 过期时间（7 天，单位：毫秒）
  expiration: 604800000
```

**安全建议**：
- 生产环境中，`jwt.secret` 应通过环境变量配置，不要硬编码
- 密钥长度至少 256 位（32 字符）
- 定期更换密钥

## 拦截器配置

在 `WebConfig` 中可以配置哪些路径需要认证：

```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(authInterceptor)
            .addPathPatterns("/**") // 拦截所有请求
            .excludePathPatterns(
                    "/user/register",  // 注册
                    "/user/login",     // 登录
                    "/error",          // 错误页面
                    "/mcp/**"          // MCP 端点
            );
}
```

## 进阶：Token 黑名单（可选）

如果需要实现真正的登出（立即使 token 失效），可以添加 Redis 黑名单机制：

```java
// 1. 添加 Redis 依赖
// 2. 在 logout 方法中添加到黑名单
public void logout(String token) {
    UserContext.clear();
    
    // 添加到 Redis 黑名单，过期时间等于 token 剩余有效期
    long expirationTime = jwtUtil.getExpirationTime(token);
    redisTemplate.opsForValue().set(
        "token:blacklist:" + token, 
        "1", 
        expirationTime, 
        TimeUnit.MILLISECONDS
    );
}

// 3. 在 validateToken 中检查黑名单
public boolean validateToken(String token) {
    // 检查是否在黑名单中
    if (redisTemplate.hasKey("token:blacklist:" + token)) {
        return false;
    }
    
    // 原有验证逻辑
    // ...
}
```

## 常见问题

### 1. Token 过期了怎么办？
- 客户端可以调用 `refreshToken` 接口获取新 token
- 或者提示用户重新登录

### 2. 如何实现"记住我"功能？
- 登录时传入 `rememberMe` 参数
- 如果为 true，生成更长有效期的 token（如 30 天）

### 3. 如何防止 Token 被盗用？
- 使用 HTTPS 传输
- Token 不要存储在容易被窃取的地方
- 实现 IP 绑定或设备指纹验证
- 敏感操作需要二次验证

### 4. 多设备登录怎么处理？
- 当前方案支持多设备同时登录
- 如需限制，可以在 Redis 中维护用户的活跃 token 列表
- 新登录时踢掉旧设备的 token

## 总结

当前实现的登录系统：

✅ **已实现**：
- JWT token 生成和验证
- 用户登录、登出
- ThreadLocal 用户上下文
- 自动认证拦截器
- Token 刷新机制

⚠️ **建议扩展**（根据需求）：
- Token 黑名单（Redis）
- 记住我功能
- 多设备管理
- 登录日志记录
- IP/设备指纹验证
