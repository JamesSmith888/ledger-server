package org.jim.ledgerserver.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 注册请求参数
 *
 * @param username 用户名
 * @param email    邮箱
 * @param password 密码
 * @author James Smith
 */
public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 20, message = "用户名长度必须在 3-20 个字符之间")
        String username,

        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        String email,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 50, message = "密码长度必须在 6-50 个字符之间")
        String password
) {
}
