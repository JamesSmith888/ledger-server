package org.jim.ledgerserver.user.enums;

/**
 * 第三方登录平台类型
 * 使用 sealed 接口确保类型安全，便于 pattern matching
 * 
 * @author James Smith
 */
public enum OAuthType {
    /**
     * 微信开放平台
     */
    WECHAT("wechat", "微信登录"),
    
    /**
     * 支付宝开放平台
     */
    ALIPAY("alipay", "支付宝登录"),
    
    /**
     * Google OAuth 2.0
     */
    GOOGLE("google", "Google登录"),
    
    /**
     * Apple Sign In
     */
    APPLE("apple", "Apple登录");
    
    private final String code;
    private final String displayName;
    
    OAuthType(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 根据 code 获取枚举类型
     * 使用 JDK 21+ 的 pattern matching
     */
    public static OAuthType fromCode(String code) {
        return switch (code.toLowerCase()) {
            case "wechat" -> WECHAT;
            case "alipay" -> ALIPAY;
            case "google" -> GOOGLE;
            case "apple" -> APPLE;
            default -> throw new IllegalArgumentException("不支持的登录类型: " + code);
        };
    }
    
    /**
     * 判断是否需要 code 换 token
     * (Google 直接返回 idToken，不需要换)
     */
    public boolean needsCodeExchange() {
        return switch (this) {
            case WECHAT, ALIPAY, APPLE -> true;
            case GOOGLE -> false;
        };
    }
    
    /**
     * 获取唯一标识字段名
     */
    public String getUniqueIdField() {
        return switch (this) {
            case WECHAT -> "unionid";
            case ALIPAY -> "user_id";
            case GOOGLE -> "sub";
            case APPLE -> "sub";
        };
    }
}
