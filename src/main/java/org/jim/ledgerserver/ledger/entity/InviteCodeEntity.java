package org.jim.ledgerserver.ledger.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jim.ledgerserver.base.BaseEntity;
import org.jim.ledgerserver.common.enums.LedgerMemberRoleEnum;

import java.time.LocalDateTime;

/**
 * 账本邀请码实体类
 * 用于生成和管理账本邀请链接
 * 
 * @author James Smith
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Entity(name = "ledger_invite_code")
public class InviteCodeEntity extends BaseEntity {

    /**
     * 邀请码（唯一）
     */
    @Column(name = "code", nullable = false, unique = true, length = 64)
    private String code;

    /**
     * 账本ID
     */
    @Column(name = "ledger_id", nullable = false)
    private Long ledgerId;

    /**
     * 创建者用户ID
     */
    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    /**
     * 邀请角色（存储角色代码）
     * 2 - ADMIN - 管理员
     * 3 - EDITOR - 记账员
     * 4 - VIEWER - 查看者
     * 注意：不允许通过邀请码直接成为所有者
     * @see LedgerMemberRoleEnum
     */
    @Column(name = "role", nullable = false)
    private Integer role;

    /**
     * 最大使用次数
     * -1 表示无限制
     */
    @Column(name = "max_uses", columnDefinition = "int default 1")
    private Integer maxUses;

    /**
     * 已使用次数
     */
    @Column(name = "used_count", columnDefinition = "int default 0")
    private Integer usedCount;

    /**
     * 过期时间
     * NULL 表示永不过期
     */
    @Column(name = "expire_time")
    private LocalDateTime expireTime;

    /**
     * 状态
     * 1 - 有效
     * 0 - 禁用
     */
    @Column(name = "status", columnDefinition = "tinyint default 1")
    private Integer status;

    /**
     * 获取邀请角色枚举
     */
    public LedgerMemberRoleEnum getInviteRole() {
        return LedgerMemberRoleEnum.getByCode(this.role);
    }

    /**
     * 设置邀请角色
     */
    public void setInviteRole(LedgerMemberRoleEnum roleEnum) {
        this.role = roleEnum != null ? roleEnum.getCode() : null;
    }

    /**
     * 判断是否有效
     */
    public boolean isValid() {
        return Integer.valueOf(1).equals(this.status);
    }

    /**
     * 判断是否已过期
     */
    public boolean isExpired() {
        return expireTime != null && LocalDateTime.now().isAfter(expireTime);
    }

    /**
     * 判断是否已达到使用次数上限
     */
    public boolean isExhausted() {
        if (maxUses == null || maxUses == -1) {
            return false; // 无限制
        }
        return usedCount != null && usedCount >= maxUses;
    }

    /**
     * 判断是否可以使用
     * 综合判断：有效、未过期、未达到使用次数上限
     */
    public boolean canBeUsed() {
        return isValid() && !isExpired() && !isExhausted();
    }

    /**
     * 增加使用次数
     */
    public void incrementUsedCount() {
        this.usedCount = (this.usedCount == null ? 0 : this.usedCount) + 1;
    }

    /**
     * 禁用邀请码
     */
    public void disable() {
        this.status = 0;
    }

    /**
     * 启用邀请码
     */
    public void enable() {
        this.status = 1;
    }

    /**
     * 邀请码状态枚举
     */
    public enum InviteCodeStatus {
        DISABLED(0, "禁用"),
        ENABLED(1, "有效");

        private final Integer code;
        private final String label;

        InviteCodeStatus(Integer code, String label) {
            this.code = code;
            this.label = label;
        }

        public Integer getCode() {
            return code;
        }

        public String getLabel() {
            return label;
        }

        public static InviteCodeStatus getByCode(Integer code) {
            if (code == null) return null;
            for (InviteCodeStatus status : values()) {
                if (status.code.equals(code)) {
                    return status;
                }
            }
            return null;
        }
    }
}
