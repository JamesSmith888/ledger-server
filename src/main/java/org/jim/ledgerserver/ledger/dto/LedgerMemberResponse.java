package org.jim.ledgerserver.ledger.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 账本成员响应DTO
 * @author James Smith
 */
@Data
public class LedgerMemberResponse {
    
    /**
     * 成员关系ID
     */
    private Long id;
    
    /**
     * 账本ID
     */
    private Long ledgerId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 用户昵称
     */
    private String nickname;
    
    /**
     * 用户头像URL
     */
    private String avatarUrl;
    
    /**
     * 成员角色代码
     */
    private Integer role;
    
    /**
     * 成员角色名称
     */
    private String roleName;
    
    /**
     * 加入时间
     */
    private LocalDateTime joinedAt;
    
    /**
     * 邀请者用户ID
     */
    private Long invitedByUserId;
    
    /**
     * 成员状态
     */
    private Integer status;
    
    /**
     * 备注
     */
    private String remark;
}