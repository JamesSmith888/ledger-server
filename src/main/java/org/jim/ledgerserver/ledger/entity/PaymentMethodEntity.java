package org.jim.ledgerserver.ledger.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * 支付方式实体类
 * @author James Smith
 */
@Data
@Entity
@Table(name = "payment_method")
public class PaymentMethodEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("支付方式ID")
    private Long id;

    @Column(nullable = false, length = 50)
    @Comment("支付方式名称")
    private String name;

    @Column(length = 10)
    @Comment("支付方式图标（emoji）")
    private String icon;

    @Column(length = 20)
    @Comment("支付方式类型：CASH-现金, ALIPAY-支付宝, WECHAT-微信, BANK_CARD-银行卡, OTHER-其他")
    private String type;

    @Column(name = "user_id", nullable = false)
    @Comment("用户ID")
    private Long userId;

    @Column(name = "is_default")
    @Comment("是否默认支付方式")
    private Boolean isDefault = false;

    @Column(name = "sort_order")
    @Comment("排序顺序（数字越小越靠前）")
    private Integer sortOrder = 0;

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
