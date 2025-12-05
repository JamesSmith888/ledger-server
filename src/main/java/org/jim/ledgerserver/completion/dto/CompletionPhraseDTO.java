package org.jim.ledgerserver.completion.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 补全短语 DTO
 *
 * @author James Smith
 */
@Data
@Accessors(chain = true)
public class CompletionPhraseDTO {
    
    /**
     * ID
     */
    private Long id;

    /**
     * 完整短语
     */
    private String phrase;

    /**
     * 使用频率
     */
    private Integer frequency;

    /**
     * 最后使用时间戳
     */
    private Long lastUsedAt;

    /**
     * 来源类型
     */
    private String sourceType;

    /**
     * 类别
     */
    private String category;
}
