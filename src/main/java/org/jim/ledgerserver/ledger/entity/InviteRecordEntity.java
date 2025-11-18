package org.jim.ledgerserver.ledger.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jim.ledgerserver.base.BaseEntity;

import java.time.LocalDateTime;

/**
 * 邀请码使用记录实体类
 * 记录每次邀请码的使用情况
 * 
 * @author James Smith
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Entity(name = "ledger_invite_record")
public class InviteRecordEntity extends BaseEntity {

    /**
     * 邀请码ID
     */
    @Column(name = "invite_code_id", nullable = false)
    private Long inviteCodeId;

    /**
     * 账本ID
     */
    @Column(name = "ledger_id", nullable = false)
    private Long ledgerId;

    /**
     * 使用者用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 使用时间
     */
    @Column(name = "use_time", columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime useTime;

    @PrePersist
    public void prePersist() {
        if (this.useTime == null) {
            this.useTime = LocalDateTime.now();
        }
    }
}
