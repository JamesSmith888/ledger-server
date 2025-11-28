-- 添加交易来源字段
-- 1 - MANUAL - 手动录入
-- 2 - AI - AI助手创建
ALTER TABLE `transaction` ADD COLUMN `source` INT DEFAULT 1 COMMENT '交易来源：1-手动，2-AI';

-- 为已有数据设置默认值为手动(1)
UPDATE `transaction` SET `source` = 1 WHERE `source` IS NULL;
