package org.jim.ledgerserver.ledger.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建账本请求DTO
 * @author James Smith
 */
public record CreateLedgerRequest(
        @NotBlank(message = "账本名称不能为空")
        @Size(max = 50, message = "账本名称长度不能超过50个字符")
        String name,

        @Size(max = 200, message = "账本描述长度不能超过200个字符")
        String description,

        Integer maxMembers,

        Boolean isPublic
) {
}