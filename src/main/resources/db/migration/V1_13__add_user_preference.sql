-- 用户偏好记忆表
CREATE TABLE IF NOT EXISTS user_preference (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    type VARCHAR(50) NOT NULL COMMENT '偏好类型: CATEGORY_MAPPING, MERCHANT_ALIAS, AMOUNT_PATTERN, PAYMENT_PREFERENCE, CUSTOM_CORRECTION',
    keyword VARCHAR(100) NOT NULL COMMENT '触发关键词',
    correction VARCHAR(200) NOT NULL COMMENT '正确的理解/分类',
    note VARCHAR(500) COMMENT '附加说明',
    category_id BIGINT COMMENT '相关分类ID',
    usage_count INT DEFAULT 1 COMMENT '使用次数',
    enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    delete_time DATETIME COMMENT '删除时间',
    
    INDEX idx_user_id (user_id),
    INDEX idx_user_keyword (user_id, keyword),
    INDEX idx_user_type (user_id, type),
    INDEX idx_usage (user_id, usage_count DESC, update_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户偏好记忆';
