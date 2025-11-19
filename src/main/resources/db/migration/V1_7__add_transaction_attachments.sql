-- 创建交易附件表
CREATE TABLE IF NOT EXISTS transaction_attachment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '附件ID',
    transaction_id BIGINT NOT NULL COMMENT '交易ID',
    file_name VARCHAR(255) NOT NULL COMMENT '文件名',
    file_type VARCHAR(100) NOT NULL COMMENT '文件类型（MIME type）',
    file_size BIGINT NOT NULL COMMENT '文件大小（字节）',
    file_data MEDIUMBLOB NOT NULL COMMENT '文件数据（最大16MB）',
    thumbnail_data BLOB COMMENT '缩略图数据（仅图片类型，最大64KB）',
    width INT COMMENT '图片宽度（仅图片类型）',
    height INT COMMENT '图片高度（仅图片类型）',
    uploaded_by_user_id BIGINT NOT NULL COMMENT '上传用户ID',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    delete_time DATETIME COMMENT '删除时间（逻辑删除）',
    INDEX idx_transaction_id (transaction_id),
    INDEX idx_uploaded_by_user_id (uploaded_by_user_id),
    INDEX idx_delete_time (delete_time),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易附件表';

-- 添加外键约束（可选，根据实际情况决定）
-- ALTER TABLE transaction_attachment 
-- ADD CONSTRAINT fk_transaction_attachment_transaction 
-- FOREIGN KEY (transaction_id) REFERENCES transaction(id) ON DELETE CASCADE;
