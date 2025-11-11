package org.jim.ledgerserver.ledger.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 添加成员请求DTO
 * @author James Smith
 */
public record AddMemberRequest(
        @NotNull(message = "用户ID不能为空")
        Long userId,

        @NotNull(message = "成员角色不能为空")
        Integer role,

        @Size(max = 100, message = "备注长度不能超过100个字符")
        String remark
) {
}