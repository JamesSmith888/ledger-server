package org.jim.ledgerserver.ledger.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jim.ledgerserver.base.BaseEntity;
import org.jim.ledgerserver.common.enums.LedgerMemberRoleEnum;

import java.time.LocalDateTime;

/**
 * 账本成员关系实体类
 * 管理账本与用户的多对多关系
 * 
 * @author James Smith
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Entity(name = "ledger_member")
@Table(uniqueConstraints = {
    @UniqueConstraint(name = "uk_ledger_user", columnNames = {"ledger_id", "user_id"})
})
public class LedgerMemberEntity extends BaseEntity {

    /**
     * 账本ID
     */
    @Column(name = "ledger_id", nullable = false)
    private Long ledgerId;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 成员角色
     * 1 - OWNER - 所有者
     * 2 - ADMIN - 管理员
     * 3 - EDITOR - 记账员
     * 4 - VIEWER - 查看者
     * @see LedgerMemberRoleEnum
     */
    @Column(name = "role", nullable = false)
    private Integer role;

    /**
     * 加入时间
     */
    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    /**
     * 邀请者用户ID（可选）
     */
    @Column(name = "invited_by_user_id")
    private Long invitedByUserId;

    /**
     * 成员状态
     * 1 - ACTIVE - 正常
     * 2 - INACTIVE - 暂停
     * 3 - PENDING - 待确认（邀请中）
     */
    @Column(name = "status", columnDefinition = "int default 1")
    private Integer status;

    /**
     * 成员备注（可选）
     */
    private String remark;

    /**
     * 获取成员角色枚举
     */
    public LedgerMemberRoleEnum getMemberRole() {
        return LedgerMemberRoleEnum.getByCode(this.role);
    }

    /**
     * 设置成员角色
     */
    public void setMemberRole(LedgerMemberRoleEnum memberRole) {
        this.role = memberRole != null ? memberRole.getCode() : null;
    }

    /**
     * 判断是否为所有者
     */
    public boolean isOwner() {
        return LedgerMemberRoleEnum.OWNER.getCode().equals(this.role);
    }

    /**
     * 判断是否有管理权限
     */
    public boolean hasManagePermission() {
        LedgerMemberRoleEnum memberRole = getMemberRole();
        return memberRole != null && memberRole.hasManagePermission();
    }

    /**
     * 判断是否有编辑权限
     */
    public boolean hasEditPermission() {
        LedgerMemberRoleEnum memberRole = getMemberRole();
        return memberRole != null && memberRole.hasEditPermission();
    }

    /**
     * 判断是否有查看权限
     */
    public boolean hasViewPermission() {
        LedgerMemberRoleEnum memberRole = getMemberRole();
        return memberRole != null && memberRole.hasViewPermission();
    }

    /**
     * 判断是否为活跃状态
     */
    public boolean isActive() {
        return Integer.valueOf(1).equals(this.status);
    }

    /**
     * 判断是否为待确认状态
     */
    public boolean isPending() {
        return Integer.valueOf(3).equals(this.status);
    }

    /**
     * 成员状态枚举
     */
    public enum MemberStatus {
        ACTIVE(1, "正常"),
        INACTIVE(2, "暂停"),
        PENDING(3, "待确认");

        private final Integer code;
        private final String label;

        MemberStatus(Integer code, String label) {
            this.code = code;
            this.label = label;
        }

        public Integer getCode() {
            return code;
        }

        public String getLabel() {
            return label;
        }
    }
}