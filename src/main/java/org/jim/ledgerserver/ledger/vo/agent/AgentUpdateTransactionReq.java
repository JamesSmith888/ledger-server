package org.jim.ledgerserver.ledger.vo.agent;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.jim.ledgerserver.common.util.FlexibleLocalDateTimeDeserializer;
import org.jim.ledgerserver.common.enums.TransactionTypeEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Agent 专用的更新交易请求
 * 所有字段可选，只更新提供的字段
 * 
 * @author James Smith
 */
public record AgentUpdateTransactionReq(
        Long id,
        String name,
        String description,
        BigDecimal amount,
        TransactionTypeEnum type,
        Long categoryId,
        Long paymentMethodId,
        @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
        LocalDateTime transactionDateTime
) {
}
