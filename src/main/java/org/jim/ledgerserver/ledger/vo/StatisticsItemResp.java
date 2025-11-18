package org.jim.ledgerserver.ledger.vo;

import java.math.BigDecimal;

/**
 * 统计项响应数据（通用统计维度）
 * @author James Smith
 */
public record StatisticsItemResp(
        /**
         * 维度标识（如分类ID、账本ID等）
         */
        String key,

        /**
         * 显示名称（如分类名称、账本名称等）
         */
        String label,

        /**
         * 图标（可选）
         */
        String icon,

        /**
         * 金额
         */
        BigDecimal amount,

        /**
         * 交易数量
         */
        Long count,

        /**
         * 占比（0-100）
         */
        Double percentage,

        /**
         * 环比增长率（可选，如 15.5 表示增长 15.5%）
         */
        Double trend
) {
    /**
     * 构造器（不含趋势）
     */
    public StatisticsItemResp(String key, String label, String icon, BigDecimal amount, Long count, Double percentage) {
        this(key, label, icon, amount, count, percentage, null);
    }
}
