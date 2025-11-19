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
}
