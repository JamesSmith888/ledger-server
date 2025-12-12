package org.jim.ledgerserver.ledger.vo.budget;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.List;

/**
 * 预算概览响应
 * @author James Smith
 */
@Data
@Accessors(chain = true)
public class BudgetOverviewResp {
    private Long ledgerId;
    private BigDecimal totalBudget;
    private BigDecimal totalExpense;
    private BigDecimal remainingBudget;
    private Integer progress; // 0-100
    private String status; // NORMAL, WARNING, EXCEEDED
    
    private List<CategoryBudgetResp> categoryBudgets;
}
