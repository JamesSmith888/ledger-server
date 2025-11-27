package org.jim.ledgerserver.ledger.vo.agent;

import org.jim.ledgerserver.common.enums.TransactionTypeEnum;

/**
 * Agent 专用的创建分类请求
 * 
 * @author James Smith
 */
public record AgentCreateCategoryReq(
        String name,
        String icon,
        TransactionTypeEnum type,
        Long ledgerId,
        Long parentId
) {
}
