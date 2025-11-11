package org.jim.ledgerserver.ledger.dto;

import jakarta.validation.constraints.Size;

/**
 * 更新账本请求DTO
 * @author James Smith
 */
public record UpdateLedgerRequest(
        @Size(max = 50, message = "账本名称长度不能超过50个字符")
        String name,

        @Size(max = 200, message = "账本描述长度不能超过200个字符")
        String description,

        Integer maxMembers,

        Boolean isPublic
) {
}