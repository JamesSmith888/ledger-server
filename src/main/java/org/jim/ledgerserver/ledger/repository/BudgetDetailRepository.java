package org.jim.ledgerserver.ledger.repository;

import org.jim.ledgerserver.ledger.entity.BudgetDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 预算明细 Repository
 * @author James Smith
 */
@Repository
public interface BudgetDetailRepository extends JpaRepository<BudgetDetailEntity, Long> {
    
    List<BudgetDetailEntity> findByLedgerId(Long ledgerId);
    
    Optional<BudgetDetailEntity> findByLedgerIdAndCategoryId(Long ledgerId, Long categoryId);
    
    void deleteByLedgerId(Long ledgerId);
}
