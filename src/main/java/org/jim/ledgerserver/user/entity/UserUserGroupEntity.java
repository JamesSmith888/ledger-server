package org.jim.ledgerserver.user.entity;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.experimental.Accessors;
import org.jim.ledgerserver.base.BaseEntity;

/**
 * 用户、用户组关联信息
 *
 * @author James Smith
 */
@Data
@Accessors(chain = true)
@Entity(name = "userUserGroup")
public class UserUserGroupEntity extends BaseEntity {

    /**
     * 组ID
     */
    private Long groupId;

    /**
     * 用户ID
     */
    private Long userId;

}
