package org.jim.ledgerserver.ledger.vo.agent;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.jim.ledgerserver.common.util.FlexibleLocalDateTimeDeserializer;

import java.time.LocalDateTime;

/**
 * Agent 专用的查询交易请求
 * 支持多种筛选条件，便于 AI 灵活查询
 * 
 * @author James Smith
 */
public record AgentQueryTransactionReq(
        // 账本ID（必填）
        Long ledgerId,
        
        // 交易类型：INCOME/EXPENSE（可选）
        String type,
        
        // 分类ID（可选）
        Long categoryId,
        
        // 时间范围（可选）
        @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
        LocalDateTime startTime,
        @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
        LocalDateTime endTime,
        
        // 关键词搜索：匹配名称或描述（可选）
        String keyword,
        
        // 金额范围（可选）
        java.math.BigDecimal minAmount,
        java.math.BigDecimal maxAmount,
        
        // 分页参数
        Integer page,
        Integer size,
        
        // 排序：transactionDateTime（默认）, amount
        String sortBy,
        // 排序方向：DESC（默认）, ASC
        String sortDirection
) {
    // 提供默认值
    public Integer page() { return page != null ? page : 0; }
    public Integer size() { return size != null ? size : 20; }
    public String sortBy() { return sortBy != null ? sortBy : "transactionDateTime"; }
    public String sortDirection() { return sortDirection != null ? sortDirection : "DESC"; }
}
