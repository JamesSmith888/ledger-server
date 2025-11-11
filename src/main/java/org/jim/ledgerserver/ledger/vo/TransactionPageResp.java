package org.jim.ledgerserver.ledger.vo;

import java.util.List;

/**
 * 交易分页响应
 * @author James Smith
 */
public record TransactionPageResp(
        /**
         * 交易列表
         */
        List<TransactionGetAllResp> content,

        /**
         * 当前页码（从0开始）
         */
        int page,

        /**
         * 每页大小
         */
        int size,

        /**
         * 总记录数
         */
        long totalElements,

        /**
         * 总页数
         */
        int totalPages,

        /**
         * 是否是第一页
         */
        boolean first,

        /**
         * 是否是最后一页
         */
        boolean last,

        /**
         * 是否有下一页
         */
        boolean hasNext,

        /**
         * 是否有上一页
         */
        boolean hasPrevious
) {
}
