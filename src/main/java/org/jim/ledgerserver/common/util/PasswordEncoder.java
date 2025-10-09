package org.jim.ledgerserver.common.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 密码加密工具类
 * 使用BCrypt算法进行密码的加密和验证
 * @author James Smith
 */
@Component
public class PasswordEncoder {

    private final BCryptPasswordEncoder encoder;
    
    // BCrypt的强度因子，值越大，哈希计算越慢，默认为10
    private static final int BCRYPT_STRENGTH = 12;
    
    public PasswordEncoder() {
        this.encoder = new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    }
    
    /**
     * 对密码进行加密
     * @param rawPassword 原始密码
     * @return 加密后的密码
     */
    public String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }
    
    /**
     * 验证密码是否匹配
     * @param rawPassword 原始密码
     * @param encodedPassword 加密后的密码
     * @return 是否匹配
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }
} 