package org.jim.ledgerserver.ledger.repository;

import org.jim.ledgerserver.ledger.entity.PaymentMethodEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 支付方式仓储接口
 * @author James Smith
 */
@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethodEntity, Long> {

    /**
     * 查询用户的所有未删除支付方式（按排序顺序）
     */
    @Query("SELECT p FROM PaymentMethodEntity p WHERE p.userId = ?1 AND p.deleteTime IS NULL ORDER BY p.sortOrder ASC, p.createTime ASC")
    List<PaymentMethodEntity> findByUserIdAndDeleteTimeIsNull(Long userId);

    /**
     * 查询用户的默认支付方式
     */
    @Query("SELECT p FROM PaymentMethodEntity p WHERE p.userId = ?1 AND p.isDefault = true AND p.deleteTime IS NULL")
    Optional<PaymentMethodEntity> findDefaultByUserId(Long userId);

    /**
     * 根据ID和用户ID查询
     */
    @Query("SELECT p FROM PaymentMethodEntity p WHERE p.id = ?1 AND p.userId = ?2 AND p.deleteTime IS NULL")
    Optional<PaymentMethodEntity> findByIdAndUserId(Long id, Long userId);
}
