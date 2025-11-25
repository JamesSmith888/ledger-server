-- 为交易表添加性能优化索引
-- 这些索引将显著提升查询性能，特别是在数据量较大时

-- 1. 复合索引：账本ID + 删除时间 + 交易时间（用于账本视图查询）
CREATE INDEX idx_transaction_ledger_query 
ON transaction(ledger_id, delete_time, transaction_date_time DESC);

-- 2. 复合索引：创建人ID + 删除时间 + 交易时间（用于用户视图查询）
CREATE INDEX idx_transaction_user_query 
ON transaction(created_by_user_id, delete_time, transaction_date_time DESC);

-- 3. 复合索引：账本ID + 类型 + 删除时间（用于分类统计）
CREATE INDEX idx_transaction_ledger_type 
ON transaction(ledger_id, type, delete_time);

-- 4. 复合索引：创建人ID + 类型 + 删除时间（用于用户统计）
CREATE INDEX idx_transaction_user_type 
ON transaction(created_by_user_id, type, delete_time);

-- 5. 复合索引：分类ID + 删除时间（用于分类查询）
CREATE INDEX idx_transaction_category 
ON transaction(category_id, delete_time);

-- 6. 复合索引：账本ID + 交易时间 + 删除时间（用于时间范围查询）
CREATE INDEX idx_transaction_ledger_datetime 
ON transaction(ledger_id, transaction_date_time, delete_time);

-- 7. 复合索引：创建人ID + 交易时间 + 删除时间（用于用户时间范围查询）
CREATE INDEX idx_transaction_user_datetime 
ON transaction(created_by_user_id, transaction_date_time, delete_time);

-- 8. 索引：支付方式ID（用于支付方式查询）
CREATE INDEX idx_transaction_payment_method 
ON transaction(payment_method_id);
