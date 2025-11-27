package org.jim.ledgerserver.ledger.vo.agent;

import java.math.BigDecimal;
import java.util.List;

/**
 * Agent 专用的统计报表响应
 * 
 * @author James Smith
 */
public record AgentStatisticsResp(
        // 汇总数据
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal balance,
        Long transactionCount,
        
        // 按分类统计
        List<CategoryStat> categoryStats,
        
        // 时间范围
        String startTime,
        String endTime
) {
    /**
     * 分类统计项
     */
    public record CategoryStat(
            Long categoryId,
            String categoryName,
            String categoryIcon,
            BigDecimal amount,
            Long count,
            Double percentage
    ) {}
}
