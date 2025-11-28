package org.jim.ledgerserver.ledger.vo.agent;

import java.util.List;

/**
 * Agent 分析请求
 * 
 * @param ledgerId 账本ID（可选，不传则查询用户所有数据）
 * @param startTime 开始时间 YYYY-MM-DD 或 ISO格式
 * @param endTime 结束时间 YYYY-MM-DD 或 ISO格式
 * @param analysisType 分析类型：summary/trend/category_breakdown/comparison
 * @param groupBy 分组维度：day/week/month/category/payment_method（trend时使用）
 * @param type 交易类型过滤：INCOME/EXPENSE（可选）
 * @param compareStartTime 对比开始时间（comparison时使用）
 * @param compareEndTime 对比结束时间（comparison时使用）
 * @param categoryIds 分类ID过滤（可选）
 * @param topN 返回前N个结果（用于排行）
 */
public record AgentAnalysisReq(
        Long ledgerId,
        String startTime,
        String endTime,
        String analysisType,
        String groupBy,
        String type,
        String compareStartTime,
        String compareEndTime,
        List<Long> categoryIds,
        Integer topN
) {
    /**
     * 分析类型枚举
     */
    public enum AnalysisType {
        /** 汇总统计 - 收支总额、分类占比 */
        SUMMARY,
        /** 趋势分析 - 按时间维度的变化趋势 */
        TREND,
        /** 分类明细 - 各分类的详细统计 */
        CATEGORY_BREAKDOWN,
        /** 对比分析 - 两个时期的对比 */
        COMPARISON,
        /** 排行榜 - 消费/收入排行 */
        RANKING
    }
    
    /**
     * 分组维度枚举
     */
    public enum GroupBy {
        DAY, WEEK, MONTH, CATEGORY, PAYMENT_METHOD
    }
}
