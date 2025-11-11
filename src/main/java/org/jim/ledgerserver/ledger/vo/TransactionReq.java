package org.jim.ledgerserver.ledger.vo;

import org.jim.ledgerserver.common.enums.TransactionTypeEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易/账目实体类
 * @author James Smith
 */
public record TransactionReq(
        String name,
        String description,
        BigDecimal amount,
        TransactionTypeEnum type,
        LocalDateTime transactionDateTime,
        Long ledgerId,
        Long createdByUserId,
        Long categoryId
) {
}
