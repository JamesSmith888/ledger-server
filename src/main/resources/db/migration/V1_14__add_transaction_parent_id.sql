-- 添加交易父子关联字段
-- 用于实现交易聚合功能（如多笔充值合并展示）

-- 添加 parent_id 字段
ALTER TABLE transaction ADD COLUMN parent_id BIGINT DEFAULT NULL COMMENT '父交易ID，用于关联聚合交易';

-- 添加索引以优化查询子交易的性能
CREATE INDEX idx_transaction_parent_id ON transaction(parent_id, delete_time);

-- 添加外键约束（可选，确保数据一致性）
-- ALTER TABLE transaction ADD CONSTRAINT fk_transaction_parent 
--     FOREIGN KEY (parent_id) REFERENCES transaction(id) ON DELETE SET NULL;
