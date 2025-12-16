package org.jim.ledgerserver.ledger.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 追加交易请求
 * 用于向现有交易追加新的金额记录
 * 
 * @author James Smith
 */
public record TransactionAppendReq(
    BigDecimal amount,              // 追加金额（必填）
    String description,             // 描述（可选，默认继承父交易）
    LocalDateTime transactionDateTime  // 交易时间（可选，默认当前时间）
) {
}
