# Ledger Server

基于 Spring Boot 的智能记账后端服务，为 **[LedgerAIClient](https://github.com/JamesSmith888/LedgerAIClient)** 提供 RESTful API 与 MCP Server 能力。

## ✨ 核心功能

### 账本管理
- 支持个人账本、共享账本及商业账本
- RBAC 权限控制，支持成员邀请与分级权限（查看/编辑/管理）
- 严格的数据隔离机制

### 交易与统计
- 支持多币种、多分类、自定义标签的交易记录
- 按时间、分类、成员等多维度统计分析
- 动态查询 API，支持复杂条件筛选与排序

### 安全与扩展
- JWT + Token 双重认证
- 模块化设计，易于扩展
- RESTful API 标准

---

## 🤖 MCP Server 支持

通过 Spring AI 集成 MCP 协议，将业务能力封装为 AI 工具，供 **[mcp-client](https://github.com/JamesSmith888/mcp-client)** 调用。

**核心工具**:
- **TransactionMCP**: 创建交易、查询交易、统计数据
- **LedgerMCP**: 账本管理、成员管理
- **CategoryMCP**: 分类管理与推荐

---

## 🛠 技术栈

-   **核心框架**: Spring Boot 3.5.6
-   **AI 框架**: Spring AI 1.1.0-M4 (MCP Server Core)
-   **语言**: Java 25
-   **数据库**: MySQL 8.0+, Spring Data JPA
-   **安全**: Spring Security, JWT
-   **构建工具**: Maven 3.8+

## 🚀 快速开始

### 前置要求
- Java 25+
- MySQL 8.0+

### 安装步骤

1.  **克隆项目**
    ```bash
    git clone https://github.com/JamesSmith888/ledger-server.git
    cd ledger-server
    ```

2.  **配置数据库**
    编辑 `src/main/resources/application.yml`，修改数据库连接信息：
    ```yaml
    spring:
      datasource:
        url: jdbc:mysql://localhost:3306/ledger_db
        username: your_username
        password: your_password
    ```

3.  **启动服务**
    ```bash
    mvn spring-boot:run
    ```

4.  **验证**
    -   API 健康检查: `http://localhost:8082/actuator/health`
    -   MCP 端点: `http://localhost:8082/mcp`

---

## 🔗 关联项目

-   **移动端 Client**: [LedgerAIClient](https://github.com/JamesSmith888/LedgerAIClient) - 配套的 React Native 移动应用。
-   **MCP Client SDK**: [mcp-client](https://github.com/JamesSmith888/mcp-client) - Java 版 MCP 客户端实现。

## 📝 许可证

本项目采用 MIT 许可证。

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！
