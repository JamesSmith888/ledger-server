package org.jim.ledgerserver.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 登录响应 DTO
 *
 * @param token      JWT token
 * @param tokenType  token 类型（默认 Bearer）
 * @param expiresAt  token 过期时间
 * @param userId     用户 ID
 * @param username   用户名
 * @param nickname   用户昵称
 * @param avatarUrl  用户头像
 * @author James Smith
 */
public record LoginResponse(
        String token,
        String tokenType,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime expiresAt,
        Long userId,
        String username,
        String nickname,
        String avatarUrl
) {
    /**
     * 便捷构造器，tokenType 默认为 "Bearer"
     */
    public LoginResponse(String token, LocalDateTime expiresAt, Long userId, String username, String nickname, String avatarUrl) {
        this(token, "Bearer", expiresAt, userId, username, nickname, avatarUrl);
    }
}
