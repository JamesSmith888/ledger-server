package org.jim.ledgerserver.ledger.vo.agent;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.jim.ledgerserver.common.util.FlexibleLocalDateTimeDeserializer;
import org.jim.ledgerserver.common.enums.TransactionTypeEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Agent 专用的批量创建交易请求
 * 
 * @author James Smith
 */
public record AgentBatchCreateTransactionReq(
        Long ledgerId,
        List<TransactionItem> transactions
) {
    /**
     * 单条交易数据
     */
    public record TransactionItem(
            String name,
            String description,
            BigDecimal amount,
            TransactionTypeEnum type,
            Long categoryId,
            Long paymentMethodId,
            @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
            LocalDateTime transactionDateTime
    ) {}
}
