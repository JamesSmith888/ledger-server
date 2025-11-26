package org.jim.ledgerserver.ledger.vo.export;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 导出预览响应
 * 
 * @author James Smith
 */
@Data
@Accessors(chain = true)
public class ExportPreviewResp {
    /**
     * 交易记录数量
     */
    private long transactionCount;
    
    /**
     * 分类数量
     */
    private long categoryCount;
    
    /**
     * 支付方式数量
     */
    private long paymentMethodCount;
    
    /**
     * 账本数量
     */
    private long ledgerCount;
    
    /**
     * 预估文件大小
     */
    private String estimatedSize;
}
