package org.jim.ledgerserver.ledger.vo.export;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 导出的支付方式数据
 * 
 * @author James Smith
 */
@Data
@Accessors(chain = true)
public class ExportPaymentMethodData {
    /**
     * 支付方式ID
     */
    private Long id;
    
    /**
     * 支付方式名称
     */
    private String name;
    
    /**
     * 支付方式图标
     */
    private String icon;
    
    /**
     * 支付方式类型: CASH, ALIPAY, WECHAT, BANK_CARD, OTHER
     */
    private String type;
    
    /**
     * 是否默认
     */
    private boolean isDefault;
    
    /**
     * 排序顺序
     */
    private Integer sortOrder;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
