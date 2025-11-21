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

    @Resource
    private org.jim.ledgerserver.ledger.repository.TransactionRepository transactionRepository;

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
            {"餐饮", "ionicons:restaurant", "#FF9500"},
            {"购物", "ionicons:cart", "#FF2D55"},
            {"交通", "ionicons:car", "#5AC8FA"},
            {"日用", "ionicons:home", "#34C759"},
            {"娱乐", "ionicons:game-controller", "#AF52DE"},
            {"医疗", "ionicons:medical", "#FF3B30"},
            {"教育", "ionicons:book", "#007AFF"},
            {"通讯", "ionicons:phone-portrait", "#5AC8FA"},
            {"运动", "ionicons:fitness", "#32ADE6"},
            {"旅游", "ionicons:airplane", "#FF6B6B"},
            {"美容", "ionicons:heart", "#FF69B4"},
            {"宠物", "ionicons:paw", "#FFB347"},
            {"社交", "ionicons:people", "#9B59B6"},
            {"维修", "ionicons:construct", "#95A5A6"},
            {"保险", "ionicons:shield-checkmark", "#3498DB"},
            {"捐赠", "ionicons:gift", "#E74C3C"},
            {"住房", "ionicons:home", "#8B7355"},
            {"电费", "ionicons:flash", "#FFC107"},
            {"水费", "ionicons:water", "#00BCD4"},
            {"煤气费", "ionicons:flame", "#FF5722"},
            {"房租", "ionicons:key", "#9C27B0"},
            {"物业费", "ionicons:building", "#607D8B"},
            {"停车费", "ionicons:car", "#455A64"},
            {"加油", "ionicons:gas-cylinder", "#FDD835"},
            {"维保", "ionicons:settings", "#78909C"},
            {"衣服", "ionicons:shirt", "#E91E63"},
            {"鞋子", "ionicons:shoe-prints", "#C2185B"},
            {"饰品", "ionicons:sparkles", "#AB47BC"},
            {"化妆品", "ionicons:sparkles", "#6A1B9A"},
            {"书籍", "ionicons:book", "#3F51B5"},
            {"手机", "ionicons:phone-portrait", "#2196F3"},
            {"电脑", "ionicons:laptop", "#03A9F4"},
            {"相机", "ionicons:camera", "#00BCD4"},
            {"音乐", "ionicons:musical-notes", "#009688"},
            {"游戏", "ionicons:game-controller", "#4CAF50"},
            {"电影", "ionicons:film", "#8BC34A"},
            {"健身房", "ionicons:dumbbell", "#CDDC39"},
            {"瑜伽", "ionicons:body", "#FFEB3B"},
            {"美发", "ionicons:sparkles", "#FFC107"},
            {"按摩", "ionicons:hand-left", "#FF9800"},
            {"眼镜", "ionicons:glasses", "#FF5722"},
            {"牙医", "ionicons:happy", "#FFFFFF"},
            {"挂号费", "ionicons:medical", "#F44336"},
            {"药费", "ionicons:flask", "#E91E63"},
            {"检查费", "ionicons:search", "#9C27B0"},
            {"住院费", "ionicons:home", "#673AB7"},
            {"补课", "ionicons:school", "#3F51B5"},
            {"兴趣班", "ionicons:school", "#2196F3"},
            {"报名费", "ionicons:document", "#03A9F4"},
            {"考试费", "ionicons:document", "#00BCD4"},
            {"充值话费", "ionicons:phone-portrait", "#009688"},
            {"网络费", "ionicons:wifi", "#4CAF50"},
            {"电视费", "ionicons:television", "#8BC34A"},
            {"流媒体", "ionicons:play-circle", "#CDDC39"},
            {"咖啡", "ionicons:coffee", "#A1887F"},
            {"甜品", "ionicons:ice-cream", "#FF69B4"},
            {"下午茶", "ionicons:leaf", "#7CB342"},
            {"饮料", "ionicons:water", "#00ACC1"},
            {"宵夜", "ionicons:moon", "#9575CD"},
            {"外卖", "ionicons:fast-food", "#FF7043"},
            {"便利店", "ionicons:storefront", "#AB47BC"},
            {"超市", "ionicons:basket", "#42A5F5"},
            {"菜市场", "ionicons:leaf", "#66BB6A"},
            {"零食", "ionicons:cube", "#FFA726"},
            {"酒", "ionicons:wine", "#EC407A"},
            {"烟", "ionicons:flame", "#78909C"},
            {"火车", "ionicons:train", "#546E7A"},
            {"飞机", "ionicons:airplane", "#37474F"},
            {"地铁", "ionicons:subway", "#455A64"},
            {"公交", "ionicons:bus", "#607D8B"},
            {"打车", "ionicons:taxi", "#78909C"},
            {"停泊费", "ionicons:car", "#90A4AE"},
            {"过路费", "ionicons:card", "#BDBDBD"},
            {"门票", "ionicons:ticket", "#A1887F"},
            {"景区", "ionicons:mountain", "#8D6E63"},
            {"酒店", "ionicons:bed", "#795548"},
            {"民宿", "ionicons:home", "#5D4037"},
            {"团购", "ionicons:layers", "#4E342E"},
            {"包裹邮费", "ionicons:mail", "#D7CCC8"},
            {"快递", "ionicons:send", "#BCAAA4"}
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
            {"工资", "ionicons:wallet", "#34C759"},
            {"奖金", "ionicons:gift", "#FF9500"},
            {"理财", "ionicons:trending-up", "#FFD60A"},
            {"兼职", "ionicons:briefcase", "#00C7BE"},
            {"投资", "ionicons:stats-chart", "#9C88FF"},
            {"红包", "ionicons:card", "#FF6B6B"},
            {"报销", "ionicons:receipt", "#4ECDC4"},
            {"退款", "ionicons:return-down-back", "#95E1D3"},
            {"租金", "ionicons:business", "#FFA07A"},
            {"分红", "ionicons:cash", "#F38181"},
            {"兼职费", "ionicons:briefcase", "#26A69A"},
            {"稿费", "ionicons:document", "#AB47BC"},
            {"版税", "ionicons:book", "#7E57C2"},
            {"咨询费", "ionicons:chatbubble", "#5C6BC0"},
            {"服务费", "ionicons:hand-right", "#3949AB"},
            {"顾问费", "ionicons:person", "#1565C0"},
            {"培训费", "ionicons:school", "#0277BD"},
            {"讲师费", "ionicons:microphone", "#00838F"},
            {"演讲费", "ionicons:mic", "#00695C"},
            {"代购佣金", "ionicons:cart", "#004D40"},
            {"转账", "ionicons:swap-horizontal", "#1B5E20"},
            {"借款返还", "ionicons:hand-left", "#33691E"},
            {"保证金", "ionicons:key", "#558B2F"},
            {"押金返还", "ionicons:return-down-back", "#9CCC65"},
            {"奖学金", "ionicons:school", "#C0CA33"},
            {"补助", "ionicons:gift", "#F57F17"},
            {"津贴", "ionicons:card", "#FF6F00"},
            {"福利", "ionicons:heart", "#E65100"},
            {"奖励", "ionicons:star", "#BF360C"},
            {"彩票", "ionicons:dice", "#D84315"},
            {"找零", "ionicons:cash", "#6D4C41"},
            {"卖东西", "ionicons:basket", "#5D4037"},
            {"二手收入", "ionicons:swap-horizontal", "#4E342E"},
            {"闲鱼", "ionicons:fish", "#3E2723"},
            {"出租房间", "ionicons:home", "#FFFFFF"},
            {"停车费收入", "ionicons:car", "#ECEFF1"},
            {"场地租赁", "ionicons:business", "#CFD8DC"},
            {"分享收益", "ionicons:share-social", "#B0BEC5"},
            {"推荐费", "ionicons:person-add", "#90A4AE"},
            {"返利", "ionicons:local-offer", "#78909C"},
            {"积分兑换", "ionicons:gift", "#607D8B"},
            {"利息", "ionicons:trending-up", "#455A64"},
            {"分期", "ionicons:calendar", "#37474F"},
            {"保险理赔", "ionicons:shield-checkmark", "#263238"},
            {"医保报销", "ionicons:medical", "#FFFFFF"},
            {"税收退款", "ionicons:receipt", "#ECEFF1"},
            {"发票返还", "ionicons:document", "#CFD8DC"},
            {"违约金", "ionicons:alert", "#B0BEC5"},
            {"赔偿费", "ionicons:warning", "#90A4AE"},
            {"保费返还", "ionicons:card", "#78909C"},
            {"股息", "ionicons:trending-up", "#607D8B"},
            {"基金分红", "ionicons:trending-up", "#455A64"},
            {"期权收益", "ionicons:stats-chart", "#37474F"},
            {"外汇收益", "ionicons:cash", "#263238"},
            {"虚拟币收益", "ionicons:logo-bitcoin", "#FFFFFF"}
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
        return convertToResponse(entity, false);
    }

    /**
     * 转换实体为响应DTO（支持标记是否为推荐）
     * @param entity 分类实体
     * @param isRecommended 是否为系统推荐
     * @return 分类响应DTO
     */
    private CategoryResponse convertToResponse(CategoryEntity entity, boolean isRecommended) {
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
                entity.getDescription(),
                entity.getIsFrequent(),
                isRecommended
        );
    }

    /**
     * 标记为常用分类
     * @param id 分类ID
     */
    @Transactional
    public void markAsFrequent(Long id) {
        CategoryEntity entity = findById(id);
        
        // 只有用户自己创建的分类才能标记为常用（或系统分类）
        Long currentUserId = UserContext.getCurrentUserId();
        if (!entity.getIsSystem() && !entity.getCreatedByUserId().equals(currentUserId)) {
            throw new BusinessException("无法标记其他用户的分类");
        }

        if (entity.getDeleteTime() != null) {
            throw new BusinessException("分类已删除，无法标记");
        }

        entity.setIsFrequent(true);
        categoryRepository.save(entity);
    }

    /**
     * 取消标记常用分类
     * @param id 分类ID
     */
    @Transactional
    public void unmarkAsFrequent(Long id) {
        CategoryEntity entity = findById(id);
        
        // 只有用户自己创建的分类才能取消标记（或系统分类）
        Long currentUserId = UserContext.getCurrentUserId();
        if (!entity.getIsSystem() && !entity.getCreatedByUserId().equals(currentUserId)) {
            throw new BusinessException("无法取消标记其他用户的分类");
        }

        if (entity.getDeleteTime() != null) {
            throw new BusinessException("分类已删除，无法取消标记");
        }

        entity.setIsFrequent(false);
        categoryRepository.save(entity);
    }

    /**
     * 获取用户的常用分类（合并用户自定义 + 系统推荐）
     * @param type 分类类型
     * @return 常用分类列表
     */
    public List<CategoryResponse> getFrequentCategories(TransactionTypeEnum type) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException("用户未登录");
        }

        // 1. 获取用户手动标记的常用分类
        List<CategoryEntity> userFrequentEntities = categoryRepository.findFrequentCategoriesByTypeAndUserId(
                type.getCode(), currentUserId);
        List<Long> userFrequentIds = userFrequentEntities.stream()
                .map(CategoryEntity::getId)
                .collect(Collectors.toList());
        
        List<CategoryResponse> result = userFrequentEntities.stream()
                .map(entity -> convertToResponse(entity, false))
                .collect(Collectors.toList());

        // 2. 获取系统推荐的常用分类（基于最近一周的交易统计 Top3）
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        List<Object[]> topCategories = transactionRepository.findTopCategoriesByUsageInLastWeek(
                currentUserId, type.getCode(), oneWeekAgo);
        
        // 取前3个，并排除已被用户手动标记的
        int recommendCount = 0;
        for (Object[] row : topCategories) {
            if (recommendCount >= 3) {
                break;
            }
            
            Long categoryId = ((Number) row[0]).longValue();
            // 跳过已在用户常用列表中的分类
            if (userFrequentIds.contains(categoryId)) {
                continue;
            }
            
            // 查询分类详情并添加到结果（标记为推荐）
            categoryRepository.findById(categoryId).ifPresent(entity -> {
                if (entity.getDeleteTime() == null) {
                    result.add(convertToResponse(entity, true));
                }
            });
            recommendCount++;
        }

        return result;
    }
}