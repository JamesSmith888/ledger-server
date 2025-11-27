package org.jim.ledgerserver.ledger.vo;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.jim.ledgerserver.common.util.FlexibleLocalDateTimeDeserializer;
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
        @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
        LocalDateTime transactionDateTime,
        Long ledgerId,
        Long paymentMethodId
) {
}
