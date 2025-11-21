package org.jim.ledgerserver.feedback.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.jim.ledgerserver.common.JSONResult;
import org.jim.ledgerserver.feedback.dto.FeedbackResponse;
import org.jim.ledgerserver.feedback.dto.SubmitFeedbackRequest;
import org.jim.ledgerserver.feedback.service.FeedbackService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 反馈控制器
 *
 * @author James Smith
 */
@RestController
@RequestMapping("/feedback")
public class FeedbackController {

    @Resource
    private FeedbackService feedbackService;

    /**
     * 提交反馈
     *
     * @param request 反馈请求
     * @return 反馈响应
     */
    @PostMapping("/submit")
    public JSONResult<FeedbackResponse> submitFeedback(@Valid @RequestBody SubmitFeedbackRequest request) {
        FeedbackResponse response = feedbackService.submitFeedback(request);
        return JSONResult.success("反馈提交成功", response);
    }

    /**
     * 获取当前用户的所有反馈
     *
     * @return 反馈列表
     */
    @GetMapping("/list")
    public JSONResult<List<FeedbackResponse>> getUserFeedbacks() {
        List<FeedbackResponse> feedbacks = feedbackService.getUserFeedbacks();
        return JSONResult.success(feedbacks);
    }

    /**
     * 根据类型获取反馈
     *
     * @param type 反馈类型（需求/优化/BUG）
     * @return 反馈列表
     */
    @GetMapping("/list/{type}")
    public JSONResult<List<FeedbackResponse>> getUserFeedbacksByType(@PathVariable String type) {
        List<FeedbackResponse> feedbacks = feedbackService.getUserFeedbacksByType(type);
        return JSONResult.success(feedbacks);
    }

    /**
     * 获取反馈详情
     *
     * @param id 反馈ID
     * @return 反馈详情
     */
    @GetMapping("/{id}")
    public JSONResult<FeedbackResponse> getFeedbackById(@PathVariable Long id) {
        FeedbackResponse feedback = feedbackService.getFeedbackById(id);
        return JSONResult.success(feedback);
    }

    /**
     * 删除反馈
     *
     * @param id 反馈ID
     * @return 成功消息
     */
    @DeleteMapping("/{id}")
    public JSONResult<String> deleteFeedback(@PathVariable Long id) {
        feedbackService.deleteFeedback(id);
        return JSONResult.success("反馈删除成功", null);
    }

    /**
     * 获取所有公开反馈
     *
     * @return 反馈列表
     */
    @GetMapping("/public")
    public JSONResult<List<FeedbackResponse>> getAllPublicFeedbacks() {
        List<FeedbackResponse> feedbacks = feedbackService.getAllPublicFeedbacks();
        return JSONResult.success(feedbacks);
    }

    /**
     * 根据类型获取公开反馈
     *
     * @param type 反馈类型
     * @return 反馈列表
     */
    @GetMapping("/public/type/{type}")
    public JSONResult<List<FeedbackResponse>> getPublicFeedbacksByType(@PathVariable String type) {
        List<FeedbackResponse> feedbacks = feedbackService.getPublicFeedbacksByType(type);
        return JSONResult.success(feedbacks);
    }

    /**
     * 根据状态获取公开反馈
     *
     * @param status 反馈状态
     * @return 反馈列表
     */
    @GetMapping("/public/status/{status}")
    public JSONResult<List<FeedbackResponse>> getPublicFeedbacksByStatus(@PathVariable String status) {
        List<FeedbackResponse> feedbacks = feedbackService.getPublicFeedbacksByStatus(status);
        return JSONResult.success(feedbacks);
    }

    /**
     * 搜索反馈
     *
     * @param keyword 关键词
     * @return 反馈列表
     */
    @GetMapping("/search")
    public JSONResult<List<FeedbackResponse>> searchFeedbacks(@RequestParam String keyword) {
        List<FeedbackResponse> feedbacks = feedbackService.searchFeedbacks(keyword);
        return JSONResult.success(feedbacks);
    }

    /**
     * 获取反馈的评论列表
     *
     * @param feedbackId 反馈ID
     * @return 评论列表
     */
    @GetMapping("/{feedbackId}/comments")
    public JSONResult<List<org.jim.ledgerserver.feedback.dto.FeedbackCommentResponse>> getFeedbackComments(@PathVariable Long feedbackId) {
        List<org.jim.ledgerserver.feedback.dto.FeedbackCommentResponse> comments = feedbackService.getFeedbackComments(feedbackId);
        return JSONResult.success(comments);
    }

    /**
     * 添加评论
     *
     * @param feedbackId 反馈ID
     * @param request 评论请求
     * @return 评论响应
     */
    @PostMapping("/{feedbackId}/comments")
    public JSONResult<org.jim.ledgerserver.feedback.dto.FeedbackCommentResponse> addComment(
            @PathVariable Long feedbackId,
            @Valid @RequestBody org.jim.ledgerserver.feedback.dto.AddCommentRequest request) {
        org.jim.ledgerserver.feedback.dto.FeedbackCommentResponse comment = feedbackService.addComment(feedbackId, request);
        return JSONResult.success("评论添加成功", comment);
    }

    /**
     * 关闭反馈
     *
     * @param id 反馈ID
     * @return 成功消息
     */
    @PutMapping("/{id}/close")
    public JSONResult<String> closeFeedback(@PathVariable Long id) {
        feedbackService.closeFeedback(id);
        return JSONResult.success("反馈已关闭", null);
    }

    /**
     * 重新打开反馈
     *
     * @param id 反馈ID
     * @return 成功消息
     */
    @PutMapping("/{id}/reopen")
    public JSONResult<String> reopenFeedback(@PathVariable Long id) {
        feedbackService.reopenFeedback(id);
        return JSONResult.success("反馈已重新打开", null);
    }

    /**
     * 对反馈进行点赞
     *
     * @param feedbackId 反馈ID
     * @return 成功消息
     */
    @PostMapping("/{feedbackId}/upvote")
    public JSONResult<String> upvoteFeedback(@PathVariable Long feedbackId) {
        feedbackService.addReaction(feedbackId, "feedback", "upvote");
        return JSONResult.success("点赞成功", null);
    }

    /**
     * 对反馈进行倒赞
     *
     * @param feedbackId 反馈ID
     * @return 成功消息
     */
    @PostMapping("/{feedbackId}/downvote")
    public JSONResult<String> downvoteFeedback(@PathVariable Long feedbackId) {
        feedbackService.addReaction(feedbackId, "feedback", "downvote");
        return JSONResult.success("倒赞成功", null);
    }

    /**
     * 取消对反馈的反应
     *
     * @param feedbackId 反馈ID
     * @return 成功消息
     */
    @DeleteMapping("/{feedbackId}/reaction")
    public JSONResult<String> removeFeedbackReaction(@PathVariable Long feedbackId) {
        feedbackService.removeReaction(feedbackId, "feedback");
        return JSONResult.success("取消反应成功", null);
    }

    /**
     * 对评论进行点赞
     *
     * @param feedbackId 反馈ID
     * @param commentId 评论ID
     * @return 成功消息
     */
    @PostMapping("/{feedbackId}/comments/{commentId}/upvote")
    public JSONResult<String> upvoteComment(@PathVariable Long feedbackId, @PathVariable Long commentId) {
        feedbackService.addReaction(commentId, "comment", "upvote");
        return JSONResult.success("点赞成功", null);
    }

    /**
     * 对评论进行倒赞
     *
     * @param feedbackId 反馈ID
     * @param commentId 评论ID
     * @return 成功消息
     */
    @PostMapping("/{feedbackId}/comments/{commentId}/downvote")
    public JSONResult<String> downvoteComment(@PathVariable Long feedbackId, @PathVariable Long commentId) {
        feedbackService.addReaction(commentId, "comment", "downvote");
        return JSONResult.success("倒赞成功", null);
    }

    /**
     * 取消对评论的反应
     *
     * @param feedbackId 反馈ID
     * @param commentId 评论ID
     * @return 成功消息
     */
    @DeleteMapping("/{feedbackId}/comments/{commentId}/reaction")
    public JSONResult<String> removeCommentReaction(@PathVariable Long feedbackId, @PathVariable Long commentId) {
        feedbackService.removeReaction(commentId, "comment");
        return JSONResult.success("取消反应成功", null);
    }
}
