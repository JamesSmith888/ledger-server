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
import org.jim.ledgerserver.feedback.entity.FeedbackReactionEntity;
import org.jim.ledgerserver.feedback.repository.FeedbackCommentRepository;
import org.jim.ledgerserver.feedback.repository.FeedbackReactionRepository;
import org.jim.ledgerserver.feedback.repository.FeedbackRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @Resource
    private FeedbackReactionRepository feedbackReactionRepository;

    @Resource
    private org.jim.ledgerserver.common.util.PermissionUtil permissionUtil;

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
     * 所有人都可以查看公开反馈
     *
     * @param id 反馈ID
     * @return 反馈响应
     */
    public FeedbackResponse getFeedbackById(Long id) {
        FeedbackEntity feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new BusinessException("反馈不存在"));

        return toResponse(feedback);
    }

    /**
     * 删除反馈（逻辑删除）
     * 权限：反馈创建者或管理员可以删除
     *
     * @param id 反馈ID
     */
    public void deleteFeedback(Long id) {
        FeedbackEntity feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new BusinessException("反馈不存在"));

        // 验证是否有删除权限（创建者或管理员）
        if (!permissionUtil.canDelete(feedback.getUserId())) {
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
     * 获取所有公开反馈（按点赞数排序）
     * 优化：使用批量查询避免N+1问题
     *
     * @return 反馈列表
     */
    public List<FeedbackResponse> getAllPublicFeedbacks() {
        List<FeedbackEntity> feedbacks = feedbackRepository.findAllPublic();
        List<FeedbackResponse> responses = toResponseList(feedbacks);
        
        // 按点赞数降序排序（默认排序方式）
        responses.sort((a, b) -> Long.compare(b.getUpvoteCount(), a.getUpvoteCount()));
        
        return responses;
    }

    /**
     * 根据类型获取公开反馈（按点赞数排序）
     * 优化：使用批量查询避免N+1问题
     *
     * @param type 反馈类型
     * @return 反馈列表
     */
    public List<FeedbackResponse> getPublicFeedbacksByType(String type) {
        if (!isValidType(type)) {
            throw new BusinessException("反馈类型无效");
        }
        
        List<FeedbackEntity> feedbacks = feedbackRepository.findAllPublicByType(type);
        List<FeedbackResponse> responses = toResponseList(feedbacks);
        
        // 按点赞数降序排序
        responses.sort((a, b) -> Long.compare(b.getUpvoteCount(), a.getUpvoteCount()));
        
        return responses;
    }

    /**
     * 根据状态获取公开反馈（按点赞数排序）
     * 优化：使用批量查询避免N+1问题
     *
     * @param status 反馈状态
     * @return 反馈列表
     */
    public List<FeedbackResponse> getPublicFeedbacksByStatus(String status) {
        if (!isValidStatus(status)) {
            throw new BusinessException("反馈状态无效");
        }
        
        List<FeedbackEntity> feedbacks = feedbackRepository.findAllPublicByStatus(status);
        List<FeedbackResponse> responses = toResponseList(feedbacks);
        
        // 按点赞数降序排序
        responses.sort((a, b) -> Long.compare(b.getUpvoteCount(), a.getUpvoteCount()));
        
        return responses;
    }

    /**
     * 搜索反馈（按点赞数排序）
     * 优化：使用批量查询避免N+1问题
     *
     * @param keyword 关键词
     * @return 反馈列表
     */
    public List<FeedbackResponse> searchFeedbacks(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllPublicFeedbacks();
        }
        
        List<FeedbackEntity> feedbacks = feedbackRepository.searchByKeyword(keyword.trim());
        List<FeedbackResponse> responses = toResponseList(feedbacks);
        
        // 按点赞数降序排序
        responses.sort((a, b) -> Long.compare(b.getUpvoteCount(), a.getUpvoteCount()));
        
        return responses;
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
     * 关闭反馈
     * 权限：反馈创建者或管理员可以关闭
     *
     * @param id 反馈ID
     */
    @Transactional
    public void closeFeedback(Long id) {
        FeedbackEntity feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new BusinessException("反馈不存在"));

        // 验证是否有关闭权限（创建者或管理员）
        if (!permissionUtil.canClose(feedback.getUserId())) {
            throw new BusinessException("无权关闭该反馈");
        }

        // 更新状态
        feedback.setStatus("已关闭");
        feedback.setUpdateTime(LocalDateTime.now());
        feedbackRepository.save(feedback);
    }

    /**
     * 重新打开反馈
     * 权限：反馈创建者或管理员可以重新打开
     *
     * @param id 反馈ID
     */
    @Transactional
    public void reopenFeedback(Long id) {
        FeedbackEntity feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new BusinessException("反馈不存在"));

        // 验证是否有重新打开权限（创建者或管理员）
        if (!permissionUtil.canClose(feedback.getUserId())) {
            throw new BusinessException("无权重新打开该反馈");
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
        
        // 添加点赞统计
        fillReactionStats(response, entity.getId(), "feedback");
        
        // 设置权限字段
        response.setCanDelete(permissionUtil.canDelete(entity.getUserId()));
        response.setCanClose(permissionUtil.canClose(entity.getUserId()));
        
        return response;
    }

    /**
     * 批量将实体转换为响应对象（性能优化版本）
     * 使用批量查询避免N+1问题
     */
    private List<FeedbackResponse> toResponseList(List<FeedbackEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return new ArrayList<>();
        }

        // 提取所有反馈ID
        List<Long> feedbackIds = entities.stream()
                .map(FeedbackEntity::getId)
                .collect(Collectors.toList());

        // 批量查询评论数量
        Map<Long, Long> commentCountMap = new HashMap<>();
        List<Map<String, Object>> commentCounts = feedbackCommentRepository.countByFeedbackIds(feedbackIds);
        for (Map<String, Object> row : commentCounts) {
            Long feedbackId = ((Number) row.get("feedbackId")).longValue();
            Long count = ((Number) row.get("count")).longValue();
            commentCountMap.put(feedbackId, count);
        }

        // 批量查询所有反应数据
        Long currentUserId = UserContext.getCurrentUserId();
        List<FeedbackReactionEntity> allReactions = feedbackReactionRepository
                .findByTargetIdsAndTargetType(feedbackIds, "feedback");

        // 构建统计Map
        Map<Long, Long> upvoteCountMap = new HashMap<>();
        Map<Long, Long> downvoteCountMap = new HashMap<>();
        Map<Long, String> userReactionMap = new HashMap<>();

        for (FeedbackReactionEntity reaction : allReactions) {
            Long targetId = reaction.getTargetId();
            String reactionType = reaction.getReactionType();
            
            // 统计点赞和倒赞数
            if ("upvote".equals(reactionType)) {
                upvoteCountMap.put(targetId, upvoteCountMap.getOrDefault(targetId, 0L) + 1);
            } else if ("downvote".equals(reactionType)) {
                downvoteCountMap.put(targetId, downvoteCountMap.getOrDefault(targetId, 0L) + 1);
            }
            
            // 记录当前用户的反应
            if (currentUserId != null && currentUserId.equals(reaction.getUserId())) {
                userReactionMap.put(targetId, reactionType);
            }
        }

        // 获取当前用户并缓存（避免重复查询）
        org.jim.ledgerserver.user.entity.UserEntity currentUser = permissionUtil.getCurrentUser();
        boolean isAdmin = currentUser != null && currentUser.isAdmin();

        // 转换为响应对象
        List<FeedbackResponse> responses = new ArrayList<>();
        for (FeedbackEntity entity : entities) {
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
            
            // 设置评论数量
            response.setCommentCount(commentCountMap.getOrDefault(entity.getId(), 0L));
            
            // 设置反应统计
            response.setUpvoteCount(upvoteCountMap.getOrDefault(entity.getId(), 0L));
            response.setDownvoteCount(downvoteCountMap.getOrDefault(entity.getId(), 0L));
            response.setUserReaction(userReactionMap.get(entity.getId()));
            
            // 设置权限字段（使用缓存的用户信息）
            boolean isOwner = currentUserId != null && currentUserId.equals(entity.getUserId());
            response.setCanDelete(isOwner || isAdmin);
            response.setCanClose(isOwner || isAdmin);
            
            responses.add(response);
        }

        return responses;
    }

    /**
     * 对反馈或评论进行点赞/倒赞操作
     *
     * @param targetId 目标ID（反馈ID或评论ID）
     * @param targetType 目标类型：feedback 或 comment
     * @param reactionType 反应类型：upvote 或 downvote
     */
    @Transactional
    public void addReaction(Long targetId, String targetType, String reactionType) {
        // 验证参数
        if (!isValidTargetType(targetType) || !isValidReactionType(reactionType)) {
            throw new BusinessException("参数无效");
        }

        Long userId = UserContext.getCurrentUserId();

        // 验证目标是否存在
        if ("feedback".equals(targetType)) {
            feedbackRepository.findById(targetId)
                    .orElseThrow(() -> new BusinessException("反馈不存在"));
        } else {
            feedbackCommentRepository.findById(targetId)
                    .orElseThrow(() -> new BusinessException("评论不存在"));
        }

        // 检查用户是否已经有反应
        var existingReaction = feedbackReactionRepository
                .findByTargetIdAndTargetTypeAndUserId(targetId, targetType, userId);

        if (existingReaction.isPresent()) {
            FeedbackReactionEntity reaction = existingReaction.get();
            // 如果是相同的反应类型，则删除（取消反应）
            if (reaction.getReactionType().equals(reactionType)) {
                feedbackReactionRepository.delete(reaction);
            } else {
                // 否则更新反应类型
                reaction.setReactionType(reactionType);
                feedbackReactionRepository.save(reaction);
            }
        } else {
            // 创建新反应
            FeedbackReactionEntity reaction = new FeedbackReactionEntity();
            reaction.setTargetId(targetId);
            reaction.setTargetType(targetType);
            reaction.setUserId(userId);
            reaction.setReactionType(reactionType);
            feedbackReactionRepository.save(reaction);
        }
    }

    /**
     * 取消反应
     *
     * @param targetId 目标ID
     * @param targetType 目标类型
     */
    @Transactional
    public void removeReaction(Long targetId, String targetType) {
        Long userId = UserContext.getCurrentUserId();
        feedbackReactionRepository.deleteByTargetIdAndTargetTypeAndUserId(targetId, targetType, userId);
    }

    /**
     * 获取特定目标的点赞和倒赞统计
     */
    private void fillReactionStats(Object response, Long targetId, String targetType) {
        long upvoteCount = feedbackReactionRepository
                .countByTargetIdAndTargetTypeAndReactionType(targetId, targetType, "upvote");
        long downvoteCount = feedbackReactionRepository
                .countByTargetIdAndTargetTypeAndReactionType(targetId, targetType, "downvote");

        Long userId = UserContext.getCurrentUserId();
        var userReaction = feedbackReactionRepository
                .findByTargetIdAndTargetTypeAndUserId(targetId, targetType, userId);
        String userReactionType = userReaction.map(FeedbackReactionEntity::getReactionType).orElse(null);

        if (response instanceof FeedbackResponse) {
            FeedbackResponse fbResponse = (FeedbackResponse) response;
            fbResponse.setUpvoteCount(upvoteCount);
            fbResponse.setDownvoteCount(downvoteCount);
            fbResponse.setUserReaction(userReactionType);
        } else if (response instanceof FeedbackCommentResponse) {
            FeedbackCommentResponse commentResponse = (FeedbackCommentResponse) response;
            commentResponse.setUpvoteCount(upvoteCount);
            commentResponse.setDownvoteCount(downvoteCount);
            commentResponse.setUserReaction(userReactionType);
        }
    }

    /**
     * 验证目标类型
     */
    private boolean isValidTargetType(String targetType) {
        return "feedback".equals(targetType) || "comment".equals(targetType);
    }

    /**
     * 验证反应类型
     */
    private boolean isValidReactionType(String reactionType) {
        return "upvote".equals(reactionType) || "downvote".equals(reactionType);
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
        
        // 添加点赞统计
        fillReactionStats(response, entity.getId(), "comment");
        
        return response;
    }
}
