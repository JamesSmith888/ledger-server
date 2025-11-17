-- 创建支付方式表
CREATE TABLE IF NOT EXISTS payment_method (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '支付方式ID',
    name VARCHAR(50) NOT NULL COMMENT '支付方式名称',
    icon VARCHAR(10) COMMENT '支付方式图标（emoji）',
    type VARCHAR(20) COMMENT '支付方式类型：CASH-现金, ALIPAY-支付宝, WECHAT-微信, BANK_CARD-银行卡, OTHER-其他',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    is_default BOOLEAN DEFAULT FALSE COMMENT '是否默认支付方式',
    sort_order INT DEFAULT 0 COMMENT '排序顺序（数字越小越靠前）',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    delete_time DATETIME COMMENT '删除时间（逻辑删除）',
    INDEX idx_user_id (user_id),
    INDEX idx_delete_time (delete_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付方式表';

-- 给交易表添加支付方式字段
ALTER TABLE transaction ADD COLUMN payment_method_id BIGINT COMMENT '支付方式ID';
ALTER TABLE transaction ADD INDEX idx_payment_method_id (payment_method_id);
