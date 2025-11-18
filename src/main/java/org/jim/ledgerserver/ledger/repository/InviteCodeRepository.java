package org.jim.ledgerserver.ledger.repository;

import org.jim.ledgerserver.ledger.entity.InviteCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 邀请码数据访问层
 * @author James Smith
 */
@Repository
public interface InviteCodeRepository extends JpaRepository<InviteCodeEntity, Long> {

    /**
     * 根据邀请码查找
     */
    @Query("SELECT ic FROM ledger_invite_code ic WHERE ic.code = :code AND ic.deleteTime IS NULL")
    Optional<InviteCodeEntity> findByCode(@Param("code") String code);

    /**
     * 根据账本ID查找所有有效的邀请码
     */
    @Query("SELECT ic FROM ledger_invite_code ic WHERE ic.ledgerId = :ledgerId AND ic.deleteTime IS NULL ORDER BY ic.createTime DESC")
    List<InviteCodeEntity> findByLedgerId(@Param("ledgerId") Long ledgerId);

    /**
     * 根据账本ID查找所有有效且未禁用的邀请码
     */
    @Query("SELECT ic FROM ledger_invite_code ic WHERE ic.ledgerId = :ledgerId AND ic.status = 1 AND ic.deleteTime IS NULL ORDER BY ic.createTime DESC")
    List<InviteCodeEntity> findActiveByLedgerId(@Param("ledgerId") Long ledgerId);

    /**
     * 根据创建者用户ID查找邀请码
     */
    @Query("SELECT ic FROM ledger_invite_code ic WHERE ic.createdByUserId = :userId AND ic.deleteTime IS NULL ORDER BY ic.createTime DESC")
    List<InviteCodeEntity> findByCreatedByUserId(@Param("userId") Long userId);

    /**
     * 查找即将过期的邀请码（用于提醒）
     */
    @Query("SELECT ic FROM ledger_invite_code ic WHERE ic.expireTime IS NOT NULL AND ic.expireTime BETWEEN :startTime AND :endTime AND ic.status = 1 AND ic.deleteTime IS NULL")
    List<InviteCodeEntity> findExpiringCodes(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 检查邀请码是否存在
     */
    @Query("SELECT CASE WHEN COUNT(ic) > 0 THEN true ELSE false END FROM ledger_invite_code ic WHERE ic.code = :code AND ic.deleteTime IS NULL")
    boolean existsByCode(@Param("code") String code);

    /**
     * 统计账本的有效邀请码数量
     */
    @Query("SELECT COUNT(ic) FROM ledger_invite_code ic WHERE ic.ledgerId = :ledgerId AND ic.status = 1 AND ic.deleteTime IS NULL")
    long countActiveByLedgerId(@Param("ledgerId") Long ledgerId);
}
