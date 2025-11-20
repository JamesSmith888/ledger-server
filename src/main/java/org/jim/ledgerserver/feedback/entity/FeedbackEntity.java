package org.jim.ledgerserver.feedback.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jim.ledgerserver.base.BaseEntity;

/**
 * 用户反馈实体
 *
 * @author James Smith
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Entity(name = "feedback")
public class FeedbackEntity extends BaseEntity {

    /**
     * 用户ID
     */
    @Column(nullable = false)
    private Long userId;

    /**
     * 用户名
     */
    @Column(length = 100)
    private String userName;

    /**
     * 用户昵称
     */
    @Column(length = 100)
    private String userNickname;

    /**
     * 反馈类型：需求/优化/BUG
     */
    @Column(nullable = false, length = 20)
    private String type;

    /**
     * 反馈标题
     */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * 反馈详细描述
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 处理状态：待处理/处理中/已完成/已关闭
     */
    @Column(length = 20)
    private String status = "待处理";

    /**
     * 管理员回复
     */
    @Column(columnDefinition = "TEXT")
    private String adminReply;
}
