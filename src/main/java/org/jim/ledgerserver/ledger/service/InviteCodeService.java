package org.jim.ledgerserver.ledger.service;

import jakarta.annotation.Resource;
import org.apache.commons.lang3.RandomStringUtils;
import org.jim.ledgerserver.common.enums.LedgerMemberRoleEnum;
import org.jim.ledgerserver.common.exception.BusinessException;
import org.jim.ledgerserver.ledger.entity.InviteCodeEntity;
import org.jim.ledgerserver.ledger.entity.InviteRecordEntity;
import org.jim.ledgerserver.ledger.entity.LedgerEntity;
import org.jim.ledgerserver.ledger.entity.LedgerMemberEntity;
import org.jim.ledgerserver.ledger.repository.InviteCodeRepository;
import org.jim.ledgerserver.ledger.repository.InviteRecordRepository;
import org.jim.ledgerserver.ledger.repository.LedgerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 邀请码业务逻辑层
 * 处理账本邀请相关的所有业务逻辑
 * 
 * @author James Smith
 */
@Service
public class InviteCodeService {

    @Resource
    private InviteCodeRepository inviteCodeRepository;

    @Resource
    private InviteRecordRepository inviteRecordRepository;

    @Resource
    private LedgerRepository ledgerRepository;

    @Resource
    private LedgerMemberService ledgerMemberService;

    /**
     * 生成邀请码
     * 
     * @param ledgerId 账本ID
     * @param createdByUserId 创建者用户ID
     * @param role 邀请角色
     * @param maxUses 最大使用次数（-1表示无限制）
     * @param expireHours 过期时间（小时，null表示永不过期）
     * @return 邀请码实体
     */
    @Transactional
    public InviteCodeEntity generateInviteCode(Long ledgerId, Long createdByUserId, 
                                               LedgerMemberRoleEnum role, Integer maxUses, 
                                               Integer expireHours) {
        // 参数验证
        validateGenerateParams(ledgerId, createdByUserId, role, maxUses);

        // 验证账本是否存在且为共享账本
        validateLedgerForInvite(ledgerId);

        // 验证创建者权限
        validateInviterPermission(ledgerId, createdByUserId);

        // 生成唯一邀请码
        String code = generateUniqueCode();

        // 计算过期时间
        LocalDateTime expireTime = null;
        if (expireHours != null && expireHours > 0) {
            expireTime = LocalDateTime.now().plusHours(expireHours);
        }

        // 创建邀请码实体
        InviteCodeEntity inviteCode = new InviteCodeEntity();
        inviteCode.setCode(code);
        inviteCode.setLedgerId(ledgerId);
        inviteCode.setCreatedByUserId(createdByUserId);
        inviteCode.setInviteRole(role);
        inviteCode.setMaxUses(maxUses != null ? maxUses : 1);
        inviteCode.setUsedCount(0);
        inviteCode.setExpireTime(expireTime);
        inviteCode.enable();

        return inviteCodeRepository.save(inviteCode);
    }

    /**
     * 验证邀请码
     * 检查邀请码是否有效、未过期、未达到使用上限
     * 
     * @param code 邀请码
     * @return 邀请码实体
     */
    public InviteCodeEntity validateInviteCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new BusinessException("邀请码不能为空");
        }

        InviteCodeEntity inviteCode = inviteCodeRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException("邀请码不存在或已失效"));

        if (!inviteCode.isValid()) {
            throw new BusinessException("邀请码已被禁用");
        }

        if (inviteCode.isExpired()) {
            throw new BusinessException("邀请码已过期");
        }

        if (inviteCode.isExhausted()) {
            throw new BusinessException("邀请码使用次数已达上限");
        }

        return inviteCode;
    }

    /**
     * 使用邀请码加入账本
     * 
     * @param code 邀请码
     * @param userId 用户ID
     * @return 创建的成员关系
     */
    @Transactional
    public LedgerMemberEntity acceptInvite(String code, Long userId) {
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }

        // 验证邀请码
        InviteCodeEntity inviteCode = validateInviteCode(code);

        // 检查用户是否已经是成员
        if (ledgerMemberService.findMemberRelation(inviteCode.getLedgerId(), userId).isPresent()) {
            throw new BusinessException("您已经是该账本的成员");
        }

        // 检查用户是否已使用过这个邀请码
        if (inviteRecordRepository.existsByInviteCodeIdAndUserId(inviteCode.getId(), userId)) {
            throw new BusinessException("您已使用过此邀请码");
        }

        // 添加成员
        LedgerMemberEntity member = ledgerMemberService.addMember(
                inviteCode.getLedgerId(),
                userId,
                inviteCode.getInviteRole(),
                inviteCode.getCreatedByUserId(),
                "通过邀请码加入"
        );

        // 记录使用
        recordInviteUsage(inviteCode, userId);

        // 增加使用次数
        inviteCode.incrementUsedCount();
        inviteCodeRepository.save(inviteCode);

        return member;
    }

    /**
     * 获取账本的所有邀请码
     * 
     * @param ledgerId 账本ID
     * @param includeInactive 是否包含已禁用的邀请码
     * @return 邀请码列表
     */
    public List<InviteCodeEntity> getInviteCodes(Long ledgerId, boolean includeInactive) {
        if (ledgerId == null) {
            throw new BusinessException("账本ID不能为空");
        }

        if (includeInactive) {
            return inviteCodeRepository.findByLedgerId(ledgerId);
        } else {
            return inviteCodeRepository.findActiveByLedgerId(ledgerId);
        }
    }

    /**
     * 根据ID获取邀请码
     * 
     * @param inviteCodeId 邀请码ID
     * @return 邀请码实体
     */
    public InviteCodeEntity getById(Long inviteCodeId) {
        if (inviteCodeId == null) {
            throw new BusinessException("邀请码ID不能为空");
        }

        return inviteCodeRepository.findById(inviteCodeId)
                .orElseThrow(() -> new BusinessException("邀请码不存在"));
    }

    /**
     * 禁用邀请码
     * 
     * @param inviteCodeId 邀请码ID
     * @param operatorUserId 操作者用户ID
     */
    @Transactional
    public void disableInviteCode(Long inviteCodeId, Long operatorUserId) {
        InviteCodeEntity inviteCode = getById(inviteCodeId);

        // 验证操作权限
        validateManagePermission(inviteCode.getLedgerId(), operatorUserId);

        inviteCode.disable();
        inviteCodeRepository.save(inviteCode);
    }

    /**
     * 删除邀请码（逻辑删除）
     * 
     * @param inviteCodeId 邀请码ID
     * @param operatorUserId 操作者用户ID
     */
    @Transactional
    public void deleteInviteCode(Long inviteCodeId, Long operatorUserId) {
        InviteCodeEntity inviteCode = getById(inviteCodeId);

        // 验证操作权限
        validateManagePermission(inviteCode.getLedgerId(), operatorUserId);

        inviteCode.setDeleteTime(LocalDateTime.now());
        inviteCodeRepository.save(inviteCode);
    }

    /**
     * 获取邀请码的使用记录
     * 
     * @param inviteCodeId 邀请码ID
     * @return 使用记录列表
     */
    public List<InviteRecordEntity> getInviteRecords(Long inviteCodeId) {
        if (inviteCodeId == null) {
            throw new BusinessException("邀请码ID不能为空");
        }

        return inviteRecordRepository.findByInviteCodeId(inviteCodeId);
    }

    /**
     * 生成唯一邀请码
     * 使用 UUID 的短格式 + 随机字符串
     */
    private String generateUniqueCode() {
        String code;
        int maxAttempts = 10;
        int attempt = 0;

        do {
            // 生成12位随机字符串（大小写字母+数字）
            code = RandomStringUtils.insecure().nextAlphanumeric(12).toUpperCase();
            attempt++;

            if (attempt >= maxAttempts) {
                // 如果多次尝试仍然冲突，使用 UUID 保证唯一性
                code = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
                break;
            }
        } while (inviteCodeRepository.existsByCode(code));

        return code;
    }

    /**
     * 记录邀请码使用
     */
    private void recordInviteUsage(InviteCodeEntity inviteCode, Long userId) {
        InviteRecordEntity record = new InviteRecordEntity();
        record.setInviteCodeId(inviteCode.getId());
        record.setLedgerId(inviteCode.getLedgerId());
        record.setUserId(userId);
        record.setUseTime(LocalDateTime.now());

        inviteRecordRepository.save(record);
    }

    /**
     * 验证生成参数
     */
    private void validateGenerateParams(Long ledgerId, Long createdByUserId, 
                                       LedgerMemberRoleEnum role, Integer maxUses) {
        if (ledgerId == null) {
            throw new BusinessException("账本ID不能为空");
        }
        if (createdByUserId == null) {
            throw new BusinessException("创建者用户ID不能为空");
        }
        if (role == null) {
            throw new BusinessException("邀请角色不能为空");
        }
        if (role == LedgerMemberRoleEnum.OWNER) {
            throw new BusinessException("不能通过邀请码成为所有者");
        }
        if (maxUses != null && maxUses != -1 && maxUses < 1) {
            throw new BusinessException("最大使用次数必须大于0或为-1（无限制）");
        }
    }

    /**
     * 验证账本是否可以邀请
     */
    private LedgerEntity validateLedgerForInvite(Long ledgerId) {
        LedgerEntity ledger = ledgerRepository.findById(ledgerId)
                .orElseThrow(() -> new BusinessException("账本不存在"));

        if (ledger.getDeleteTime() != null) {
            throw new BusinessException("账本已删除");
        }

        if (ledger.isPersonal()) {
            throw new BusinessException("个人账本不支持邀请成员");
        }

        return ledger;
    }

    /**
     * 验证邀请者权限
     */
    private void validateInviterPermission(Long ledgerId, Long userId) {
        if (!ledgerMemberService.hasManagePermission(ledgerId, userId)) {
            throw new BusinessException("无权限生成邀请码");
        }
    }

    /**
     * 验证管理权限
     */
    private void validateManagePermission(Long ledgerId, Long userId) {
        if (!ledgerMemberService.hasManagePermission(ledgerId, userId)) {
            throw new BusinessException("无权限管理邀请码");
        }
    }
}
