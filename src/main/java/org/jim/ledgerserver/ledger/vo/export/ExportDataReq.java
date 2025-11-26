package org.jim.ledgerserver.ledger.vo.export;

/**
 * 导出数据请求
 * 
 * @author James Smith
 */
public record ExportDataReq(
    /**
     * 导出格式: JSON, CSV, EXCEL
     */
    String format,
    
    /**
     * 导出数据类型: ALL, TRANSACTIONS, CATEGORIES, PAYMENT_METHODS, LEDGERS
     */
    String dataType,
    
    /**
     * 指定账本ID（可选，为null则导出所有账本数据）
     */
    Long ledgerId,
    
    /**
     * 开始日期（可选，格式：yyyy-MM-dd）
     */
    String startDate,
    
    /**
     * 结束日期（可选，格式：yyyy-MM-dd）
     */
    String endDate
) {
    // 默认值处理
    public String format() {
        return format != null ? format : "JSON";
    }
    
    public String dataType() {
        return dataType != null ? dataType : "ALL";
    }
}
