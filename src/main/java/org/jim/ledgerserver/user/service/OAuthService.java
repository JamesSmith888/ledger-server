package org.jim.ledgerserver.user.service;

import org.jim.ledgerserver.user.dto.OAuthUserInfo;
import org.jim.ledgerserver.user.enums.OAuthType;

/**
 * 第三方登录服务接口
 * 使用策略模式，每个第三方平台实现各自的逻辑
 * 
 * @author James Smith
 */
public interface OAuthService {
    
    /**
     * 获取第三方用户信息
     * 
     * @param credential 凭证（code 或 idToken）
     * @return 统一的用户信息
     */
    OAuthUserInfo getUserInfo(String credential);
    
    /**
     * 获取当前服务支持的登录类型
     * 
     * @return 登录类型
     */
    OAuthType getOAuthType();
    
    /**
     * 刷新访问令牌（可选实现）
     * 
     * @param refreshToken 刷新令牌
     * @return 新的用户信息（包含新令牌）
     */
    default OAuthUserInfo refreshAccessToken(String refreshToken) {
        throw new UnsupportedOperationException(
            getOAuthType().getDisplayName() + " 不支持刷新令牌"
        );
    }
    
    /**
     * 验证令牌是否有效（可选实现）
     * 
     * @param accessToken 访问令牌
     * @return 是否有效
     */
    default boolean validateToken(String accessToken) {
        return false;
    }
}
