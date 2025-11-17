package org.jim.ledgerserver.ledger.vo;

import org.jim.ledgerserver.common.enums.TransactionTypeEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易更新请求
 * @author James Smith
 */
public record TransactionUpdateReq(
        TransactionTypeEnum type,
        BigDecimal amount,
        Long categoryId,
        String description,
        LocalDateTime transactionDateTime,
        Long ledgerId,
        Long paymentMethodId
) {
}
