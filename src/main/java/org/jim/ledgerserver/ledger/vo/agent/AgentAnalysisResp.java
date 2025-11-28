package org.jim.ledgerserver.ledger.vo.agent;

import java.math.BigDecimal;
import java.util.List;

/**
 * Agent 分析响应
 * 统一的分析结果结构，支持多种分析类型
 */
public record AgentAnalysisResp(
        /** 分析类型 */
        String analysisType,
        /** 时间范围 */
        String startTime,
        String endTime,
        
        // ========== 汇总数据（所有类型都返回） ==========
        /** 总收入 */
        BigDecimal totalIncome,
        /** 总支出 */
        BigDecimal totalExpense,
        /** 结余 */
        BigDecimal balance,
        /** 交易笔数 */
        Long transactionCount,
        /** 日均支出 */
        BigDecimal dailyAvgExpense,
        /** 日均收入 */
        BigDecimal dailyAvgIncome,
        
        // ========== 趋势数据（trend类型） ==========
        /** 趋势数据点 */
        List<TrendPoint> trendData,
        /** 分组维度 */
        String groupBy,
        
        // ========== 分类明细（category_breakdown类型） ==========
        /** 分类统计 */
        List<CategoryDetail> categoryBreakdown,
        
        // ========== 对比数据（comparison类型） ==========
        /** 对比分析 */
        ComparisonData comparison,
        
        // ========== 排行数据（ranking类型） ==========
        /** 排行榜 */
        List<RankingItem> ranking
) {
    /**
     * 趋势数据点
     */
    public record TrendPoint(
            /** 时间标签（如 "11-28"、"第48周"、"11月"） */
            String label,
            /** 完整日期 */
            String date,
            /** 收入 */
            BigDecimal income,
            /** 支出 */
            BigDecimal expense,
            /** 结余 */
            BigDecimal balance,
            /** 交易笔数 */
            Long count
    ) {}
    
    /**
     * 分类详情
     */
    public record CategoryDetail(
            Long categoryId,
            String categoryName,
            String categoryIcon,
            /** 交易类型：INCOME/EXPENSE */
            String type,
            /** 金额 */
            BigDecimal amount,
            /** 交易笔数 */
            Long count,
            /** 占比（百分比） */
            Double percentage,
            /** 日均金额 */
            BigDecimal dailyAvg,
            /** 环比变化（百分比，正为增长） */
            Double changeRate
    ) {}
    
    /**
     * 对比数据
     */
    public record ComparisonData(
            /** 当前期间 */
            PeriodSummary current,
            /** 对比期间 */
            PeriodSummary previous,
            /** 收入变化率（百分比） */
            Double incomeChangeRate,
            /** 支出变化率（百分比） */
            Double expenseChangeRate,
            /** 结余变化率（百分比） */
            Double balanceChangeRate,
            /** 分类对比 */
            List<CategoryComparison> categoryComparisons
    ) {}
    
    /**
     * 期间汇总
     */
    public record PeriodSummary(
            String startTime,
            String endTime,
            String label,
            BigDecimal totalIncome,
            BigDecimal totalExpense,
            BigDecimal balance,
            Long transactionCount
    ) {}
    
    /**
     * 分类对比
     */
    public record CategoryComparison(
            Long categoryId,
            String categoryName,
            String categoryIcon,
            String type,
            BigDecimal currentAmount,
            BigDecimal previousAmount,
            Double changeRate
    ) {}
    
    /**
     * 排行项
     */
    public record RankingItem(
            /** 排名 */
            Integer rank,
            /** 分类/描述 */
            String label,
            /** 图标 */
            String icon,
            /** 金额 */
            BigDecimal amount,
            /** 笔数 */
            Long count,
            /** 占比 */
            Double percentage
    ) {}
    
    // ========== 构建器方法 ==========
    
    /**
     * 创建汇总分析响应
     */
    public static AgentAnalysisResp summary(
            String startTime, String endTime,
            BigDecimal totalIncome, BigDecimal totalExpense,
            Long transactionCount, long days,
            List<CategoryDetail> categoryBreakdown
    ) {
        BigDecimal balance = totalIncome.subtract(totalExpense);
        BigDecimal dailyAvgExpense = days > 0 
                ? totalExpense.divide(BigDecimal.valueOf(days), 2, java.math.RoundingMode.HALF_UP) 
                : BigDecimal.ZERO;
        BigDecimal dailyAvgIncome = days > 0 
                ? totalIncome.divide(BigDecimal.valueOf(days), 2, java.math.RoundingMode.HALF_UP) 
                : BigDecimal.ZERO;
        
        return new AgentAnalysisResp(
                "summary", startTime, endTime,
                totalIncome, totalExpense, balance, transactionCount,
                dailyAvgExpense, dailyAvgIncome,
                null, null, categoryBreakdown, null, null
        );
    }
    
    /**
     * 创建趋势分析响应
     */
    public static AgentAnalysisResp trend(
            String startTime, String endTime, String groupBy,
            BigDecimal totalIncome, BigDecimal totalExpense,
            Long transactionCount, long days,
            List<TrendPoint> trendData
    ) {
        BigDecimal balance = totalIncome.subtract(totalExpense);
        BigDecimal dailyAvgExpense = days > 0 
                ? totalExpense.divide(BigDecimal.valueOf(days), 2, java.math.RoundingMode.HALF_UP) 
                : BigDecimal.ZERO;
        BigDecimal dailyAvgIncome = days > 0 
                ? totalIncome.divide(BigDecimal.valueOf(days), 2, java.math.RoundingMode.HALF_UP) 
                : BigDecimal.ZERO;
        
        return new AgentAnalysisResp(
                "trend", startTime, endTime,
                totalIncome, totalExpense, balance, transactionCount,
                dailyAvgExpense, dailyAvgIncome,
                trendData, groupBy, null, null, null
        );
    }
    
    /**
     * 创建对比分析响应
     */
    public static AgentAnalysisResp comparison(
            String startTime, String endTime,
            BigDecimal totalIncome, BigDecimal totalExpense,
            Long transactionCount, long days,
            ComparisonData comparison
    ) {
        BigDecimal balance = totalIncome.subtract(totalExpense);
        BigDecimal dailyAvgExpense = days > 0 
                ? totalExpense.divide(BigDecimal.valueOf(days), 2, java.math.RoundingMode.HALF_UP) 
                : BigDecimal.ZERO;
        BigDecimal dailyAvgIncome = days > 0 
                ? totalIncome.divide(BigDecimal.valueOf(days), 2, java.math.RoundingMode.HALF_UP) 
                : BigDecimal.ZERO;
        
        return new AgentAnalysisResp(
                "comparison", startTime, endTime,
                totalIncome, totalExpense, balance, transactionCount,
                dailyAvgExpense, dailyAvgIncome,
                null, null, null, comparison, null
        );
    }
    
    /**
     * 创建排行分析响应
     */
    public static AgentAnalysisResp ranking(
            String startTime, String endTime,
            BigDecimal totalIncome, BigDecimal totalExpense,
            Long transactionCount, long days,
            List<RankingItem> ranking
    ) {
        BigDecimal balance = totalIncome.subtract(totalExpense);
        BigDecimal dailyAvgExpense = days > 0 
                ? totalExpense.divide(BigDecimal.valueOf(days), 2, java.math.RoundingMode.HALF_UP) 
                : BigDecimal.ZERO;
        BigDecimal dailyAvgIncome = days > 0 
                ? totalIncome.divide(BigDecimal.valueOf(days), 2, java.math.RoundingMode.HALF_UP) 
                : BigDecimal.ZERO;
        
        return new AgentAnalysisResp(
                "ranking", startTime, endTime,
                totalIncome, totalExpense, balance, transactionCount,
                dailyAvgExpense, dailyAvgIncome,
                null, null, null, null, ranking
        );
    }
}
