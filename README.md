# Ledger Server - AI 智能记账后端服务

<div align="center">

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.1.0--M4-blue.svg)](https://spring.io/projects/spring-ai)
[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://openjdk.org/)
[![MCP](https://img.shields.io/badge/MCP-Server-purple.svg)](https://modelcontextprotocol.io/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**智能记账业务服务 & MCP Server**

</div>

## 🔗 相关项目

- **[MCP Client](https://github.com/JamesSmith888/mcp-client)** - AI Agent 对话客户端，智能任务编排
- **[LedgerAI Client](https://github.com/JamesSmith888/LedgerAIClient)** - React Native 移动应用，AI 智能记账 App

## 📖 项目简介

Ledger Server 是功能完整的**智能记账后端服务**，同时也是 **MCP (Model Context Protocol) Server**。提供传统 RESTful API，并通过 MCP 协议将业务能力暴露为 AI 可调用的工具。

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

- Spring Boot 3.5.6
- Spring AI 1.1.0-M4 (MCP Server)
- MySQL + Spring Data JPA
- Spring Security + JWT
- Maven + Java 25

### 模块架构

- **ledger/** - 记账业务核心（Controller、Service、Repository、Entity）
- **mcp/** - MCP Server 实现，暴露业务工具
- **user/** - 用户管理
- **base/** - 基础设施
- **common/** - 通用组件
- **config/** - 配置管理

## 🔧 核心功能

### 1. 用户管理

- 注册/登录、JWT 认证、个人信息管理

### 2. 账本管理 (Ledger)

**账本类型**: 个人账本 (PERSONAL) / 共享账本 (SHARED) / 商业账本 (BUSINESS)

**API 端点**:
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

**创建交易**:
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

```

**高级查询**（支持分页、多条件筛选、排序）:
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
```

### 4. 分类管理 (Category)

预置分类：支出类（餐饮、购物、交通等）/ 收入类（工资、奖金、理财等）

支持创建自定义分类。

### 5. 账本成员管理

添加/移除成员、权限管理（VIEW / EDIT / MANAGE）

## 🤖 MCP Server 实现

通过 MCP 协议暴露业务能力，供 [MCP Client](https://github.com/JamesSmith888/mcp-client) 调用。

**工具分类**:

- **UserMCP**: 注册、登录、用户信息
- **LedgerMCP**: 创建、查询、更新、删除账本
- **TransactionMCP**: 交易记录管理、统计查询
- **CategoryMCP**: 分类管理

**配置**:

```yaml
spring:
  ai:
    mcp:
      server:
        enabled: true
        path: /mcp
```

**调用示例**: AI Agent 说"帮我记一笔午餐支出50元" → 调用 `createTransaction` 工具 → 返回成功结果

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
curl http://localhost:8082/actuator/health
curl http://localhost:8082/mcp
```

## 💡 API 使用示例

**注册/登录**:
```bash
curl -X POST http://localhost:8082/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"username": "demo", "password": "123456", "email": "demo@example.com"}'
```

**创建交易**:

**创建交易**:
```bash
curl -X POST http://localhost:8082/api/transactions/create \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"name": "午餐", "amount": 25.5, "type": "EXPENSE", "ledgerId": 1, "categoryId": 1}'
```

完整 API 文档请参考代码注释。

## 🔐 安全机制

- **JWT 认证**: 基于 Token 的身份验证
- **权限控制**: 账本所有者和成员分级权限
- **数据隔离**: 用户只能访问自己的数据

## 📝 许可证

本项目采用 MIT 许可证。

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

---

⭐ 如果这个项目对你有帮助，请给它一个星标！
