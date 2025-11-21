package org.jim.ledgerserver.ledger.vo;

import java.math.BigDecimal;

/**
 * 快速创建交易请求（基于模板）
 * @author James Smith
 */
public record QuickCreateTransactionReq(
        BigDecimal amount,  // 可选：覆盖模板金额
        String description, // 可选：覆盖模板描述
        String transactionDateTime  // 可选：指定交易时间，默认当前时间
) {
}
