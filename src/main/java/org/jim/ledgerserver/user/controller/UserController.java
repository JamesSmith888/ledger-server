package org.jim.ledgerserver.user.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.jim.ledgerserver.common.JSONResult;
import org.jim.ledgerserver.common.util.UserContext;
import org.jim.ledgerserver.user.dto.*;
import org.jim.ledgerserver.user.entity.UserEntity;
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

    /**
     * 获取当前用户信息
     *
     * @return 用户信息
     */
    @GetMapping("/profile")
    public JSONResult<UserProfileResponse> getProfile() {
        Long currentUserId = UserContext.getCurrentUserId();
        UserEntity user = userService.findById(currentUserId);
        return JSONResult.success(convertToProfileResponse(user));
    }

    /**
     * 更新用户默认账本
     *
     * @param request 更新请求
     * @return 更新后的用户信息
     */
    @PutMapping("/default-ledger")
    public JSONResult<UserProfileResponse> updateDefaultLedger(@Valid @RequestBody UpdateDefaultLedgerRequest request) {
        Long currentUserId = UserContext.getCurrentUserId();
        UserEntity user = userService.updateDefaultLedger(currentUserId, request.ledgerId());
        return JSONResult.success(convertToProfileResponse(user));
    }

    /**
     * 获取用户默认账本ID
     *
     * @return 默认账本ID
     */
    @GetMapping("/default-ledger")
    public JSONResult<Long> getDefaultLedger() {
        Long currentUserId = UserContext.getCurrentUserId();
        Long defaultLedgerId = userService.getDefaultLedgerId(currentUserId);
        return JSONResult.success(defaultLedgerId);
    }

    /**
     * 转换为用户信息响应
     */
    private UserProfileResponse convertToProfileResponse(UserEntity user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getAvatarUrl(),
                user.getDefaultLedgerId(),
                user.getStatus(),
                user.getCreateTime(),
                user.getUpdateTime()
        );
    }
}
