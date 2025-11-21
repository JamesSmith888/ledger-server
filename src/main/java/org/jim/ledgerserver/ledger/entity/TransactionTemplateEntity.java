package org.jim.ledgerserver.ledger.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易模板实体类
 * @author James Smith
 */
@Data
@Entity
@Table(name = "transaction_template")
public class TransactionTemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("模板ID")
    private Long id;

    @Column(name = "user_id", nullable = false)
    @Comment("用户ID")
    private Long userId;

    @Column(nullable = false, length = 100)
    @Comment("模板名称")
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    @Comment("默认金额")
    private BigDecimal amount;

    @Column(nullable = false)
    @Comment("交易类型：1-支出, 2-收入")
    private Integer type;

    @Column(name = "category_id")
    @Comment("分类ID")
    private Long categoryId;

    @Column(name = "payment_method_id")
    @Comment("支付方式ID")
    private Long paymentMethodId;

    @Column(length = 500)
    @Comment("描述")
    private String description;

    @Column(name = "allow_amount_edit")
    @Comment("使用时是否允许修改金额")
    private Boolean allowAmountEdit = true;

    @Column(name = "show_in_quick_panel")
    @Comment("是否显示在快捷面板")
    private Boolean showInQuickPanel = false;

    @Column(name = "sort_order")
    @Comment("排序顺序（数字越小越靠前）")
    private Integer sortOrder = 0;

    @Column(length = 50)
    @Comment("自定义图标名称")
    private String icon;

    @Column(length = 20)
    @Comment("自定义颜色")
    private String color;

    @Column(name = "ledger_id")
    @Comment("默认账本ID")
    private Long ledgerId;

    @Column(name = "create_time", updatable = false)
    @Comment("创建时间")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    @Comment("更新时间")
    private LocalDateTime updateTime;

    @Column(name = "delete_time")
    @Comment("删除时间（逻辑删除）")
    private LocalDateTime deleteTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
