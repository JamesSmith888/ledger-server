package org.jim.ledgerserver.user.repository;

import org.jim.ledgerserver.user.entity.UserOAuthEntity;
import org.jim.ledgerserver.user.enums.OAuthType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户第三方账号 Repository
 * 
 * @author James Smith
 */
@Repository
public interface UserOAuthRepository extends JpaRepository<UserOAuthEntity, Long> {
    
    /**
     * 根据第三方类型和第三方ID查找绑定关系
     * 
     * @param oauthType 第三方类型
     * @param oauthId 第三方用户ID
     * @return 绑定关系
     */
    Optional<UserOAuthEntity> findByOauthTypeAndOauthId(OAuthType oauthType, String oauthId);
    
    /**
     * 查询用户的所有第三方账号绑定
     * 
     * @param userId 用户ID
     * @return 绑定列表
     */
    List<UserOAuthEntity> findByUserId(Long userId);
    
    /**
     * 查询用户某个平台的绑定关系
     * 
     * @param userId 用户ID
     * @param oauthType 第三方类型
     * @return 绑定关系
     */
    Optional<UserOAuthEntity> findByUserIdAndOauthType(Long userId, OAuthType oauthType);
    
    /**
     * 判断第三方账号是否已绑定
     * 
     * @param oauthType 第三方类型
     * @param oauthId 第三方用户ID
     * @return 是否已绑定
     */
    boolean existsByOauthTypeAndOauthId(OAuthType oauthType, String oauthId);
    
    /**
     * 删除用户的某个第三方账号绑定
     * 
     * @param userId 用户ID
     * @param oauthType 第三方类型
     */
    void deleteByUserIdAndOauthType(Long userId, OAuthType oauthType);
}
