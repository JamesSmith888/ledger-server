package org.jim.ledgerserver.user.entity;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.experimental.Accessors;
import org.jim.ledgerserver.base.BaseEntity;

/**
 * 用户组基本信息
 *
 * @author James Smith
 */
@Data
@Accessors(chain = true)
@Entity(name = "userGroup")
public class UserGroupEntity extends BaseEntity {

    /**
     * 组名称
     */
    private String name;

    /**
     * 组描述
     */
    private String description;

    /**
     * 组所有者用户ID
     */
    private Long ownerUserId;

}
