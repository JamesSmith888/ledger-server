package org.jim.ledgerserver.user.enums;

/**
 * 用户偏好类型枚举
 *
 * @author James Smith
 */
public enum PreferenceType {
    /**
     * 分类映射：如"青桔" -> "交通"
     */
    CATEGORY_MAPPING,

    /**
     * 商户别名：如"星巴" -> "星巴克"
     */
    MERCHANT_ALIAS,

    /**
     * 金额模式：如"早餐通常15-30元"
     */
    AMOUNT_PATTERN,

    /**
     * 支付偏好：如"网购用支付宝"
     */
    PAYMENT_PREFERENCE,

    /**
     * 自定义纠正
     */
    CUSTOM_CORRECTION
}
