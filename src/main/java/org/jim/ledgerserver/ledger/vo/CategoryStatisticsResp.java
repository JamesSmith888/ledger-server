package org.jim.ledgerserver.ledger.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 按分类统计响应数据
 * @author James Smith
 */
public record CategoryStatisticsResp(
        /**
         * 统计项列表
         */
        List<StatisticsItemResp> items,

        /**
         * 总金额
         */
        BigDecimal totalAmount,

        /**
         * 总交易数
         */
        Long totalCount,

        /**
         * 查询时间范围
         */
        TimeRange timeRange
) {
    /**
     * 时间范围信息
     */
    public record TimeRange(
            String startTime,
            String endTime
    ) {}
}
