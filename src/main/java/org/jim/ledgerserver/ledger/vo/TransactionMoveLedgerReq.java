package org.jim.ledgerserver.ledger.vo;

/**
 * 请求对象：移动交易到其他账本
 */
public record TransactionMoveLedgerReq(Long targetLedgerId) {
}
