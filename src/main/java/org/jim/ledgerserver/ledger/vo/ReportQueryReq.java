package org.jim.ledgerserver.ledger.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 报表查询请求参数
 * @author James Smith
 */
public record ReportQueryReq(
        /**
         * 账本ID（可选，为空则查询所有账本）
         */
        Long ledgerId,

        /**
         * 交易类型（可选，1=收入 2=支出，为空则查询全部）
         */
        Integer type,

        /**
         * 分类ID列表（可选，为空则查询所有分类）
         */
        List<Long> categoryIds,

        /**
         * 开始时间（必填）
         */
        LocalDateTime startTime,

        /**
         * 结束时间（必填）
         */
        LocalDateTime endTime,

        /**
         * 时间分组粒度（day/week/month/year，默认 month）
         */
        String groupBy,

        /**
         * 分析维度（category/ledger/paymentMethod/creator，默认 category）
         */
        String dimension
) {
    /**
     * 默认构造器，提供默认值
     */
    public ReportQueryReq {
        if (groupBy == null || groupBy.isBlank()) {
            groupBy = "month";
        }
        if (dimension == null || dimension.isBlank()) {
            dimension = "category";
        }
    }
}
