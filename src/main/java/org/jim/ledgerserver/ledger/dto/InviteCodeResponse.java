package org.jim.ledgerserver.ledger.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 邀请码响应DTO
 * @author James Smith
 */
@Data
public class InviteCodeResponse {
    /**
     * 邀请码ID
     */
    private Long id;

    /**
     * 邀请码
     */
    private String code;

    /**
     * 账本ID
     */
    private Long ledgerId;

    /**
     * 账本名称
     */
    private String ledgerName;

    /**
     * 创建者用户ID
     */
    private Long createdByUserId;

    /**
     * 创建者用户名
     */
    private String createdByUserName;

    /**
     * 邀请角色代码
     */
    private Integer role;

    /**
     * 邀请角色名称
     */
    private String roleName;

    /**
     * 最大使用次数
     */
    private Integer maxUses;

    /**
     * 已使用次数
     */
    private Integer usedCount;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 是否已过期
     */
    private Boolean isExpired;

    /**
     * 是否已达上限
     */
    private Boolean isExhausted;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 完整邀请链接（可选）
     */
    private String inviteUrl;
}
