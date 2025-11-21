package org.jim.ledgerserver.ledger.vo;

import java.math.BigDecimal;

/**
 * 交易模板响应
 * @author James Smith
 */
public record TransactionTemplateResp(
        Long id,
        Long userId,
        String name,
        BigDecimal amount,
        Integer type,
        Long categoryId,
        Long paymentMethodId,
        String description,
        Boolean allowAmountEdit,
        Boolean showInQuickPanel,
        Integer sortOrder,
        String icon,
        String color,
        Long ledgerId,
        String createTime,
        String updateTime
) {
}
