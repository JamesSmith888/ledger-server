# 权限管理 - 快速设置管理员

## 设置第一个管理员用户

运行数据库迁移后，执行以下SQL设置管理员：

```sql
-- 方法1：通过用户ID设置（推荐）
UPDATE user SET role = 'ADMIN' WHERE id = 1;

-- 方法2：通过用户名设置
UPDATE user SET role = 'ADMIN' WHERE username = 'your_username';

-- 查看所有用户及其角色
SELECT id, username, nickname, role FROM user;
```

## 验证管理员权限

管理员登录后，在"帮助与反馈"界面可以：
- 删除任何用户的反馈（包括其他用户的）
- 关闭/重开任何反馈

普通用户只能：
- 删除自己的反馈
- 关闭/重开自己的反馈

## 权限说明

- **USER**: 普通用户（默认角色）
- **ADMIN**: 管理员（需手动设置）

详细文档请查看：[PERMISSION_SYSTEM_GUIDE.md](./PERMISSION_SYSTEM_GUIDE.md)
