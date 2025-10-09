package org.jim.ledgerserver.user.repository;

import org.jim.ledgerserver.user.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @author James Smith
 */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Page<UserEntity> findAllByNickname(String name, Pageable pageable);

    /**
     * 根据邮箱查找用户
     * @param email 用户邮箱
     * @return 用户实体
     */
    UserEntity findByEmail(String email);

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByUsernameOrEmailOrPhone(String username, String email, String phone);
}
