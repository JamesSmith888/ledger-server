package org.jim.ledgerserver.ledger.entity;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jim.ledgerserver.base.BaseEntity;
import org.jim.ledgerserver.common.enums.TransactionTypeEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易/账目实体类
 * @author James Smith
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Entity(name = "transaction")
public class TransactionEntity extends BaseEntity {

    /**
     * 交易名称
     */
    private String name;

    /**
     * 交易描述
     */
    private String description;

    /**
     * 交易金额
     */
    private BigDecimal amount;

    /**
     * 交易类型，收入或支出
     * 1 - INCOME - 收入
     * 2 - EXPENSE - 支出
     * @see TransactionTypeEnum
     */
    private Integer type;

    /**
     * 交易日期时间
     */
    private LocalDateTime transactionDateTime;

    /**
     * 关联的账本ID(非必填)
     */
    private Long ledgerId;

    /**
     * 创建该交易的用户ID
     */
    private Long createdByUserId;

    /**
     * 交易分类ID（可选）
     */
    private Long categoryId;

    /**
     * 支付方式ID（可选）
     */
    private Long paymentMethodId;

}
