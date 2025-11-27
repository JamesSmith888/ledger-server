package org.jim.ledgerserver.ledger.vo;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.jim.ledgerserver.common.util.FlexibleLocalDateTimeDeserializer;

import java.time.LocalDateTime;

/**
 * 交易查询请求参数
 * @author James Smith
 */
public record TransactionQueryReq(
        /**
         * 账本ID（可选，null表示查询所有账本）
         */
        Long ledgerId,

        /**
         * 交易类型（可选，1-收入，2-支出，null表示查询所有类型）
         */
        Integer type,

        /**
         * 分类ID（可选）
         */
        Long categoryId,

        /**
         * 开始时间（可选）
         */
        @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
        LocalDateTime startTime,

        /**
         * 结束时间（可选）
         */
        @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
        LocalDateTime endTime,

        /**
         * 页码（从0开始，默认0）
         */
        Integer page,

        /**
         * 每页大小（默认20，最大100）
         */
        Integer size,

        /**
         * 排序字段（默认transactionDateTime）
         * 可选值：transactionDateTime, amount, createTime
         */
        String sortBy,

        /**
         * 排序方向（默认DESC降序）
         * 可选值：ASC, DESC
         */
        String sortDirection,

        /**
         * 搜索关键词（可选）
         * 模糊匹配交易名称、描述
         */
        String keyword
) {
    public TransactionQueryReq {
        // 设置默认值
        if (page == null || page < 0) {
            page = 0;
        }
        if (size == null || size <= 0) {
            size = 20;
        }
        if (size > 100) {
            size = 100; // 限制最大页面大小
        }
        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "transactionDateTime";
        }
        if (sortDirection == null || sortDirection.isBlank()) {
            sortDirection = "DESC";
        }
    }

    /**
     * 获取有效的页码
     */
    public int getPage() {
        return page;
    }

    /**
     * 获取有效的每页大小
     */
    public int getSize() {
        return size;
    }

    /**
     * 获取有效的排序字段
     */
    public String getSortBy() {
        return sortBy;
    }

    /**
     * 获取有效的排序方向
     */
    public String getSortDirection() {
        return sortDirection;
    }
}
