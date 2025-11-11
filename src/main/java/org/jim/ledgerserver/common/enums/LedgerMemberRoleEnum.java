package org.jim.ledgerserver.common.enums;

import lombok.Getter;

import java.util.stream.Stream;

/**
 * 账本成员角色枚举
 * 1 - OWNER - 所有者（创建者，拥有最高权限）
 * 2 - ADMIN - 管理员（可以邀请/移除成员，修改账本设置）
 * 3 - EDITOR - 记账员（可以添加/编辑/删除交易记录）
 * 4 - VIEWER - 查看者（只能查看，不能修改）
 *
 * @author James Smith
 */
@Getter
public enum LedgerMemberRoleEnum {

    OWNER(1, "所有者"),
    ADMIN(2, "管理员"),
    EDITOR(3, "记账员"),
    VIEWER(4, "查看者");

    private final Integer code;
    private final String label;

    LedgerMemberRoleEnum(Integer code, String label) {
        this.code = code;
        this.label = label;
    }

    /**
     * 根据代码获取描述
     */
    public static String getRoleDescription(Integer code) {
        if (code == null) {
            return "未知角色";
        }

        return Stream.of(LedgerMemberRoleEnum.values())
                .filter(role -> role.getCode().equals(code))
                .findFirst()
                .map(LedgerMemberRoleEnum::getLabel)
                .orElse("未知角色");
    }

    /**
     * 验证代码是否有效
     */
    public static void validateCode(Integer code) {
        if (code == null) {
            throw new IllegalArgumentException("成员角色代码不能为空");
        }

        boolean exists = Stream.of(LedgerMemberRoleEnum.values())
                .anyMatch(role -> role.getCode().equals(code));

        if (!exists) {
            throw new IllegalArgumentException("无效的成员角色代码: " + code);
        }
    }

    /**
     * 根据代码获取枚举值
     */
    public static LedgerMemberRoleEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }

        return Stream.of(LedgerMemberRoleEnum.values())
                .filter(role -> role.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }

    /**
     * 判断是否有管理权限（所有者或管理员）
     */
    public boolean hasManagePermission() {
        return this == OWNER || this == ADMIN;
    }

    /**
     * 判断是否有编辑权限（所有者、管理员或记账员）
     */
    public boolean hasEditPermission() {
        return this == OWNER || this == ADMIN || this == EDITOR;
    }

    /**
     * 判断是否有查看权限（所有角色都有）
     */
    public boolean hasViewPermission() {
        return true;
    }

    /**
     * 判断是否有删除成员权限（仅所有者和管理员）
     */
    public boolean canRemoveMembers() {
        return this == OWNER || this == ADMIN;
    }

    /**
     * 判断是否有邀请成员权限（仅所有者和管理员）
     */
    public boolean canInviteMembers() {
        return this == OWNER || this == ADMIN;
    }

    /**
     * 判断是否有修改账本设置权限（仅所有者和管理员）
     */
    public boolean canModifyLedgerSettings() {
        return this == OWNER || this == ADMIN;
    }

    /**
     * 比较角色权限级别，返回值：
     * > 0: 当前角色权限更高
     * = 0: 权限相等
     * < 0: 当前角色权限更低
     */
    public int comparePermissionLevel(LedgerMemberRoleEnum other) {
        if (other == null) {
            return 1;
        }
        // 权限级别：OWNER > ADMIN > EDITOR > VIEWER
        // 代码值越小，权限越高
        return other.getCode().compareTo(this.getCode());
    }
}