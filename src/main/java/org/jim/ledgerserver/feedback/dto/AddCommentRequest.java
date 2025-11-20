package org.jim.ledgerserver.feedback.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 添加评论请求
 *
 * @author James Smith
 */
public record AddCommentRequest(
        @NotBlank(message = "评论内容不能为空")
        String content
) {
}
