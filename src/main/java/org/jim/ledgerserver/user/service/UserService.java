package org.jim.ledgerserver.user.service;

import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.jim.ledgerserver.common.exception.BusinessException;
import org.jim.ledgerserver.common.util.JwtUtil;
import org.jim.ledgerserver.common.util.PasswordEncoder;
import org.jim.ledgerserver.common.util.UserContext;
import org.jim.ledgerserver.user.dto.LoginRequest;
import org.jim.ledgerserver.user.dto.LoginResponse;
import org.jim.ledgerserver.user.dto.RegisterRequest;
import org.jim.ledgerserver.user.dto.RegisterResponse;
import org.jim.ledgerserver.user.dto.UpdateProfileRequest;
import org.jim.ledgerserver.user.entity.UserEntity;
import org.jim.ledgerserver.user.repository.UserRepository;
import org.jim.ledgerserver.user.event.UserRegisteredEvent;
import org.jim.ledgerserver.ledger.service.PaymentMethodService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * @author James Smith
 */
@Component
public class UserService {

    @Resource
    private UserRepository userRepository;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private PaymentMethodService paymentMethodService;

    @Resource
    private ApplicationEventPublisher eventPublisher;

    /**
     * 通过用户名、密码简单注册用户
     */
    public UserEntity register(String username, String password) {
        if (StringUtils.isAnyBlank(username, password)) {
            throw new BusinessException("用户名或密码不能为空");
        }

        userRepository.findByUsername(username)
                .ifPresent(existingUser -> {
                    throw new BusinessException("用户名已存在");
                });

        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        return userRepository.save(user);
    }

    /**
     * 用户注册（支持用户名、邮箱）
     *
     * @param request 注册请求
     * @return 注册响应
     */
    public RegisterResponse register(RegisterRequest request) {
        // 1. 检查用户名是否已存在
        userRepository.findByUsername(request.username())
                .ifPresent(u -> {
                    throw new BusinessException("用户名已存在");
                });

        // 2. 检查邮箱是否已存在
        if (userRepository.findByEmail(request.email()) != null) {
            throw new BusinessException("邮箱已被注册");
        }

        // 3. 创建用户
        UserEntity user = new UserEntity();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setStatus(1); // 默认正常状态
        
        UserEntity savedUser = userRepository.save(user);

        // 4. 初始化默认支付方式
        try {
            paymentMethodService.createDefaultPaymentMethods(savedUser.getId());
        } catch (Exception e) {
            // 如果创建支付方式失败，记录日志但不影响注册流程
            System.err.println("创建默认支付方式失败: " + e.getMessage());
        }

        // 5. 发布用户注册事件（异步创建默认账本）
        eventPublisher.publishEvent(new UserRegisteredEvent(this, savedUser.getId(), savedUser.getUsername()));

        // 6. 返回注册结果
        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getRole(),
                savedUser.getCreateTime()
        );
    }

    /**
     * 用户登录（支持用户名或邮箱登录）
     *
     * @param request 登录请求
     * @return 登录响应
     */
    public LoginResponse login(LoginRequest request) {
        // 1. 查询用户（支持用户名或邮箱）
        UserEntity user = findUserByUsernameOrEmail(request.username());
        
        // 2. 验证密码
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        // 3. 检查用户状态
        checkUserStatus(user);

        // 4. 更新登录信息
        user.setLastLoginTime(LocalDateTime.now());
        userRepository.save(user);

        // 5. 生成 JWT token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

        // 6. 返回登录响应
        return new LoginResponse(
                token,
                expiresAt,
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getRole()
        );
    }

    /**
     * 根据用户名或邮箱查找用户
     */
    private UserEntity findUserByUsernameOrEmail(String usernameOrEmail) {
        // 判断是邮箱还是用户名
        if (usernameOrEmail.contains("@")) {
            UserEntity user = userRepository.findByEmail(usernameOrEmail);
            if (user == null) {
                throw new BusinessException("用户名或密码错误");
            }
            return user;
        } else {
            return userRepository.findByUsername(usernameOrEmail)
                    .orElseThrow(() -> new BusinessException("用户名或密码错误"));
        }
    }

    /**
     * 检查用户状态
     */
    private void checkUserStatus(UserEntity user) {
        if (user.getStatus() != null && user.getStatus() != 1) {
            String statusMsg = switch (user.getStatus()) {
                case 2 -> "您的账号已被禁言";
                case 3 -> "您的账号已被封号";
                default -> "您的账号状态异常";
            };
            throw new BusinessException(statusMsg);
        }
    }

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 密码
     * @return 登录响应（包含 token 和用户信息）
     */
    public LoginResponse login(String username, String password) {
        // 1. 参数校验
        if (StringUtils.isAnyBlank(username, password)) {
            throw new BusinessException("用户名或密码不能为空");
        }

        // 2. 查询用户
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户名或密码错误"));

        // 3. 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        // 4. 检查用户状态
        if (user.getStatus() != null && user.getStatus() != 1) {
            String statusMsg = switch (user.getStatus()) {
                case 2 -> "您的账号已被禁言";
                case 3 -> "您的账号已被封号";
                default -> "您的账号状态异常";
            };
            throw new BusinessException(statusMsg);
        }

        // 5. 更新登录信息（可选，根据需求决定是否记录）
        user.setLastLoginTime(LocalDateTime.now());
        // 如果需要记录 IP，可以从 HttpServletRequest 中获取
        // user.setLastLoginIp(ipAddress);
        userRepository.save(user);

        // 6. 生成 JWT token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        // 7. 计算过期时间（假设从配置文件读取的过期时间是 7 天）
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

        // 8. 构建返回结果
        return new LoginResponse(
                token,
                expiresAt,
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getRole()
        );
    }

    /**
     * 用户登出
     * 由于 JWT 是无状态的，服务端不需要做任何操作
     * 客户端只需要删除本地存储的 token 即可
     * 
     * 如果需要实现真正的登出（立即失效 token），需要：
     * 1. 维护一个 token 黑名单（Redis）
     * 2. 或者使用 refresh token 机制
     */
    public void logout() {
        // 清除当前线程的用户上下文
        UserContext.clear();
        
        // 如果需要实现 token 黑名单，可以在这里添加到 Redis
        // 例如：redisTemplate.opsForValue().set("blacklist:" + token, "1", expirationTime);
    }

    /**
     * 根据 token 获取用户信息
     *
     * @param token JWT token
     * @return 用户实体
     */
    public UserEntity getUserByToken(String token) {
        // 1. 验证 token
        if (!jwtUtil.validateToken(token)) {
            throw new BusinessException("token 无效或已过期");
        }

        // 2. 从 token 中解析用户 ID
        Long userId = jwtUtil.getUserIdFromToken(token);

        // 3. 查询用户
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }

    /**
     * 刷新 token（可选功能）
     *
     * @param oldToken 旧的 token
     * @return 新的登录响应
     */
    public LoginResponse refreshToken(String oldToken) {
        // 1. 验证旧 token
        if (!jwtUtil.validateToken(oldToken)) {
            throw new BusinessException("token 无效或已过期");
        }

        // 2. 获取用户信息
        UserEntity user = getUserByToken(oldToken);

        // 3. 生成新 token
        String newToken = jwtUtil.generateToken(user.getId(), user.getUsername());
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

        // 4. 返回新的登录响应
        return new LoginResponse(
                newToken,
                expiresAt,
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getRole()
        );
    }

    /**
     * 根据用户ID查找用户
     * @param userId 用户ID
     * @return 用户实体
     */
    public UserEntity findById(Long userId) {
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }

    /**
     * 更新用户默认账本
     * @param userId 用户ID
     * @param ledgerId 账本ID
     * @return 更新后的用户实体
     */
    public UserEntity updateDefaultLedger(Long userId, Long ledgerId) {
        UserEntity user = findById(userId);
        user.setDefaultLedgerId(ledgerId);
        return userRepository.save(user);
    }

    /**
     * 获取用户默认账本ID
     * @param userId 用户ID
     * @return 默认账本ID，可能为null
     */
    public Long getDefaultLedgerId(Long userId) {
        UserEntity user = findById(userId);
        return user.getDefaultLedgerId();
    }

    /**
     * 更新用户信息
     * @param userId 用户ID
     * @param request 更新请求
     * @return 更新后的用户实体
     */
    public UserEntity updateProfile(Long userId, UpdateProfileRequest request) {
        UserEntity user = findById(userId);
        
        // 更新昵称
        if (request.nickname() != null) {
            user.setNickname(request.nickname());
        }
        
        // 更新邮箱（需要检查邮箱是否已被使用）
        if (request.email() != null && !request.email().equals(user.getEmail())) {
            UserEntity existingUser = userRepository.findByEmail(request.email());
            if (existingUser != null && !existingUser.getId().equals(userId)) {
                throw new BusinessException("邮箱已被其他用户使用");
            }
            user.setEmail(request.email());
        }
        
        // 更新头像
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl());
        }
        
        return userRepository.save(user);
    }

}
