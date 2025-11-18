package org.jim.ledgerserver.ledger.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 趋势统计响应数据
 * @author James Smith
 */
public record TrendStatisticsResp(
        /**
         * 趋势数据点列表（时间序列）
         */
        List<TrendDataPoint> dataPoints,

        /**
         * 汇总信息
         */
        Summary summary,

        /**
         * 分组粒度（day/week/month/year）
         */
        String groupBy
) {
    /**
     * 汇总信息
     */
    public record Summary(
            BigDecimal totalIncome,
            BigDecimal totalExpense,
            BigDecimal netBalance,
            Long totalCount,
            BigDecimal avgIncome,
            BigDecimal avgExpense
    ) {}
}
