package org.jim.ledgerserver.ledger.vo.export;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 导出的账本数据
 * 
 * @author James Smith
 */
@Data
@Accessors(chain = true)
public class ExportLedgerData {
    /**
     * 账本ID
     */
    private Long id;
    
    /**
     * 账本名称
     */
    private String name;
    
    /**
     * 账本描述
     */
    private String description;
    
    /**
     * 账本类型: PERSONAL/SHARED
     */
    private String type;
    
    /**
     * 是否为账本所有者
     */
    private boolean isOwner;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
