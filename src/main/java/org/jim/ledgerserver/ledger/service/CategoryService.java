package org.jim.ledgerserver.ledger.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.jim.ledgerserver.common.enums.TransactionTypeEnum;
import org.jim.ledgerserver.common.exception.BusinessException;
import org.jim.ledgerserver.common.util.UserContext;
import org.jim.ledgerserver.ledger.dto.CategoryResponse;
import org.jim.ledgerserver.ledger.dto.CreateCategoryRequest;
import org.jim.ledgerserver.ledger.dto.UpdateCategoryRequest;
import org.jim.ledgerserver.ledger.entity.CategoryEntity;
import org.jim.ledgerserver.ledger.repository.CategoryRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 交易分类业务逻辑层
 * @author James Smith
 */
@Component
public class CategoryService {

    @Resource
    private CategoryRepository categoryRepository;

    /**
     * 应用启动时初始化系统预设分类
     */
    @PostConstruct
    @Transactional
    public void initSystemCategories() {
        // 检查是否已经初始化过系统分类
        List<CategoryEntity> systemCategories = categoryRepository.findByIsSystemTrueAndDeleteTimeIsNull();
        if (!systemCategories.isEmpty()) {
            return; // 已经初始化过，跳过
        }

        // 初始化支出分类
        createSystemExpenseCategories();
        
        // 初始化收入分类
        createSystemIncomeCategories();
    }

    /**
     * 创建系统预设的支出分类
     */
    private void createSystemExpenseCategories() {
        String[][] expenseCategories = {
            {"餐饮", "🍜", "#FF9500"},
            {"购物", "🛍️", "#FF2D55"},
            {"交通", "🚗", "#5AC8FA"},
            {"日用", "🏠", "#34C759"},
            {"娱乐", "🎮", "#AF52DE"},
            {"医疗", "💊", "#FF3B30"},
            {"教育", "📚", "#007AFF"},
            {"通讯", "📱", "#5AC8FA"}
        };

        for (int i = 0; i < expenseCategories.length; i++) {
            String[] category = expenseCategories[i];
            CategoryEntity entity = new CategoryEntity();
            entity.setName(category[0]);
            entity.setIcon(category[1]);
            entity.setColor(category[2]);
            entity.setType(TransactionTypeEnum.EXPENSE.getCode());
            entity.setSortOrder(i + 1);
            entity.setIsSystem(true);
            entity.setCreatedByUserId(null); // 系统分类不属于任何用户
            categoryRepository.save(entity);
        }
    }

    /**
     * 创建系统预设的收入分类
     */
    private void createSystemIncomeCategories() {
        String[][] incomeCategories = {
            {"工资", "💰", "#34C759"},
            {"奖金", "🎁", "#FF9500"},
            {"理财", "📈", "#FFD60A"},
            {"兼职", "💼", "#00C7BE"}
        };

        for (int i = 0; i < incomeCategories.length; i++) {
            String[] category = incomeCategories[i];
            CategoryEntity entity = new CategoryEntity();
            entity.setName(category[0]);
            entity.setIcon(category[1]);
            entity.setColor(category[2]);
            entity.setType(TransactionTypeEnum.INCOME.getCode());
            entity.setSortOrder(i + 1);
            entity.setIsSystem(true);
            entity.setCreatedByUserId(null); // 系统分类不属于任何用户
            categoryRepository.save(entity);
        }
    }

    /**
     * 创建用户自定义分类
     * @param request 创建分类请求
     * @return 分类响应
     */
    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException("用户未登录");
        }

        // 验证交易类型
        TransactionTypeEnum.fromCode(request.type().getCode());

        // 检查同名分类是否存在（同一用户下同一类型的分类名称不能重复）
        categoryRepository.findByNameAndTypeAndUserId(request.name(), request.type().getCode(), currentUserId)
                .ifPresent(c -> {
                    throw new BusinessException("该类型下已存在同名分类");
                });

        CategoryEntity entity = new CategoryEntity();
        entity.setName(request.name());
        entity.setIcon(request.icon());
        entity.setColor(request.color());
        entity.setType(request.type().getCode());
        entity.setIsSystem(false);
        entity.setCreatedByUserId(currentUserId);
        entity.setDescription(request.description());

        // 设置排序顺序（在用户自定义分类中排最后）
        if (request.sortOrder() != null) {
            entity.setSortOrder(request.sortOrder());
        } else {
            Integer maxSortOrder = categoryRepository.findMaxSortOrderByTypeAndUserId(
                    request.type().getCode(), currentUserId);
            entity.setSortOrder(maxSortOrder + 1);
        }

        CategoryEntity savedEntity = categoryRepository.save(entity);
        return convertToResponse(savedEntity);
    }

    /**
     * 更新分类
     * @param id 分类ID
     * @param request 更新分类请求
     * @return 分类响应
     */
    @Transactional
    public CategoryResponse updateCategory(Long id, UpdateCategoryRequest request) {
        CategoryEntity entity = findById(id);
        
        // 只能更新用户自己创建的分类
        Long currentUserId = UserContext.getCurrentUserId();
        if (entity.getIsSystem() || !entity.getCreatedByUserId().equals(currentUserId)) {
            throw new BusinessException("无法修改系统预设分类或其他用户的分类");
        }

        // 更新字段
        if (StringUtils.isNotBlank(request.name())) {
            // 检查同名分类（排除当前分类）
            categoryRepository.findByNameAndTypeAndUserId(request.name(), entity.getType(), currentUserId)
                    .ifPresent(c -> {
                        if (!c.getId().equals(id)) {
                            throw new BusinessException("该类型下已存在同名分类");
                        }
                    });
            entity.setName(request.name());
        }

        if (request.icon() != null) {
            entity.setIcon(request.icon());
        }

        if (request.color() != null) {
            entity.setColor(request.color());
        }

        if (request.type() != null) {
            TransactionTypeEnum.fromCode(request.type().getCode());
            entity.setType(request.type().getCode());
        }

        if (request.sortOrder() != null) {
            entity.setSortOrder(request.sortOrder());
        }

        if (request.description() != null) {
            entity.setDescription(request.description());
        }

        CategoryEntity savedEntity = categoryRepository.save(entity);
        return convertToResponse(savedEntity);
    }

    /**
     * 删除分类（逻辑删除）
     * @param id 分类ID
     */
    @Transactional
    public void deleteCategory(Long id) {
        CategoryEntity entity = findById(id);
        
        // 不能删除系统预设分类
        if (entity.getIsSystem()) {
            throw new BusinessException("无法删除系统预设分类");
        }

        // 只能删除用户自己创建的分类
        Long currentUserId = UserContext.getCurrentUserId();
        if (!entity.getCreatedByUserId().equals(currentUserId)) {
            throw new BusinessException("无法删除其他用户的分类");
        }

        if (entity.getDeleteTime() != null) {
            throw new BusinessException("分类已删除");
        }

        entity.setDeleteTime(LocalDateTime.now());
        categoryRepository.save(entity);
    }

    /**
     * 根据ID查询分类
     * @param id 分类ID
     * @return 分类实体
     */
    public CategoryEntity findById(Long id) {
        if (id == null) {
            throw new BusinessException("分类ID不能为空");
        }
        return categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("分类不存在"));
    }

    /**
     * 查询用户可见的所有分类（系统预设 + 用户自定义）
     * @return 分类响应列表
     */
    public List<CategoryResponse> getAllCategories() {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException("用户未登录");
        }

        List<CategoryEntity> entities = categoryRepository.findAllByUserId(currentUserId);
        return entities.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 根据类型查询用户可见的分类
     * @param type 分类类型
     * @return 分类响应列表
     */
    public List<CategoryResponse> getCategoriesByType(TransactionTypeEnum type) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException("用户未登录");
        }

        TransactionTypeEnum.fromCode(type.getCode()); // 验证类型
        List<CategoryEntity> entities = categoryRepository.findByTypeAndUserId(type.getCode(), currentUserId);
        return entities.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 查询支出分类
     * @return 支出分类列表
     */
    public List<CategoryResponse> getExpenseCategories() {
        return getCategoriesByType(TransactionTypeEnum.EXPENSE);
    }

    /**
     * 查询收入分类
     * @return 收入分类列表
     */
    public List<CategoryResponse> getIncomeCategories() {
        return getCategoriesByType(TransactionTypeEnum.INCOME);
    }

    /**
     * 查询用户自定义的分类
     * @return 用户自定义分类列表
     */
    public List<CategoryResponse> getUserCustomCategories() {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException("用户未登录");
        }

        List<CategoryEntity> entities = categoryRepository.findByCreatedByUserIdAndDeleteTimeIsNull(currentUserId);
        return entities.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 转换实体为响应DTO
     * @param entity 分类实体
     * @return 分类响应DTO
     */
    private CategoryResponse convertToResponse(CategoryEntity entity) {
        TransactionTypeEnum typeEnum = entity.getType() == 1 ? 
                TransactionTypeEnum.INCOME : TransactionTypeEnum.EXPENSE;
                
        return new CategoryResponse(
                entity.getId(),
                entity.getName(),
                entity.getIcon(),
                entity.getColor(),
                typeEnum,
                entity.getSortOrder(),
                entity.getIsSystem(),
                entity.getDescription()
        );
    }
}