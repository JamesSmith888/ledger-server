-- 创建交易模板表
CREATE TABLE IF NOT EXISTS transaction_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '模板ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    name VARCHAR(100) NOT NULL COMMENT '模板名称',
    amount DECIMAL(10,2) NOT NULL COMMENT '默认金额',
    type TINYINT NOT NULL COMMENT '交易类型：1-支出, 2-收入',
    category_id BIGINT COMMENT '分类ID',
    payment_method_id BIGINT COMMENT '支付方式ID',
    description VARCHAR(500) COMMENT '描述',
    allow_amount_edit BOOLEAN DEFAULT TRUE COMMENT '使用时是否允许修改金额',
    show_in_quick_panel BOOLEAN DEFAULT FALSE COMMENT '是否显示在快捷面板',
    sort_order INT DEFAULT 0 COMMENT '排序顺序（数字越小越靠前）',
    icon VARCHAR(50) COMMENT '自定义图标名称',
    color VARCHAR(20) COMMENT '自定义颜色',
    ledger_id BIGINT COMMENT '默认账本ID',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    delete_time DATETIME COMMENT '删除时间（逻辑删除）',
    INDEX idx_user_id (user_id),
    INDEX idx_show_in_quick_panel (show_in_quick_panel),
    INDEX idx_delete_time (delete_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易模板表';
