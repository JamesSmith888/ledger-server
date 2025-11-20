package org.jim.ledgerserver.feedback.service;

import jakarta.annotation.Resource;
import org.jim.ledgerserver.common.exception.BusinessException;
import org.jim.ledgerserver.common.util.UserContext;
import org.jim.ledgerserver.feedback.dto.AddCommentRequest;
import org.jim.ledgerserver.feedback.dto.FeedbackCommentResponse;
import org.jim.ledgerserver.feedback.dto.FeedbackResponse;
import org.jim.ledgerserver.feedback.dto.SubmitFeedbackRequest;
import org.jim.ledgerserver.feedback.entity.FeedbackCommentEntity;
import org.jim.ledgerserver.feedback.entity.FeedbackEntity;
import org.jim.ledgerserver.feedback.repository.FeedbackCommentRepository;
import org.jim.ledgerserver.feedback.repository.FeedbackRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Resource
    private FeedbackCommentRepository feedbackCommentRepository;

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
     * 获取所有公开反馈
     *
     * @return 反馈列表
     */
    public List<FeedbackResponse> getAllPublicFeedbacks() {
        List<FeedbackEntity> feedbacks = feedbackRepository.findAllPublic();
        return feedbacks.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 根据类型获取公开反馈
     *
     * @param type 反馈类型
     * @return 反馈列表
     */
    public List<FeedbackResponse> getPublicFeedbacksByType(String type) {
        if (!isValidType(type)) {
            throw new BusinessException("反馈类型无效");
        }
        
        List<FeedbackEntity> feedbacks = feedbackRepository.findAllPublicByType(type);
        return feedbacks.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 根据状态获取公开反馈
     *
     * @param status 反馈状态
     * @return 反馈列表
     */
    public List<FeedbackResponse> getPublicFeedbacksByStatus(String status) {
        if (!isValidStatus(status)) {
            throw new BusinessException("反馈状态无效");
        }
        
        List<FeedbackEntity> feedbacks = feedbackRepository.findAllPublicByStatus(status);
        return feedbacks.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 搜索反馈
     *
     * @param keyword 关键词
     * @return 反馈列表
     */
    public List<FeedbackResponse> searchFeedbacks(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllPublicFeedbacks();
        }
        
        List<FeedbackEntity> feedbacks = feedbackRepository.searchByKeyword(keyword.trim());
        return feedbacks.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取反馈的评论列表
     *
     * @param feedbackId 反馈ID
     * @return 评论列表
     */
    public List<FeedbackCommentResponse> getFeedbackComments(Long feedbackId) {
        // 验证反馈是否存在
        feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new BusinessException("反馈不存在"));

        List<FeedbackCommentEntity> comments = feedbackCommentRepository
                .findByFeedbackIdAndDeleteTimeIsNullOrderByCreateTimeAsc(feedbackId);
        
        return comments.stream()
                .map(this::toCommentResponse)
                .collect(Collectors.toList());
    }

    /**
     * 添加评论
     *
     * @param feedbackId 反馈ID
     * @param request 评论请求
     * @return 评论响应
     */
    @Transactional
    public FeedbackCommentResponse addComment(Long feedbackId, AddCommentRequest request) {
        // 验证反馈是否存在
        FeedbackEntity feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new BusinessException("反馈不存在"));

        Long userId = UserContext.getCurrentUserId();

        // 创建评论
        FeedbackCommentEntity comment = new FeedbackCommentEntity();
        comment.setFeedbackId(feedbackId);
        comment.setUserId(userId);
        comment.setContent(request.content());

        FeedbackCommentEntity savedComment = feedbackCommentRepository.save(comment);
        
        // 更新反馈的更新时间
        feedback.setUpdateTime(LocalDateTime.now());
        feedbackRepository.save(feedback);

        return toCommentResponse(savedComment);
    }

    /**
     * 关闭反馈（仅创建者可关闭）
     *
     * @param id 反馈ID
     */
    @Transactional
    public void closeFeedback(Long id) {
        FeedbackEntity feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new BusinessException("反馈不存在"));

        // 验证是否是创建者
        Long userId = UserContext.getCurrentUserId();
        if (!feedback.getUserId().equals(userId)) {
            throw new BusinessException("只有反馈创建者可以关闭反馈");
        }

        // 更新状态
        feedback.setStatus("已关闭");
        feedback.setUpdateTime(LocalDateTime.now());
        feedbackRepository.save(feedback);
    }

    /**
     * 重新打开反馈（仅创建者可操作）
     *
     * @param id 反馈ID
     */
    @Transactional
    public void reopenFeedback(Long id) {
        FeedbackEntity feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new BusinessException("反馈不存在"));

        // 验证是否是创建者
        Long userId = UserContext.getCurrentUserId();
        if (!feedback.getUserId().equals(userId)) {
            throw new BusinessException("只有反馈创建者可以重新打开反馈");
        }

        // 更新状态
        feedback.setStatus("待处理");
        feedback.setUpdateTime(LocalDateTime.now());
        feedbackRepository.save(feedback);
    }

    /**
     * 验证反馈状态是否有效
     */
    private boolean isValidStatus(String status) {
        return "待处理".equals(status) || "处理中".equals(status) || 
               "已完成".equals(status) || "已关闭".equals(status);
    }

    /**
     * 将实体转换为响应对象
     */
    private FeedbackResponse toResponse(FeedbackEntity entity) {
        FeedbackResponse response = new FeedbackResponse();
        response.setId(entity.getId());
        response.setUserId(entity.getUserId());
        response.setUserName(entity.getUserName());
        response.setUserNickname(entity.getUserNickname());
        response.setType(entity.getType());
        response.setTitle(entity.getTitle());
        response.setDescription(entity.getDescription());
        response.setStatus(entity.getStatus());
        response.setAdminReply(entity.getAdminReply());
        response.setCreateTime(entity.getCreateTime());
        response.setUpdateTime(entity.getUpdateTime());
        
        // 统计评论数量
        long commentCount = feedbackCommentRepository.countByFeedbackIdAndDeleteTimeIsNull(entity.getId());
        response.setCommentCount(commentCount);
        
        return response;
    }

    /**
     * 将评论实体转换为响应对象
     */
    private FeedbackCommentResponse toCommentResponse(FeedbackCommentEntity entity) {
        FeedbackCommentResponse response = new FeedbackCommentResponse();
        response.setId(entity.getId());
        response.setFeedbackId(entity.getFeedbackId());
        response.setUserId(entity.getUserId());
        response.setUserName(entity.getUserName());
        response.setUserNickname(entity.getUserNickname());
        response.setContent(entity.getContent());
        response.setCreateTime(entity.getCreateTime());
        return response;
    }
}
