package org.jim.ledgerserver.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 注册响应 DTO
 *
 * @param userId     用户 ID
 * @param username   用户名
 * @param email      邮箱
 * @param role       用户角色（USER/ADMIN）
 * @param createdAt  创建时间
 * @author James Smith
 */
public record RegisterResponse(
        Long userId,
        String username,
        String email,
        String role,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdAt
) {
}
