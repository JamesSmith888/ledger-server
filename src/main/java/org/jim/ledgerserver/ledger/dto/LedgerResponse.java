package org.jim.ledgerserver.ledger.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 账本响应DTO
 * @author James Smith
 */
@Data
public class LedgerResponse {
    
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
     * 所有者用户ID
     */
    private Long ownerUserId;
    
    /**
     * 账本类型代码
     */
    private Integer type;
    
    /**
     * 账本类型描述
     */
    private String typeName;
    
    /**
     * 最大成员数限制
     */
    private Integer maxMembers;
    
    /**
     * 当前成员数量
     */
    private Integer memberCount;
    
    /**
     * 是否公开
     */
    private Boolean isPublic;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}