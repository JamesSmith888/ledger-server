package org.jim.ledgerserver.ledger.vo.agent;

import java.math.BigDecimal;
import java.util.List;

/**
 * Agent 专用的交易列表响应
 * 包含分页信息和汇总统计
 * 
 * @author James Smith
 */
public record AgentTransactionListResp(
        // 交易列表（包含完整关联信息）
        List<AgentTransactionResp> transactions,
        
        // 分页信息
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean isFirst,
        boolean isLast,
        
        // 当前查询结果的汇总统计
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal balance,
        int transactionCount
) {
}
