package org.jim.ledgerserver.common.util;

import org.jim.ledgerserver.user.entity.UserEntity;

/**
 * 用户上下文管理类
 * 使用 ThreadLocal 存储当前请求线程的用户信息
 * 
 * @author James Smith
 */
public class UserContext {

    private static final ThreadLocal<UserEntity> CURRENT_USER = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_TOKEN = new ThreadLocal<>();

    /**
     * 设置当前用户
     *
     * @param user 用户实体
     */
    public static void setCurrentUser(UserEntity user) {
        CURRENT_USER.set(user);
    }

    /**
     * 获取当前用户
     *
     * @return 用户实体，如果未登录则返回 null
     */
    public static UserEntity getCurrentUser() {
        return CURRENT_USER.get();
    }

    /**
     * 获取当前用户 ID
     *
     * @return 用户 ID，如果未登录则返回 null
     */
    public static Long getCurrentUserId() {
        UserEntity user = CURRENT_USER.get();
        return user != null ? user.getId() : null;
    }

    /**
     * 获取当前用户名
     *
     * @return 用户名，如果未登录则返回 null
     */
    public static String getCurrentUsername() {
        UserEntity user = CURRENT_USER.get();
        return user != null ? user.getUsername() : null;
    }

    /**
     * 清除当前用户信息
     * 防止内存泄漏，请求结束后必须调用
     */
    public static void clear() {
        CURRENT_USER.remove();
        CURRENT_TOKEN.remove();
    }

    /**
     * 设置当前用户token
     *
     * @param token 用户token
     */
    public static void setCurrentToken(String token) {
        CURRENT_TOKEN.set(token);
    }

    /**
     * 获取当前用户token
     *
     * @return 用户token，如果未设置则返回 null
     */
    public static String getCurrentToken() {
        return CURRENT_TOKEN.get();
    }

    /**
     * 检查是否已登录
     *
     * @return true-已登录，false-未登录
     */
    public static boolean isLoggedIn() {
        return CURRENT_USER.get() != null;
    }
}
