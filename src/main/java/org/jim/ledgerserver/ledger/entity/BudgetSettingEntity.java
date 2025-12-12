package org.jim.ledgerserver.ledger.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jim.ledgerserver.base.BaseEntity;

import java.math.BigDecimal;

/**
 * 预算设置实体
 * @author James Smith
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Entity(name = "budget_setting")
public class BudgetSettingEntity extends BaseEntity {

    /**
     * 关联账本ID
     */
    @Column(nullable = false)
    private Long ledgerId;

    /**
     * 月度总预算金额
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    /**
     * 预算周期类型 (目前仅支持 MONTHLY)
     */
    @Column(nullable = false, length = 20)
    private String periodType = "MONTHLY";

    /**
     * 起始日 (默认1号)
     */
    @Column(nullable = false)
    private Integer startDay = 1;
}
