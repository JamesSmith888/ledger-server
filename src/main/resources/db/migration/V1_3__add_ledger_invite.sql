-- 创建邀请码表
CREATE TABLE IF NOT EXISTS ledger_invite_code (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '邀请码ID',
    code VARCHAR(64) NOT NULL UNIQUE COMMENT '邀请码',
    ledger_id BIGINT NOT NULL COMMENT '账本ID',
    created_by_user_id BIGINT NOT NULL COMMENT '创建者用户ID',
    
    -- 邀请配置
    role INT NOT NULL COMMENT '邀请角色代码（2-管理员/3-记账员/4-查看者）',
    max_uses INT DEFAULT 1 COMMENT '最大使用次数（-1表示无限制）',
    used_count INT DEFAULT 0 COMMENT '已使用次数',
    
    -- 有效期
    expire_time DATETIME COMMENT '过期时间（NULL表示永不过期）',
    
    -- 状态
    status TINYINT DEFAULT 1 COMMENT '状态：1-有效，0-禁用',
    
    -- 时间戳
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    delete_time DATETIME COMMENT '删除时间（逻辑删除）',
    
    INDEX idx_code (code),
    INDEX idx_ledger_id (ledger_id),
    INDEX idx_created_by (created_by_user_id),
    INDEX idx_status (status),
    INDEX idx_delete_time (delete_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账本邀请码表';

-- 创建邀请使用记录表
CREATE TABLE IF NOT EXISTS ledger_invite_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    invite_code_id BIGINT NOT NULL COMMENT '邀请码ID',
    ledger_id BIGINT NOT NULL COMMENT '账本ID',
    user_id BIGINT NOT NULL COMMENT '使用者用户ID',
    
    use_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '使用时间',
    
    INDEX idx_invite_code_id (invite_code_id),
    INDEX idx_ledger_id (ledger_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邀请码使用记录表';
