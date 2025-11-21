package org.jim.ledgerserver.ledger.vo;

import java.math.BigDecimal;

/**
 * 交易模板创建/更新请求
 * @author James Smith
 */
public record TransactionTemplateReq(
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
        Long ledgerId
) {
}
