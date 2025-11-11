package org.jim.ledgerserver.user.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.jim.ledgerserver.common.JSONResult;
import org.jim.ledgerserver.user.dto.LoginRequest;
import org.jim.ledgerserver.user.dto.LoginResponse;
import org.jim.ledgerserver.user.dto.RegisterRequest;
import org.jim.ledgerserver.user.dto.RegisterResponse;
import org.jim.ledgerserver.user.service.UserService;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 *
 * @author James Smith
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @return 注册响应
     */
    @PostMapping("/register")
    public JSONResult<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = userService.register(request);
        return JSONResult.success(response);
    }

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 登录响应（包含 JWT token）
     */
    @PostMapping("/login")
    public JSONResult<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return JSONResult.success(response);
    }

    /**
     * 用户登出
     *
     * @return 成功消息
     */
    @PostMapping("/logout")
    public JSONResult<String> logouxt() {
        userService.logout();
        return JSONResult.success("登出成功", null);
    }

    /**
     * 刷新 Token
     *
     * @param authorization Authorization header (Bearer token)
     * @return 新的登录响应
     */
    @PostMapping("/refresh-token")
    public JSONResult<LoginResponse> refreshToken(@RequestHeader("Authorization") String authorization) {
        // 提取 token（去掉 "Bearer " 前缀）
        String token = authorization.startsWith("Bearer ") ? authorization.substring(7) : authorization;
        LoginResponse response = userService.refreshToken(token);
        return JSONResult.success(response);
    }
}
