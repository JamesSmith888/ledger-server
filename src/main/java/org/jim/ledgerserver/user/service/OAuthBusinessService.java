package org.jim.ledgerserver.user.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.jim.ledgerserver.common.exception.BusinessException;
import org.jim.ledgerserver.common.util.JwtUtil;
import org.jim.ledgerserver.common.util.PasswordEncoder;
import org.jim.ledgerserver.user.dto.LoginResponse;
import org.jim.ledgerserver.user.dto.OAuthUserInfo;
import org.jim.ledgerserver.user.entity.UserEntity;
import org.jim.ledgerserver.user.entity.UserOAuthEntity;
import org.jim.ledgerserver.user.enums.OAuthType;
import org.jim.ledgerserver.user.repository.UserOAuthRepository;
import org.jim.ledgerserver.user.repository.UserRepository;
import org.jim.ledgerserver.ledger.service.PaymentMethodService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * OAuth 业务服务
 * 处理第三方登录的核心业务逻辑
 * 使用 JDK 21+ 特性
 * 
 * @author James Smith
 */
@Service
public class OAuthBusinessService {
    
    private static final Logger log = LoggerFactory.getLogger(OAuthBusinessService.class);
    
    @Resource
    private UserRepository userRepository;
    
    @Resource
    private UserOAuthRepository userOAuthRepository;
    
    @Resource
    private JwtUtil jwtUtil;
    
    @Resource
    private PasswordEncoder passwordEncoder;
    
    @Resource
    private PaymentMethodService paymentMethodService;
    
    @Resource
    private List<OAuthService> oauthServices;
    
    /**
     * OAuth服务映射表
     * 使用 Map 实现策略模式
     */
    private Map<OAuthType, OAuthService> oauthServiceMap;
    
    /**
     * 初始化 OAuth 服务映射
     * 使用 Stream API 和 Function 接口
     */
    @PostConstruct
    public void init() {
        this.oauthServiceMap = oauthServices.stream()
            .collect(Collectors.toMap(
                OAuthService::getOAuthType,
                Function.identity()
            ));
        
        log.info("已注册的 OAuth 服务: {}", 
            oauthServiceMap.keySet().stream()
                .map(OAuthType::getDisplayName)
                .toList()
        );
    }
    
    /**
     * 第三方登录
     * 如果用户不存在则自动注册
     * 
     * @param oauthType 登录类型
     * @param credential 凭证（code 或 idToken）
     * @return 登录响应
     */
    @Transactional
    public LoginResponse login(OAuthType oauthType, String credential) {
        // 1. 获取对应的 OAuth 服务
        OAuthService oauthService = getOAuthService(oauthType);
        
        // 2. 调用第三方接口获取用户信息
        OAuthUserInfo oauthUserInfo = oauthService.getUserInfo(credential);
        oauthUserInfo.validate();
        
        log.info("获取到{}用户信息: oauthId={}, nickname={}", 
            oauthType.getDisplayName(), 
            oauthUserInfo.oauthId(), 
            oauthUserInfo.nickname()
        );
        
        // 3. 查找或创建用户
        UserEntity user = findOrCreateUser(oauthUserInfo);
        
        // 4. 更新或创建 OAuth 绑定关系
        updateOAuthBinding(user.getId(), oauthUserInfo);
        
        // 5. 生成 JWT token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
        
        return new LoginResponse(
            token,
            expiresAt,
            user.getId(),
            user.getUsername(),
            user.getNickname(),
            user.getAvatarUrl()
        );
    }
    
    /**
     * 绑定第三方账号
     * 用于已登录用户绑定新的第三方账号
     * 
     * @param userId 用户ID
     * @param oauthType 第三方类型
     * @param credential 凭证
     */
    @Transactional
    public void bindOAuthAccount(Long userId, OAuthType oauthType, String credential) {
        // 1. 验证用户存在
        userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException("用户不存在"));
        
        // 2. 检查是否已绑定该类型
        Optional<UserOAuthEntity> existing = userOAuthRepository
            .findByUserIdAndOauthType(userId, oauthType);
        
        if (existing.isPresent()) {
            throw new BusinessException("您已绑定" + oauthType.getDisplayName());
        }
        
        // 3. 获取第三方用户信息
        OAuthService oauthService = getOAuthService(oauthType);
        OAuthUserInfo oauthUserInfo = oauthService.getUserInfo(credential);
        oauthUserInfo.validate();
        
        // 4. 检查该第三方账号是否已被其他用户绑定
        Optional<UserOAuthEntity> bound = userOAuthRepository
            .findByOauthTypeAndOauthId(oauthType, oauthUserInfo.oauthId());
        
        if (bound.isPresent() && !bound.get().getUserId().equals(userId)) {
            throw new BusinessException(
                "该" + oauthType.getDisplayName() + "账号已被其他用户绑定"
            );
        }
        
        // 5. 创建绑定关系
        updateOAuthBinding(userId, oauthUserInfo);
        
        log.info("用户 {} 成功绑定 {} 账号: {}", 
            userId, oauthType.getDisplayName(), oauthUserInfo.oauthId());
    }
    
    /**
     * 解绑第三方账号
     * 
     * @param userId 用户ID
     * @param oauthType 第三方类型
     */
    @Transactional
    public void unbindOAuthAccount(Long userId, OAuthType oauthType) {
        UserOAuthEntity oauth = userOAuthRepository
            .findByUserIdAndOauthType(userId, oauthType)
            .orElseThrow(() -> new BusinessException(
                "您未绑定" + oauthType.getDisplayName()
            ));
        
        userOAuthRepository.delete(oauth);
        
        log.info("用户 {} 成功解绑 {} 账号", userId, oauthType.getDisplayName());
    }
    
    /**
     * 查找或创建用户
     * 使用 JDK 21+ pattern matching
     */
    private UserEntity findOrCreateUser(OAuthUserInfo oauthUserInfo) {
        // 1. 尝试通过 OAuth 绑定关系查找用户
        Optional<UserOAuthEntity> oauthBinding = userOAuthRepository
            .findByOauthTypeAndOauthId(
                oauthUserInfo.oauthType(), 
                oauthUserInfo.oauthId()
            );
        
        if (oauthBinding.isPresent()) {
            Long userId = oauthBinding.get().getUserId();
            return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户数据异常"));
        }
        
        // 2. 如果有邮箱，尝试通过邮箱查找
        if (oauthUserInfo.hasVerifiedEmail()) {
            UserEntity existingUser = userRepository.findByEmail(oauthUserInfo.email());
            if (existingUser != null) {
                log.info("通过邮箱找到已存在用户: {}", existingUser.getId());
                return existingUser;
            }
        }
        
        // 3. 创建新用户
        return createNewUser(oauthUserInfo);
    }
    
    /**
     * 创建新用户
     * 使用第三方信息初始化
     */
    private UserEntity createNewUser(OAuthUserInfo oauthUserInfo) {
        UserEntity user = new UserEntity();
        
        // 生成唯一用户名
        String username = generateUniqueUsername(oauthUserInfo);
        user.setUsername(username);
        
        // 设置基本信息
        user.setNickname(oauthUserInfo.nickname());
        user.setAvatarUrl(oauthUserInfo.avatarUrl());
        user.setEmail(oauthUserInfo.email());
        
        // 生成随机密码（用于可能的密码登录）
        String randomPassword = UUID.randomUUID().toString();
        user.setPassword(passwordEncoder.encode(randomPassword));
        
        user.setStatus(1); // 正常状态
        
        UserEntity savedUser = userRepository.save(user);
        
        // 初始化默认支付方式
        try {
            paymentMethodService.createDefaultPaymentMethods(savedUser.getId());
        } catch (Exception e) {
            log.error("创建默认支付方式失败: {}", e.getMessage());
        }
        
        log.info("创建新用户成功: userId={}, username={}", 
            savedUser.getId(), savedUser.getUsername());
        
        return savedUser;
    }
    
    /**
     * 生成唯一用户名
     * 格式: {平台前缀}_{oauthId后8位}
     */
    private String generateUniqueUsername(OAuthUserInfo oauthUserInfo) {
        String prefix = switch (oauthUserInfo.oauthType()) {
            case WECHAT -> "wx";
            case ALIPAY -> "ali";
            case GOOGLE -> "gg";
            case APPLE -> "apple";
        };
        
        String oauthId = oauthUserInfo.oauthId();
        String suffix = oauthId.length() > 8 
            ? oauthId.substring(oauthId.length() - 8) 
            : oauthId;
        
        String username = prefix + "_" + suffix;
        
        // 检查是否已存在，如果存在则添加随机后缀
        int attempt = 0;
        String finalUsername = username;
        while (userRepository.findByUsername(finalUsername).isPresent() && attempt < 10) {
            finalUsername = username + "_" + (int)(Math.random() * 1000);
            attempt++;
        }
        
        return finalUsername;
    }
    
    /**
     * 更新或创建 OAuth 绑定关系
     */
    private void updateOAuthBinding(Long userId, OAuthUserInfo oauthUserInfo) {
        Optional<UserOAuthEntity> existing = userOAuthRepository
            .findByOauthTypeAndOauthId(
                oauthUserInfo.oauthType(), 
                oauthUserInfo.oauthId()
            );
        
        UserOAuthEntity oauth = existing.orElseGet(UserOAuthEntity::new);
        
        oauth.setUserId(userId);
        oauth.setOauthType(oauthUserInfo.oauthType());
        oauth.setOauthId(oauthUserInfo.oauthId());
        oauth.setOauthOpenid(oauthUserInfo.openid());
        oauth.setOauthName(oauthUserInfo.nickname());
        oauth.setOauthAvatar(oauthUserInfo.avatarUrl());
        oauth.setOauthEmail(oauthUserInfo.email());
        oauth.setEmailVerified(oauthUserInfo.emailVerified());
        oauth.setAccessToken(oauthUserInfo.accessToken());
        oauth.setRefreshToken(oauthUserInfo.refreshToken());
        oauth.setExpiresAt(oauthUserInfo.expiresAt());
        oauth.setLastLoginTime(LocalDateTime.now());
        
        userOAuthRepository.save(oauth);
    }
    
    /**
     * 获取 OAuth 服务
     */
    private OAuthService getOAuthService(OAuthType oauthType) {
        OAuthService service = oauthServiceMap.get(oauthType);
        if (service == null) {
            throw new BusinessException(
                "不支持的登录类型: " + oauthType.getDisplayName()
            );
        }
        return service;
    }
}
