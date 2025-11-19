-- 创建反馈表
CREATE TABLE IF NOT EXISTS feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '反馈ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    
    -- 反馈内容
    type VARCHAR(20) NOT NULL COMMENT '反馈类型：需求/优化/BUG',
    title VARCHAR(200) NOT NULL COMMENT '反馈标题',
    description TEXT COMMENT '反馈详细描述',
    
    -- 处理状态
    status VARCHAR(20) DEFAULT '待处理' COMMENT '处理状态：待处理/处理中/已完成/已关闭',
    admin_reply TEXT COMMENT '管理员回复',
    
    -- 时间戳
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    delete_time DATETIME COMMENT '删除时间（逻辑删除）',
    
    INDEX idx_user_id (user_id),
    INDEX idx_type (type),
    INDEX idx_status (status),
    INDEX idx_delete_time (delete_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户反馈表';
