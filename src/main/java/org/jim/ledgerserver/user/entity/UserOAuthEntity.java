package org.jim.ledgerserver.user.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jim.ledgerserver.base.BaseEntity;
import org.jim.ledgerserver.user.enums.OAuthType;

import java.time.LocalDateTime;

/**
 * 用户第三方账号绑定信息
 * 
 * @author James Smith
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(
    name = "user_oauth",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_oauth_type_id",
            columnNames = {"oauth_type", "oauth_id"}
        )
    },
    indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_oauth_type", columnList = "oauth_type")
    }
)
public class UserOAuthEntity extends BaseEntity {
    
    /**
     * 关联的用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    /**
     * 第三方平台类型: WECHAT, ALIPAY, GOOGLE, APPLE
     */
    @Column(name = "oauth_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private OAuthType oauthType;
    
    /**
     * 第三方平台的唯一用户ID
     * - 微信: unionid
     * - 支付宝: user_id
     * - Google: sub
     * - Apple: sub
     */
    @Column(name = "oauth_id", nullable = false, length = 100)
    private String oauthId;
    
    /**
     * OpenID（仅微信使用，单应用唯一标识）
     */
    @Column(name = "oauth_openid", length = 100)
    private String oauthOpenid;
    
    /**
     * 第三方平台的昵称
     */
    @Column(name = "oauth_name", length = 100)
    private String oauthName;
    
    /**
     * 第三方平台的头像URL
     */
    @Column(name = "oauth_avatar", length = 500)
    private String oauthAvatar;
    
    /**
     * 第三方平台的邮箱
     */
    @Column(name = "oauth_email", length = 100)
    private String oauthEmail;
    
    /**
     * 邮箱是否已验证（主要用于Google）
     */
    @Column(name = "email_verified")
    private Boolean emailVerified;
    
    /**
     * 访问令牌（用于后续调用第三方API）
     */
    @Column(name = "access_token", length = 500)
    private String accessToken;
    
    /**
     * 刷新令牌（用于刷新access_token）
     */
    @Column(name = "refresh_token", length = 500)
    private String refreshToken;
    
    /**
     * 令牌过期时间
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    /**
     * 最后一次使用该方式登录的时间
     */
    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;
}
