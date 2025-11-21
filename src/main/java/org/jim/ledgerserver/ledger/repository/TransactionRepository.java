package org.jim.ledgerserver.ledger.repository;

import org.jim.ledgerserver.ledger.entity.TransactionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 交易数据访问层
 * @author James Smith
 */
@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long>, JpaSpecificationExecutor<TransactionEntity> {

    /**
     * 根据账本ID查找所有交易
     * @param ledgerId 账本ID
     * @return 交易列表
     */
    List<TransactionEntity> findByLedgerId(Long ledgerId);

    /**
     * 根据账本ID分页查找交易
     * @param ledgerId 账本ID
     * @param pageable 分页参数
     * @return 交易分页结果
     */
    Page<TransactionEntity> findByLedgerId(Long ledgerId, Pageable pageable);

    /**
     * 根据创建用户ID查找所有交易
     * @param createdByUserId 创建用户ID
     * @return 交易列表
     */
    List<TransactionEntity> findByCreatedByUserId(Long createdByUserId);

    /**
     * 根据创建用户ID分页查找交易
     * @param createdByUserId 创建用户ID
     * @param pageable 分页参数
     * @return 交易分页结果
     */
    Page<TransactionEntity> findByCreatedByUserId(Long createdByUserId, Pageable pageable);

    /**
     * 根据交易类型查找交易
     * @param type 交易类型
     * @param createdByUserId 创建用户ID
     * @return 交易列表
     */
    List<TransactionEntity> findByTypeAndCreatedByUserId(Integer type, Long createdByUserId);

    /**
     * 根据时间范围查找交易
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param createdByUserId 创建用户ID
     * @return 交易列表
     */
    List<TransactionEntity> findByTransactionDateTimeBetweenAndCreatedByUserId(
            LocalDateTime startTime, LocalDateTime endTime, Long createdByUserId);

    /**
     * 根据分类ID查找交易
     * @param categoryId 分类ID
     * @return 交易列表
     */
    List<TransactionEntity> findByCategoryId(Long categoryId);

    /**
     * 根据分类ID和用户ID查找交易
     * @param categoryId 分类ID
     * @param createdByUserId 创建用户ID
     * @return 交易列表
     */
    List<TransactionEntity> findByCategoryIdAndCreatedByUserId(Long categoryId, Long createdByUserId);

    /**
     * 查询指定账本的交易统计
     * @param ledgerId 账本ID
     * @param type 交易类型
     * @return 总金额
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM transaction t WHERE t.ledgerId = :ledgerId AND t.type = :type AND t.deleteTime IS NULL")
    BigDecimal sumAmountByLedgerIdAndType(@Param("ledgerId") Long ledgerId, @Param("type") Integer type);

    /**
     * 查询指定用户的交易统计
     * @param createdByUserId 创建用户ID
     * @param type 交易类型
     * @return 总金额
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM transaction t WHERE t.createdByUserId = :createdByUserId AND t.type = :type AND t.deleteTime IS NULL")
    BigDecimal sumAmountByCreatedByUserIdAndType(@Param("createdByUserId") Long createdByUserId, @Param("type") Integer type);

    /**
     * 查询用户最近一周的分类使用频率统计（用于推荐常用分类）
     * @param userId 用户ID
     * @param type 交易类型
     * @param startTime 开始时间（一周前）
     * @return [categoryId, count] 分类ID和使用次数
     */
    @Query("SELECT t.categoryId, COUNT(t.id) as cnt FROM transaction t " +
           "WHERE t.createdByUserId = :userId AND t.type = :type " +
           "AND t.transactionDateTime >= :startTime AND t.deleteTime IS NULL " +
           "GROUP BY t.categoryId ORDER BY cnt DESC")
    List<Object[]> findTopCategoriesByUsageInLastWeek(@Param("userId") Long userId, 
                                                       @Param("type") Integer type, 
                                                       @Param("startTime") LocalDateTime startTime);
}
