package org.jim.ledgerserver.feedback.repository;

import org.jim.ledgerserver.feedback.entity.FeedbackCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 反馈评论仓储
 *
 * @author James Smith
 */
@Repository
public interface FeedbackCommentRepository extends JpaRepository<FeedbackCommentEntity, Long> {

    /**
     * 根据反馈ID查询评论列表
     *
     * @param feedbackId 反馈ID
     * @return 评论列表
     */
    List<FeedbackCommentEntity> findByFeedbackIdAndDeleteTimeIsNullOrderByCreateTimeAsc(Long feedbackId);

    /**
     * 统计反馈的评论数量
     *
     * @param feedbackId 反馈ID
     * @return 评论数量
     */
    long countByFeedbackIdAndDeleteTimeIsNull(Long feedbackId);
}
