package org.jim.ledgerserver.user.service;

import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.jim.ledgerserver.common.exception.BusinessException;
import org.jim.ledgerserver.common.util.JwtUtil;
import org.jim.ledgerserver.common.util.PasswordEncoder;
import org.jim.ledgerserver.common.util.UserContext;
import org.jim.ledgerserver.user.dto.LoginResponse;
import org.jim.ledgerserver.user.entity.UserEntity;
import org.jim.ledgerserver.user.repository.UserRepository;
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
                user.getAvatarUrl()
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
                user.getAvatarUrl()
        );
    }


}
