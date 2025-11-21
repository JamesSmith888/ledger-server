package org.jim.ledgerserver.feedback.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 反馈反应响应（点赞/倒赞）
 *
 * @author James Smith
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackReactionResponse {
    /**
     * 反应ID
     */
    private Long id;

    /**
     * 目标ID（反馈ID或评论ID）
     */
    private Long targetId;

    /**
     * 目标类型：feedback 或 comment
     */
    private String targetType;

    /**
     * 反应用户ID
     */
    private Long userId;

    /**
     * 反应类型：upvote（赞） 或 downvote（踩）
     */
    private String reactionType;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
