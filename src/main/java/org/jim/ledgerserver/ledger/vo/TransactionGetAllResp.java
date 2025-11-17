package org.jim.ledgerserver.ledger.vo;

import org.jim.ledgerserver.common.enums.TransactionTypeEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionGetAllResp(
        Long id,
        /**
         * 交易名称
         */
        String name,

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
         * 交易分类ID（可选）
         */
        Long categoryId,

        /**
         * 支付方式ID（可选）
         */
        Long paymentMethodId
) {
}
