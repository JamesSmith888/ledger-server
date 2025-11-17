package org.jim.ledgerserver.user.dto;

/**
 * 更新用户默认账本请求
 * @author James Smith
 */
public record UpdateDefaultLedgerRequest(
        Long ledgerId
) {
}
