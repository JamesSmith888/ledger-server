# 交易分类功能说明

## 概述

交易分类功能允许用户对收入和支出交易进行分类管理，提供更好的财务记录组织和分析能力。

## 功能特性

### 1. 系统预设分类

应用启动时会自动创建系统预设分类：

**支出分类：**
- 🍜 餐饮 (#FF9500)
- 🛍️ 购物 (#FF2D55)
- 🚗 交通 (#5AC8FA)
- 🏠 日用 (#34C759)
- 🎮 娱乐 (#AF52DE)
- 💊 医疗 (#FF3B30)
- 📚 教育 (#007AFF)
- 📱 通讯 (#5AC8FA)

**收入分类：**
- 💰 工资 (#34C759)
- 🎁 奖金 (#FF9500)
- 📈 理财 (#FFD60A)
- 💼 兼职 (#00C7BE)

### 2. 用户自定义分类

- 用户可以创建自己的分类
- 可以设置分类名称、图标、颜色和描述
- 同一用户下同一类型的分类名称不能重复
- 用户只能修改和删除自己创建的分类

### 3. 分类管理

- 支持按类型查询分类（收入/支出）
- 支持分类的增删改查
- 系统预设分类不可删除
- 分类删除为软删除，不影响已有交易记录

## API 接口

### 分类管理接口

```
GET /api/categories                    # 获取所有分类
GET /api/categories/type/{type}        # 根据类型获取分类 (INCOME/EXPENSE)
GET /api/categories/expense            # 获取支出分类
GET /api/categories/income             # 获取收入分类
GET /api/categories/custom             # 获取用户自定义分类
GET /api/categories/{id}               # 根据ID获取分类详情
POST /api/categories                   # 创建自定义分类
PUT /api/categories/{id}               # 更新分类
DELETE /api/categories/{id}            # 删除分类
```

### 交易接口更新

```
POST /api/transactions/create          # 创建交易（支持分类）
GET /api/transactions/category/{categoryId}  # 根据分类查询交易
```

## 数据模型

### CategoryEntity（分类实体）

```java
@Entity(name = "category")
public class CategoryEntity extends BaseEntity {
    private String name;              // 分类名称
    private String icon;              // 分类图标
    private String color;             // 分类颜色
    private Integer type;             // 分类类型（1-收入，2-支出）
    private Integer sortOrder;        // 排序顺序
    private Boolean isSystem;         // 是否为系统预设
    private Long createdByUserId;     // 创建用户ID
    private String description;       // 分类描述
}
```

### TransactionEntity（交易实体）更新

```java
@Entity(name = "transaction")
public class TransactionEntity extends BaseEntity {
    // ... 其他字段
    private Long categoryId;          // 分类ID（新增）
}
```

## MCP工具集成

新增了CategoryMCP工具类，支持通过AI助手进行分类管理：

- `getAllCategories()` - 获取所有分类
- `getCategoriesByType(type)` - 根据类型获取分类
- `getExpenseCategories()` - 获取支出分类
- `getIncomeCategories()` - 获取收入分类
- `createCategory(name, type, icon, color, description)` - 创建分类
- `deleteCategory(categoryId)` - 删除分类
- `getUserCustomCategories()` - 获取用户自定义分类

TransactionMCP工具类也进行了更新：

- `createTransaction()` - 支持指定分类
- `updateTransaction()` - 支持更新分类
- `listTransactionsByCategory(categoryId)` - 根据分类查询交易

## 使用示例

### 1. 通过REST API创建分类

```json
POST /api/categories
{
  "name": "外卖",
  "icon": "🍕",
  "color": "#FF6B35",
  "type": "EXPENSE",
  "description": "外卖订餐支出"
}
```

### 2. 创建带分类的交易

```json
POST /api/transactions/create
{
  "name": "午餐",
  "description": "工作日午餐",
  "amount": 25.50,
  "type": "EXPENSE",
  "categoryId": 1,
  "transactionDateTime": "2024-01-15 12:30:00"
}
```

### 3. 通过AI助手操作

```
用户：帮我创建一个新的支出分类叫"宠物用品"，图标用🐕，颜色用#8B4513
AI：我来帮您创建这个分类...

用户：查看所有收入分类
AI：为您查询收入分类...

用户：记录一笔餐饮支出，50元，备注午餐
AI：我来帮您记录这笔交易，会自动归类到餐饮分类...
```

## 数据库变更

### 新增表：category

```sql
CREATE TABLE category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    icon VARCHAR(10),
    color VARCHAR(10),
    type INT NOT NULL,
    sort_order INT,
    is_system BOOLEAN DEFAULT FALSE,
    created_by_user_id BIGINT,
    description VARCHAR(200),
    create_time DATETIME,
    update_time DATETIME,
    delete_time DATETIME
);
```

### 修改表：transaction

```sql
ALTER TABLE transaction ADD COLUMN category_id BIGINT;
```

## 注意事项

1. **系统分类保护**：系统预设分类不能被修改或删除
2. **数据一致性**：删除分类不会影响已有交易记录的分类关联
3. **权限控制**：用户只能操作自己创建的分类
4. **软删除**：分类删除采用软删除机制，便于数据恢复
5. **自动初始化**：应用首次启动时会自动创建系统预设分类

## 扩展计划

- 分类统计分析
- 分类图表展示
- 分类导入导出
- 分类模板功能
- 子分类支持