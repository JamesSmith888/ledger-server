package org.jim.ledgerserver.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 登录响应 DTO
 *
 * @author James Smith
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /**
     * JWT token
     */
    private String token;

    /**
     * token 类型（默认 Bearer）
     */
    private String tokenType = "Bearer";

    /**
     * token 过期时间
     */
    private LocalDateTime expiresAt;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 用户头像
     */
    private String avatarUrl;

    public LoginResponse(String token, LocalDateTime expiresAt, Long userId, String username, String nickname, String avatarUrl) {
        this.token = token;
        this.expiresAt = expiresAt;
        this.userId = userId;
        this.username = username;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
    }
}
