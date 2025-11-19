package org.jim.ledgerserver.user.dto;

import org.jim.ledgerserver.user.enums.OAuthType;

import java.time.LocalDateTime;

/**
 * 第三方平台返回的统一用户信息
 * 使用 record 提供不可变性和简洁性
 * 
 * @author James Smith
 */
public record OAuthUserInfo(
    /**
     * 第三方平台的唯一用户ID
     * - 微信: unionid
     * - 支付宝: user_id
     * - Google: sub
     * - Apple: sub
     */
    String oauthId,
    
    /**
     * 第三方平台类型
     */
    OAuthType oauthType,
    
    /**
     * 昵称
     */
    String nickname,
    
    /**
     * 头像URL
     */
    String avatarUrl,
    
    /**
     * 邮箱（可选）
     */
    String email,
    
    /**
     * 邮箱是否已验证（仅 Google 提供）
     */
    Boolean emailVerified,
    
    /**
     * OpenID（仅微信使用，单应用唯一）
     */
    String openid,
    
    /**
     * 访问令牌（可选，用于后续调用第三方API）
     */
    String accessToken,
    
    /**
     * 刷新令牌（可选）
     */
    String refreshToken,
    
    /**
     * 令牌过期时间
     */
    LocalDateTime expiresAt
) {
    /**
     * 简化构造器 - 仅核心字段
     */
    public OAuthUserInfo(String oauthId, OAuthType oauthType, String nickname, String avatarUrl) {
        this(oauthId, oauthType, nickname, avatarUrl, null, null, null, null, null, null);
    }
    
    /**
     * 验证必填字段
     */
    public void validate() {
        if (oauthId == null || oauthId.isBlank()) {
            throw new IllegalArgumentException("oauthId 不能为空");
        }
        if (oauthType == null) {
            throw new IllegalArgumentException("oauthType 不能为空");
        }
    }
    
    /**
     * 判断是否有邮箱且已验证
     */
    public boolean hasVerifiedEmail() {
        return email != null && !email.isBlank() && 
               Boolean.TRUE.equals(emailVerified);
    }
}
