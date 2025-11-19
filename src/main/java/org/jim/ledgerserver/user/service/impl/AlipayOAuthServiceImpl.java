package org.jim.ledgerserver.user.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipaySystemOauthTokenRequest;
import com.alipay.api.request.AlipayUserInfoShareRequest;
import com.alipay.api.response.AlipaySystemOauthTokenResponse;
import com.alipay.api.response.AlipayUserInfoShareResponse;
import jakarta.annotation.PostConstruct;
import org.jim.ledgerserver.common.exception.BusinessException;
import org.jim.ledgerserver.user.dto.OAuthUserInfo;
import org.jim.ledgerserver.user.enums.OAuthType;
import org.jim.ledgerserver.user.service.OAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 支付宝登录服务实现
 * 使用支付宝官方 SDK 进行 OAuth 2.0 授权
 * 
 * @author James Smith
 */
@Service
public class AlipayOAuthServiceImpl implements OAuthService {
    
    private static final Logger log = LoggerFactory.getLogger(AlipayOAuthServiceImpl.class);
    
    private static final String GATEWAY_URL = "https://openapi.alipay.com/gateway.do";
    private static final String FORMAT = "json";
    private static final String CHARSET = "UTF-8";
    private static final String SIGN_TYPE = "RSA2";
    
    @Value("${oauth.alipay.app-id}")
    private String appId;
    
    @Value("${oauth.alipay.private-key}")
    private String privateKey;
    
    @Value("${oauth.alipay.alipay-public-key}")
    private String alipayPublicKey;
    
    private AlipayClient alipayClient;
    
    /**
     * 初始化支付宝客户端
     * 使用 @PostConstruct 确保配置加载后初始化
     */
    @PostConstruct
    public void init() {
        this.alipayClient = new DefaultAlipayClient(
            GATEWAY_URL,
            appId,
            privateKey,
            FORMAT,
            CHARSET,
            alipayPublicKey,
            SIGN_TYPE
        );
        log.info("支付宝登录服务初始化完成, AppID: {}", appId);
    }
    
    @Override
    public OAuthUserInfo getUserInfo(String authCode) {
        try {
            // 步骤1: 用授权码换取访问令牌
            var tokenInfo = exchangeToken(authCode);
            
            // 步骤2: 用访问令牌获取用户信息
            var userInfo = fetchUserInfo(tokenInfo.accessToken());
            
            // 步骤3: 构建统一的用户信息
            return new OAuthUserInfo(
                userInfo.userId(),
                OAuthType.ALIPAY,
                userInfo.nickName(),
                userInfo.avatar(),
                userInfo.email(),
                null, // 支付宝不提供邮箱验证状态
                null, // 支付宝没有 openid 概念
                tokenInfo.accessToken(),
                tokenInfo.refreshToken(),
                tokenInfo.expiresAt()
            );
            
        } catch (AlipayApiException e) {
            log.error("支付宝登录失败: {}", e.getMessage(), e);
            throw new BusinessException("支付宝登录失败: " + e.getErrMsg());
        } catch (Exception e) {
            log.error("支付宝登录异常: {}", e.getMessage(), e);
            throw new BusinessException("支付宝登录异常: " + e.getMessage());
        }
    }
    
    /**
     * 用授权码换取访问令牌
     * 
     * @param authCode 授权码
     * @return 令牌信息
     */
    private TokenInfo exchangeToken(String authCode) throws AlipayApiException {
        var request = new AlipaySystemOauthTokenRequest();
        request.setGrantType("authorization_code");
        request.setCode(authCode);
        
        AlipaySystemOauthTokenResponse response = alipayClient.execute(request);
        
        if (!response.isSuccess()) {
            throw new AlipayApiException(
                response.getSubCode(),
                response.getSubMsg()
            );
        }
        
        // 计算过期时间（支付宝返回的是秒数）
        LocalDateTime expiresAt = LocalDateTime.now()
            .plusSeconds(Long.parseLong(response.getExpiresIn()));
        
        return new TokenInfo(
            response.getAccessToken(),
            response.getRefreshToken(),
            response.getUserId(),
            expiresAt
        );
    }
    
    /**
     * 获取用户详细信息
     * 
     * @param accessToken 访问令牌
     * @return 用户信息
     */
    private AlipayUserInfo fetchUserInfo(String accessToken) throws AlipayApiException {
        var request = new AlipayUserInfoShareRequest();
        
        AlipayUserInfoShareResponse response = alipayClient.execute(request, accessToken);
        
        if (!response.isSuccess()) {
            throw new AlipayApiException(
                response.getSubCode(),
                response.getSubMsg()
            );
        }
        
        return new AlipayUserInfo(
            response.getUserId(),
            response.getNickName(),
            response.getAvatar(),
            response.getEmail(),
            response.getProvince(),
            response.getCity()
        );
    }
    
    @Override
    public OAuthType getOAuthType() {
        return OAuthType.ALIPAY;
    }
    
    @Override
    public OAuthUserInfo refreshAccessToken(String refreshToken) {
        try {
            var request = new AlipaySystemOauthTokenRequest();
            request.setGrantType("refresh_token");
            request.setRefreshToken(refreshToken);
            
            AlipaySystemOauthTokenResponse response = alipayClient.execute(request);
            
            if (!response.isSuccess()) {
                throw new AlipayApiException(
                    response.getSubCode(),
                    response.getSubMsg()
                );
            }
            
            // 刷新后重新获取用户信息
            var userInfo = fetchUserInfo(response.getAccessToken());
            
            LocalDateTime expiresAt = LocalDateTime.now()
                .plusSeconds(Long.parseLong(response.getExpiresIn()));
            
            return new OAuthUserInfo(
                userInfo.userId(),
                OAuthType.ALIPAY,
                userInfo.nickName(),
                userInfo.avatar(),
                userInfo.email(),
                null,
                null,
                response.getAccessToken(),
                response.getRefreshToken(),
                expiresAt
            );
            
        } catch (AlipayApiException e) {
            log.error("刷新支付宝令牌失败: {}", e.getMessage(), e);
            throw new BusinessException("刷新支付宝令牌失败: " + e.getErrMsg());
        } catch (Exception e) {
            log.error("刷新支付宝令牌异常: {}", e.getMessage(), e);
            throw new BusinessException("刷新支付宝令牌异常: " + e.getMessage());
        }
    }
    
    /**
     * 令牌信息（内部使用）
     */
    private record TokenInfo(
        String accessToken,
        String refreshToken,
        String userId,
        LocalDateTime expiresAt
    ) {}
    
    /**
     * 支付宝用户信息（内部使用）
     */
    private record AlipayUserInfo(
        String userId,
        String nickName,
        String avatar,
        String email,
        String province,
        String city
    ) {}
}
