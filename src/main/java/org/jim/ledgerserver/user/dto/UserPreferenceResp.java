package org.jim.ledgerserver.user.dto;

import org.jim.ledgerserver.user.entity.UserPreferenceEntity;
import org.jim.ledgerserver.user.enums.PreferenceType;

import java.time.LocalDateTime;

/**
 * 用户偏好响应
 *
 * @author James Smith
 */
public record UserPreferenceResp(
        Long id,
        PreferenceType type,
        String keyword,
        String correction,
        String note,
        Long categoryId,
        Integer usageCount,
        Boolean enabled,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
    /**
     * 从实体转换
     */
    public static UserPreferenceResp from(UserPreferenceEntity entity) {
        return new UserPreferenceResp(
                entity.getId(),
                entity.getType(),
                entity.getKeyword(),
                entity.getCorrection(),
                entity.getNote(),
                entity.getCategoryId(),
                entity.getUsageCount(),
                entity.getEnabled(),
                entity.getCreateTime(),
                entity.getUpdateTime()
        );
    }
}
