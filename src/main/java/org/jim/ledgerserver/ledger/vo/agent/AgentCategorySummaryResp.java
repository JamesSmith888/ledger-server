package org.jim.ledgerserver.ledger.vo.agent;

import java.math.BigDecimal;
import java.util.List;

/**
 * Agent 专用的分类汇总响应
 * 用于明细弹窗展示完整的分类汇总数据
 * 
 * @author James Smith
 */
public record AgentCategorySummaryResp(
        BigDecimal totalAmount,
        Long totalCount,
        List<CategoryItem> categories
) {
    /**
     * 分类汇总项
     */
    public record CategoryItem(
            Long categoryId,
            String categoryName,
            String icon,
            String color,
            BigDecimal amount,
            Long count,
            Double percentage
    ) {
        // 构造函数重载（不含百分比）
        public CategoryItem(Long categoryId, String categoryName, String icon, 
                          String color, BigDecimal amount, Long count) {
            this(categoryId, categoryName, icon, color, amount, count, 0.0);
        }
    }
}
