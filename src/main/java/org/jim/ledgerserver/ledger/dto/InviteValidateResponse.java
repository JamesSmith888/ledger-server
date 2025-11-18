package org.jim.ledgerserver.ledger.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 邀请码验证响应DTO
 * 用于在用户接受邀请前展示邀请信息
 * @author James Smith
 */
@Data
public class InviteValidateResponse {
    /**
     * 邀请码是否有效
     */
    private Boolean isValid;

    /**
     * 验证失败原因（如果失败）
     */
    private String errorMessage;

    /**
     * 账本ID
     */
    private Long ledgerId;

    /**
     * 账本名称
     */
    private String ledgerName;

    /**
     * 账本描述
     */
    private String ledgerDescription;

    /**
     * 邀请者用户名
     */
    private String inviterName;

    /**
     * 邀请角色代码
     */
    private Integer role;

    /**
     * 邀请角色名称
     */
    private String roleName;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 当前成员数
     */
    private Integer memberCount;

    /**
     * 最大成员数
     */
    private Integer maxMembers;
}
