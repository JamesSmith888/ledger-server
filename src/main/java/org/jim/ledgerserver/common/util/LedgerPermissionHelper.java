package org.jim.ledgerserver.common.util;

import jakarta.annotation.Resource;
import org.jim.ledgerserver.common.exception.BusinessException;
import org.jim.ledgerserver.ledger.entity.LedgerEntity;
import org.jim.ledgerserver.ledger.service.LedgerService;
import org.jim.ledgerserver.ledger.service.LedgerMemberService;
import org.springframework.stereotype.Component;

/**
 * 账本权限验证工具类
 * @author James Smith
 */
@Component
public class LedgerPermissionHelper {

    @Resource
    private LedgerService ledgerService;
    
    @Resource
    private LedgerMemberService ledgerMemberService;

    /**
     * 验证用户是否有账本的查看权限
     * @param ledgerId 账本ID
     * @param userId 用户ID
     * @throws BusinessException 无权限时抛出异常
     */
    public void requireViewPermission(Long ledgerId, Long userId) {
        if (!hasViewPermission(ledgerId, userId)) {
            throw new BusinessException("无权限访问该账本");
        }
    }

    /**
     * 验证用户是否有账本的编辑权限
     * @param ledgerId 账本ID
     * @param userId 用户ID
     * @throws BusinessException 无权限时抛出异常
     */
    public void requireEditPermission(Long ledgerId, Long userId) {
        if (!hasEditPermission(ledgerId, userId)) {
            throw new BusinessException("无权限编辑该账本");
        }
    }

    /**
     * 验证用户是否有账本的管理权限
     * @param ledgerId 账本ID
     * @param userId 用户ID
     * @throws BusinessException 无权限时抛出异常
     */
    public void requireManagePermission(Long ledgerId, Long userId) {
        if (!hasManagePermission(ledgerId, userId)) {
            throw new BusinessException("无权限管理该账本");
        }
    }

    /**
     * 验证用户是否为账本所有者
     * @param ledgerId 账本ID
     * @param userId 用户ID
     * @throws BusinessException 非所有者时抛出异常
     */
    public void requireOwnerPermission(Long ledgerId, Long userId) {
        if (!isOwner(ledgerId, userId)) {
            throw new BusinessException("只有账本所有者可以执行此操作");
        }
    }

    /**
     * 检查用户是否有账本的查看权限
     * @param ledgerId 账本ID
     * @param userId 用户ID
     * @return 是否有查看权限
     */
    public boolean hasViewPermission(Long ledgerId, Long userId) {
        if (ledgerId == null || userId == null) {
            return false;
        }

        try {
            LedgerEntity ledger = ledgerService.findById(ledgerId);
            
            // 所有者总是有权限
            if (ledger.getOwnerUserId().equals(userId)) {
                return true;
            }
            
            // 个人账本只有所有者可以访问
            if (ledger.isPersonal()) {
                return false;
            }
            
            // 共享账本检查成员权限
            return ledgerMemberService.hasViewPermission(ledgerId, userId);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查用户是否有账本的编辑权限
     * @param ledgerId 账本ID
     * @param userId 用户ID
     * @return 是否有编辑权限
     */
    public boolean hasEditPermission(Long ledgerId, Long userId) {
        if (ledgerId == null || userId == null) {
            return false;
        }

        try {
            LedgerEntity ledger = ledgerService.findById(ledgerId);
            
            // 所有者总是有权限
            if (ledger.getOwnerUserId().equals(userId)) {
                return true;
            }
            
            // 个人账本只有所有者可以编辑
            if (ledger.isPersonal()) {
                return false;
            }
            
            // 共享账本检查成员权限
            return ledgerMemberService.hasEditPermission(ledgerId, userId);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查用户是否有账本的管理权限
     * @param ledgerId 账本ID
     * @param userId 用户ID
     * @return 是否有管理权限
     */
    public boolean hasManagePermission(Long ledgerId, Long userId) {
        if (ledgerId == null || userId == null) {
            return false;
        }

        try {
            LedgerEntity ledger = ledgerService.findById(ledgerId);
            
            // 所有者总是有管理权限
            if (ledger.getOwnerUserId().equals(userId)) {
                return true;
            }
            
            // 个人账本只有所有者可以管理
            if (ledger.isPersonal()) {
                return false;
            }
            
            // 共享账本检查管理权限
            return ledgerMemberService.hasManagePermission(ledgerId, userId);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查用户是否为账本所有者
     * @param ledgerId 账本ID
     * @param userId 用户ID
     * @return 是否为所有者
     */
    public boolean isOwner(Long ledgerId, Long userId) {
        if (ledgerId == null || userId == null) {
            return false;
        }

        try {
            LedgerEntity ledger = ledgerService.findById(ledgerId);
            return ledger.getOwnerUserId().equals(userId);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取用户在账本中的角色信息
     * @param ledgerId 账本ID
     * @param userId 用户ID
     * @return 权限信息对象
     */
    public LedgerPermissionInfo getPermissionInfo(Long ledgerId, Long userId) {
        LedgerPermissionInfo info = new LedgerPermissionInfo();
        info.setHasViewPermission(hasViewPermission(ledgerId, userId));
        info.setHasEditPermission(hasEditPermission(ledgerId, userId));
        info.setHasManagePermission(hasManagePermission(ledgerId, userId));
        info.setIsOwner(isOwner(ledgerId, userId));
        return info;
    }

    /**
     * 权限信息对象
     */
    public static class LedgerPermissionInfo {
        private boolean hasViewPermission;
        private boolean hasEditPermission;
        private boolean hasManagePermission;
        private boolean isOwner;

        // Getters and Setters
        public boolean isHasViewPermission() {
            return hasViewPermission;
        }

        public void setHasViewPermission(boolean hasViewPermission) {
            this.hasViewPermission = hasViewPermission;
        }

        public boolean isHasEditPermission() {
            return hasEditPermission;
        }

        public void setHasEditPermission(boolean hasEditPermission) {
            this.hasEditPermission = hasEditPermission;
        }

        public boolean isHasManagePermission() {
            return hasManagePermission;
        }

        public void setHasManagePermission(boolean hasManagePermission) {
            this.hasManagePermission = hasManagePermission;
        }

        public boolean isOwner() {
            return isOwner;
        }

        public void setIsOwner(boolean isOwner) {
            this.isOwner = isOwner;
        }
    }
}