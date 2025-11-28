/*
package org.jim.ledgerserver.ledger.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jim.ledgerserver.ledger.entity.TransactionEntity;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.context.McpSyncRequestContext;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

import static org.jim.ledgerserver.common.enums.TransactionTypeEnum.getTypeDescription;

*/
/**
 * 交易 MCP 工具类
 * @author James Smith
 *//*

@Component
@Slf4j
public class TransactionMCP {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    @Resource
    private TransactionService transactionService;

    @McpTool(description = """
            Purpose: Create a new transaction (income or expense record)

            Prerequisites:
            - NONE - Direct transaction creation process

            Parameters:
            - name: Transaction name/title (required)
            - description: Transaction description (optional)
            - amount: Transaction amount (required, must be positive)
            - type: Transaction type (required, "1" for INCOME, "2" for EXPENSE)
            - ledgerId: Associated ledger ID (optional)
            - categoryId: Associated category ID (optional)

            Returns:
            - Success: Transaction creation successful message with transaction info
            - Failure: Error message if creation fails

            Error Handling:
            - If amount is not positive: Return "交易金额必须大于0" error
            - If type is invalid: Return "无效的交易类型" error

            Workflow:
            1. Validate required parameters (name, amount, type)
            2. Validate amount is positive
            3. Validate transaction type (1=INCOME, 2=EXPENSE)
            4. Set transactionDateTime to now if not provided
            5. Create transaction record
            6. Return success message with transaction details
            """)
    public String createTransaction(String name, String description, BigDecimal amount,
                                    Integer type, Long ledgerId, Long categoryId, McpSyncRequestContext context) {
        log.info("Creating transaction: name={}, amount={}, type={}, categoryId={} , toolContext={}",
                name, amount, type, categoryId, context);

        // Send logging notification
        context.info("createTransaction Processing");

        // Ping the client
        context.ping();

        // Send progress updates
        context.progress(50); // 50% complete


        String progressToken = context.request().progressToken();
        log.info("Creating transaction: progressToken={}", progressToken);
        if (progressToken != null) {
            context.progress(p -> p.progress(0.0).total(1.0).message("Starting task"));

            // Perform work...

            context.progress(p -> p.progress(1.0).total(1.0).message("Task completed"));
        }

        var transaction = transactionService.create(name, description, amount, type, LocalDateTime.now(), ledgerId, categoryId,null);
        log.info("Transaction created: {}", transaction);

        return String.format("交易创建成功: ID=%d, 名称=%s, 金额=%.2f, 类型=%s",
                transaction.getId(),
                transaction.getName(),
                transaction.getAmount(),
                getTypeDescription(transaction.getType()));
    }

    @McpTool(description = """
            Purpose: Query transaction details by transaction ID

            Prerequisites:
            - NONE

            Parameters:
            - id: Transaction ID (required)

            Returns:
            - Success: Transaction details including name, amount, type, date, etc.
            - Failure: Error message if transaction not found

            Workflow:
            1. Validate transaction ID is provided
            2. Query transaction from database
            3. Return formatted transaction details
            """)
    public String getTransaction(Long id) {
        log.info("Getting transaction by id: {}", id);
        var transaction = transactionService.findById(id);
        return formatTransactionInfo(transaction);
    }

    @McpTool(description = """
            Purpose: Query all transactions for a specific ledger

            Prerequisites:
            - NONE

            Parameters:
            - ledgerId: Ledger ID (required)

            Returns:
            - Success: List of transactions with their details
            - Failure: Error message if query fails

            Workflow:
            1. Validate ledger ID is provided
            2. Query all transactions by ledger ID
            3. Return formatted list of transaction details
            """)
    public String listLedgerTransactions(Long ledgerId) {
        log.info("Listing transactions for ledger: {}", ledgerId);
        var transactions = transactionService.findByLedgerId(ledgerId);

        if (transactions.isEmpty()) {
            return "该账本暂无交易记录";
        }

        return transactions.stream()
                .map(this::formatTransactionInfo)
                .collect(Collectors.joining("\n---\n"));
    }

    @McpTool(description = """
            Purpose: Query all transactions created by a specific user

            Prerequisites:
            - NONE

            Parameters:
            - createdByUserId: Creator user ID (required)

            Returns:
            - Success: List of transactions with their details
            - Failure: Error message if query fails

            Workflow:
            1. Validate user ID is provided
            2. Query all transactions by user ID
            3. Return formatted list of transaction details
            """)
    public String listUserTransactions(Long createdByUserId) {
        log.info("Listing transactions for user: {}", createdByUserId);
        var transactions = transactionService.findByCreatedByUserId(createdByUserId);

        if (transactions.isEmpty()) {
            return "该用户暂无交易记录";
        }

        return transactions.stream()
                .map(this::formatTransactionInfo)
                .collect(Collectors.joining("\n---\n"));
    }

    @McpTool(description = """
            Purpose: Query transactions by type (income or expense) for a specific user

            Prerequisites:
            - NONE

            Parameters:
            - type: Transaction type (required, "1" for INCOME, "2" for EXPENSE)
            - createdByUserId: Creator user ID (required)

            Returns:
            - Success: List of transactions matching the type
            - Failure: Error message if query fails

            Workflow:
            1. Validate parameters
            2. Query transactions by type and user ID
            3. Return formatted list
            """)
    public String listTransactionsByType(Integer type, Long createdByUserId) {
        log.info("Listing transactions by type: type={}, userId={}", type, createdByUserId);
        var transactions = transactionService.findByTypeAndCreatedByUserId(type, createdByUserId);

        if (transactions.isEmpty()) {
            return String.format("该用户暂无%s记录", getTypeDescription(type));
        }

        return transactions.stream()
                .map(this::formatTransactionInfo)
                .collect(Collectors.joining("\n---\n"));
    }

    @McpTool(description = """
            Purpose: Query transactions within a specific date range

            Prerequisites:
            - NONE

            Parameters:
            - startTime: Start date time (required, format: yyyy-MM-dd HH:mm:ss)
            - endTime: End date time (required, format: yyyy-MM-dd HH:mm:ss)
            - createdByUserId: Creator user ID (required)

            Returns:
            - Success: List of transactions within the date range
            - Failure: Error message if query fails

            Error Handling:
            - If startTime is after endTime: Return "开始时间不能晚于结束时间" error

            Workflow:
            1. Validate and parse date time parameters
            2. Query transactions within date range
            3. Return formatted list
            """)
    public String listTransactionsByDateRange(String startTime, String endTime, Long createdByUserId) {
        log.info("Listing transactions by date range: start={}, end={}, userId={}", startTime, endTime, createdByUserId);

        var start = LocalDateTime.parse(startTime, FORMATTER);
        var end = LocalDateTime.parse(endTime, FORMATTER);

        var transactions = transactionService.findByDateRange(start, end, createdByUserId);

        if (transactions.isEmpty()) {
            return String.format("在 %s 至 %s 期间暂无交易记录", startTime, endTime);
        }

        return transactions.stream()
                .map(this::formatTransactionInfo)
                .collect(Collectors.joining("\n---\n"));
    }

    @McpTool(description = """
            Purpose: Update transaction information

            Prerequisites:
            - NONE

            Parameters:
            - id: Transaction ID (required)
            - name: New transaction name (optional)
            - description: New transaction description (optional)
            - amount: New transaction amount (optional, must be positive if provided)
            - type: New transaction type (optional, "1" for INCOME, "2" for EXPENSE)
            - transactionDateTime: New transaction date time (optional, format: yyyy-MM-dd HH:mm:ss)
            - categoryId: New category ID (optional)

            Returns:
            - Success: Update successful message with updated transaction info
            - Failure: Error message if update fails

            Error Handling:
            - If amount is not positive: Return "交易金额必须大于0" error
            - If type is invalid: Return "无效的交易类型" error

            Workflow:
            1. Validate transaction ID is provided
            2. Check if transaction exists
            3. Validate new values if provided
            4. Update transaction information
            5. Return success message
            """)
    public String updateTransaction(Long id, String name, String description, BigDecimal amount,
                                    Integer type, String transactionDateTime, Long categoryId) {
        log.info("Updating transaction: id={}, name={}, amount={}, type={}, categoryId={}", id, name, amount, type, categoryId);

        LocalDateTime dateTime = transactionDateTime != null
                ? LocalDateTime.parse(transactionDateTime, FORMATTER)
                : null;

        var transaction = transactionService.update(id, name, description, amount, type, dateTime, categoryId);
        log.info("Transaction updated: {}", transaction);

        return String.format("交易更新成功: %s", formatTransactionInfo(transaction));
    }

    */
/**
     * 更新交易（兼容旧接口，不更新分类）
     *//*

    public String updateTransaction(Long id, String name, String description, BigDecimal amount,
                                    Integer type, String transactionDateTime) {
        return updateTransaction(id, name, description, amount, type, transactionDateTime, null);
    }

    @McpTool(description = """
            Purpose: Delete a transaction (soft delete - mark as deleted)

            Prerequisites:
            - NONE

            Parameters:
            - id: Transaction ID (required)

            Returns:
            - Success: Deletion successful message
            - Failure: Error message if deletion fails

            Note:
            - This is a soft delete operation, the transaction record remains in database
            - The transaction's deleteTime field will be set to current timestamp

            Workflow:
            1. Validate transaction ID is provided
            2. Check if transaction exists and not already deleted
            3. Set deleteTime to current timestamp
            4. Return success message
            """)
    public String deleteTransaction(Long id) {
        log.info("Deleting transaction: id={}", id);
        transactionService.delete(id);
        log.info("Transaction deleted: id={}", id);
        return String.format("交易已删除: ID=%d", id);
    }

    @McpTool(description = """
            Purpose: Calculate financial statistics for a ledger

            Prerequisites:
            - NONE

            Parameters:
            - ledgerId: Ledger ID (required)

            Returns:
            - Success: Financial summary including total income, total expense, and balance
            - Failure: Error message if calculation fails

            Workflow:
            1. Validate ledger ID is provided
            2. Calculate total income (sum of all INCOME transactions)
            3. Calculate total expense (sum of all EXPENSE transactions)
            4. Calculate balance (income - expense)
            5. Return formatted statistics
            """)
    public String calculateLedgerStatistics(Long ledgerId) {
        log.info("Calculating statistics for ledger: {}", ledgerId);

        var totalIncome = transactionService.calculateTotalIncome(ledgerId);
        var totalExpense = transactionService.calculateTotalExpense(ledgerId);
        var balance = transactionService.calculateBalance(ledgerId);

        return String.format("""
                账本财务统计:
                总收入: %.2f
                总支出: %.2f
                余额: %.2f
                """, totalIncome, totalExpense, balance);
    }

    @McpTool(description = """
            Purpose: Calculate financial statistics for a user across all transactions

            Prerequisites:
            - NONE

            Parameters:
            - createdByUserId: Creator user ID (required)

            Returns:
            - Success: Financial summary including total income, total expense, and balance
            - Failure: Error message if calculation fails

            Workflow:
            1. Validate user ID is provided
            2. Calculate total income (sum of all INCOME transactions)
            3. Calculate total expense (sum of all EXPENSE transactions)
            4. Calculate balance (income - expense)
            5. Return formatted statistics
            """)
    public String calculateUserStatistics(Long createdByUserId, ToolContext toolContext) {
        log.info("Calculating statistics for user: {}, toolContext: {}", createdByUserId, toolContext);

        // 从UserContext获取当前token
        String currentToken = org.jim.ledgerserver.common.util.UserContext.getCurrentToken();
        log.info("Current token from UserContext: {}", currentToken);

        // 从UserContext获取当前用户
        org.jim.ledgerserver.user.entity.UserEntity currentUser = org.jim.ledgerserver.common.util.UserContext.getCurrentUser();
        if (currentUser != null) {
            log.info("Current user from UserContext: {} (ID: {})", currentUser.getUsername(), currentUser.getId());
        } else {
            log.info("No current user found in UserContext");
        }

        var totalIncome = transactionService.calculateUserTotalIncome(createdByUserId);
        var totalExpense = transactionService.calculateUserTotalExpense(createdByUserId);
        var balance = transactionService.calculateUserBalance(createdByUserId);

        return String.format("""
                用户财务统计:
                总收入: %.2f
                总支出: %.2f
                余额: %.2f
                """, totalIncome, totalExpense, balance);
    }

    */
/**
     * 格式化交易信息
     *//*

    private String formatTransactionInfo(TransactionEntity transaction) {
        return String.format("""
                        交易ID: %d
                        名称: %s
                        描述: %s
                        金额: %.2f
                        类型: %s
                        交易时间: %s
                        账本ID: %s
                        分类ID: %s
                        创建用户ID: %d
                        创建时间: %s
                        """,
                transaction.getId(),
                transaction.getName(),
                transaction.getDescription() != null ? transaction.getDescription() : "无",
                transaction.getAmount(),
                getTypeDescription(transaction.getType()),
                transaction.getTransactionDateTime().format(FORMATTER),
                transaction.getLedgerId() != null ? transaction.getLedgerId().toString() : "无",
                transaction.getCategoryId() != null ? transaction.getCategoryId().toString() : "无",
                transaction.getCreatedByUserId(),
                transaction.getCreateTime().format(FORMATTER)
        );
    }

    @McpTool(description = """
            Purpose: Query transactions by category

            Prerequisites:
            - NONE

            Parameters:
            - categoryId: Category ID (required)

            Returns:
            - Success: List of transactions in the specified category
            - Failure: Error message if query fails

            Workflow:
            1. Validate category ID is provided
            2. Query all transactions by category ID
            3. Return formatted list of transaction details
            """)
    public String listTransactionsByCategory(Long categoryId) {
        log.info("Listing transactions for category: {}", categoryId);
        var transactions = transactionService.findByCategoryId(categoryId);

        if (transactions.isEmpty()) {
            return "该分类暂无交易记录";
        }

        return transactions.stream()
                .map(this::formatTransactionInfo)
                .collect(Collectors.joining("\n---\n"));
    }


}
*/
