package org.jim.ledgerserver.user.service;

import jakarta.annotation.Resource;
import org.jim.ledgerserver.user.entity.UserPreferenceEntity;
import org.jim.ledgerserver.user.enums.PreferenceType;
import org.jim.ledgerserver.user.repository.UserPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 用户偏好记忆服务
 *
 * @author James Smith
 */
@Service
public class UserPreferenceService {

    @Resource
    private UserPreferenceRepository preferenceRepository;

    /**
     * 获取用户的所有偏好
     */
    public List<UserPreferenceEntity> getAllByUserId(Long userId) {
        return preferenceRepository.findByUserId(userId);
    }

    /**
     * 获取用户的所有启用偏好（按使用频率排序）
     */
    public List<UserPreferenceEntity> getActiveByUserId(Long userId) {
        return preferenceRepository.findByUserIdAndEnabledTrueOrderByUsageCountDescUpdateTimeDesc(userId);
    }

    /**
     * 按类型获取用户偏好
     */
    public List<UserPreferenceEntity> getByUserIdAndType(Long userId, PreferenceType type) {
        return preferenceRepository.findByUserIdAndTypeAndEnabledTrue(userId, type);
    }

    /**
     * 按关键词查找偏好
     */
    public Optional<UserPreferenceEntity> findByKeyword(Long userId, String keyword) {
        return preferenceRepository.findByUserIdAndKeywordIgnoreCase(userId, keyword);
    }

    /**
     * 按关键词和类型查找偏好
     */
    public Optional<UserPreferenceEntity> findByKeywordAndType(Long userId, String keyword, PreferenceType type) {
        return preferenceRepository.findByUserIdAndKeywordIgnoreCaseAndType(userId, keyword, type);
    }

    /**
     * 添加或更新偏好
     */
    @Transactional
    public UserPreferenceEntity saveOrUpdate(Long userId, PreferenceType type, String keyword, 
                                              String correction, String note, Long categoryId) {
        // 检查是否已存在
        Optional<UserPreferenceEntity> existing = preferenceRepository
                .findByUserIdAndKeywordIgnoreCaseAndType(userId, keyword, type);

        if (existing.isPresent()) {
            // 更新现有记录
            UserPreferenceEntity entity = existing.get();
            entity.setCorrection(correction);
            entity.setNote(note);
            entity.setCategoryId(categoryId);
            entity.setUsageCount(entity.getUsageCount() + 1);
            return preferenceRepository.save(entity);
        }

        // 创建新记录
        UserPreferenceEntity entity = new UserPreferenceEntity()
                .setUserId(userId)
                .setType(type)
                .setKeyword(keyword)
                .setCorrection(correction)
                .setNote(note)
                .setCategoryId(categoryId)
                .setUsageCount(1)
                .setEnabled(true);

        return preferenceRepository.save(entity);
    }

    /**
     * 更新偏好
     */
    @Transactional
    public Optional<UserPreferenceEntity> update(Long userId, Long id, String correction, 
                                                  String note, Long categoryId, Boolean enabled) {
        return preferenceRepository.findById(id)
                .filter(p -> p.getUserId().equals(userId))
                .map(entity -> {
                    if (correction != null) entity.setCorrection(correction);
                    if (note != null) entity.setNote(note);
                    if (categoryId != null) entity.setCategoryId(categoryId);
                    if (enabled != null) entity.setEnabled(enabled);
                    return preferenceRepository.save(entity);
                });
    }

    /**
     * 增加使用次数
     */
    @Transactional
    public void incrementUsage(Long userId, Long id) {
        preferenceRepository.findById(id)
                .filter(p -> p.getUserId().equals(userId))
                .ifPresent(entity -> {
                    entity.setUsageCount(entity.getUsageCount() + 1);
                    preferenceRepository.save(entity);
                });
    }

    /**
     * 启用/禁用偏好
     */
    @Transactional
    public void toggleEnabled(Long userId, Long id, boolean enabled) {
        preferenceRepository.findById(id)
                .filter(p -> p.getUserId().equals(userId))
                .ifPresent(entity -> {
                    entity.setEnabled(enabled);
                    preferenceRepository.save(entity);
                });
    }

    /**
     * 删除偏好
     */
    @Transactional
    public boolean delete(Long userId, Long id) {
        return preferenceRepository.findById(id)
                .filter(p -> p.getUserId().equals(userId))
                .map(entity -> {
                    preferenceRepository.delete(entity);
                    return true;
                })
                .orElse(false);
    }

    /**
     * 删除用户的所有偏好
     */
    @Transactional
    public void deleteAll(Long userId) {
        preferenceRepository.deleteByUserId(userId);
    }

    /**
     * 获取统计信息
     */
    public PreferenceStats getStats(Long userId) {
        long total = preferenceRepository.countByUserId(userId);
        long enabled = preferenceRepository.countByUserIdAndEnabledTrue(userId);
        return new PreferenceStats(total, enabled);
    }

    /**
     * 批量保存偏好（用于从本地同步）
     */
    @Transactional
    public List<UserPreferenceEntity> batchSave(Long userId, List<UserPreferenceEntity> preferences) {
        // 先删除用户的所有偏好
        preferenceRepository.deleteByUserId(userId);
        
        // 设置用户ID并保存
        preferences.forEach(p -> p.setUserId(userId));
        return preferenceRepository.saveAll(preferences);
    }

    /**
     * 统计信息
     */
    public record PreferenceStats(long total, long enabled) {}
}
