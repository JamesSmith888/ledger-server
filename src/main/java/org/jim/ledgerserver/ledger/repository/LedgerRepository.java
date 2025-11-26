package org.jim.ledgerserver.ledger.repository;

import org.jim.ledgerserver.ledger.entity.LedgerEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
     * 根据所有者用户ID查找所有未删除的账本
     * @param ownerUserId 所有者用户ID
     * @return 账本列表
     */
    List<LedgerEntity> findByOwnerUserIdAndDeleteTimeIsNull(Long ownerUserId);

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

    /**
     * 根据账本类型查找账本
     * @param type 账本类型
     * @return 账本列表
     */
    List<LedgerEntity> findByType(Integer type);

    /**
     * 根据账本类型分页查找账本
     * @param type 账本类型
     * @param pageable 分页参数
     * @return 账本分页结果
     */
    Page<LedgerEntity> findByType(Integer type, Pageable pageable);

    /**
     * 根据所有者和账本类型查找账本
     * @param ownerUserId 所有者用户ID
     * @param type 账本类型
     * @return 账本列表
     */
    List<LedgerEntity> findByOwnerUserIdAndType(Long ownerUserId, Integer type);

    /**
     * 查找用户可访问的所有账本（包括自己创建和参与的）
     * @param userId 用户ID
     * @return 账本列表
     */
    @Query("SELECT DISTINCT l FROM ledger l " +
           "LEFT JOIN ledger_member lm ON l.id = lm.ledgerId " +
           "WHERE (l.ownerUserId = :userId OR (lm.userId = :userId AND lm.deleteTime IS NULL)) " +
           "AND l.deleteTime IS NULL " +
           "ORDER BY l.type ASC, l.createTime DESC")
    List<LedgerEntity> findAccessibleLedgersByUserId(@Param("userId") Long userId);

    /**
     * 分页查找用户可访问的所有账本
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 账本分页结果
     */
    @Query("SELECT DISTINCT l FROM ledger l " +
           "LEFT JOIN ledger_member lm ON l.id = lm.ledgerId " +
           "WHERE (l.ownerUserId = :userId OR (lm.userId = :userId AND lm.deleteTime IS NULL)) " +
           "AND l.deleteTime IS NULL " +
           "ORDER BY l.type ASC, l.createTime DESC")
    Page<LedgerEntity> findAccessibleLedgersByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * 查找用户参与的共享账本
     * @param userId 用户ID
     * @return 共享账本列表
     */
    @Query("SELECT DISTINCT l FROM ledger l " +
           "JOIN ledger_member lm ON l.id = lm.ledgerId " +
           "WHERE lm.userId = :userId AND l.type IN (2, 3) " +
           "AND l.deleteTime IS NULL AND lm.deleteTime IS NULL " +
           "ORDER BY l.createTime DESC")
    List<LedgerEntity> findSharedLedgersByUserId(@Param("userId") Long userId);

    /**
     * 查找公开的共享账本（预留功能）
     * @param type 账本类型
     * @return 公开账本列表
     */
    @Query("SELECT l FROM ledger l WHERE l.type = :type AND l.isPublic = true AND l.deleteTime IS NULL ORDER BY l.createTime DESC")
    List<LedgerEntity> findPublicLedgersByType(@Param("type") Integer type);

    /**
     * 统计用户拥有的账本数量
     * @param ownerUserId 所有者用户ID
     * @return 账本数量
     */
    long countByOwnerUserId(Long ownerUserId);

    /**
     * 统计指定类型的账本数量
     * @param type 账本类型
     * @return 账本数量
     */
    long countByType(Integer type);

    /**
     * 检查账本名称在用户范围内是否重复
     * @param name 账本名称
     * @param ownerUserId 所有者用户ID
     * @param excludeId 排除的账本ID（用于更新时检查）
     * @return 是否存在重复
     */
    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM ledger l " +
           "WHERE l.name = :name AND l.ownerUserId = :ownerUserId " +
           "AND (:excludeId IS NULL OR l.id != :excludeId) " +
           "AND l.deleteTime IS NULL")
    boolean existsByNameAndOwnerUserIdExcludingId(@Param("name") String name, 
                                                  @Param("ownerUserId") Long ownerUserId, 
                                                  @Param("excludeId") Long excludeId);
}
