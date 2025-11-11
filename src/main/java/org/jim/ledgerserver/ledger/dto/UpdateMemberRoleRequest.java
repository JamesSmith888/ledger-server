package org.jim.ledgerserver.ledger.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 更新成员角色请求DTO
 * @author James Smith
 */
public record UpdateMemberRoleRequest(
        @NotNull(message = "成员角色不能为空")
        Integer role
) {
}