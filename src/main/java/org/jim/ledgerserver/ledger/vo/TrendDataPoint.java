package org.jim.ledgerserver.ledger.vo;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 趋势数据点（时间序列数据）
 * @author James Smith
 */
public record TrendDataPoint(
        /**
         * 日期（如 2024-11、2024-11-18）
         */
        String date,

        /**
         * 收入金额
         */
        BigDecimal income,

        /**
         * 支出金额
         */
        BigDecimal expense,

        /**
         * 余额（收入 - 支出）
         */
        BigDecimal balance,

        /**
         * 交易数量
         */
        Long count,

        /**
         * 各分类金额明细（可选，用于堆叠图）
         */
        Map<String, BigDecimal> categories
) {
    /**
     * 构造器（不含分类明细）
     */
    public TrendDataPoint(String date, BigDecimal income, BigDecimal expense, BigDecimal balance, Long count) {
        this(date, income, expense, balance, count, null);
    }
}
