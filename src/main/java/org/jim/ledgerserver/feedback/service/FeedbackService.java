package org.jim.ledgerserver.feedback.service;

import jakarta.annotation.Resource;
import org.jim.ledgerserver.common.exception.BusinessException;
import org.jim.ledgerserver.common.util.UserContext;
import org.jim.ledgerserver.feedback.dto.FeedbackResponse;
import org.jim.ledgerserver.feedback.dto.SubmitFeedbackRequest;
import org.jim.ledgerserver.feedback.entity.FeedbackEntity;
import org.jim.ledgerserver.feedback.repository.FeedbackRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 反馈服务
 *
 * @author James Smith
 */
@Service
public class FeedbackService {

    @Resource
    private FeedbackRepository feedbackRepository;

    /**
     * 提交反馈
     *
     * @param request 反馈请求
     * @return 反馈响应
     */
    public FeedbackResponse submitFeedback(SubmitFeedbackRequest request) {
        Long userId = UserContext.getCurrentUserId();

        // 验证反馈类型
        if (!isValidType(request.type())) {
            throw new BusinessException("反馈类型无效，请选择：需求、优化、BUG");
        }

        // 创建反馈
        FeedbackEntity feedback = new FeedbackEntity();
        feedback.setUserId(userId);
        feedback.setType(request.type());
        feedback.setTitle(request.title());
        feedback.setDescription(request.description());
        feedback.setStatus("待处理");

        FeedbackEntity savedFeedback = feedbackRepository.save(feedback);
        return toResponse(savedFeedback);
    }

    /**
     * 获取当前用户的所有反馈
     *
     * @return 反馈列表
     */
    public List<FeedbackResponse> getUserFeedbacks() {
        Long userId = UserContext.getCurrentUserId();
        List<FeedbackEntity> feedbacks = feedbackRepository.findByUserId(userId);
        return feedbacks.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 根据类型获取用户反馈
     *
     * @param type 反馈类型
     * @return 反馈列表
     */
    public List<FeedbackResponse> getUserFeedbacksByType(String type) {
        Long userId = UserContext.getCurrentUserId();
        
        if (!isValidType(type)) {
            throw new BusinessException("反馈类型无效");
        }
        
        List<FeedbackEntity> feedbacks = feedbackRepository.findByUserIdAndType(userId, type);
        return feedbacks.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取反馈详情
     *
     * @param id 反馈ID
     * @return 反馈响应
     */
    public FeedbackResponse getFeedbackById(Long id) {
        FeedbackEntity feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new BusinessException("反馈不存在"));

        // 验证是否是当前用户的反馈
        Long userId = UserContext.getCurrentUserId();
        if (!feedback.getUserId().equals(userId)) {
            throw new BusinessException("无权查看该反馈");
        }

        return toResponse(feedback);
    }

    /**
     * 删除反馈（逻辑删除）
     *
     * @param id 反馈ID
     */
    public void deleteFeedback(Long id) {
        FeedbackEntity feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new BusinessException("反馈不存在"));

        // 验证是否是当前用户的反馈
        Long userId = UserContext.getCurrentUserId();
        if (!feedback.getUserId().equals(userId)) {
            throw new BusinessException("无权删除该反馈");
        }

        // 逻辑删除
        feedback.setDeleteTime(LocalDateTime.now());
        feedbackRepository.save(feedback);
    }

    /**
     * 验证反馈类型是否有效
     */
    private boolean isValidType(String type) {
        return "需求".equals(type) || "优化".equals(type) || "BUG".equals(type);
    }

    /**
     * 将实体转换为响应对象
     */
    private FeedbackResponse toResponse(FeedbackEntity entity) {
        FeedbackResponse response = new FeedbackResponse();
        response.setId(entity.getId());
        response.setUserId(entity.getUserId());
        response.setType(entity.getType());
        response.setTitle(entity.getTitle());
        response.setDescription(entity.getDescription());
        response.setStatus(entity.getStatus());
        response.setAdminReply(entity.getAdminReply());
        response.setCreateTime(entity.getCreateTime());
        response.setUpdateTime(entity.getUpdateTime());
        return response;
    }
}
