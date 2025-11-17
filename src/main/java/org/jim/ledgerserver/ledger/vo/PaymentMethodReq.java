package org.jim.ledgerserver.ledger.vo;

/**
 * 支付方式创建/更新请求
 * @author James Smith
 */
public record PaymentMethodReq(
        String name,
        String icon,
        String type,
        Boolean isDefault,
        Integer sortOrder
) {
}
