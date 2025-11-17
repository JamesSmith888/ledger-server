-- 账本管理功能优化 - 数据库迁移脚本
-- 日期: 2025-11-17
-- 说明: 为用户表添加默认账本ID字段

-- ============================================
-- 1. 添加 default_ledger_id 字段
-- ============================================
ALTER TABLE `user` 
ADD COLUMN `default_ledger_id` BIGINT NULL COMMENT '默认账本ID' 
AFTER `last_login_time`;

-- ============================================
-- 2. 添加外键约束（可选，根据实际需求）
-- ============================================
-- 注意: 如果启用外键，删除账本时需要先处理引用关系
-- ALTER TABLE `user` 
-- ADD CONSTRAINT `fk_user_default_ledger` 
-- FOREIGN KEY (`default_ledger_id`) 
-- REFERENCES `ledger`(`id`) 
-- ON DELETE SET NULL;

-- ============================================
-- 3. 添加索引（可选，提升查询性能）
-- ============================================
CREATE INDEX `idx_user_default_ledger_id` 
ON `user`(`default_ledger_id`);

-- ============================================
-- 4. 数据迁移（可选）
-- ============================================
-- 为每个用户设置默认账本为其第一个个人账本
-- UPDATE `user` u
-- LEFT JOIN (
--     SELECT owner_user_id, MIN(id) as first_ledger_id
--     FROM `ledger`
--     WHERE type = 1 -- 个人账本
--     GROUP BY owner_user_id
-- ) l ON u.id = l.owner_user_id
-- SET u.default_ledger_id = l.first_ledger_id
-- WHERE u.default_ledger_id IS NULL;

-- ============================================
-- 5. 验证
-- ============================================
-- 查看用户表结构
-- DESCRIBE `user`;

-- 查看有默认账本的用户数量
-- SELECT COUNT(*) as users_with_default_ledger
-- FROM `user`
-- WHERE default_ledger_id IS NOT NULL;

-- 查看默认账本分布
-- SELECT 
--     u.id as user_id,
--     u.username,
--     u.default_ledger_id,
--     l.name as default_ledger_name,
--     l.type as ledger_type
-- FROM `user` u
-- LEFT JOIN `ledger` l ON u.default_ledger_id = l.id
-- WHERE u.default_ledger_id IS NOT NULL
-- LIMIT 10;
