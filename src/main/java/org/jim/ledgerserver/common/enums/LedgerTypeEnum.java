package org.jim.ledgerserver.common.enums;

import lombok.Getter;

import java.util.stream.Stream;

/**
 * 账本类型枚举
 * 1 - PERSONAL - 个人账本
 * 2 - SHARED - 共享账本（家庭/团队）
 * 3 - BUSINESS - 企业账本（预留）
 *
 * @author James Smith
 */
@Getter
public enum LedgerTypeEnum {

    PERSONAL(1, "个人账本"),
    SHARED(2, "共享账本"),
    BUSINESS(3, "企业账本");

    private final Integer code;
    private final String label;

    LedgerTypeEnum(Integer code, String label) {
        this.code = code;
        this.label = label;
    }

    /**
     * 根据代码获取描述
     */
    public static String getTypeDescription(Integer code) {
        if (code == null) {
            return "未知类型";
        }

        return Stream.of(LedgerTypeEnum.values())
                .filter(type -> type.getCode().equals(code))
                .findFirst()
                .map(LedgerTypeEnum::getLabel)
                .orElse("未知类型");
    }

    /**
     * 验证代码是否有效
     */
    public static void validateCode(Integer code) {
        if (code == null) {
            throw new IllegalArgumentException("账本类型代码不能为空");
        }

        boolean exists = Stream.of(LedgerTypeEnum.values())
                .anyMatch(type -> type.getCode().equals(code));

        if (!exists) {
            throw new IllegalArgumentException("无效的账本类型代码: " + code);
        }
    }

    /**
     * 根据代码获取枚举值
     */
    public static LedgerTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }

        return Stream.of(LedgerTypeEnum.values())
                .filter(type -> type.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }

    /**
     * 判断是否为共享账本
     */
    public boolean isShared() {
        return this == SHARED || this == BUSINESS;
    }

    /**
     * 判断是否为个人账本
     */
    public boolean isPersonal() {
        return this == PERSONAL;
    }
}