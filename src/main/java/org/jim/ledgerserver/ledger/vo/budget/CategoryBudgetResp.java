package org.jim.ledgerserver.ledger.vo.budget;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 分类预算响应
 * @author James Smith
 */
@Data
@Accessors(chain = true)
public class CategoryBudgetResp {
    private Long categoryId;
    private String categoryName;
    private String categoryIcon;
    
    private BigDecimal budgetAmount;
    private BigDecimal expenseAmount;
    private BigDecimal remainingAmount;
    private Integer progress; // 0-100
    private String status; // NORMAL, WARNING, EXCEEDED
}
