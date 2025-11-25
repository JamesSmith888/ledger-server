package org.jim.ledgerserver.common.util;

import jakarta.annotation.Resource;
import org.jim.ledgerserver.user.entity.UserEntity;
import org.jim.ledgerserver.user.repository.UserRepository;
import org.springframework.stereotype.Component;

/**
 * 权限工具类
 * 提供统一的权限验证功能，支持整个系统的权限管控
 *
 * @author James Smith
 */
@Component
public class PermissionUtil {

    @Resource
    private UserRepository userRepository;

    /**
     * 判断当前用户是否为管理员
     *
     * @return true-是管理员，false-不是管理员
     */
    public boolean isCurrentUserAdmin() {
        Long currentUserId = UserContext.getCurrentUserId();
        return isAdmin(currentUserId);
    }

    /**
     * 判断指定用户是否为管理员
     *
     * @param userId 用户ID
     * @return true-是管理员，false-不是管理员
     */
    public boolean isAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        
        UserEntity user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }
        
        return user.isAdmin();
    }

    /**
     * 判断当前用户是否是指定资源的所有者
     *
     * @param ownerId 资源所有者ID
     * @return true-是所有者，false-不是所有者
     */
    public boolean isOwner(Long ownerId) {
        if (ownerId == null) {
            return false;
        }
        
        Long currentUserId = UserContext.getCurrentUserId();
        return ownerId.equals(currentUserId);
    }

    /**
     * 判断当前用户是否有权限删除指定资源
     * 规则：资源所有者或管理员可以删除
     *
     * @param ownerId 资源所有者ID
     * @return true-有权限删除，false-无权限删除
     */
    public boolean canDelete(Long ownerId) {
        return isOwner(ownerId) || isCurrentUserAdmin();
    }

    /**
     * 判断当前用户是否有权限编辑指定资源
     * 规则：资源所有者或管理员可以编辑
     *
     * @param ownerId 资源所有者ID
     * @return true-有权限编辑，false-无权限编辑
     */
    public boolean canEdit(Long ownerId) {
        return isOwner(ownerId) || isCurrentUserAdmin();
    }

    /**
     * 判断当前用户是否有权限关闭/重开指定资源
     * 规则：资源所有者或管理员可以关闭/重开
     *
     * @param ownerId 资源所有者ID
     * @return true-有权限操作，false-无权限操作
     */
    public boolean canClose(Long ownerId) {
        return isOwner(ownerId) || isCurrentUserAdmin();
    }

    /**
     * 判断当前用户是否有管理员回复权限
     * 规则：仅管理员可以回复
     *
     * @return true-有权限回复，false-无权限回复
     */
    public boolean canAdminReply() {
        return isCurrentUserAdmin();
    }

    /**
     * 判断当前用户是否有权限修改状态
     * 规则：仅管理员可以修改状态
     *
     * @return true-有权限修改，false-无权限修改
     */
    public boolean canChangeStatus() {
        return isCurrentUserAdmin();
    }

    /**
     * 获取当前用户
     *
     * @return 当前用户实体
     */
    public UserEntity getCurrentUser() {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return null;
        }
        return userRepository.findById(currentUserId).orElse(null);
    }

    /**
     * 获取当前用户角色
     *
     * @return 用户角色
     */
    public String getCurrentUserRole() {
        UserEntity user = getCurrentUser();
        return user != null ? user.getRole() : "USER";
    }
}
