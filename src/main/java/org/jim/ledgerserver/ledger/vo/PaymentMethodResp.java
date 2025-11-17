package org.jim.ledgerserver.ledger.vo;

/**
 * 支付方式响应
 * @author James Smith
 */
public record PaymentMethodResp(
        Long id,
        String name,
        String icon,
        String type,
        Boolean isDefault,
        Integer sortOrder
) {
}
