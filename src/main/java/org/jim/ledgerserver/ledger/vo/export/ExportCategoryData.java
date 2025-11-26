package org.jim.ledgerserver.ledger.vo.export;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 导出的分类数据
 * 
 * @author James Smith
 */
@Data
@Accessors(chain = true)
public class ExportCategoryData {
    /**
     * 分类ID
     */
    private Long id;
    
    /**
     * 分类名称
     */
    private String name;
    
    /**
     * 分类图标
     */
    private String icon;
    
    /**
     * 分类颜色
     */
    private String color;
    
    /**
     * 分类类型: INCOME/EXPENSE
     */
    private String type;
    
    /**
     * 排序顺序
     */
    private Integer sortOrder;
    
    /**
     * 是否为系统分类
     */
    private boolean isSystem;
    
    /**
     * 分类描述
     */
    private String description;
    
    /**
     * 是否常用
     */
    private boolean isFrequent;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
