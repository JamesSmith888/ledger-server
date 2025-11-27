package org.jim.ledgerserver.ledger.vo.agent;

/**
 * Agent 专用的分类信息响应
 * 包含分类的完整信息
 * 
 * @author James Smith
 */
public record AgentCategoryResp(
        Long id,
        String name,
        String icon,
        Integer type,
        String typeName,
        Long ledgerId,
        String ledgerName,
        Long parentId,
        String parentName,
        Integer sortOrder,
        Long transactionCount
) {
}
