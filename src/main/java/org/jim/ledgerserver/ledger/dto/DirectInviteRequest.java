package org.jim.ledgerserver.ledger.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 直接邀请请求DTO
 * 通过用户ID直接邀请用户加入账本
 * @author James Smith
 */
public record DirectInviteRequest(
        @NotNull(message = "用户ID不能为空")
        Long userId,

        @NotNull(message = "角色不能为空")
        Integer role
) {
}
