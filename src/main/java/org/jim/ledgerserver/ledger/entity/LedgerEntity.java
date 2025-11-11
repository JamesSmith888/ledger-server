package org.jim.ledgerserver.ledger.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jim.ledgerserver.base.BaseEntity;
import org.jim.ledgerserver.common.enums.LedgerTypeEnum;

/**
 * 账本实体类
 * @author James Smith
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Entity(name = "ledger")
public class LedgerEntity extends BaseEntity {

    /**
     * 账本名称
     */
    private String name;

    /**
     * 账本描述
     */
    private String description;

    /**
     * 账本所有者用户ID
     */
    private Long ownerUserId;

    /**
     * 账本类型
     * 1 - PERSONAL - 个人账本
     * 2 - SHARED - 共享账本
     * 3 - BUSINESS - 企业账本
     * 默认为个人账本，保持向后兼容
     * @see LedgerTypeEnum
     */
    @Column(name = "type", columnDefinition = "int default 1")
    private Integer type;

    /**
     * 账本最大成员数限制（仅对共享账本有效）
     * null 表示无限制
     */
    private Integer maxMembers;

    /**
     * 账本是否公开（预留字段）
     * true - 公开，可以被搜索到
     * false - 私有，仅邀请加入
     */
    @Column(name = "is_public", columnDefinition = "boolean default false")
    private Boolean isPublic;

    /**
     * 获取账本类型枚举
     */
    public LedgerTypeEnum getLedgerType() {
        return LedgerTypeEnum.getByCode(this.type);
    }

    /**
     * 设置账本类型
     */
    public void setLedgerType(LedgerTypeEnum ledgerType) {
        this.type = ledgerType != null ? ledgerType.getCode() : LedgerTypeEnum.PERSONAL.getCode();
    }

    /**
     * 判断是否为共享账本
     */
    public boolean isShared() {
        LedgerTypeEnum ledgerType = getLedgerType();
        return ledgerType != null && ledgerType.isShared();
    }

    /**
     * 判断是否为个人账本
     */
    public boolean isPersonal() {
        LedgerTypeEnum ledgerType = getLedgerType();
        return ledgerType == null || ledgerType.isPersonal();
    }

}
