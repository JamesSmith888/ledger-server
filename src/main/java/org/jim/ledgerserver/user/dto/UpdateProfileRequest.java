package org.jim.ledgerserver.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * 更新用户信息请求
 * @author James Smith
 */
public record UpdateProfileRequest(
        /**
         * 昵称
         */
        @Size(max = 50, message = "昵称长度不能超过50")
        String nickname,

        /**
         * 邮箱
         */
        @Email(message = "邮箱格式不正确")
        String email,

        /**
         * 头像URL
         */
        @Size(max = 500, message = "头像URL长度不能超过500")
        String avatarUrl
) {
}
