package org.jim.ledgerserver.feedback.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 提交反馈请求
 *
 * @param type 反馈类型：需求/优化/BUG
 * @param title 反馈标题
 * @param description 反馈详细描述
 * @author James Smith
 */
public record SubmitFeedbackRequest(
        @NotBlank(message = "反馈类型不能为空")
        String type,

        @NotBlank(message = "反馈标题不能为空")
        @Size(max = 200, message = "标题不能超过200个字符")
        String title,

        @Size(max = 5000, message = "描述不能超过5000个字符")
        String description
) {
}
