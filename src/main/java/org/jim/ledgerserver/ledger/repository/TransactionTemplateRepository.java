package org.jim.ledgerserver.ledger.repository;

import org.jim.ledgerserver.ledger.entity.TransactionTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 交易模板仓储接口
 * @author James Smith
 */
@Repository
public interface TransactionTemplateRepository extends JpaRepository<TransactionTemplateEntity, Long> {

    /**
     * 查询用户的所有未删除模板（按排序顺序）
     */
    @Query("SELECT t FROM TransactionTemplateEntity t WHERE t.userId = ?1 AND t.deleteTime IS NULL ORDER BY t.sortOrder ASC, t.createTime ASC")
    List<TransactionTemplateEntity> findByUserIdAndDeleteTimeIsNull(Long userId);

    /**
     * 查询用户在快捷面板显示的模板（按排序顺序）
     */
    @Query("SELECT t FROM TransactionTemplateEntity t WHERE t.userId = ?1 AND t.showInQuickPanel = true AND t.deleteTime IS NULL ORDER BY t.sortOrder ASC, t.createTime ASC")
    List<TransactionTemplateEntity> findQuickPanelTemplates(Long userId);

    /**
     * 根据ID和用户ID查询
     */
    @Query("SELECT t FROM TransactionTemplateEntity t WHERE t.id = ?1 AND t.userId = ?2 AND t.deleteTime IS NULL")
    Optional<TransactionTemplateEntity> findByIdAndUserId(Long id, Long userId);
}
