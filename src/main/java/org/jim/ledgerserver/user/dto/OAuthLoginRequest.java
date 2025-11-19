package org.jim.ledgerserver.user.dto;

import jakarta.validation.constraints.NotNull;
import org.jim.ledgerserver.user.enums.OAuthType;

/**
 * 第三方登录请求
 * 使用 record 和 sealed 接口提供类型安全
 * 
 * @author James Smith
 */
public record OAuthLoginRequest(
    /**
     * 第三方平台类型
     */
    @NotNull(message = "登录类型不能为空")
    OAuthType oauthType,
    
    /**
     * 授权码（微信、支付宝、Apple使用）
     */
    String code,
    
    /**
     * ID令牌（Google使用）
     */
    String idToken,
    
    /**
     * 邀请码（可选）
     */
    String inviteCode
) {
    /**
     * 验证请求参数的完整性
     * 使用 JDK 21+ pattern matching for switch
     */
    public void validate() {
        switch (oauthType) {
            case WECHAT, ALIPAY, APPLE -> {
                if (code == null || code.isBlank()) {
                    throw new IllegalArgumentException(
                        oauthType.getDisplayName() + " 需要提供 code 参数"
                    );
                }
            }
            case GOOGLE -> {
                if (idToken == null || idToken.isBlank()) {
                    throw new IllegalArgumentException(
                        oauthType.getDisplayName() + " 需要提供 idToken 参数"
                    );
                }
            }
        }
    }
    
    /**
     * 获取凭证（code 或 idToken）
     */
    public String getCredential() {
        return switch (oauthType) {
            case WECHAT, ALIPAY, APPLE -> code;
            case GOOGLE -> idToken;
        };
    }
}
