package org.jim.ledgerserver.ledger.vo;

import java.math.BigDecimal;

/**
 * 每日统计响应
 * @author James Smith
 */
public record DailyStatisticsResp(
        /**
         * 日期 (YYYY-MM-DD)
         */
        String date,

        /**
         * 当日总收入
         */
        BigDecimal income,

        /**
         * 当日总支出
         */
        BigDecimal expense,

        /**
         * 当日交易笔数
         */
        int count
) {
}
