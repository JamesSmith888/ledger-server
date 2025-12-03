package org.jim.ledgerserver.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jim.ledgerserver.base.BaseEntity;
import org.jim.ledgerserver.user.enums.PreferenceType;

/**
 * 用户偏好记忆实体
 * 
 * 存储 AI 学习的用户个性化偏好，例如：
 * - "青桔" -> 青桔单车（交通类），而非水果
 * - "711" -> 7-Eleven 便利店
 * - "星巴克" -> 咖啡/餐饮
 *
 * @author James Smith
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Entity(name = "user_preference")
public class UserPreferenceEntity extends BaseEntity {

    /**
     * 用户ID
     */
    @Column(nullable = false)
    private Long userId;

    /**
     * 偏好类型
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PreferenceType type;

    /**
     * 触发关键词
     */
    @Column(nullable = false, length = 100)
    private String keyword;

    /**
     * 正确的理解/分类
     */
    @Column(nullable = false, length = 200)
    private String correction;

    /**
     * 附加说明
     */
    @Column(length = 500)
    private String note;

    /**
     * 相关分类ID（如果适用）
     */
    private Long categoryId;

    /**
     * 使用次数（用于排序和清理）
     */
    @Column(columnDefinition = "int default 1")
    private Integer usageCount = 1;

    /**
     * 是否启用
     */
    @Column(columnDefinition = "boolean default true")
    private Boolean enabled = true;
}
