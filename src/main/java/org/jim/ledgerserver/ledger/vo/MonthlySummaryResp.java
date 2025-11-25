package org.jim.ledgerserver.ledger.vo;

import java.math.BigDecimal;

/**
 * 月度汇总统计响应
 * @author James Smith
 */
public record MonthlySummaryResp(
        /**
         * 总收入
         */
        BigDecimal totalIncome,

        /**
         * 总支出
         */
        BigDecimal totalExpense,

        /**
         * 结余（总收入 - 总支出）
         */
        BigDecimal balance,

        /**
         * 交易总笔数
         */
        int totalCount
) {
}
