package org.jim.ledgerserver.ledger.vo.agent;

import org.jim.ledgerserver.common.enums.TransactionSourceEnum;
import org.jim.ledgerserver.common.enums.TransactionTypeEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Agent 专用的交易响应对象
 * 包含完整的关联信息，便于 AI 直接展示给用户
 * 
 * @author James Smith
 */
public record AgentTransactionResp(
        // === 基础信息 ===
        Long id,
        String description,
        BigDecimal amount,
        TransactionTypeEnum type,
        LocalDateTime transactionDateTime,
        
        // === 账本信息 ===
        Long ledgerId,
        String ledgerName,
        
        // === 分类信息 ===
        Long categoryId,
        String categoryName,
        String categoryIcon,
        
        // === 支付方式信息 ===
        Long paymentMethodId,
        String paymentMethodName,
        
        // === 创建人信息 ===
        Long createdByUserId,
        String createdByUserName,
        String createdByUserNickname,
        
        // === 附件信息 ===
        Long attachmentCount,
        
        // === 来源信息 ===
        TransactionSourceEnum source
) {
}
