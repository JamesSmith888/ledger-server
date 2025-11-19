-- 创建用户第三方账号绑定表
CREATE TABLE IF NOT EXISTS user_oauth (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '关联的用户ID',
    oauth_type VARCHAR(20) NOT NULL COMMENT '第三方平台类型: WECHAT, ALIPAY, GOOGLE, APPLE',
    oauth_id VARCHAR(100) NOT NULL COMMENT '第三方平台的唯一用户ID',
    oauth_openid VARCHAR(100) COMMENT 'OpenID（仅微信使用）',
    oauth_name VARCHAR(100) COMMENT '第三方平台的昵称',
    oauth_avatar VARCHAR(500) COMMENT '第三方平台的头像URL',
    oauth_email VARCHAR(100) COMMENT '第三方平台的邮箱',
    email_verified BOOLEAN COMMENT '邮箱是否已验证',
    access_token VARCHAR(500) COMMENT '访问令牌',
    refresh_token VARCHAR(500) COMMENT '刷新令牌',
    expires_at TIMESTAMP COMMENT '令牌过期时间',
    last_login_time TIMESTAMP COMMENT '最后登录时间',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    delete_time TIMESTAMP NULL COMMENT '删除时间',
    
    UNIQUE KEY uk_oauth_type_id (oauth_type, oauth_id),
    INDEX idx_user_id (user_id),
    INDEX idx_oauth_type (oauth_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户第三方账号绑定表';
