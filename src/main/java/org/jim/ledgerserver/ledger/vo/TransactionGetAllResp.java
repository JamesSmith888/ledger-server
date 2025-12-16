package org.jim.ledgerserver.ledger.vo;

import org.jim.ledgerserver.common.enums.TransactionSourceEnum;
import org.jim.ledgerserver.common.enums.TransactionTypeEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionGetAllResp(
        Long id,

        /**
         * 交易描述
         */
        String description,

        /**
         * 交易金额
         */
        BigDecimal amount,

        /**
         * 交易类型，收入或支出
         * 1 - INCOME - 收入
         * 2 - EXPENSE - 支出
         * @see TransactionTypeEnum
         */
        TransactionTypeEnum type,

        /**
         * 交易日期时间
         */
        LocalDateTime transactionDateTime,

        /**
         * 关联的账本ID(非必填)
         */
        Long ledgerId,

        /**
         * 创建该交易的用户ID
         */
        Long createdByUserId,

        /**
         * 创建人用户名
         */
        String createdByUserName,

        /**
         * 创建人昵称
         */
        String createdByUserNickname,

        /**
         * 交易分类ID（可选）
         */
        Long categoryId,

        /**
         * 支付方式ID（可选）
         */
        Long paymentMethodId,

        /**
         * 附件数量
         */
        Long attachmentCount,

        /**
         * 交易来源
         * 1 - MANUAL - 手动录入
         * 2 - AI - AI助手创建
         * @see TransactionSourceEnum
         */
        TransactionSourceEnum source,

        /**
         * 聚合总金额（父交易+所有子交易）
         * 如果没有子交易，则等于 amount
         */
        BigDecimal aggregatedAmount,

        /**
         * 子交易数量
         */
        Long childCount
) {
    // 兼容旧构造函数
    public TransactionGetAllResp(
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
            Long attachmentCount,
            TransactionSourceEnum source
    ) {
        this(id, description, amount, type, transactionDateTime, ledgerId, createdByUserId, 
             createdByUserName, createdByUserNickname, categoryId, paymentMethodId, 
             attachmentCount, source, amount, 0L);
    }
}
