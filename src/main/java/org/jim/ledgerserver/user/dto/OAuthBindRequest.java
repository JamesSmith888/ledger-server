package org.jim.ledgerserver.user.dto;

import org.jim.ledgerserver.user.enums.OAuthType;

/**
 * 绑定第三方账号请求
 * 用于已登录用户绑定第三方账号
 * 
 * @author James Smith
 */
public record OAuthBindRequest(
    OAuthType oauthType,
    String code,
    String idToken
) {
    public void validate() {
        if (oauthType == null) {
            throw new IllegalArgumentException("登录类型不能为空");
        }
        
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
}
