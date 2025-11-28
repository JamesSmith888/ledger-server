package org.jim.ledgerserver.ledger.vo.agent;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.jim.ledgerserver.common.util.FlexibleLocalDateTimeDeserializer;
import org.jim.ledgerserver.common.enums.TransactionTypeEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Agent 专用的创建交易请求
 * 
 * @author James Smith
 */
public record AgentCreateTransactionReq(
        String description,
        BigDecimal amount,
        TransactionTypeEnum type,
        Long ledgerId,
        Long categoryId,
        Long paymentMethodId,
        @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
        LocalDateTime transactionDateTime
) {
}
