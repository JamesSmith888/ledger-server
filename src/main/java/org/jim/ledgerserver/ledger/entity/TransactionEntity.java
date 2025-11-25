package org.jim.ledgerserver.ledger.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
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
@Table(name = "transaction", indexes = {
    @Index(name = "idx_transaction_ledger_query", columnList = "ledger_id,delete_time,transaction_date_time"),
    @Index(name = "idx_transaction_user_query", columnList = "created_by_user_id,delete_time,transaction_date_time"),
    @Index(name = "idx_transaction_ledger_type", columnList = "ledger_id,type,delete_time"),
    @Index(name = "idx_transaction_user_type", columnList = "created_by_user_id,type,delete_time"),
    @Index(name = "idx_transaction_category", columnList = "category_id,delete_time"),
    @Index(name = "idx_transaction_ledger_datetime", columnList = "ledger_id,transaction_date_time,delete_time"),
    @Index(name = "idx_transaction_user_datetime", columnList = "created_by_user_id,transaction_date_time,delete_time"),
    @Index(name = "idx_transaction_payment_method", columnList = "payment_method_id")
})
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
