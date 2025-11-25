package org.jim.ledgerserver.feedback.repository;

import org.jim.ledgerserver.feedback.entity.FeedbackCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

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

    /**
     * 批量统计多个反馈的评论数量
     *
     * @param feedbackIds 反馈ID列表
     * @return Map<反馈ID, 评论数量>
     */
    @Query("SELECT c.feedbackId as feedbackId, COUNT(c) as count " +
           "FROM feedback_comment c " +
           "WHERE c.feedbackId IN :feedbackIds AND c.deleteTime IS NULL " +
           "GROUP BY c.feedbackId")
    List<Map<String, Object>> countByFeedbackIds(@Param("feedbackIds") List<Long> feedbackIds);
}
