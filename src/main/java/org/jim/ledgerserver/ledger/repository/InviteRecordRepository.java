package org.jim.ledgerserver.ledger.repository;

import org.jim.ledgerserver.ledger.entity.InviteRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 邀请记录数据访问层
 * @author James Smith
 */
@Repository
public interface InviteRecordRepository extends JpaRepository<InviteRecordEntity, Long> {

    /**
     * 根据邀请码ID查找所有使用记录
     */
    @Query("SELECT ir FROM ledger_invite_record ir WHERE ir.inviteCodeId = :inviteCodeId ORDER BY ir.useTime DESC")
    List<InviteRecordEntity> findByInviteCodeId(@Param("inviteCodeId") Long inviteCodeId);

    /**
     * 根据账本ID查找所有使用记录
     */
    @Query("SELECT ir FROM ledger_invite_record ir WHERE ir.ledgerId = :ledgerId ORDER BY ir.useTime DESC")
    List<InviteRecordEntity> findByLedgerId(@Param("ledgerId") Long ledgerId);

    /**
     * 根据用户ID查找所有使用记录
     */
    @Query("SELECT ir FROM ledger_invite_record ir WHERE ir.userId = :userId ORDER BY ir.useTime DESC")
    List<InviteRecordEntity> findByUserId(@Param("userId") Long userId);

    /**
     * 统计邀请码的使用次数
     */
    @Query("SELECT COUNT(ir) FROM ledger_invite_record ir WHERE ir.inviteCodeId = :inviteCodeId")
    long countByInviteCodeId(@Param("inviteCodeId") Long inviteCodeId);

    /**
     * 检查用户是否已使用过某个邀请码
     */
    @Query("SELECT CASE WHEN COUNT(ir) > 0 THEN true ELSE false END FROM ledger_invite_record ir WHERE ir.inviteCodeId = :inviteCodeId AND ir.userId = :userId")
    boolean existsByInviteCodeIdAndUserId(@Param("inviteCodeId") Long inviteCodeId, @Param("userId") Long userId);

    /**
     * 检查用户是否已加入某个账本（通过任意邀请码）
     */
    @Query("SELECT CASE WHEN COUNT(ir) > 0 THEN true ELSE false END FROM ledger_invite_record ir WHERE ir.ledgerId = :ledgerId AND ir.userId = :userId")
    boolean existsByLedgerIdAndUserId(@Param("ledgerId") Long ledgerId, @Param("userId") Long userId);
}
