# Ledger Server - AI 智能记账后端服务

<div align="center">

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.1.0--M4-blue.svg)](https://spring.io/projects/spring-ai)
[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://openjdk.org/)
[![MCP](https://img.shields.io/badge/MCP-Server-purple.svg)](https://modelcontextprotocol.io/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Status](https://img.shields.io/badge/Status-In%20Development-red.svg)]()

**基于 MCP 协议的智能记账业务服务 & MCP Server**

[English](README_EN.md) | 简体中文

</div>

## 📖 项目简介

Ledger Server 是一个功能完整的**智能记账后端服务**，同时也是一个 **MCP (Model Context Protocol) Server**。它不仅提供传统的 RESTful API，还通过 MCP 协议将业务能力暴露为 AI 可调用的工具，实现**自然语言操作账本**的创新体验。

### ✨ 核心特性

- 📚 **完整的记账业务**: 账本管理、交易记录、分类管理、数据统计
- 🔧 **MCP Server**: 将业务能力封装为 MCP 工具，供 AI Agent 调用
- 🔐 **安全认证**: JWT + Token 双重认证机制
- 🏢 **多账本支持**: 个人账本、共享账本、商业账本
- 📊 **智能统计**: 支出收入分析、时间范围查询、分类统计
- 🔍 **高级查询**: 动态查询、分页、排序、多条件筛选
- 🗄️ **数据持久化**: Spring Data JPA + MySQL

## 🏗️ 技术架构

### 技术栈

- **后端框架**: Spring Boot 3.5.6
- **AI 框架**: Spring AI 1.1.0-M4 (MCP Server)
- **数据库**: MySQL + Spring Data JPA
- **安全认证**: Spring Security + JWT
- **API 文档**: RESTful API
- **构建工具**: Maven
- **JDK 版本**: Java 25

### 模块架构

```
ledger-server/
├── base/           # 基础设施层
├── common/         # 通用组件
├── config/         # 配置管理
├── ledger/         # 记账业务核心
│   ├── controller/ # REST API 控制器
│   ├── service/    # 业务逻辑层
│   ├── repository/ # 数据访问层
│   ├── entity/     # 实体模型
│   └── vo/         # 视图对象
├── mcp/            # MCP Server 实现 ⭐
├── user/           # 用户管理
└── test/           # 测试工具
```

## 🔧 核心功能

### 1. 用户管理

- **注册/登录**: 用户注册、JWT 认证登录
- **个人信息**: 用户信息查询与更新
- **权限管理**: 基于角色的访问控制

### 2. 账本管理 (Ledger)

#### 账本类型
- **个人账本** (PERSONAL): 个人私有记账
- **共享账本** (SHARED): 多人协作记账
- **商业账本** (BUSINESS): 企业/团队记账

#### 核心功能
```java
// 创建账本
POST /api/ledgers

// 获取我的账本列表
GET /api/ledgers

// 账本详情
GET /api/ledgers/{id}

// 更新账本
PUT /api/ledgers/{id}

// 删除账本
DELETE /api/ledgers/{id}
```

### 3. 交易管理 (Transaction)

#### RESTful API

```java
// 创建交易记录
POST /api/transactions/create
{
    "name": "午餐",
    "description": "公司楼下",
    "amount": 45.50,
    "type": "EXPENSE",
    "transactionDateTime": "2025-11-11T12:30:00",
    "ledgerId": 1,
    "categoryId": 1
}

// 高级查询（支持分页、筛选、排序）⭐
POST /api/transactions/query
{
    "ledgerId": 1,
    "type": "EXPENSE",
    "categoryId": 1,
    "startTime": "2025-11-01T00:00:00",
    "endTime": "2025-11-30T23:59:59",
    "page": 0,
    "size": 20,
    "sortBy": "transactionDateTime",
    "sortDirection": "DESC"
}

// 移动交易到其他账本
POST /api/transactions/{id}/move-ledger

// 删除交易
DELETE /api/transactions/{id}
```

#### 查询特性
- ✅ 动态多条件查询 (Specification)
- ✅ 分页支持
- ✅ 自定义排序
- ✅ 时间范围筛选
- ✅ 类型筛选
- ✅ 账本筛选

### 4. 分类管理 (Category)

```java
// 获取所有分类
GET /api/categories

// 按类型获取分类
GET /api/categories/type/{type}

// 创建自定义分类
POST /api/categories
```

**预置分类**:
- 支出: 餐饮、购物、交通、日用、娱乐、医疗、教育、通讯
- 收入: 工资、奖金、理财、兼职

### 5. 账本成员管理

```java
// 添加成员
POST /api/ledger-members/{ledgerId}/add

// 移除成员
DELETE /api/ledger-members/{ledgerId}/remove/{userId}

// 更新权限
PUT /api/ledger-members/{ledgerId}/permission/{userId}

// 查询成员列表
GET /api/ledger-members/{ledgerId}/members
```

**权限级别**:
- `VIEW`: 仅查看
- `EDIT`: 查看 + 编辑交易
- `MANAGE`: 全部权限（成员管理、账本设置）

## 🤖 MCP Server 实现

### MCP 工具列表

#### 用户工具 (UserMCP)

```java
@McpTool
public String registerUser(String username, String password, String email)

@McpTool
public String loginUser(String username, String password)

@McpTool
public String getUserProfile()
```

#### 账本工具 (LedgerMCP)

```java
@McpTool
public String createLedger(String name, String description)

@McpTool
public String listMyLedgers()

@McpTool
public String getLedger(Long id)

@McpTool
public String updateLedger(Long id, String name, String description)

@McpTool
public String deleteLedger(Long id)
```

#### 交易工具 (TransactionMCP)

```java
@McpTool
public String createTransaction(
    String name, 
    String description, 
    BigDecimal amount,
    Integer type,  // 1=INCOME, 2=EXPENSE
    Long ledgerId,
    Long categoryId
)

@McpTool
public String getTransaction(Long id)

@McpTool
public String listLedgerTransactions(Long ledgerId)

@McpTool
public String listUserTransactions(Long createdByUserId)

@McpTool
public String listTransactionsByType(Integer type, Long createdByUserId)

@McpTool
public String listTransactionsByDateRange(
    String startTime,
    String endTime,
    Long createdByUserId
)

@McpTool
public String calculateLedgerSummary(Long ledgerId)

@McpTool
public String calculateUserSummary(Long createdByUserId)
```

#### 分类工具 (CategoryMCP)

```java
@McpTool
public String listCategories()

@McpTool
public String listCategoriesByType(String type)

@McpTool
public String createCategory(String name, String icon, String color, String type)
```

### MCP 配置

```yaml
spring:
  ai:
    mcp:
      server:
        enabled: true
        path: /mcp
```

### MCP 工具调用示例

**AI Agent**: "帮我记一笔午餐支出，花了50元"

**MCP 调用流程**:
1. AI 解析意图
2. 调用工具: `createTransaction(name="午餐", amount=50, type=2, ...)`
3. 返回结果: "已成功创建交易记录: 午餐 - ¥50.00 (支出)"

## 📦 项目结构

```
ledger-server/
├── src/main/java/org/jim/ledgerserver/
│   ├── base/
│   │   └── BaseEntity.java              # 实体基类
│   ├── common/
│   │   ├── JSONResult.java              # 统一响应格式
│   │   ├── exception/                   # 异常处理
│   │   ├── enums/                       # 枚举定义
│   │   └── util/                        # 工具类
│   ├── config/
│   │   ├── JwtConfig.java               # JWT 配置
│   │   └── WebMvcConfig.java            # Web 配置
│   ├── ledger/
│   │   ├── controller/                  # REST Controllers
│   │   │   ├── TransactionController.java    # 交易控制器 ⭐
│   │   │   ├── LedgerController.java         # 账本控制器
│   │   │   ├── CategoryController.java       # 分类控制器
│   │   │   └── LedgerMemberController.java   # 成员控制器
│   │   ├── service/                     # 业务逻辑
│   │   │   ├── TransactionService.java       # 交易服务 ⭐
│   │   │   ├── LedgerService.java            # 账本服务
│   │   │   ├── CategoryService.java          # 分类服务
│   │   │   └── LedgerMemberService.java      # 成员服务
│   │   ├── repository/                  # 数据访问
│   │   │   ├── TransactionRepository.java
│   │   │   ├── LedgerRepository.java
│   │   │   └── ...
│   │   ├── entity/                      # 实体类
│   │   │   ├── TransactionEntity.java        # 交易实体
│   │   │   ├── LedgerEntity.java             # 账本实体
│   │   │   └── ...
│   │   └── vo/                          # 视图对象
│   │       ├── TransactionQueryReq.java      # 查询请求 ⭐
│   │       ├── TransactionPageResp.java      # 分页响应 ⭐
│   │       └── ...
│   ├── mcp/                             # MCP Server
│   │   ├── TransactionMCP.java              # 交易 MCP 工具 ⭐
│   │   ├── LedgerMCP.java                   # 账本 MCP 工具
│   │   ├── CategoryMCP.java                 # 分类 MCP 工具
│   │   └── UserMCP.java                     # 用户 MCP 工具
│   ├── user/
│   │   ├── controller/
│   │   ├── service/
│   │   └── ...
│   └── LedgerServerApplication.java     # 应用入口
├── src/main/resources/
│   ├── application.yml                  # 应用配置
│   └── application.properties
└── pom.xml                              # Maven 配置
```

## 🚀 快速开始

### 前置要求

- Java 25+
- Maven 3.8+
- MySQL 8.0+

### 安装步骤

1. **克隆项目**
```bash
git clone <repository-url>
cd ledger-server
```

2. **创建数据库**
```sql
CREATE DATABASE ledger_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

3. **配置数据库**

编辑 `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ledger_db
    username: your_username
    password: your_password
    
  jpa:
    hibernate:
      ddl-auto: update  # 首次运行，自动创建表
```

4. **配置 JWT**

```yaml
jwt:
  secret: your-secret-key-at-least-256-bits
  expiration: 86400000  # 24小时
```

5. **启动应用**
```bash
mvn clean install
mvn spring-boot:run
```

6. **验证启动**
```bash
# 健康检查
curl http://localhost:8082/actuator/health

# MCP 端点
curl http://localhost:8082/mcp
```

## 💡 API 使用示例

### 1. 用户注册登录

```bash
# 注册
curl -X POST http://localhost:8082/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "demo",
    "password": "123456",
    "email": "demo@example.com"
  }'

# 登录
curl -X POST http://localhost:8082/api/users/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "demo",
    "password": "123456"
  }'

# 响应
{
  "code": 200,
  "message": "成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "userId": 1
  }
}
```

### 2. 创建账本

```bash
curl -X POST http://localhost:8082/api/ledgers \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "name": "我的日常账本",
    "description": "记录日常开销"
  }'
```

### 3. 记账

```bash
curl -X POST http://localhost:8082/api/transactions/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "name": "午餐",
    "description": "公司食堂",
    "amount": 25.5,
    "type": "EXPENSE",
    "ledgerId": 1,
    "categoryId": 1
  }'
```

### 4. 高级查询

```bash
# 查询本月的所有支出，按时间降序
curl -X POST http://localhost:8082/api/transactions/query \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "ledgerId": 1,
    "type": 2,
    "startTime": "2025-11-01T00:00:00",
    "endTime": "2025-11-30T23:59:59",
    "page": 0,
    "size": 20,
    "sortBy": "transactionDateTime",
    "sortDirection": "DESC"
  }'
```

## 🗄️ 数据模型

### 核心实体关系

```
User (用户)
  ├─── Ledger (账本)
  │     ├─── Transaction (交易)
  │     └─── LedgerMember (账本成员)
  └─── Category (分类)
```

### 主要字段

**TransactionEntity**:
- `id`: 交易ID
- `name`: 交易名称
- `amount`: 金额
- `type`: 类型 (1=收入, 2=支出)
- `transactionDateTime`: 交易时间
- `ledgerId`: 所属账本
- `categoryId`: 所属分类
- `createdByUserId`: 创建者

**LedgerEntity**:
- `id`: 账本ID
- `name`: 账本名称
- `type`: 类型 (1=个人, 2=共享, 3=商业)
- `ownerUserId`: 所有者
- `maxMembers`: 最大成员数
- `isPublic`: 是否公开

## 🔐 安全机制

### JWT 认证

```java
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    // 自动从 JWT 中获取当前用户
    Long currentUserId = UserContext.getCurrentUserId();
}
```

### 权限控制

- **账本权限**: 所有者、成员权限分级
- **交易权限**: 仅创建者和账本所有者可操作
- **数据隔离**: 用户只能访问自己的数据

## 📊 性能优化

- **分页查询**: 避免一次性加载大量数据
- **动态查询**: JPA Specification 实现高效筛选
- **索引优化**: 数据库索引优化查询性能
- **连接池**: HikariCP 高性能连接池

## 🚧 待开发功能

- [ ] 数据统计报表 API
- [ ] 预算管理功能
- [ ] 定期交易（自动记账）
- [ ] 导入/导出功能
- [ ] 数据备份与恢复
- [ ] 更多 MCP 工具（图表生成、智能分析等）

## 🧪 测试

```bash
# 运行单元测试
mvn test

# 运行集成测试
mvn verify
```

## 📝 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

## 📧 联系方式

- 作者: James Smith
- Email: your.email@example.com
- GitHub: [@your-username](https://github.com/your-username)

---

⭐ 如果这个项目对你有帮助，请给它一个星标！
