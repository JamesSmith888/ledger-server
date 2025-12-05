package org.jim.ledgerserver.completion.repository;

import org.jim.ledgerserver.completion.entity.CompletionPhraseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 补全短语仓库
 * 
 * 核心查询优化：
 * 1. 前缀匹配 + 频率排序
 * 2. 热度衰减算法（时间越久权重越低）
 *
 * @author James Smith
 */
@Repository
public interface CompletionPhraseRepository extends JpaRepository<CompletionPhraseEntity, Long> {

    /**
     * 根据前缀查找短语（高频优先）
     * 使用 phrasePrefix 字段进行快速匹配，再用完整 phrase 进行过滤
     */
    @Query("""
        SELECT c FROM completion_phrase c 
        WHERE c.userId = :userId 
        AND c.deleteTime IS NULL
        AND c.phrase LIKE :prefix% 
        ORDER BY c.frequency DESC, c.lastUsedAt DESC
        """)
    List<CompletionPhraseEntity> findByUserIdAndPhrasePrefix(
        @Param("userId") Long userId, 
        @Param("prefix") String prefix,
        Pageable pageable
    );

    /**
     * 查找用户的高频短语（用于初始化缓存）
     */
    @Query("""
        SELECT c FROM completion_phrase c 
        WHERE c.userId = :userId 
        AND c.deleteTime IS NULL
        ORDER BY c.frequency DESC, c.lastUsedAt DESC
        """)
    List<CompletionPhraseEntity> findTopPhrasesByUserId(
        @Param("userId") Long userId, 
        Pageable pageable
    );

    /**
     * 查找用户指定短语是否已存在
     */
    Optional<CompletionPhraseEntity> findByUserIdAndPhraseAndDeleteTimeIsNull(Long userId, String phrase);

    /**
     * 增加短语使用频率
     */
    @Modifying
    @Query("""
        UPDATE completion_phrase c 
        SET c.frequency = c.frequency + 1, c.lastUsedAt = :lastUsedAt 
        WHERE c.id = :id
        """)
    void incrementFrequency(@Param("id") Long id, @Param("lastUsedAt") Long lastUsedAt);

    /**
     * 获取用户短语总数
     */
    long countByUserIdAndDeleteTimeIsNull(Long userId);

    /**
     * 删除用户最旧的短语（LRU淘汰）
     */
    @Modifying
    @Query(value = """
        UPDATE completion_phrase SET delete_time = NOW() 
        WHERE user_id = :userId AND delete_time IS NULL
        ORDER BY frequency ASC, last_used_at ASC 
        LIMIT :count
        """, nativeQuery = true)
    void deleteOldestPhrases(@Param("userId") Long userId, @Param("count") int count);

    /**
     * 批量查询（用于同步）
     */
    @Query("""
        SELECT c FROM completion_phrase c 
        WHERE c.userId = :userId 
        AND c.deleteTime IS NULL
        AND c.updateTime > :since
        ORDER BY c.updateTime ASC
        """)
    List<CompletionPhraseEntity> findUpdatedSince(
        @Param("userId") Long userId, 
        @Param("since") java.time.LocalDateTime since
    );
}
