package org.jim.ledgerserver.completion.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 添加/更新补全短语请求
 *
 * @author James Smith
 */
@Data
@Accessors(chain = true)
public class CompletionPhraseRequest {
    
    /**
     * 完整短语
     */
    @NotBlank(message = "短语不能为空")
    @Size(min = 2, max = 500, message = "短语长度需在2-500字符之间")
    private String phrase;

    /**
     * 来源类型
     * USER_INPUT - 用户直接输入
     * SUGGESTION_ACCEPTED - 用户采纳的建议
     */
    private String sourceType = "USER_INPUT";

    /**
     * 类别（可选）
     */
    private String category;
}
