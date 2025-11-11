package org.jim.ledgerserver.common.interceptor;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jim.ledgerserver.common.util.JwtUtil;
import org.jim.ledgerserver.common.util.UserContext;
import org.jim.ledgerserver.user.entity.UserEntity;
import org.jim.ledgerserver.user.service.UserService;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 认证拦截器
 * 用于验证 JWT token 并设置用户上下文
 *
 * @author James Smith
 */
@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private UserService userService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 打印请求的 URL
        log.info("Incoming request: {}", request.getRequestURI());

        // 放行 /mcp
        if (request.getRequestURI().startsWith("/mcp")) {
            String authorization = request.getHeader("Authorization");
            log.info("MCP Authorization header========================: {}", authorization);
            
            // 打印请求体信息
            printRequestBody(request);

            // 尝试从请求体中提取token并设置到上下文
            extractAndSetTokenFromRequestBody(request);

            return true;
        }

        // 1. 从请求头中获取 token
        String token = getTokenFromRequest(request);

        // 2. 检查 token 是否存在
        if (StringUtils.isBlank(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未提供认证token\",\"data\":null}");
            return false;
        }

        // 3. 验证 token 有效性
        if (!jwtUtil.validateToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"token无效或已过期\",\"data\":null}");
            return false;
        }

        // 4. 从 token 获取用户信息并设置上下文
        try {
            UserEntity user = userService.getUserByToken(token);
            UserContext.setCurrentUser(user);
        } catch (Exception e) {
            // token 解析失败或用户不存在
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"用户信息获取失败\",\"data\":null}");
            return false;
        }

        // 5. 继续执行后续处理
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求完成后清除用户上下文，防止内存泄漏
        UserContext.clear();
    }

    /**
     * 从请求中获取 token
     * 支持两种方式：
     * 1. Authorization header: Bearer {token}
     * 2. Query parameter: token={token}
     *
     * @param request HTTP 请求
     * @return token 字符串，如果不存在则返回 null
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        // 1. 尝试从 Authorization header 获取
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.isNotBlank(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // 2. 尝试从 query parameter 获取
        String queryToken = request.getParameter("token");
        if (StringUtils.isNotBlank(queryToken)) {
            return queryToken;
        }

        return null;
    }

    /**
     * 打印请求体信息
     * 使用缓存包装器来读取请求体明文内容
     *
     * @param request HTTP 请求
     */
    private void printRequestBody(HttpServletRequest request) {
        try {
            // 打印请求基本信息
            log.info("=== MCP Request Info ===");
            log.info("Method: {}", request.getMethod());
            log.info("URI: {}", request.getRequestURI());
            log.info("Query String: {}", request.getQueryString());
            log.info("Content-Type: {}", request.getContentType());
            log.info("Content-Length: {}", request.getContentLength());
            log.info("Remote Address: {}", request.getRemoteAddr());
            
            // 打印所有请求头
            log.info("=== Request Headers ===");
            request.getHeaderNames().asIterator().forEachRemaining(headerName -> {
                log.info("{}: {}", headerName, request.getHeader(headerName));
            });
            
            // 如果是缓存包装器，读取请求体明文内容
            if (request instanceof CachedBodyHttpServletRequest) {
                CachedBodyHttpServletRequest cachedRequest = (CachedBodyHttpServletRequest) request;
                String body = cachedRequest.getBody();
                
                log.info("=== Request Body (Plain Text) ===");
                if (body != null && !body.trim().isEmpty()) {
                    log.info("Body Content:\n{}", body);
                } else {
                    log.info("Request body is empty");
                }
            } else {
                // 如果不是缓存包装器，只打印基本信息
                log.info("=== Request Body Info ===");
                if (request.getContentLength() > 0) {
                    log.info("Request has body with length: {} bytes", request.getContentLength());
                    log.info("Request body content type: {}", request.getContentType());
                    log.info("Note: Body content not cached, cannot read plain text");
                } else {
                    log.info("No request body");
                }
            }
            
        } catch (Exception e) {
            log.warn("Failed to print request body info: {}", e.getMessage());
        }
    }

    /**
     * 从MCP请求体中提取token并设置到上下文
     *
     * @param request HTTP 请求
     */
    private void extractAndSetTokenFromRequestBody(HttpServletRequest request) {
        try {
            // 只处理缓存包装器的请求
            if (request instanceof CachedBodyHttpServletRequest) {
                CachedBodyHttpServletRequest cachedRequest = (CachedBodyHttpServletRequest) request;
                String body = cachedRequest.getBody();
                
                if (body != null && !body.trim().isEmpty()) {
                    log.info("Attempting to extract token from MCP request body");
                    
                    // 解析JSON请求体
                    JsonNode rootNode = objectMapper.readTree(body);
                    
                    // 检查是否存在 params._meta.token 字段
                    if (rootNode.has("params")) {
                        JsonNode paramsNode = rootNode.get("params");
                        if (paramsNode.has("_meta")) {
                            JsonNode metaNode = paramsNode.get("_meta");
                            if (metaNode.has("token")) {
                                String token = metaNode.get("token").asText();
                                if (StringUtils.isNotBlank(token)) {
                                    log.info("Found token in MCP request, setting to UserContext");
                                    UserContext.setCurrentToken(token);
                                    
                                    // 尝试根据token设置用户信息
                                    try {
                                        if (jwtUtil.validateToken(token)) {
                                            UserEntity user = userService.getUserByToken(token);
                                            UserContext.setCurrentUser(user);
                                            log.info("Successfully set user context from MCP token: {}", user.getUsername());
                                        } else {
                                            log.warn("Token from MCP request is invalid");
                                        }
                                    } catch (Exception e) {
                                        log.warn("Failed to set user context from MCP token: {}", e.getMessage());
                                    }
                                } else {
                                    log.debug("Token field exists but is empty in MCP request");
                                }
                            } else {
                                log.debug("No token field found in _meta of MCP request");
                            }
                        } else {
                            log.debug("No _meta field found in params of MCP request");
                        }
                    } else {
                        log.debug("No params field found in MCP request");
                    }
                } else {
                    log.debug("MCP request body is empty, cannot extract token");
                }
            } else {
                log.debug("Request is not a cached body request, cannot extract token from body");
            }
        } catch (Exception e) {
            log.warn("Failed to extract token from MCP request body: {}", e.getMessage());
        }
    }
}
