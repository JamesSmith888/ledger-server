package org.jim.ledgerserver.user.repository;

import org.jim.ledgerserver.user.entity.UserAvatarEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户头像仓库
 * @author James Smith
 */
@Repository
public interface UserAvatarRepository extends JpaRepository<UserAvatarEntity, Long> {
    
    /**
     * 根据用户ID查找头像
     */
    Optional<UserAvatarEntity> findByUserId(Long userId);
    
    /**
     * 根据用户ID删除头像
     */
    void deleteByUserId(Long userId);
}
