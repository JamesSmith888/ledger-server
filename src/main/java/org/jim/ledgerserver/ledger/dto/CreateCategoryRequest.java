package org.jim.ledgerserver.ledger.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jim.ledgerserver.common.enums.TransactionTypeEnum;

/**
 * 创建分类请求DTO
 * @author James Smith
 */
public record CreateCategoryRequest(
        @NotBlank(message = "分类名称不能为空")
        @Size(max = 50, message = "分类名称长度不能超过50个字符")
        String name,

        @Size(max = 10, message = "分类图标长度不能超过10个字符")
        String icon,

        @Size(max = 10, message = "分类颜色长度不能超过10个字符")
        String color,

        @NotNull(message = "分类类型不能为空")
        TransactionTypeEnum type,

        Integer sortOrder,

        @Size(max = 200, message = "分类描述长度不能超过200个字符")
        String description
) {
}