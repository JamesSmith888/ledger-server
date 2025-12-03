package org.jim.ledgerserver.user.controller;

import jakarta.annotation.Resource;
import org.jim.ledgerserver.common.JSONResult;
import org.jim.ledgerserver.common.util.UserContext;
import org.jim.ledgerserver.user.dto.UserPreferenceReq;
import org.jim.ledgerserver.user.dto.UserPreferenceResp;
import org.jim.ledgerserver.user.dto.UserPreferenceSyncReq;
import org.jim.ledgerserver.user.entity.UserPreferenceEntity;
import org.jim.ledgerserver.user.enums.PreferenceType;
import org.jim.ledgerserver.user.service.UserPreferenceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户偏好记忆 API
 * 
 * 用于存储和管理 AI 学习的用户个性化偏好
 *
 * @author James Smith
 */
@RestController
@RequestMapping("/api/user/preferences")
public class UserPreferenceController {

    @Resource
    private UserPreferenceService preferenceService;

    /**
     * 获取当前用户的所有偏好
     */
    @GetMapping
    public JSONResult<List<UserPreferenceResp>> getAll() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return JSONResult.fail("未登录");
        }

        List<UserPreferenceResp> preferences = preferenceService.getAllByUserId(userId)
                .stream()
                .map(UserPreferenceResp::from)
                .toList();

        return JSONResult.success(preferences);
    }

    /**
     * 获取当前用户的所有启用偏好（按使用频率排序）
     */
    @GetMapping("/active")
    public JSONResult<List<UserPreferenceResp>> getActive() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return JSONResult.fail("未登录");
        }

        List<UserPreferenceResp> preferences = preferenceService.getActiveByUserId(userId)
                .stream()
                .map(UserPreferenceResp::from)
                .toList();

        return JSONResult.success(preferences);
    }

    /**
     * 按类型获取偏好
     */
    @GetMapping("/type/{type}")
    public JSONResult<List<UserPreferenceResp>> getByType(@PathVariable PreferenceType type) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return JSONResult.fail("未登录");
        }

        List<UserPreferenceResp> preferences = preferenceService.getByUserIdAndType(userId, type)
                .stream()
                .map(UserPreferenceResp::from)
                .toList();

        return JSONResult.success(preferences);
    }

    /**
     * 按关键词查找偏好
     */
    @GetMapping("/search")
    public JSONResult<UserPreferenceResp> search(
            @RequestParam String keyword,
            @RequestParam(required = false) PreferenceType type) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return JSONResult.fail("未登录");
        }

        var preference = type != null 
                ? preferenceService.findByKeywordAndType(userId, keyword, type)
                : preferenceService.findByKeyword(userId, keyword);

        return preference
                .map(p -> JSONResult.success(UserPreferenceResp.from(p)))
                .orElse(JSONResult.success(null));
    }

    /**
     * 添加或更新偏好
     */
    @PostMapping
    public JSONResult<UserPreferenceResp> save(@RequestBody UserPreferenceReq req) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return JSONResult.fail("未登录");
        }

        if (req.keyword() == null || req.keyword().isBlank()) {
            return JSONResult.fail("关键词不能为空");
        }
        if (req.correction() == null || req.correction().isBlank()) {
            return JSONResult.fail("纠正内容不能为空");
        }

        PreferenceType type = req.type() != null ? req.type() : PreferenceType.CATEGORY_MAPPING;

        UserPreferenceEntity entity = preferenceService.saveOrUpdate(
                userId, type, req.keyword(), req.correction(), req.note(), req.categoryId());

        return JSONResult.success(UserPreferenceResp.from(entity));
    }

    /**
     * 更新偏好
     */
    @PutMapping("/{id}")
    public JSONResult<UserPreferenceResp> update(
            @PathVariable Long id,
            @RequestBody UserPreferenceReq req) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return JSONResult.fail("未登录");
        }

        return preferenceService.update(userId, id, req.correction(), req.note(), req.categoryId(), null)
                .map(p -> JSONResult.success(UserPreferenceResp.from(p)))
                .orElse(JSONResult.fail("偏好不存在"));
    }

    /**
     * 启用/禁用偏好
     */
    @PutMapping("/{id}/toggle")
    public JSONResult<Void> toggle(@PathVariable Long id, @RequestParam boolean enabled) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return JSONResult.fail("未登录");
        }

        preferenceService.toggleEnabled(userId, id, enabled);
        return JSONResult.success(null);
    }

    /**
     * 增加使用次数
     */
    @PostMapping("/{id}/usage")
    public JSONResult<Void> incrementUsage(@PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return JSONResult.fail("未登录");
        }

        preferenceService.incrementUsage(userId, id);
        return JSONResult.success(null);
    }

    /**
     * 删除偏好
     */
    @DeleteMapping("/{id}")
    public JSONResult<Void> delete(@PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return JSONResult.fail("未登录");
        }

        boolean deleted = preferenceService.delete(userId, id);
        return deleted ? JSONResult.success(null) : JSONResult.fail("偏好不存在");
    }

    /**
     * 删除所有偏好
     */
    @DeleteMapping("/all")
    public JSONResult<Void> deleteAll() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return JSONResult.fail("未登录");
        }

        preferenceService.deleteAll(userId);
        return JSONResult.success(null);
    }

    /**
     * 获取统计信息
     */
    @GetMapping("/stats")
    public JSONResult<Map<String, Long>> getStats() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return JSONResult.fail("未登录");
        }

        var stats = preferenceService.getStats(userId);
        return JSONResult.success(Map.of(
                "total", stats.total(),
                "enabled", stats.enabled()
        ));
    }

    /**
     * 批量同步偏好（从本地上传到云端）
     */
    @PostMapping("/sync")
    public JSONResult<List<UserPreferenceResp>> sync(@RequestBody UserPreferenceSyncReq req) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return JSONResult.fail("未登录");
        }

        if (req.preferences() == null || req.preferences().isEmpty()) {
            return JSONResult.success(List.of());
        }

        // 转换请求为实体
        List<UserPreferenceEntity> entities = req.preferences().stream()
                .map(p -> new UserPreferenceEntity()
                        .setType(p.type() != null ? p.type() : PreferenceType.CATEGORY_MAPPING)
                        .setKeyword(p.keyword())
                        .setCorrection(p.correction())
                        .setNote(p.note())
                        .setCategoryId(p.categoryId())
                        .setUsageCount(1)
                        .setEnabled(true))
                .toList();

        List<UserPreferenceResp> saved = preferenceService.batchSave(userId, entities)
                .stream()
                .map(UserPreferenceResp::from)
                .toList();

        return JSONResult.success(saved);
    }
}
