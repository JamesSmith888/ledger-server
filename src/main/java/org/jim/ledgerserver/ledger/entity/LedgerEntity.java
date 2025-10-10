package org.jim.ledgerserver.ledger.entity;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.experimental.Accessors;
import org.jim.ledgerserver.base.BaseEntity;

/**
 * 账本实体类
 * @author James Smith
 */
@Data
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



}
