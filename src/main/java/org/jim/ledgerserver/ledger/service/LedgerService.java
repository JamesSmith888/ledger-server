package org.jim.ledgerserver.ledger.service;

import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.jim.ledgerserver.common.exception.BusinessException;
import org.jim.ledgerserver.ledger.entity.LedgerEntity;
import org.jim.ledgerserver.ledger.repository.LedgerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

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

    /**
     * 创建账本
     * @param name 账本名称
     * @param description 账本描述
     * @param ownerUserId 所有者用户ID
     * @return 创建的账本实体
     */
    public LedgerEntity create(String name, String description, Long ownerUserId) {
        if (StringUtils.isBlank(name)) {
            throw new BusinessException("账本名称不能为空");
        }
        if (ownerUserId == null) {
            throw new BusinessException("所有者用户ID不能为空");
        }

        // 检查同一用户下是否已存在同名账本
        ledgerRepository.findByNameAndOwnerUserId(name, ownerUserId)
                .ifPresent(l -> {
                    throw new BusinessException("账本名称已存在");
                });

        LedgerEntity ledger = new LedgerEntity();
        ledger.setName(name);
        ledger.setDescription(description);
        ledger.setOwnerUserId(ownerUserId);
        return ledgerRepository.save(ledger);
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
     * 更新账本
     * @param id 账本ID
     * @param name 账本名称
     * @param description 账本描述
     * @return 更新后的账本实体
     */
    public LedgerEntity update(Long id, String name, String description) {
        LedgerEntity ledger = findById(id);

        if (StringUtils.isNotBlank(name)) {
            // 检查同一用户下是否已存在同名账本（排除当前账本）
            ledgerRepository.findByNameAndOwnerUserId(name, ledger.getOwnerUserId())
                    .ifPresent(l -> {
                        if (!l.getId().equals(id)) {
                            throw new BusinessException("账本名称已存在");
                        }
                    });
            ledger.setName(name);
        }

        if (description != null) {
            ledger.setDescription(description);
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
