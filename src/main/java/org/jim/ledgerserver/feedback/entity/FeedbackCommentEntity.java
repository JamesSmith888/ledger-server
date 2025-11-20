package org.jim.ledgerserver.feedback.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jim.ledgerserver.base.BaseEntity;

/**
 * 反馈评论实体
 *
 * @author James Smith
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Entity(name = "feedback_comment")
public class FeedbackCommentEntity extends BaseEntity {

    /**
     * 反馈ID
     */
    @Column(nullable = false)
    private Long feedbackId;

    /**
     * 评论用户ID
     */
    @Column(nullable = false)
    private Long userId;

    /**
     * 评论用户名
     */
    @Column(length = 100)
    private String userName;

    /**
     * 评论用户昵称
     */
    @Column(length = 100)
    private String userNickname;

    /**
     * 评论内容
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
}
