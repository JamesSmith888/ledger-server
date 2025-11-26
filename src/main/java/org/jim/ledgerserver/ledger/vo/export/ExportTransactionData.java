package org.jim.ledgerserver.ledger.vo.export;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 导出的交易数据
 * 
 * @author James Smith
 */
@Data
@Accessors(chain = true)
public class ExportTransactionData {
    /**
     * 交易ID
     */
    private Long id;
    
    /**
     * 交易名称
     */
    private String name;
    
    /**
     * 交易描述
     */
    private String description;
    
    /**
     * 交易金额
     */
    private BigDecimal amount;
    
    /**
     * 交易类型: INCOME/EXPENSE
     */
    private String type;
    
    /**
     * 交易日期时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime transactionDateTime;
    
    /**
     * 账本名称
     */
    private String ledgerName;
    
    /**
     * 分类名称
     */
    private String categoryName;
    
    /**
     * 分类图标
     */
    private String categoryIcon;
    
    /**
     * 支付方式名称
     */
    private String paymentMethodName;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
