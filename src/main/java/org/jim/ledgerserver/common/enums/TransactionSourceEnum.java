package org.jim.ledgerserver.common.enums;

import lombok.Getter;

import java.util.stream.Stream;

/**
 * 交易来源枚举
 * 1 - MANUAL - 手动录入
 * 2 - AI - AI助手创建
 *
 * @author James Smith
 */
@Getter
public enum TransactionSourceEnum {

    MANUAL(1, "手动"),
    AI(2, "AI");

    private final Integer code;
    private final String label;

    TransactionSourceEnum(Integer code, String label) {
        this.code = code;
        this.label = label;
    }

    public static String getSourceDescription(Integer code) {
        if (code == null) {
            return "手动"; // 默认返回手动
        }

        return Stream.of(TransactionSourceEnum.values())
                .filter(source -> source.getCode().equals(code))
                .findFirst()
                .map(TransactionSourceEnum::getLabel)
                .orElse("手动");
    }

    public static TransactionSourceEnum getByCode(Integer code) {
        if (code == null) {
            return MANUAL; // 默认返回手动
        }

        return Stream.of(TransactionSourceEnum.values())
                .filter(source -> source.getCode().equals(code))
                .findFirst()
                .orElse(MANUAL);
    }

    public static void fromCode(Integer code) {
        if (code == null) {
            return; // 允许为空，默认使用 MANUAL
        }

        boolean exists = Stream.of(TransactionSourceEnum.values())
                .anyMatch(source -> source.getCode().equals(code));

        if (!exists) {
            throw new IllegalArgumentException("无效的交易来源代码: " + code);
        }
    }
}
