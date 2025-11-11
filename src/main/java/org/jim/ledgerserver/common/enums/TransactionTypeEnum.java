package org.jim.ledgerserver.common.enums;

import lombok.Getter;

import java.util.stream.Stream;

/**
 * 交易类型，收入或支出
 * 1 - INCOME - 收入
 * 2 - EXPENSE - 支出
 *
 * @author James Smith
 */
@Getter
public enum TransactionTypeEnum {

    INCOME(1, "收入"),
    EXPENSE(2, "支出");

    private final Integer code;
    private final String label;

    TransactionTypeEnum(Integer code, String label) {
        this.code = code;
        this.label = label;
    }

    public static String getTypeDescription(Integer code) {
        if (code == null) {
            return "未知类型";
        }

        return Stream.of(TransactionTypeEnum.values())
                .filter(type -> type.getCode().equals(code))
                .findFirst()
                .map(TransactionTypeEnum::getLabel)
                .orElse("未知类型");

    }

    public static void fromCode(Integer code) {
        if (code == null) {
            throw new IllegalArgumentException("交易类型代码不能为空");
        }

        boolean exists = Stream.of(TransactionTypeEnum.values())
                .anyMatch(type -> type.getCode().equals(code));

        if (!exists) {
            throw new IllegalArgumentException("无效的交易类型代码: " + code);
        }
    }

    public static TransactionTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }

        return Stream.of(TransactionTypeEnum.values())
                .filter(type -> type.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }

}
