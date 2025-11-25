-- 为user表添加角色字段
ALTER TABLE user
ADD COLUMN role VARCHAR(20) DEFAULT 'USER' COMMENT '用户角色：USER-普通用户，ADMIN-管理员';

-- 为现有用户设置默认角色
UPDATE user SET role = 'USER' WHERE role IS NULL;

-- 添加索引以优化角色查询
CREATE INDEX idx_role ON user(role);
