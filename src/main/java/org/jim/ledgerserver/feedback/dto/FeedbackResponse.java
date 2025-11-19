package org.jim.ledgerserver.feedback.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 反馈响应
 *
 * @author James Smith
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackResponse {
    /**
     * 反馈ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 反馈类型：需求/优化/BUG
     */
    private String type;

    /**
     * 反馈标题
     */
    private String title;

    /**
     * 反馈详细描述
     */
    private String description;

    /**
     * 处理状态：待处理/处理中/已完成/已关闭
     */
    private String status;

    /**
     * 管理员回复
     */
    private String adminReply;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
