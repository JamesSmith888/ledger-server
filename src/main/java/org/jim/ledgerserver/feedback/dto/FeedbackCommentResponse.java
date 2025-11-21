package org.jim.ledgerserver.feedback.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 反馈评论响应
 *
 * @author James Smith
 */
@Data
public class FeedbackCommentResponse {

    /**
     * 评论ID
     */
    private Long id;

    /**
     * 反馈ID
     */
    private Long feedbackId;

    /**
     * 评论用户ID
     */
    private Long userId;

    /**
     * 评论用户名
     */
    private String userName;

    /**
     * 评论用户昵称
     */
    private String userNickname;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 点赞数
     */
    private Long upvoteCount = 0L;

    /**
     * 倒赞数
     */
    private Long downvoteCount = 0L;

    /**
     * 当前用户的反应类型：upvote/downvote/null
     */
    private String userReaction;
}
