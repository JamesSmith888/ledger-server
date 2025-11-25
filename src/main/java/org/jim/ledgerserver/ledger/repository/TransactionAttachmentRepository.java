package org.jim.ledgerserver.ledger.repository;

import org.jim.ledgerserver.ledger.entity.TransactionAttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 交易附件数据访问层
 * @author James Smith
 */
@Repository
public interface TransactionAttachmentRepository extends JpaRepository<TransactionAttachmentEntity, Long> {

    /**
     * 根据交易ID查找所有附件（不包含文件数据和缩略图，仅元数据）
     * @param transactionId 交易ID
     * @return 附件列表
     */
    @Query("SELECT a FROM transaction_attachment a WHERE a.transactionId = :transactionId AND a.deleteTime IS NULL")
    List<TransactionAttachmentEntity> findMetadataByTransactionId(@Param("transactionId") Long transactionId);

    /**
     * 根据交易ID查找所有附件（包含缩略图，但不包含原始文件数据）
     * @param transactionId 交易ID
     * @return 附件列表
     */
    @Query("SELECT a FROM transaction_attachment a WHERE a.transactionId = :transactionId AND a.deleteTime IS NULL")
    List<TransactionAttachmentEntity> findByTransactionIdWithThumbnails(@Param("transactionId") Long transactionId);

    /**
     * 统计交易的附件数量
     * @param transactionId 交易ID
     * @return 附件数量
     */
    @Query("SELECT COUNT(a) FROM transaction_attachment a WHERE a.transactionId = :transactionId AND a.deleteTime IS NULL")
    long countByTransactionId(@Param("transactionId") Long transactionId);

    /**
     * 统计交易的附件总大小
     * @param transactionId 交易ID
     * @return 总大小（字节）
     */
    @Query("SELECT COALESCE(SUM(a.fileSize), 0) FROM transaction_attachment a WHERE a.transactionId = :transactionId AND a.deleteTime IS NULL")
    long sumFileSizeByTransactionId(@Param("transactionId") Long transactionId);

    /**
     * 批量统计多个交易的附件数量
     * @param transactionIds 交易ID列表
     * @return 对象数组，[transactionId, count]
     */
    @Query("SELECT a.transactionId, COUNT(a) FROM transaction_attachment a " +
           "WHERE a.transactionId IN :transactionIds AND a.deleteTime IS NULL " +
           "GROUP BY a.transactionId")
    List<Object[]> countByTransactionIds(@Param("transactionIds") List<Long> transactionIds);

}
