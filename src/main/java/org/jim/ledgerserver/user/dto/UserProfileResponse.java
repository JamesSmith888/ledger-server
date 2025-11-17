package org.jim.ledgerserver.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户信息响应
 * @author James Smith
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 用户邮箱
     */
    private String email;

    /**
     * 用户头像URL
     */
    private String avatarUrl;

    /**
     * 默认账本ID
     */
    private Long defaultLedgerId;

    /**
     * 用户状态
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
