# 权限管理系统说明文档

## 概述

系统现已实现完整的权限管理功能，支持基于角色的访问控制（RBAC）。

## 用户角色

系统支持两种角色：
- **USER**: 普通用户（默认）
- **ADMIN**: 管理员

## 权限规则

### 反馈系统权限

1. **删除权限** (`canDelete`)
   - 反馈创建者可以删除自己的反馈
   - 管理员可以删除任何反馈

2. **关闭/重开权限** (`canClose`)
   - 反馈创建者可以关闭/重开自己的反馈
   - 管理员可以关闭/重开任何反馈

3. **管理员回复权限** (`canAdminReply`)
   - 仅管理员可以添加官方回复

4. **状态修改权限** (`canChangeStatus`)
   - 仅管理员可以修改反馈状态（待处理/处理中/已完成/已关闭）

## 如何设置管理员

### 方法1：通过数据库直接设置

连接到MySQL数据库，执行以下SQL语句：

```sql
-- 查看用户列表
SELECT id, username, nickname, role FROM user;

-- 将特定用户设置为管理员（替换 {user_id} 为实际的用户ID）
UPDATE user SET role = 'ADMIN' WHERE id = {user_id};

-- 例如：将ID为1的用户设置为管理员
UPDATE user SET role = 'ADMIN' WHERE id = 1;

-- 通过用户名设置管理员
UPDATE user SET role = 'ADMIN' WHERE username = 'admin';
```

### 方法2：在用户注册时直接设置

修改 `UserService.java` 的 `register` 方法，为特定用户设置管理员角色：

```java
// 在注册逻辑中
if ("admin".equals(request.username())) {
    user.setRole("ADMIN");
} else {
    user.setRole("USER");
}
```

## 前端权限展示

### 反馈列表界面

- 每个反馈项根据 `canDelete` 字段决定是否显示删除按钮
- 只有有权限的用户才能看到删除图标

### 反馈详情界面

- 根据 `canClose` 字段显示关闭/重开按钮
- 管理员会看到额外的管理功能

## 技术实现

### 后端实现

1. **数据库层**
   - `V1_10__add_user_role.sql`: 添加role字段到user表

2. **实体层**
   - `UserEntity`: 添加role字段和isAdmin()、isUser()方法

3. **工具类**
   - `PermissionUtil`: 统一的权限验证工具类，可在整个系统中复用
   - 提供方法：
     - `isCurrentUserAdmin()`: 判断当前用户是否为管理员
     - `isAdmin(userId)`: 判断指定用户是否为管理员
     - `isOwner(ownerId)`: 判断当前用户是否是资源所有者
     - `canDelete(ownerId)`: 判断是否有删除权限
     - `canEdit(ownerId)`: 判断是否有编辑权限
     - `canClose(ownerId)`: 判断是否有关闭权限
     - `canAdminReply()`: 判断是否有管理员回复权限
     - `canChangeStatus()`: 判断是否有状态修改权限

4. **服务层**
   - `FeedbackService`: 在toResponse方法中计算权限字段
   - 在删除、关闭、重开等操作中使用PermissionUtil进行权限验证

5. **DTO层**
   - `FeedbackResponse`: 添加canDelete和canClose字段
   - `UserProfileResponse`: 添加role字段

### 前端实现

1. **类型定义**
   - `src/types/user.ts`: User和AuthResponse接口添加role字段
   - `src/api/services/feedbackAPI.ts`: Feedback接口添加canDelete和canClose字段

2. **UI层**
   - `FeedbackScreen.tsx`: 根据canDelete字段条件渲染删除按钮
   - 使用 `{feedback.canDelete && (<删除按钮组件>)}` 实现权限控制

## 扩展其他功能

PermissionUtil工具类可以在整个系统中复用，例如：

### 账本管理权限
```java
// 在LedgerService中
if (!permissionUtil.canEdit(ledger.getOwnerId())) {
    throw new BusinessException("无权编辑该账本");
}
```

### 交易记录权限
```java
// 在TransactionService中
if (!permissionUtil.canDelete(transaction.getUserId())) {
    throw new BusinessException("无权删除该交易");
}
```

### 自定义权限规则
```java
// 在PermissionUtil中添加新方法
public boolean canManageLedger(Long ledgerId) {
    if (isCurrentUserAdmin()) {
        return true;
    }
    // 自定义逻辑：检查是否是账本成员
    return ledgerMemberRepository.existsByLedgerIdAndUserId(ledgerId, UserContext.getCurrentUserId());
}
```

## 安全建议

1. **默认角色**: 所有新注册用户默认为USER角色
2. **管理员设置**: 建议通过数据库手动设置第一个管理员，避免在代码中硬编码
3. **权限验证**: 所有敏感操作都应在后端进行权限验证，前端的权限控制仅用于UI展示
4. **日志记录**: 建议记录管理员操作日志，便于审计

## 测试步骤

1. 创建两个测试账号：普通用户和管理员
2. 使用普通用户登录，提交反馈，验证只能删除自己的反馈
3. 使用管理员登录，验证可以删除任何反馈
4. 验证前端UI正确显示/隐藏删除按钮

## 常见问题

### Q: 如何验证用户是否为管理员？
A: 在后端使用 `permissionUtil.isCurrentUserAdmin()`，在前端检查 `user.role === 'ADMIN'`

### Q: 如何添加新的权限规则？
A: 在PermissionUtil类中添加新的方法，遵循现有的命名规范（canXxx）

### Q: 前端如何知道当前用户的角色？
A: 用户登录后，role字段会包含在用户信息中，存储在AuthContext中

### Q: 权限字段是否会影响性能？
A: 权限计算在toResponse方法中进行，每次查询会调用数据库，但由于有Spring的事务管理和连接池，性能影响很小。如需优化，可以考虑缓存用户角色信息。
