package org.jim.ledgerserver.ledger.dto;

import org.jim.ledgerserver.common.enums.TransactionTypeEnum;

/**
 * 分类响应DTO
 * @author James Smith
 */
public record CategoryResponse(
        Long id,
        String name,
        String icon,
        String color,
        TransactionTypeEnum type,
        Integer sortOrder,
        Boolean isSystem,
        String description
) {
}