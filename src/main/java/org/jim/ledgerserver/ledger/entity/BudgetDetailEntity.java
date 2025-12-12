package org.jim.ledgerserver.ledger.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jim.ledgerserver.base.BaseEntity;

import java.math.BigDecimal;

/**
 * 预算明细实体 (分类预算)
 * @author James Smith
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Entity(name = "budget_detail")
public class BudgetDetailEntity extends BaseEntity {

    /**
     * 关联账本ID (冗余字段，方便查询)
     */
    @Column(nullable = false)
    private Long ledgerId;

    /**
     * 关联分类ID
     */
    @Column(nullable = false)
    private Long categoryId;

    /**
     * 预算金额
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
}
