package org.jim.ledgerserver.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求参数
 *
 * @param username 用户名或邮箱
 * @param password 密码
 * @author James Smith
 */
public record LoginRequest(
        @NotBlank(message = "用户名或邮箱不能为空")
        String username,

        @NotBlank(message = "密码不能为空")
        String password
) {
}
