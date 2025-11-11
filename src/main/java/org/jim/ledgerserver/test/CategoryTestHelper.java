package org.jim.ledgerserver.test;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jim.ledgerserver.common.enums.TransactionTypeEnum;
import org.jim.ledgerserver.ledger.dto.CategoryResponse;
import org.jim.ledgerserver.ledger.dto.CreateCategoryRequest;
import org.jim.ledgerserver.ledger.service.CategoryService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 分类功能测试类
 * @author James Smith
 */
@Component
@Slf4j
public class CategoryTestHelper {

    @Resource
    private CategoryService categoryService;

    /**
     * 测试分类功能
     */
    public void testCategoryFeatures() {
        log.info("开始测试分类功能...");

        try {
            // 1. 查询支出分类
            log.info("=== 查询支出分类 ===");
            List<CategoryResponse> expenseCategories = categoryService.getExpenseCategories();
            expenseCategories.forEach(category -> 
                log.info("支出分类: {} {} - {}", category.icon(), category.name(), category.color())
            );

            // 2. 查询收入分类
            log.info("=== 查询收入分类 ===");
            List<CategoryResponse> incomeCategories = categoryService.getIncomeCategories();
            incomeCategories.forEach(category -> 
                log.info("收入分类: {} {} - {}", category.icon(), category.name(), category.color())
            );

            // 3. 创建自定义分类（需要用户登录状态，这里仅作演示）
            log.info("=== 创建自定义分类示例 ===");
            log.info("注意：需要用户登录状态才能创建自定义分类");

            log.info("分类功能测试完成");
        } catch (Exception e) {
            log.error("分类功能测试出错", e);
        }
    }
}