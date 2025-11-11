package org.jim.ledgerserver.ledger.service;

import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.jim.ledgerserver.common.enums.LedgerMemberRoleEnum;
import org.jim.ledgerserver.common.enums.LedgerTypeEnum;
import org.jim.ledgerserver.common.exception.BusinessException;
import org.jim.ledgerserver.ledger.entity.LedgerEntity;
import org.jim.ledgerserver.ledger.entity.LedgerMemberEntity;
import org.jim.ledgerserver.ledger.repository.LedgerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 账本业务逻辑层
 * @author James Smith
 */
@Component
public class LedgerService {

    @Resource
    private LedgerRepository ledgerRepository;

    @Resource
    private LedgerMemberService ledgerMemberService;

    /**
     * 创建个人账本
     * @param name 账本名称
     * @param description 账本描述
     * @param ownerUserId 所有者用户ID
     * @return 创建的账本实体
     */
    public LedgerEntity create(String name, String description, Long ownerUserId) {
        return createLedger(name, description, ownerUserId, LedgerTypeEnum.PERSONAL, null, null);
    }

    /**
     * 创建共享账本
     * @param name 账本名称
     * @param description 账本描述
     * @param ownerUserId 所有者用户ID
     * @param maxMembers 最大成员数限制
     * @param isPublic 是否公开
     * @return 创建的账本实体
     */
    @Transactional
    public LedgerEntity createSharedLedger(String name, String description, Long ownerUserId, 
                                          Integer maxMembers, Boolean isPublic) {
        return createLedger(name, description, ownerUserId, LedgerTypeEnum.SHARED, maxMembers, isPublic);
    }

    /**
     * 通用的创建账本方法
     */
    @Transactional
    public LedgerEntity createLedger(String name, String description, Long ownerUserId, 
                                    LedgerTypeEnum ledgerType, Integer maxMembers, Boolean isPublic) {
        if (StringUtils.isBlank(name)) {
            throw new BusinessException("账本名称不能为空");
        }
        if (ownerUserId == null) {
            throw new BusinessException("所有者用户ID不能为空");
        }
        if (ledgerType == null) {
            ledgerType = LedgerTypeEnum.PERSONAL;
        }

        // 检查同一用户下是否已存在同名账本
        if (ledgerRepository.existsByNameAndOwnerUserIdExcludingId(name, ownerUserId, null)) {
            throw new BusinessException("账本名称已存在");
        }

        LedgerEntity ledger = new LedgerEntity();
        ledger.setName(name);
        ledger.setDescription(description);
        ledger.setOwnerUserId(ownerUserId);
        ledger.setLedgerType(ledgerType);
        ledger.setMaxMembers(maxMembers);
        ledger.setIsPublic(isPublic != null ? isPublic : false);
        
        // 保存账本
        ledger = ledgerRepository.save(ledger);
        
        // 如果是共享账本，自动添加所有者为成员
        if (ledgerType.isShared()) {
            LedgerMemberEntity ownerMember = new LedgerMemberEntity();
            ownerMember.setLedgerId(ledger.getId());
            ownerMember.setUserId(ownerUserId);
            ownerMember.setMemberRole(LedgerMemberRoleEnum.OWNER);
            ownerMember.setJoinedAt(LocalDateTime.now());
            ownerMember.setStatus(LedgerMemberEntity.MemberStatus.ACTIVE.getCode());
            ledgerMemberService.save(ownerMember); // 需要在 LedgerMemberService 中添加 save 方法
        }
        
        return ledger;
    }

    /**
     * 根据ID查询账本
     * @param id 账本ID
     * @return 账本实体
     */
    public LedgerEntity findById(Long id) {
        if (id == null) {
            throw new BusinessException("账本ID不能为空");
        }
        return ledgerRepository.findById(id)
                .orElseThrow(() -> new BusinessException("账本不存在"));
    }

    /**
     * 根据用户ID查询所有账本
     * @param ownerUserId 所有者用户ID
     * @return 账本列表
     */
    public List<LedgerEntity> findByOwnerUserId(Long ownerUserId) {
        if (ownerUserId == null) {
            throw new BusinessException("所有者用户ID不能为空");
        }
        return ledgerRepository.findByOwnerUserId(ownerUserId);
    }

    /**
     * 根据用户ID分页查询账本
     * @param ownerUserId 所有者用户ID
     * @param pageable 分页参数
     * @return 账本分页结果
     */
    public Page<LedgerEntity> findByOwnerUserId(Long ownerUserId, Pageable pageable) {
        if (ownerUserId == null) {
            throw new BusinessException("所有者用户ID不能为空");
        }
        return ledgerRepository.findByOwnerUserId(ownerUserId, pageable);
    }

    /**
     * 查询用户可访问的所有账本（包括自己创建和参与的）
     * @param userId 用户ID
     * @return 账本列表
     */
    public List<LedgerEntity> findAccessibleLedgersByUserId(Long userId) {
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }
        return ledgerRepository.findAccessibleLedgersByUserId(userId);
    }

    /**
     * 分页查询用户可访问的所有账本
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 账本分页结果
     */
    public Page<LedgerEntity> findAccessibleLedgersByUserId(Long userId, Pageable pageable) {
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }
        return ledgerRepository.findAccessibleLedgersByUserId(userId, pageable);
    }

    /**
     * 查询用户参与的共享账本
     * @param userId 用户ID
     * @return 共享账本列表
     */
    public List<LedgerEntity> findSharedLedgersByUserId(Long userId) {
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }
        return ledgerRepository.findSharedLedgersByUserId(userId);
    }

    /**
     * 根据账本类型查找账本
     * @param type 账本类型
     * @return 账本列表
     */
    public List<LedgerEntity> findByType(Integer type) {
        if (type == null) {
            throw new BusinessException("账本类型不能为空");
        }
        LedgerTypeEnum.validateCode(type);
        return ledgerRepository.findByType(type);
    }

    /**
     * 更新账本
     * @param id 账本ID
     * @param name 账本名称
     * @param description 账本描述
     * @return 更新后的账本实体
     */
    public LedgerEntity update(Long id, String name, String description) {
        return update(id, name, description, null, null);
    }

    /**
     * 更新账本（完整版本）
     * @param id 账本ID
     * @param name 账本名称
     * @param description 账本描述
     * @param maxMembers 最大成员数
     * @param isPublic 是否公开
     * @return 更新后的账本实体
     */
    public LedgerEntity update(Long id, String name, String description, Integer maxMembers, Boolean isPublic) {
        LedgerEntity ledger = findById(id);

        if (StringUtils.isNotBlank(name)) {
            // 检查同一用户下是否已存在同名账本（排除当前账本）
            if (ledgerRepository.existsByNameAndOwnerUserIdExcludingId(name, ledger.getOwnerUserId(), id)) {
                throw new BusinessException("账本名称已存在");
            }
            ledger.setName(name);
        }

        if (description != null) {
            ledger.setDescription(description);
        }

        // 只有共享账本才能设置成员数限制和公开性
        if (ledger.isShared()) {
            if (maxMembers != null) {
                // 验证最大成员数不能小于当前成员数
                if (maxMembers > 0) {
                    long currentMemberCount = ledgerMemberService.countMembers(id);
                    if (maxMembers < currentMemberCount) {
                        throw new BusinessException("最大成员数不能小于当前成员数（" + currentMemberCount + "）");
                    }
                }
                ledger.setMaxMembers(maxMembers);
            }

            if (isPublic != null) {
                ledger.setIsPublic(isPublic);
            }
        }

        return ledgerRepository.save(ledger);
    }

    /**
     * 删除账本（逻辑删除）
     * @param id 账本ID
     */
    public void delete(Long id) {
        LedgerEntity ledger = findById(id);
        if (ledger.getDeleteTime() != null) {
            throw new BusinessException("账本已删除");
        }
        ledger.setDeleteTime(LocalDateTime.now());
        ledgerRepository.save(ledger);
    }

    /**
     * 永久删除账本（物理删除）
     * @param id 账本ID
     */
    public void deletePermanently(Long id) {
        if (id == null) {
            throw new BusinessException("账本ID不能为空");
        }
        if (!ledgerRepository.existsById(id)) {
            throw new BusinessException("账本不存在");
        }
        ledgerRepository.deleteById(id);
    }
}
