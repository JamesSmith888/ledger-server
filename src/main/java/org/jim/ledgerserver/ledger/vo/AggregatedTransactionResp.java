package org.jim.ledgerserver.ledger.vo;

import org.jim.ledgerserver.common.enums.TransactionSourceEnum;
import org.jim.ledgerserver.common.enums.TransactionTypeEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 聚合交易响应
 * 包含父交易和所有子交易的信息
 * 
 * @author James Smith
 */
public record AggregatedTransactionResp(
    // 父交易基本信息
    Long id,
    String description,
    BigDecimal amount,                    // 父交易原始金额
    BigDecimal aggregatedAmount,          // 聚合总金额（父+所有子）
    TransactionTypeEnum type,
    LocalDateTime transactionDateTime,    // 父交易时间
    LocalDateTime latestDateTime,         // 最新交易时间（子交易中最晚的）
    Long ledgerId,
    Long createdByUserId,
    String createdByUserName,
    String createdByUserNickname,
    Long categoryId,
    Long paymentMethodId,
    long attachmentCount,
    TransactionSourceEnum source,
    
    // 子交易列表
    List<ChildTransactionResp> children
) {
    /**
     * 子交易响应
     */
    public record ChildTransactionResp(
        Long id,
        String description,
        BigDecimal amount,
        TransactionTypeEnum type,
        LocalDateTime transactionDateTime,
        Long ledgerId,
        Long createdByUserId,
        String createdByUserName,
        String createdByUserNickname,
        Long categoryId,
        Long paymentMethodId,
        long attachmentCount,
        TransactionSourceEnum source,
        Long parentId,
        LocalDateTime createTime
    ) {}
}
