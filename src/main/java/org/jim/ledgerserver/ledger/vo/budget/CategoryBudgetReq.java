package org.jim.ledgerserver.ledger.vo.budget;

import java.math.BigDecimal;

/**
 * 分类预算请求
 * @author James Smith
 */
public record CategoryBudgetReq(
    Long categoryId,
    BigDecimal amount
) {}
