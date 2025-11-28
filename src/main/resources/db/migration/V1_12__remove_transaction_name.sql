-- 删除交易表的 name 字段
-- 在删除之前，将 name 的值迁移到 description（如果 description 为空）

-- 1. 将 name 值迁移到 description（仅当 description 为空或 null 时）
UPDATE transaction 
SET description = name 
WHERE (description IS NULL OR description = '') 
  AND name IS NOT NULL 
  AND name != '';

-- 2. 删除 name 列
ALTER TABLE transaction DROP COLUMN name;
