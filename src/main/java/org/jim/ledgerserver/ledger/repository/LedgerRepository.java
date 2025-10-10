package org.jim.ledgerserver.ledger.repository;

import org.jim.ledgerserver.ledger.entity.LedgerEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 账本数据访问层
 * @author James Smith
 */
@Repository
public interface LedgerRepository extends JpaRepository<LedgerEntity, Long> {

    /**
     * 根据账本名称查找
     * @param name 账本名称
     * @return 账本实体
     */
    Optional<LedgerEntity> findByName(String name);

    /**
     * 根据所有者用户ID查找所有账本
     * @param ownerUserId 所有者用户ID
     * @return 账本列表
     */
    List<LedgerEntity> findByOwnerUserId(Long ownerUserId);

    /**
     * 根据所有者用户ID分页查找账本
     * @param ownerUserId 所有者用户ID
     * @param pageable 分页参数
     * @return 账本分页结果
     */
    Page<LedgerEntity> findByOwnerUserId(Long ownerUserId, Pageable pageable);

    /**
     * 根据账本名称和所有者用户ID查找
     * @param name 账本名称
     * @param ownerUserId 所有者用户ID
     * @return 账本实体
     */
    Optional<LedgerEntity> findByNameAndOwnerUserId(String name, Long ownerUserId);
}
