package org.jim.ledgerserver.user.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.jim.ledgerserver.common.JSONResult;
import org.jim.ledgerserver.common.util.UserContext;
import org.jim.ledgerserver.user.dto.LoginResponse;
import org.jim.ledgerserver.user.dto.OAuthBindRequest;
import org.jim.ledgerserver.user.dto.OAuthLoginRequest;
import org.jim.ledgerserver.user.enums.OAuthType;
import org.jim.ledgerserver.user.service.OAuthBusinessService;
import org.springframework.web.bind.annotation.*;

/**
 * OAuth 第三方登录控制器
 * 处理第三方登录、绑定、解绑等操作
 * 
 * @author James Smith
 */
@RestController
@RequestMapping("/oauth")
public class OAuthController {
    
    @Resource
    private OAuthBusinessService oauthBusinessService;
    
    /**
     * 第三方登录
     * 支持微信、支付宝、Google、Apple
     * 
     * @param request 登录请求
     * @return 登录响应（包含JWT token）
     */
    @PostMapping("/login")
    public JSONResult<LoginResponse> login(@Valid @RequestBody OAuthLoginRequest request) {
        // 验证请求参数
        request.validate();
        
        // 获取凭证
        String credential = request.getCredential();
        
        // 执行登录
        LoginResponse response = oauthBusinessService.login(
            request.oauthType(), 
            credential
        );
        
        return JSONResult.success("登录成功", response);
    }
    
    /**
     * 绑定第三方账号
     * 需要用户已登录
     * 
     * @param request 绑定请求
     * @return 成功消息
     */
    @PostMapping("/bind")
    public JSONResult<String> bind(@Valid @RequestBody OAuthBindRequest request) {
        // 验证请求参数
        request.validate();
        
        // 获取当前登录用户
        Long userId = UserContext.getCurrentUserId();
        
        // 获取凭证
        String credential = switch (request.oauthType()) {
            case WECHAT, ALIPAY, APPLE -> request.code();
            case GOOGLE -> request.idToken();
        };
        
        // 执行绑定
        oauthBusinessService.bindOAuthAccount(
            userId, 
            request.oauthType(), 
            credential
        );
        
        return JSONResult.success(
            "绑定" + request.oauthType().getDisplayName() + "成功", 
            null
        );
    }
    
    /**
     * 解绑第三方账号
     * 
     * @param oauthType 第三方类型
     * @return 成功消息
     */
    @DeleteMapping("/unbind/{oauthType}")
    public JSONResult<String> unbind(@PathVariable String oauthType) {
        // 获取当前登录用户
        Long userId = UserContext.getCurrentUserId();
        
        // 解析登录类型
        OAuthType type = OAuthType.fromCode(oauthType);
        
        // 执行解绑
        oauthBusinessService.unbindOAuthAccount(userId, type);
        
        return JSONResult.success(
            "解绑" + type.getDisplayName() + "成功", 
            null
        );
    }
}
