package org.jim.ledgerserver.ledger.repository;

import org.jim.ledgerserver.ledger.entity.LedgerMemberEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 账本成员关系数据访问层
 * @author James Smith
 */
@Repository
public interface LedgerMemberRepository extends JpaRepository<LedgerMemberEntity, Long> {

    /**
     * 根据账本ID查找所有成员
     */
    @Query("SELECT lm FROM ledger_member lm WHERE lm.ledgerId = :ledgerId AND lm.deleteTime IS NULL ORDER BY lm.role ASC, lm.joinedAt ASC")
    List<LedgerMemberEntity> findByLedgerId(@Param("ledgerId") Long ledgerId);

    /**
     * 根据账本ID分页查找成员
     */
    @Query("SELECT lm FROM ledger_member lm WHERE lm.ledgerId = :ledgerId AND lm.deleteTime IS NULL ORDER BY lm.role ASC, lm.joinedAt ASC")
    Page<LedgerMemberEntity> findByLedgerId(@Param("ledgerId") Long ledgerId, Pageable pageable);

    /**
     * 根据用户ID查找所有参与的账本
     */
    @Query("SELECT lm FROM ledger_member lm WHERE lm.userId = :userId AND lm.deleteTime IS NULL ORDER BY lm.role ASC, lm.joinedAt DESC")
    List<LedgerMemberEntity> findByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID分页查找参与的账本
     */
    @Query("SELECT lm FROM ledger_member lm WHERE lm.userId = :userId AND lm.deleteTime IS NULL ORDER BY lm.role ASC, lm.joinedAt DESC")
    Page<LedgerMemberEntity> findByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * 查找特定账本中的特定用户关系
     */
    @Query("SELECT lm FROM ledger_member lm WHERE lm.ledgerId = :ledgerId AND lm.userId = :userId AND lm.deleteTime IS NULL")
    Optional<LedgerMemberEntity> findByLedgerIdAndUserId(@Param("ledgerId") Long ledgerId, @Param("userId") Long userId);

    /**
     * 根据账本ID和状态查找成员
     */
    @Query("SELECT lm FROM ledger_member lm WHERE lm.ledgerId = :ledgerId AND lm.status = :status AND lm.deleteTime IS NULL ORDER BY lm.role ASC, lm.joinedAt ASC")
    List<LedgerMemberEntity> findByLedgerIdAndStatus(@Param("ledgerId") Long ledgerId, @Param("status") Integer status);

    /**
     * 根据账本ID和角色查找成员
     */
    @Query("SELECT lm FROM ledger_member lm WHERE lm.ledgerId = :ledgerId AND lm.role = :role AND lm.deleteTime IS NULL ORDER BY lm.joinedAt ASC")
    List<LedgerMemberEntity> findByLedgerIdAndRole(@Param("ledgerId") Long ledgerId, @Param("role") Integer role);

    /**
     * 查找账本的所有者
     */
    @Query("SELECT lm FROM ledger_member lm WHERE lm.ledgerId = :ledgerId AND lm.role = 1 AND lm.deleteTime IS NULL")
    Optional<LedgerMemberEntity> findOwnerByLedgerId(@Param("ledgerId") Long ledgerId);

    /**
     * 查找账本的管理员（包括所有者）
     */
    @Query("SELECT lm FROM ledger_member lm WHERE lm.ledgerId = :ledgerId AND lm.role IN (1, 2) AND lm.deleteTime IS NULL ORDER BY lm.role ASC, lm.joinedAt ASC")
    List<LedgerMemberEntity> findManagersByLedgerId(@Param("ledgerId") Long ledgerId);

    /**
     * 统计账本成员数量
     */
    @Query("SELECT COUNT(lm) FROM ledger_member lm WHERE lm.ledgerId = :ledgerId AND lm.deleteTime IS NULL")
    long countByLedgerId(@Param("ledgerId") Long ledgerId);

    /**
     * 统计账本活跃成员数量
     */
    @Query("SELECT COUNT(lm) FROM ledger_member lm WHERE lm.ledgerId = :ledgerId AND lm.status = 1 AND lm.deleteTime IS NULL")
    long countActiveMembersByLedgerId(@Param("ledgerId") Long ledgerId);

    /**
     * 统计用户参与的账本数量
     */
    @Query("SELECT COUNT(lm) FROM ledger_member lm WHERE lm.userId = :userId AND lm.deleteTime IS NULL")
    long countByUserId(@Param("userId") Long userId);

    /**
     * 统计用户作为所有者的账本数量
     */
    @Query("SELECT COUNT(lm) FROM ledger_member lm WHERE lm.userId = :userId AND lm.role = 1 AND lm.deleteTime IS NULL")
    long countOwnedLedgersByUserId(@Param("userId") Long userId);

    /**
     * 检查用户是否已经是账本成员
     */
    @Query("SELECT CASE WHEN COUNT(lm) > 0 THEN true ELSE false END FROM ledger_member lm WHERE lm.ledgerId = :ledgerId AND lm.userId = :userId AND lm.deleteTime IS NULL")
    boolean existsByLedgerIdAndUserId(@Param("ledgerId") Long ledgerId, @Param("userId") Long userId);

    /**
     * 检查用户是否有指定账本的管理权限
     */
    @Query("SELECT CASE WHEN COUNT(lm) > 0 THEN true ELSE false END FROM ledger_member lm WHERE lm.ledgerId = :ledgerId AND lm.userId = :userId AND lm.role IN (1, 2) AND lm.status = 1 AND lm.deleteTime IS NULL")
    boolean hasManagePermission(@Param("ledgerId") Long ledgerId, @Param("userId") Long userId);

    /**
     * 检查用户是否有指定账本的编辑权限
     */
    @Query("SELECT CASE WHEN COUNT(lm) > 0 THEN true ELSE false END FROM ledger_member lm WHERE lm.ledgerId = :ledgerId AND lm.userId = :userId AND lm.role IN (1, 2, 3) AND lm.status = 1 AND lm.deleteTime IS NULL")
    boolean hasEditPermission(@Param("ledgerId") Long ledgerId, @Param("userId") Long userId);

    /**
     * 检查用户是否有指定账本的查看权限
     */
    @Query("SELECT CASE WHEN COUNT(lm) > 0 THEN true ELSE false END FROM ledger_member lm WHERE lm.ledgerId = :ledgerId AND lm.userId = :userId AND lm.status = 1 AND lm.deleteTime IS NULL")
    boolean hasViewPermission(@Param("ledgerId") Long ledgerId, @Param("userId") Long userId);

    /**
     * 根据邀请者查找成员
     */
    @Query("SELECT lm FROM ledger_member lm WHERE lm.invitedByUserId = :invitedByUserId AND lm.deleteTime IS NULL ORDER BY lm.joinedAt DESC")
    List<LedgerMemberEntity> findByInvitedByUserId(@Param("invitedByUserId") Long invitedByUserId);

    /**
     * 查找待确认的邀请
     */
    @Query("SELECT lm FROM ledger_member lm WHERE lm.userId = :userId AND lm.status = 3 AND lm.deleteTime IS NULL ORDER BY lm.createTime DESC")
    List<LedgerMemberEntity> findPendingInvitationsByUserId(@Param("userId") Long userId);
}