package org.jim.ledgerserver.common.interceptor;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.jim.ledgerserver.common.util.JwtUtil;
import org.jim.ledgerserver.common.util.UserContext;
import org.jim.ledgerserver.user.entity.UserEntity;
import org.jim.ledgerserver.user.service.UserService;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 认证拦截器
 * 用于验证 JWT token 并设置用户上下文
 *
 * @author James Smith
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 从请求头中获取 token
        String token = getTokenFromRequest(request);

        // 2. 如果 token 存在且有效，设置用户上下文
        if (StringUtils.isNotBlank(token) && jwtUtil.validateToken(token)) {
            try {
                UserEntity user = userService.getUserByToken(token);
                UserContext.setCurrentUser(user);
            } catch (Exception e) {
                // token 无效或用户不存在，清除上下文
                UserContext.clear();
            }
        }

        // 3. 继续执行后续处理
        // 注意：这里返回 true 表示继续执行，即使没有 token 也会放行
        // 如果需要强制登录，可以根据具体接口判断并返回 false
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
}
