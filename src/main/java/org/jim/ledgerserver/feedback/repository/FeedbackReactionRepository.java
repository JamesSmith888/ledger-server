package org.jim.ledgerserver.feedback.repository;

import org.jim.ledgerserver.feedback.entity.FeedbackReactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 反馈反应Repository
 *
 * @author James Smith
 */
@Repository
public interface FeedbackReactionRepository extends JpaRepository<FeedbackReactionEntity, Long> {

    /**
     * 获取用户对特定目标的反应
     */
    Optional<FeedbackReactionEntity> findByTargetIdAndTargetTypeAndUserId(
            Long targetId, String targetType, Long userId);

    /**
     * 获取特定目标的所有点赞
     */
    long countByTargetIdAndTargetTypeAndReactionType(
            Long targetId, String targetType, String reactionType);

    /**
     * 获取特定目标的所有反应
     */
    List<FeedbackReactionEntity> findByTargetIdAndTargetType(Long targetId, String targetType);

    /**
     * 删除用户对特定目标的反应
     */
    void deleteByTargetIdAndTargetTypeAndUserId(Long targetId, String targetType, Long userId);
}
