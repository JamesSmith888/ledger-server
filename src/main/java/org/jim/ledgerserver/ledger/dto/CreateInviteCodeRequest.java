package org.jim.ledgerserver.ledger.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 创建邀请码请求DTO
 * @author James Smith
 */
public record CreateInviteCodeRequest(
        @NotNull(message = "邀请角色不能为空")
        @Min(value = 2, message = "邀请角色最小为2（管理员）")
        @Max(value = 4, message = "邀请角色最大为4（查看者）")
        Integer role,

        @Min(value = -1, message = "最大使用次数最小为-1（无限制）")
        Integer maxUses,

        @Min(value = 1, message = "过期时间最小为1小时")
        @Max(value = 8760, message = "过期时间最大为8760小时（1年）")
        Integer expireHours
) {
}
