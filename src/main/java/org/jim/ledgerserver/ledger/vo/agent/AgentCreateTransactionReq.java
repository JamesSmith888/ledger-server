package org.jim.ledgerserver.ledger.vo.agent;

import org.jim.ledgerserver.common.enums.TransactionTypeEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Agent 专用的创建交易请求
 * 
 * @author James Smith
 */
public record AgentCreateTransactionReq(
        String name,
        String description,
        BigDecimal amount,
        TransactionTypeEnum type,
        Long ledgerId,
        Long categoryId,
        Long paymentMethodId,
        LocalDateTime transactionDateTime
) {
}
