package org.jim.ledgerserver.user.repository;

import org.jim.ledgerserver.user.entity.UserPreferenceEntity;
import org.jim.ledgerserver.user.enums.PreferenceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户偏好记忆仓库
 *
 * @author James Smith
 */
@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreferenceEntity, Long> {

    /**
     * 按用户ID查找所有偏好
     */
    List<UserPreferenceEntity> findByUserId(Long userId);

    /**
     * 按用户ID查找所有启用的偏好
     */
    List<UserPreferenceEntity> findByUserIdAndEnabledTrue(Long userId);

    /**
     * 按用户ID和类型查找偏好
     */
    List<UserPreferenceEntity> findByUserIdAndType(Long userId, PreferenceType type);

    /**
     * 按用户ID和关键词查找偏好（不区分大小写）
     */
    Optional<UserPreferenceEntity> findByUserIdAndKeywordIgnoreCase(Long userId, String keyword);

    /**
     * 按用户ID、关键词和类型查找偏好（不区分大小写）
     */
    Optional<UserPreferenceEntity> findByUserIdAndKeywordIgnoreCaseAndType(
            Long userId, String keyword, PreferenceType type);

    /**
     * 按用户ID查找所有启用的偏好，按使用次数和更新时间排序
     */
    List<UserPreferenceEntity> findByUserIdAndEnabledTrueOrderByUsageCountDescUpdateTimeDesc(Long userId);

    /**
     * 按用户ID和类型查找所有启用的偏好
     */
    List<UserPreferenceEntity> findByUserIdAndTypeAndEnabledTrue(Long userId, PreferenceType type);

    /**
     * 统计用户的偏好数量
     */
    long countByUserId(Long userId);

    /**
     * 统计用户启用的偏好数量
     */
    long countByUserIdAndEnabledTrue(Long userId);

    /**
     * 删除用户的所有偏好
     */
    void deleteByUserId(Long userId);
}
