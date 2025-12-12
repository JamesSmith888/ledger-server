package org.jim.ledgerserver.ledger.vo.budget;

import java.math.BigDecimal;
import java.util.List;

/**
 * 预算设置请求
 * @author James Smith
 */
public record BudgetSettingReq(
    Long ledgerId,
    BigDecimal totalAmount,
    List<CategoryBudgetReq> categoryBudgets
) {}
