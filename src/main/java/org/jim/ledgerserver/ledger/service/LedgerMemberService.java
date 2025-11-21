package org.jim.ledgerserver.ledger.service;

import jakarta.annotation.Resource;
import org.jim.ledgerserver.common.enums.LedgerMemberRoleEnum;
import org.jim.ledgerserver.common.enums.LedgerTypeEnum;
import org.jim.ledgerserver.common.exception.BusinessException;
import org.jim.ledgerserver.ledger.entity.LedgerEntity;
import org.jim.ledgerserver.ledger.entity.LedgerMemberEntity;
import org.jim.ledgerserver.ledger.repository.LedgerMemberRepository;
import org.jim.ledgerserver.ledger.repository.LedgerRepository;
import org.jim.ledgerserver.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 账本成员管理业务逻辑层
 * @author James Smith
 */
@Service
public class LedgerMemberService {

    @Resource
    private LedgerMemberRepository ledgerMemberRepository;
    
    @Resource
    private LedgerRepository ledgerRepository;
    
    @Resource
    private UserRepository userRepository;

    /**
     * 添加成员到账本
     * @param ledgerId 账本ID
     * @param userId 用户ID
     * @param role 成员角色
     * @param invitedByUserId 邀请者用户ID
     * @param remark 备注
     * @return 成员关系实体
     */
    @Transactional
    public LedgerMemberEntity addMember(Long ledgerId, Long userId, LedgerMemberRoleEnum role, 
                                       Long invitedByUserId, String remark) {
        // 参数验证
        validateAddMemberParams(ledgerId, userId, role, invitedByUserId);
        
        // 验证账本是否存在且为共享账本
        LedgerEntity ledger = validateLedgerForSharing(ledgerId);
        
        // 验证用户是否存在
        if (!userRepository.existsById(userId)) {
            throw new BusinessException("用户不存在");
        }
        
        // 检查用户是否已经是成员
        if (ledgerMemberRepository.existsByLedgerIdAndUserId(ledgerId, userId)) {
            throw new BusinessException("用户已经是该账本的成员");
        }
        
        // 验证邀请者权限
        validateInviterPermission(ledgerId, invitedByUserId);
        
        // 检查成员数量限制
        checkMemberLimit(ledger);
        
        // 创建成员关系
        LedgerMemberEntity member = new LedgerMemberEntity();
        member.setLedgerId(ledgerId);
        member.setUserId(userId);
        member.setMemberRole(role);
        member.setJoinedAt(LocalDateTime.now());
        member.setInvitedByUserId(invitedByUserId);
        member.setRemark(remark);
        member.setStatus(LedgerMemberEntity.MemberStatus.ACTIVE.getCode());
        
        return ledgerMemberRepository.save(member);
    }

    /**
     * 移除账本成员
     * @param ledgerId 账本ID
     * @param userId 要移除的用户ID
     * @param operatorUserId 操作者用户ID
     */
    @Transactional
    public void removeMember(Long ledgerId, Long userId, Long operatorUserId) {
        if (ledgerId == null || userId == null || operatorUserId == null) {
            throw new BusinessException("参数不能为空");
        }
        
        // 查找成员关系
        LedgerMemberEntity member = ledgerMemberRepository.findByLedgerIdAndUserId(ledgerId, userId)
                .orElseThrow(() -> new BusinessException("成员关系不存在"));
        
        // 验证操作者权限
        validateRemovePermission(ledgerId, operatorUserId, member);
        
        // 逻辑删除
        member.setDeleteTime(LocalDateTime.now());
        ledgerMemberRepository.save(member);
    }

    /**
     * 更新成员角色
     * @param ledgerId 账本ID
     * @param userId 用户ID
     * @param newRole 新角色
     * @param operatorUserId 操作者用户ID
     * @return 更新后的成员实体
     */
    @Transactional
    public LedgerMemberEntity updateMemberRole(Long ledgerId, Long userId, LedgerMemberRoleEnum newRole, Long operatorUserId) {
        if (ledgerId == null || userId == null || newRole == null || operatorUserId == null) {
            throw new BusinessException("参数不能为空");
        }
        
        // 查找成员关系
        LedgerMemberEntity member = ledgerMemberRepository.findByLedgerIdAndUserId(ledgerId, userId)
                .orElseThrow(() -> new BusinessException("成员关系不存在"));
        
        // 验证操作者权限
        validateUpdateRolePermission(ledgerId, operatorUserId, member, newRole);
        
        member.setMemberRole(newRole);
        return ledgerMemberRepository.save(member);
    }

    /**
     * 查询账本的所有成员
     * @param ledgerId 账本ID
     * @return 成员列表
     */
    public List<LedgerMemberEntity> findMembersByLedgerId(Long ledgerId) {
        if (ledgerId == null) {
            throw new BusinessException("账本ID不能为空");
        }
        return ledgerMemberRepository.findByLedgerId(ledgerId);
    }

    /**
     * 分页查询账本成员
     * @param ledgerId 账本ID
     * @param pageable 分页参数
     * @return 成员分页结果
     */
    public Page<LedgerMemberEntity> findMembersByLedgerId(Long ledgerId, Pageable pageable) {
        if (ledgerId == null) {
            throw new BusinessException("账本ID不能为空");
        }
        return ledgerMemberRepository.findByLedgerId(ledgerId, pageable);
    }

    /**
     * 查询用户参与的所有账本
     * @param userId 用户ID
     * @return 成员关系列表
     */
    public List<LedgerMemberEntity> findLedgersByUserId(Long userId) {
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }
        return ledgerMemberRepository.findByUserId(userId);
    }

    /**
     * 检查用户对账本的权限
     * @param ledgerId 账本ID
     * @param userId 用户ID
     * @return 用户在该账本中的成员关系，如果不是成员则返回null
     */
    public Optional<LedgerMemberEntity> findMemberRelation(Long ledgerId, Long userId) {
        if (ledgerId == null || userId == null) {
            return Optional.empty();
        }
        return ledgerMemberRepository.findByLedgerIdAndUserId(ledgerId, userId);
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
        return ledgerMemberRepository.hasManagePermission(ledgerId, userId);
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
        return ledgerMemberRepository.hasEditPermission(ledgerId, userId);
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
        return ledgerMemberRepository.hasViewPermission(ledgerId, userId);
    }

    /**
     * 统计账本成员数量
     * @param ledgerId 账本ID
     * @return 成员数量
     */
    public long countMembers(Long ledgerId) {
        if (ledgerId == null) {
            return 0;
        }
        return ledgerMemberRepository.countByLedgerId(ledgerId);
    }

    /**
     * 保存成员实体（内部方法）
     * @param member 成员实体
     * @return 保存后的成员实体
     */
    public LedgerMemberEntity save(LedgerMemberEntity member) {
        return ledgerMemberRepository.save(member);
    }

    /**
     * 验证添加成员的参数
     */
    private void validateAddMemberParams(Long ledgerId, Long userId, LedgerMemberRoleEnum role, Long invitedByUserId) {
        if (ledgerId == null) {
            throw new BusinessException("账本ID不能为空");
        }
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }
        if (role == null) {
            throw new BusinessException("成员角色不能为空");
        }
        if (invitedByUserId == null) {
            throw new BusinessException("邀请者用户ID不能为空");
        }
        if (userId.equals(invitedByUserId)) {
            throw new BusinessException("不能邀请自己");
        }
        if (role == LedgerMemberRoleEnum.OWNER) {
            throw new BusinessException("不能直接设置成员为所有者");
        }
    }

    /**
     * 验证账本是否可以共享
     */
    private LedgerEntity validateLedgerForSharing(Long ledgerId) {
        LedgerEntity ledger = ledgerRepository.findById(ledgerId)
                .orElseThrow(() -> new BusinessException("账本不存在"));
        
        if (ledger.getDeleteTime() != null) {
            throw new BusinessException("账本已删除");
        }
        
        LedgerTypeEnum ledgerType = ledger.getLedgerType();
        if (ledgerType == null || ledgerType.isPersonal()) {
            throw new BusinessException("个人账本不支持添加成员");
        }
        
        return ledger;
    }

    /**
     * 验证邀请者权限
     */
    private void validateInviterPermission(Long ledgerId, Long invitedByUserId) {
        if (!hasManagePermission(ledgerId, invitedByUserId)) {
            throw new BusinessException("无权限邀请成员");
        }
    }

    /**
     * 检查成员数量限制
     */
    private void checkMemberLimit(LedgerEntity ledger) {
        if (ledger.getMaxMembers() != null && ledger.getMaxMembers() > 0) {
            long currentMemberCount = ledgerMemberRepository.countActiveMembersByLedgerId(ledger.getId());
            if (currentMemberCount >= ledger.getMaxMembers()) {
                throw new BusinessException("账本成员数量已达上限（" + ledger.getMaxMembers() + "人）");
            }
        }
    }

    /**
     * 验证移除成员权限
     */
    private void validateRemovePermission(Long ledgerId, Long operatorUserId, LedgerMemberEntity memberToRemove) {
        // 查找操作者的成员关系
        LedgerMemberEntity operator = ledgerMemberRepository.findByLedgerIdAndUserId(ledgerId, operatorUserId)
                .orElseThrow(() -> new BusinessException("无权限执行此操作"));
        
        // 不能移除所有者
        if (memberToRemove.isOwner()) {
            throw new BusinessException("不能移除账本所有者");
        }
        
        // 只有管理员以上权限才能移除成员
        if (!operator.hasManagePermission()) {
            throw new BusinessException("无权限移除成员");
        }
        
        // 管理员不能移除同级或更高级别的成员
        LedgerMemberRoleEnum operatorRole = operator.getMemberRole();
        LedgerMemberRoleEnum targetRole = memberToRemove.getMemberRole();
        if (operatorRole != LedgerMemberRoleEnum.OWNER && 
            operatorRole.comparePermissionLevel(targetRole) <= 0) {
            throw new BusinessException("无权限移除该成员");
        }
    }

    /**
     * 验证更新角色权限
     */
    private void validateUpdateRolePermission(Long ledgerId, Long operatorUserId, 
                                            LedgerMemberEntity memberToUpdate, LedgerMemberRoleEnum newRole) {
        // 查找操作者的成员关系
        LedgerMemberEntity operator = ledgerMemberRepository.findByLedgerIdAndUserId(ledgerId, operatorUserId)
                .orElseThrow(() -> new BusinessException("无权限执行此操作"));
        
        // 不能修改所有者角色
        if (memberToUpdate.isOwner()) {
            throw new BusinessException("不能修改所有者角色");
        }
        
        // 不能设置为所有者
        if (newRole == LedgerMemberRoleEnum.OWNER) {
            throw new BusinessException("不能设置成员为所有者");
        }
        
        // 只有管理员以上权限才能修改成员角色
        if (!operator.hasManagePermission()) {
            throw new BusinessException("无权限修改成员角色");
        }
        
        // 管理员不能修改同级或更高级别成员的角色
        LedgerMemberRoleEnum operatorRole = operator.getMemberRole();
        LedgerMemberRoleEnum currentRole = memberToUpdate.getMemberRole();
        if (operatorRole != LedgerMemberRoleEnum.OWNER && 
            operatorRole.comparePermissionLevel(currentRole) <= 0) {
            throw new BusinessException("无权限修改该成员角色");
        }
    }
}