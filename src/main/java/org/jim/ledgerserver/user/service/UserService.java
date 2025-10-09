package org.jim.ledgerserver.user.service;

import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.jim.ledgerserver.common.exception.BusinessException;
import org.jim.ledgerserver.common.util.PasswordEncoder;
import org.jim.ledgerserver.user.UserEntity;
import org.jim.ledgerserver.user.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * @author James Smith
 */
@Component
public class UserService {

    @Resource
    private UserRepository userRepository;

    @Resource
    private PasswordEncoder passwordEncoder;


    /**
     * 通过用户名、密码简单注册用户
     */
    public UserEntity register(String username, String password) {
        if (StringUtils.isAnyBlank(username, password)) {
            throw new BusinessException("用户名或密码不能为空");
        }

        Optional.ofNullable(userRepository.findByUsername(username))
                .ifPresent(u -> {
                    throw new BusinessException("用户名已存在");
                });

        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        return userRepository.save(user);
    }

}
