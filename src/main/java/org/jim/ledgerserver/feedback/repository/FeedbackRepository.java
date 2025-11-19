package org.jim.ledgerserver.feedback.repository;

import org.jim.ledgerserver.feedback.entity.FeedbackEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 反馈数据访问层
 *
 * @author James Smith
 */
@Repository
public interface FeedbackRepository extends JpaRepository<FeedbackEntity, Long> {

    /**
     * 查询用户的所有反馈（未删除）
     */
    @Query("SELECT f FROM feedback f WHERE f.userId = :userId AND f.deleteTime IS NULL ORDER BY f.createTime DESC")
    List<FeedbackEntity> findByUserId(@Param("userId") Long userId);

    /**
     * 根据类型查询用户的反馈（未删除）
     */
    @Query("SELECT f FROM feedback f WHERE f.userId = :userId AND f.type = :type AND f.deleteTime IS NULL ORDER BY f.createTime DESC")
    List<FeedbackEntity> findByUserIdAndType(@Param("userId") Long userId, @Param("type") String type);
}
