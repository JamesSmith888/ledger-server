package org.jim.ledgerserver.feedback.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jim.ledgerserver.base.BaseEntity;

/**
 * 反馈反应实体（点赞/倒赞）
 * 用于同时支持反馈和评论的反应（通过targetId和targetType区分）
 *
 * @author James Smith
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Entity(name = "feedback_reaction")
@Table(indexes = {
    @Index(name = "idx_target_user_reaction", columnList = "targetId,targetType,userId", unique = true),
    @Index(name = "idx_target_id_type", columnList = "targetId,targetType")
})
public class FeedbackReactionEntity extends BaseEntity {

    /**
     * 目标ID（反馈ID或评论ID）
     */
    @Column(nullable = false)
    private Long targetId;

    /**
     * 目标类型：feedback 或 comment
     */
    @Column(nullable = false, length = 20)
    private String targetType;

    /**
     * 反应用户ID
     */
    @Column(nullable = false)
    private Long userId;

    /**
     * 反应类型：upvote（赞） 或 downvote（踩）
     */
    @Column(nullable = false, length = 20)
    private String reactionType;
}
