package org.jim.ledgerserver.completion.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 补全查询响应
 *
 * @author James Smith
 */
@Data
@Accessors(chain = true)
public class CompletionQueryResponse {
    
    /**
     * 查询的前缀
     */
    private String prefix;

    /**
     * 匹配的补全结果列表
     */
    private List<CompletionResult> results;

    /**
     * 是否来自缓存
     */
    private Boolean fromCache;

    /**
     * 查询耗时（毫秒）
     */
    private Long queryTimeMs;

    @Data
    @Accessors(chain = true)
    public static class CompletionResult {
        /**
         * 完整短语
         */
        private String phrase;

        /**
         * 补全部分（phrase 减去 prefix）
         */
        private String completion;

        /**
         * 匹配分数（用于排序，越高越优先）
         */
        private Double score;

        /**
         * 来源类型
         */
        private String sourceType;
    }
}
