package org.jim.ledgerserver.ledger.repository;

import org.jim.ledgerserver.ledger.entity.BudgetSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 预算设置 Repository
 * @author James Smith
 */
@Repository
public interface BudgetSettingRepository extends JpaRepository<BudgetSettingEntity, Long> {
    
    Optional<BudgetSettingEntity> findByLedgerId(Long ledgerId);
}
