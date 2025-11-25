# 反馈列表性能优化说明

## 问题分析

### 性能瓶颈
反馈列表查询非常慢（约8秒），主要原因是 **N+1查询问题**：

每个反馈在转换为响应对象时都会执行多次数据库查询：
1. 统计评论数量：`countByFeedbackIdAndDeleteTimeIsNull`
2. 统计点赞数：`countByTargetIdAndTargetTypeAndReactionType`（upvote）
3. 统计倒赞数：`countByTargetIdAndTargetTypeAndReactionType`（downvote）
4. 查询用户反应：`findByTargetIdAndTargetTypeAndUserId`
5. 权限检查：`permissionUtil.canDelete` 和 `canClose`（每次都查询用户表）

**举例**：如果有10个反馈，就会执行：
- 1次查询反馈列表
- 10次查询评论数
- 20次查询点赞/倒赞数
- 10次查询用户反应
- 20次查询用户权限
- **总计：61次数据库查询！**

## 优化方案

### 1. 批量查询评论数量

**修改文件**: `FeedbackCommentRepository.java`

添加批量查询方法：
```java
@Query("SELECT c.feedbackId as feedbackId, COUNT(c) as count " +
       "FROM FeedbackCommentEntity c " +
       "WHERE c.feedbackId IN :feedbackIds AND c.deleteTime IS NULL " +
       "GROUP BY c.feedbackId")
List<Map<String, Object>> countByFeedbackIds(@Param("feedbackIds") List<Long> feedbackIds);
```

**优化效果**: 10次查询 → 1次查询

### 2. 批量查询反应数据

**修改文件**: `FeedbackReactionRepository.java`

添加批量查询方法：
```java
@Query("SELECT r FROM FeedbackReactionEntity r " +
       "WHERE r.targetId IN :targetIds AND r.targetType = :targetType")
List<FeedbackReactionEntity> findByTargetIdsAndTargetType(
        @Param("targetIds") List<Long> targetIds,
        @Param("targetType") String targetType);
```

**优化效果**: 30次查询 → 1次查询

### 3. 优化权限检查

**修改文件**: `FeedbackService.java`

- 只查询一次当前用户信息
- 在内存中计算所有反馈的权限
- 避免每个反馈都查询用户表

**优化效果**: 20次查询 → 1次查询

### 4. 新增批量转换方法

**修改文件**: `FeedbackService.java`

添加 `toResponseList()` 方法：
```java
private List<FeedbackResponse> toResponseList(List<FeedbackEntity> entities) {
    // 1. 批量查询评论数
    // 2. 批量查询反应数据
    // 3. 在内存中计算权限
    // 4. 组装响应对象
}
```

**优化效果**: 
- 所有公开反馈查询方法现在使用批量转换
- 从 61次查询减少到 3次查询（反馈列表 + 评论统计 + 反应数据）

## 性能提升

### 优化前
- 10个反馈：约60次数据库查询
- 响应时间：8秒

### 优化后
- 10个反馈：3次数据库查询
- 预期响应时间：<500ms

**性能提升**: 约95%的数据库查询减少，预期速度提升16倍！

## 受影响的API

以下API已优化：
- `GET /feedback/public` - 获取所有公开反馈
- `GET /feedback/public/type/{type}` - 根据类型获取公开反馈
- `GET /feedback/public/status/{status}` - 根据状态获取公开反馈
- `GET /feedback/search?keyword=xxx` - 搜索反馈

## 技术细节

### 批量查询实现

1. **收集所有ID**
```java
List<Long> feedbackIds = entities.stream()
    .map(FeedbackEntity::getId)
    .collect(Collectors.toList());
```

2. **批量查询并构建Map**
```java
Map<Long, Long> commentCountMap = new HashMap<>();
List<Map<String, Object>> commentCounts = 
    feedbackCommentRepository.countByFeedbackIds(feedbackIds);
for (Map<String, Object> row : commentCounts) {
    Long feedbackId = ((Number) row.get("feedbackId")).longValue();
    Long count = ((Number) row.get("count")).longValue();
    commentCountMap.put(feedbackId, count);
}
```

3. **从Map中快速获取数据**
```java
response.setCommentCount(commentCountMap.getOrDefault(entity.getId(), 0L));
```

### 权限优化

```java
// 只查询一次当前用户
UserEntity currentUser = permissionUtil.getCurrentUser();
boolean isAdmin = currentUser != null && currentUser.isAdmin();

// 在循环中使用缓存的数据
for (FeedbackEntity entity : entities) {
    boolean isOwner = currentUserId != null && 
                      currentUserId.equals(entity.getUserId());
    response.setCanDelete(isOwner || isAdmin);
    response.setCanClose(isOwner || isAdmin);
}
```

## 兼容性

- ✅ 保持了原有的 `toResponse()` 方法，用于单个反馈的转换
- ✅ 新增的 `toResponseList()` 方法仅用于列表查询
- ✅ API接口和响应格式完全不变
- ✅ 前端无需任何修改

## 测试建议

1. **功能测试**
   - 验证反馈列表数据正确性
   - 验证评论数统计准确
   - 验证点赞/倒赞数统计准确
   - 验证权限字段正确

2. **性能测试**
   - 测试不同数量反馈的响应时间
   - 监控数据库查询次数
   - 验证内存使用情况

3. **边界测试**
   - 空列表
   - 单个反馈
   - 大量反馈（100+）

## 监控建议

可以在日志中添加性能监控：

```java
long startTime = System.currentTimeMillis();
List<FeedbackResponse> responses = toResponseList(feedbacks);
long endTime = System.currentTimeMillis();
log.info("Feedback list conversion took {} ms for {} items", 
         endTime - startTime, feedbacks.size());
```

## 进一步优化（可选）

如果数据量继续增长，可以考虑：

1. **添加Redis缓存**
   - 缓存热门反馈数据
   - 缓存用户权限信息
   - 设置合理的过期时间

2. **分页查询**
   - 限制每次返回的反馈数量
   - 使用游标分页避免深分页问题

3. **数据库索引优化**
   - 为常用查询字段添加索引
   - 复合索引优化多条件查询

4. **异步加载**
   - 前端先展示基本信息
   - 异步加载统计数据
