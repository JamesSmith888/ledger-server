package org.jim.ledgerserver.ledger.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jim.ledgerserver.base.BaseEntity;
import org.jim.ledgerserver.common.enums.TransactionTypeEnum;

/**
 * 交易分类实体类
 * @author James Smith
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Entity(name = "category")
public class CategoryEntity extends BaseEntity {

    /**
     * 分类名称
     */
    @Column(nullable = false, length = 50)
    private String name;

    /**
     * 分类图标（emoji或图标代码）
     */
    @Column(length = 50)
    private String icon;

    /**
     * 分类颜色（十六进制颜色代码）
     */
    @Column(length = 10)
    private String color;

    /**
     * 分类类型
     * 1 - INCOME - 收入分类
     * 2 - EXPENSE - 支出分类
     * @see TransactionTypeEnum
     */
    @Column(nullable = false)
    private Integer type;

    /**
     * 排序顺序
     */
    @Column(name = "sort_order")
    private Integer sortOrder;

    /**
     * 是否为系统预设分类
     * true - 系统预设，不可删除
     * false - 用户自定义，可删除
     */
    @Column(name = "is_system", nullable = false, columnDefinition = "boolean default false")
    private Boolean isSystem = false;

    /**
     * 创建该分类的用户ID（系统预设分类此字段为null）
     */
    private Long createdByUserId;

    /**
     * 分类描述
     */
    @Column(length = 200)
    private String description;
}
