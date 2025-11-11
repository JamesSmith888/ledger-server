package org.jim.ledgerserver.ledger.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.jim.ledgerserver.common.JSONResult;
import org.jim.ledgerserver.common.enums.TransactionTypeEnum;
import org.jim.ledgerserver.ledger.dto.CategoryResponse;
import org.jim.ledgerserver.ledger.dto.CreateCategoryRequest;
import org.jim.ledgerserver.ledger.dto.UpdateCategoryRequest;
import org.jim.ledgerserver.ledger.service.CategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 交易分类控制器
 * @author James Smith
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Resource
    private CategoryService categoryService;

    /**
     * 获取所有分类（系统预设 + 用户自定义）
     */
    @GetMapping
    public JSONResult<List<CategoryResponse>> getAllCategories() {
        List<CategoryResponse> categories = categoryService.getAllCategories();
        return JSONResult.success(categories);
    }

    /**
     * 根据类型获取分类
     */
    @GetMapping("/type/{type}")
    public JSONResult<List<CategoryResponse>> getCategoriesByType(@PathVariable String type) {
        TransactionTypeEnum typeEnum;
        try {
            typeEnum = TransactionTypeEnum.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return JSONResult.fail("无效的分类类型");
        }
        
        List<CategoryResponse> categories = categoryService.getCategoriesByType(typeEnum);
        return JSONResult.success(categories);
    }

    /**
     * 获取支出分类
     */
    @GetMapping("/expense")
    public JSONResult<List<CategoryResponse>> getExpenseCategories() {
        List<CategoryResponse> categories = categoryService.getExpenseCategories();
        return JSONResult.success(categories);
    }

    /**
     * 获取收入分类
     */
    @GetMapping("/income")
    public JSONResult<List<CategoryResponse>> getIncomeCategories() {
        List<CategoryResponse> categories = categoryService.getIncomeCategories();
        return JSONResult.success(categories);
    }

    /**
     * 获取用户自定义分类
     */
    @GetMapping("/custom")
    public JSONResult<List<CategoryResponse>> getUserCustomCategories() {
        List<CategoryResponse> categories = categoryService.getUserCustomCategories();
        return JSONResult.success(categories);
    }

    /**
     * 创建自定义分类
     */
    @PostMapping
    public JSONResult<CategoryResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        CategoryResponse category = categoryService.createCategory(request);
        return JSONResult.success(category);
    }

    /**
     * 更新分类
     */
    @PutMapping("/{id}")
    public JSONResult<CategoryResponse> updateCategory(@PathVariable Long id, 
                                                      @Valid @RequestBody UpdateCategoryRequest request) {
        CategoryResponse category = categoryService.updateCategory(id, request);
        return JSONResult.success(category);
    }

    /**
     * 删除分类
     */
    @DeleteMapping("/{id}")
    public JSONResult<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return JSONResult.success();
    }

    /**
     * 根据ID获取分类详情
     */
    @GetMapping("/{id}")
    public JSONResult<CategoryResponse> getCategoryById(@PathVariable Long id) {
        var entity = categoryService.findById(id);
        // 手动转换为响应DTO
        TransactionTypeEnum typeEnum = entity.getType() == 1 ? 
                TransactionTypeEnum.INCOME : TransactionTypeEnum.EXPENSE;
        CategoryResponse response = new CategoryResponse(
                entity.getId(),
                entity.getName(),
                entity.getIcon(),
                entity.getColor(),
                typeEnum,
                entity.getSortOrder(),
                entity.getIsSystem(),
                entity.getDescription()
        );
        return JSONResult.success(response);
    }
}