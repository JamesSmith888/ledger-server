package org.jim.ledgerserver.ledger.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jim.ledgerserver.common.enums.TransactionTypeEnum;
import org.jim.ledgerserver.ledger.dto.CategoryResponse;
import org.jim.ledgerserver.ledger.dto.CreateCategoryRequest;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 交易分类 MCP 工具类
 * @author James Smith
 */
@Component
@Slf4j
public class CategoryMCP {

    @Resource
    private CategoryService categoryService;

    @McpTool(description = """
            Purpose: Get all available transaction categories for the current user
            
            Prerequisites:
            - User must be logged in
            
            Returns:
            - Success: List of all categories (system preset + user custom) with details
            - Failure: Error message if retrieval fails
            
            Response includes:
            - Category ID, name, icon, color
            - Category type (INCOME/EXPENSE)
            - Whether it's a system preset category
            - Sort order and description
            
            Workflow:
            1. Check user authentication
            2. Retrieve all visible categories for user
            3. Return formatted category list
            """)
    public String getAllCategories() {
        log.info("Getting all categories for current user");
        List<CategoryResponse> categories = categoryService.getAllCategories();


        if (categories.isEmpty()) {
            return "暂无可用分类";
        }

        return categories.stream()
                .map(this::formatCategoryInfo)
                .collect(Collectors.joining("\n---\n"));
    }

    @McpTool(description = """
            Purpose: Get transaction categories by type (INCOME or EXPENSE)
            
            Prerequisites:
            - User must be logged in
            
            Parameters:
            - type: Category type ("INCOME" for income categories, "EXPENSE" for expense categories)
            
            Returns:
            - Success: List of categories matching the specified type
            - Failure: Error message if type is invalid or retrieval fails
            
            Workflow:
            1. Validate category type parameter
            2. Retrieve categories of specified type
            3. Return formatted category list
            """)
    public String getCategoriesByType(String type) {
        log.info("Getting categories by type: {}", type);

        TransactionTypeEnum typeEnum;
        try {
            typeEnum = TransactionTypeEnum.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return "无效的分类类型，请使用 'INCOME' 或 'EXPENSE'";
        }

        List<CategoryResponse> categories = categoryService.getCategoriesByType(typeEnum);

        if (categories.isEmpty()) {
            return String.format("暂无%s分类", typeEnum.getLabel());
        }

        return categories.stream()
                .map(this::formatCategoryInfo)
                .collect(Collectors.joining("\n---\n"));
    }

    @McpTool(description = """
            Purpose: Get all expense categories for the current user
            
            Prerequisites:
            - User must be logged in
            
            Returns:
            - Success: List of expense categories with details
            - Failure: Error message if retrieval fails
            
            This is a convenience method that filters categories to show only expense types.
            
            Workflow:
            1. Check user authentication
            2. Retrieve all expense categories
            3. Return formatted expense category list
            """)
    public String getExpenseCategories() {
        log.info("Getting expense categories for current user");
        List<CategoryResponse> categories = categoryService.getExpenseCategories();

        if (categories.isEmpty()) {
            return "暂无支出分类";
        }

        return "支出分类列表：\n" + categories.stream()
                .map(this::formatCategoryInfo)
                .collect(Collectors.joining("\n---\n"));
    }

    @McpTool(description = """
            Purpose: Get all income categories for the current user
            
            Prerequisites:
            - User must be logged in
            
            Returns:
            - Success: List of income categories with details
            - Failure: Error message if retrieval fails
            
            This is a convenience method that filters categories to show only income types.
            
            Workflow:
            1. Check user authentication
            2. Retrieve all income categories
            3. Return formatted income category list
            """)
    public String getIncomeCategories() {
        log.info("Getting income categories for current user");
        List<CategoryResponse> categories = categoryService.getIncomeCategories();

        if (categories.isEmpty()) {
            return "暂无收入分类";
        }

        return "收入分类列表：\n" + categories.stream()
                .map(this::formatCategoryInfo)
                .collect(Collectors.joining("\n---\n"));
    }

    @McpTool(description = """
            Purpose: Create a new custom transaction category
            
            Prerequisites:
            - User must be logged in
            
            Parameters:
            - name: Category name (required, max 50 characters)
            - type: Category type (required, "INCOME" or "EXPENSE")
            - icon: Category icon (optional, emoji or icon code, max 10 characters)
            - color: Category color (optional, hex color code, max 10 characters)
            - description: Category description (optional, max 200 characters)
            
            Returns:
            - Success: Category creation successful message with category details
            - Failure: Error message if creation fails
            
            Error Handling:
            - If category name already exists for the same type: Return "该类型下已存在同名分类" error
            - If type is invalid: Return "无效的分类类型" error
            
            Workflow:
            1. Validate required parameters
            2. Check for duplicate category names
            3. Create new custom category
            4. Return success message with category details
            """)
    public String createCategory(String name, String type, String icon, String color, String description) {
        log.info("Creating category: name={}, type={}", name, type);

        if (name == null || name.trim().isEmpty()) {
            return "分类名称不能为空";
        }

        TransactionTypeEnum typeEnum;
        try {
            typeEnum = TransactionTypeEnum.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return "无效的分类类型，请使用 'INCOME' 或 'EXPENSE'";
        }

        CreateCategoryRequest request = new CreateCategoryRequest(
                name.trim(),
                icon,
                color,
                typeEnum,
                null, // sortOrder will be auto-assigned
                description
        );

        try {
            CategoryResponse category = categoryService.createCategory(request);
            log.info("Category created: {}", category);
            return String.format("分类创建成功：%s", formatCategoryInfo(category));
        } catch (Exception e) {
            log.error("Failed to create category", e);
            return "分类创建失败：" + e.getMessage();
        }
    }

    @McpTool(description = """
            Purpose: Delete a custom transaction category
            
            Prerequisites:
            - User must be logged in
            - Category must be user-created (not system preset)
            
            Parameters:
            - categoryId: ID of the category to delete (required)
            
            Returns:
            - Success: Category deletion successful message
            - Failure: Error message if deletion fails
            
            Error Handling:
            - If category is system preset: Return "无法删除系统预设分类" error
            - If category belongs to another user: Return "无法删除其他用户的分类" error
            - If category not found: Return "分类不存在" error
            
            Note:
            - This is a soft delete operation, the category record remains in database
            - Associated transactions will keep their category reference
            
            Workflow:
            1. Validate category ID
            2. Check category ownership and type
            3. Perform soft delete
            4. Return success message
            """)
    public String deleteCategory(Long categoryId) {
        log.info("Deleting category: id={}", categoryId);

        if (categoryId == null) {
            return "分类ID不能为空";
        }

        try {
            categoryService.deleteCategory(categoryId);
            log.info("Category deleted: id={}", categoryId);
            return String.format("分类已删除：ID=%d", categoryId);
        } catch (Exception e) {
            log.error("Failed to delete category", e);
            return "分类删除失败：" + e.getMessage();
        }
    }

    @McpTool(description = """
            Purpose: Get user's custom categories (excludes system preset categories)
            
            Prerequisites:
            - User must be logged in
            
            Returns:
            - Success: List of user-created custom categories
            - Failure: Error message if retrieval fails
            
            This method only returns categories created by the current user,
            excluding system preset categories.
            
            Workflow:
            1. Check user authentication
            2. Retrieve user's custom categories
            3. Return formatted custom category list
            """)
    public String getUserCustomCategories() {
        log.info("Getting user custom categories");
        List<CategoryResponse> categories = categoryService.getUserCustomCategories();

        if (categories.isEmpty()) {
            return "您还没有创建自定义分类";
        }

        return "您的自定义分类：\n" + categories.stream()
                .map(this::formatCategoryInfo)
                .collect(Collectors.joining("\n---\n"));
    }

    /**
     * 格式化分类信息
     */
    private String formatCategoryInfo(CategoryResponse category) {
        return String.format("""
                分类ID: %d
                名称: %s %s
                类型: %s
                颜色: %s
                排序: %d
                系统预设: %s
                描述: %s
                """,
                category.id(),
                category.icon() != null ? category.icon() : "",
                category.name(),
                category.type().getLabel(),
                category.color() != null ? category.color() : "无",
                category.sortOrder() != null ? category.sortOrder() : 0,
                category.isSystem() ? "是" : "否",
                category.description() != null ? category.description() : "无"
        );
    }
}
