package org.jim.ledgerserver.completion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jim.ledgerserver.base.BaseEntity;

/**
 * 补全短语实体
 * 
 * 存储用户的高频输入短语，用于智能补全
 * 设计考虑：
 * 1. 按用户隔离数据
 * 2. 使用频率权重优化排序
 * 3. 支持前缀匹配查询
 *
 * @author James Smith
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Entity(name = "completion_phrase")
@Table(indexes = {
    @Index(name = "idx_user_phrase", columnList = "userId, phrase"),
    @Index(name = "idx_user_frequency", columnList = "userId, frequency DESC"),
    @Index(name = "idx_user_prefix", columnList = "userId, phrasePrefix")
})
public class CompletionPhraseEntity extends BaseEntity {

    /**
     * 用户ID
     */
    @Column(nullable = false)
    private Long userId;

    /**
     * 完整短语（用户曾经输入过的完整内容）
     */
    @Column(nullable = false, length = 500)
    private String phrase;

    /**
     * 短语前缀（用于快速前缀匹配，取前10个字符）
     */
    @Column(nullable = false, length = 10)
    private String phrasePrefix;

    /**
     * 使用频率（每次使用+1）
     */
    @Column(nullable = false, columnDefinition = "int default 1")
    private Integer frequency;

    /**
     * 最后使用时间戳（毫秒）
     * 用于结合频率计算热度权重
     */
    @Column(nullable = false)
    private Long lastUsedAt;

    /**
     * 来源类型
     * USER_INPUT - 用户直接输入
     * SUGGESTION_ACCEPTED - 用户采纳的建议
     * PRESET - 系统预设
     */
    @Column(nullable = false, length = 20)
    private String sourceType;

    /**
     * 类别（可选，用于分类管理）
     * QUERY - 查询类
     * RECORD - 记录类
     * COMMAND - 命令类
     */
    @Column(length = 20)
    private String category;
}
