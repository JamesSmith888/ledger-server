package org.jim.ledgerserver.ledger.vo.export;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 完整导出数据结构
 * 
 * @author James Smith
 */
@Data
@Accessors(chain = true)
public class ExportFullData {
    /**
     * 导出版本（便于后续导入时的兼容性处理）
     */
    private String exportVersion = "1.0";
    
    /**
     * 导出时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime exportTime;
    
    /**
     * 导出的用户ID
     */
    private Long userId;
    
    /**
     * 账本列表
     */
    private List<ExportLedgerData> ledgers;
    
    /**
     * 交易记录列表
     */
    private List<ExportTransactionData> transactions;
    
    /**
     * 分类列表
     */
    private List<ExportCategoryData> categories;
    
    /**
     * 支付方式列表
     */
    private List<ExportPaymentMethodData> paymentMethods;
    
    /**
     * 数据统计
     */
    private ExportStatistics statistics;
    
    @Data
    @Accessors(chain = true)
    public static class ExportStatistics {
        private long ledgerCount;
        private long transactionCount;
        private long categoryCount;
        private long paymentMethodCount;
    }
}
