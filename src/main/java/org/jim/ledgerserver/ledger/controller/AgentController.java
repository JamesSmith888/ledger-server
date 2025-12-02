package org.jim.ledgerserver.ledger.controller;

import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import org.jim.ledgerserver.common.JSONResult;
import org.jim.ledgerserver.common.util.UserContext;
import org.jim.ledgerserver.ledger.entity.CategoryEntity;
import org.jim.ledgerserver.ledger.entity.LedgerEntity;
import org.jim.ledgerserver.ledger.entity.PaymentMethodEntity;
import org.jim.ledgerserver.ledger.entity.TransactionEntity;
import org.jim.ledgerserver.ledger.repository.TransactionRepository;
import org.jim.ledgerserver.ledger.repository.LedgerRepository;
import org.jim.ledgerserver.ledger.repository.CategoryRepository;
import org.jim.ledgerserver.ledger.repository.PaymentMethodRepository;
import org.jim.ledgerserver.ledger.service.*;
import org.jim.ledgerserver.ledger.vo.agent.*;
import org.jim.ledgerserver.common.enums.TransactionSourceEnum;
import org.jim.ledgerserver.common.enums.TransactionTypeEnum;
import org.jim.ledgerserver.user.entity.UserEntity;
import org.jim.ledgerserver.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Agent 专用 API Controller
 * 
 * 专门为 AI Agent 设计的接口，特点：
 * 1. 返回完整的关联数据（如分类名称、账本名称等），便于 AI 直接展示给用户
 * 2. 与前端页面接口分离，可独立演进
 * 3. 便于监控和限流 AI 调用
 * 
 * @author James Smith
 */
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    @Resource
    private TransactionService transactionService;

    @Resource
    private LedgerService ledgerService;

    @Resource
    private LedgerMemberService ledgerMemberService;

    @Resource
    private CategoryService categoryService;

    @Resource
    private PaymentMethodService paymentMethodService;

    @Resource
    private UserRepository userRepository;

    @Resource
    private TransactionAttachmentService attachmentService;

    @Resource
    private LedgerRepository ledgerRepository;

    @Resource
    private CategoryRepository categoryRepository;

    @Resource
    private PaymentMethodRepository paymentMethodRepository;

    /**
     * 创建交易 - Agent 专用
     * 返回完整的交易记录，包含所有关联信息
     */
    @PostMapping("/transactions/create")
    public JSONResult<AgentTransactionResp> createTransaction(@RequestBody AgentCreateTransactionReq request) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return JSONResult.fail("用户未登录");
        }

        // 验证账本权限
        if (!hasTransactionEditPermission(request.ledgerId(), currentUserId)) {
            return JSONResult.fail("无权限在该账本中创建交易");
        }

        // 创建交易（来源为 AI）
        TransactionEntity transaction = transactionService.create(
                request.description(),
                request.amount(),
                request.type().getCode(),
                request.transactionDateTime() != null ? request.transactionDateTime() : LocalDateTime.now(),
                request.ledgerId(),
                request.categoryId(),
                request.paymentMethodId(),
                TransactionSourceEnum.AI.getCode()
        );

        // 构建完整的响应数据
        AgentTransactionResp response = buildAgentTransactionResp(transaction);

        return JSONResult.success(response);
    }

    /**
     * 根据ID获取交易详情 - Agent 专用
     * 返回完整的交易记录，包含所有关联信息
     */
    @GetMapping("/transactions/{id}")
    public JSONResult<AgentTransactionResp> getTransaction(@PathVariable Long id) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return JSONResult.fail("用户未登录");
        }

        TransactionEntity transaction = transactionService.findById(id);
        
        // 验证查看权限
        if (!canViewTransaction(transaction, currentUserId)) {
            return JSONResult.fail("无权限查看该交易");
        }

        AgentTransactionResp response = buildAgentTransactionResp(transaction);

        return JSONResult.success(response);
    }

    /**
     * 查询交易列表 - Agent 专用
     * 支持多种筛选条件，返回完整的交易信息和汇总统计
     */
    @PostMapping("/transactions/query")
    public JSONResult<AgentTransactionListResp> queryTransactions(@RequestBody AgentQueryTransactionReq request) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return JSONResult.fail("用户未登录");
        }

        // 验证账本权限
        if (request.ledgerId() != null && !canViewLedger(request.ledgerId(), currentUserId)) {
            return JSONResult.fail("无权限查看该账本的交易");
        }

        // 构建排序
        Sort.Direction direction = "ASC".equalsIgnoreCase(request.sortDirection()) 
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortField = "transactionDateTime";
        if ("amount".equalsIgnoreCase(request.sortBy())) {
            sortField = "amount";
        }
        Sort sort = Sort.by(direction, sortField);
        
        // 构建分页
        Pageable pageable = PageRequest.of(request.page(), request.size(), sort);

        // 构建查询条件
        Specification<TransactionEntity> spec = buildQuerySpecification(request, currentUserId);
        
        // 执行查询
        Page<TransactionEntity> page = transactionRepository.findAll(spec, pageable);

        // 转换为响应对象（使用批量查询优化）
        List<AgentTransactionResp> transactions = buildAgentTransactionRespBatch(page.getContent());

        // 计算当前页的汇总统计
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        for (TransactionEntity tx : page.getContent()) {
            if (tx.getType() == TransactionTypeEnum.INCOME.getCode()) {
                totalIncome = totalIncome.add(tx.getAmount());
            } else if (tx.getType() == TransactionTypeEnum.EXPENSE.getCode()) {
                totalExpense = totalExpense.add(tx.getAmount());
            }
        }
        BigDecimal balance = totalIncome.subtract(totalExpense);

        AgentTransactionListResp response = new AgentTransactionListResp(
                transactions,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                totalIncome,
                totalExpense,
                balance,
                page.getContent().size()
        );

        return JSONResult.success(response);
    }

    /**
     * 搜索交易 - Agent 专用
     * 通过关键词搜索交易（名称、描述）
     */
    @GetMapping("/transactions/search")
    public JSONResult<AgentTransactionListResp> searchTransactions(
            @RequestParam(required = false) Long ledgerId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return JSONResult.fail("用户未登录");
        }

        if (ledgerId != null && !canViewLedger(ledgerId, currentUserId)) {
            return JSONResult.fail("无权限查看该账本");
        }

        // 构建搜索条件
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "transactionDateTime"));
        
        Specification<TransactionEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 未删除
            predicates.add(cb.isNull(root.get("deleteTime")));
            
            // 账本或用户筛选
            if (ledgerId != null) {
                predicates.add(cb.equal(root.get("ledgerId"), ledgerId));
            } else {
                predicates.add(cb.equal(root.get("createdByUserId"), currentUserId));
            }
            
            // 关键词搜索（名称或描述）
            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        Page<TransactionEntity> pageResult = transactionRepository.findAll(spec, pageable);
        
        // 使用批量查询优化
        List<AgentTransactionResp> transactions = buildAgentTransactionRespBatch(pageResult.getContent());

        // 计算汇总
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        for (TransactionEntity tx : pageResult.getContent()) {
            if (tx.getType() == TransactionTypeEnum.INCOME.getCode()) {
                totalIncome = totalIncome.add(tx.getAmount());
            } else {
                totalExpense = totalExpense.add(tx.getAmount());
            }
        }

        AgentTransactionListResp response = new AgentTransactionListResp(
                transactions,
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.isFirst(),
                pageResult.isLast(),
                totalIncome,
                totalExpense,
                totalIncome.subtract(totalExpense),
                pageResult.getContent().size()
        );

        return JSONResult.success(response);
    }

    /**
     * 获取最近交易 - Agent 专用
     * 快速获取最近 N 条交易记录
     */
    @GetMapping("/transactions/recent")
    public JSONResult<List<AgentTransactionResp>> getRecentTransactions(
            @RequestParam(required = false) Long ledgerId,
            @RequestParam(defaultValue = "10") int limit) {
        
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return JSONResult.fail("用户未登录");
        }

        if (ledgerId != null && !canViewLedger(ledgerId, currentUserId)) {
            return JSONResult.fail("无权限查看该账本");
        }

        // 限制最大数量
        limit = Math.min(limit, 50);
        
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "transactionDateTime"));
        
        Specification<TransactionEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deleteTime")));
            
            if (ledgerId != null) {
                predicates.add(cb.equal(root.get("ledgerId"), ledgerId));
            } else {
                predicates.add(cb.equal(root.get("createdByUserId"), currentUserId));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        Page<TransactionEntity> page = transactionRepository.findAll(spec, pageable);
        
        // 使用批量查询优化
        List<AgentTransactionResp> transactions = buildAgentTransactionRespBatch(page.getContent());

        return JSONResult.success(transactions);
    }

    /**
     * 构建查询条件
     */
    private Specification<TransactionEntity> buildQuerySpecification(AgentQueryTransactionReq request, Long userId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 未删除
            predicates.add(cb.isNull(root.get("deleteTime")));
            
            // 账本筛选
            if (request.ledgerId() != null) {
                predicates.add(cb.equal(root.get("ledgerId"), request.ledgerId()));
            } else {
                predicates.add(cb.equal(root.get("createdByUserId"), userId));
            }
            
            // 交易类型
            if (request.type() != null && !request.type().isEmpty()) {
                TransactionTypeEnum typeEnum = TransactionTypeEnum.valueOf(request.type());
                predicates.add(cb.equal(root.get("type"), typeEnum.getCode()));
            }
            
            // 分类ID
            if (request.categoryId() != null) {
                predicates.add(cb.equal(root.get("categoryId"), request.categoryId()));
            }
            
            // 时间范围
            if (request.startTime() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("transactionDateTime"), request.startTime()));
            }
            if (request.endTime() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("transactionDateTime"), request.endTime()));
            }
            
            // 金额范围
            if (request.minAmount() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), request.minAmount()));
            }
            if (request.maxAmount() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("amount"), request.maxAmount()));
            }
            
            // 关键词搜索（TransactionEntity 只有 description 字段，没有 name）
            if (request.keyword() != null && !request.keyword().trim().isEmpty()) {
                String pattern = "%" + request.keyword().trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("description")), pattern));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 检查用户是否有账本查看权限
     */
    private boolean canViewLedger(Long ledgerId, Long userId) {
        try {
            LedgerEntity ledger = ledgerService.findById(ledgerId);
            if (ledger.getOwnerUserId().equals(userId)) {
                return true;
            }
            return ledgerMemberService.hasViewPermission(ledgerId, userId);
        } catch (Exception e) {
            return false;
        }
    }

    @Resource
    private TransactionRepository transactionRepository;

    /**
     * 构建 Agent 专用的交易响应对象
     * 填充所有关联信息
     */
    private AgentTransactionResp buildAgentTransactionResp(TransactionEntity tx) {
        // 获取账本信息
        String ledgerName = null;
        if (tx.getLedgerId() != null) {
            try {
                LedgerEntity ledger = ledgerService.findById(tx.getLedgerId());
                ledgerName = ledger.getName();
            } catch (Exception e) {
                // 忽略
            }
        }

        // 获取分类信息
        String categoryName = null;
        String categoryIcon = null;
        if (tx.getCategoryId() != null) {
            try {
                CategoryEntity category = categoryService.findById(tx.getCategoryId());
                categoryName = category.getName();
                categoryIcon = category.getIcon();
            } catch (Exception e) {
                // 忽略
            }
        }

        // 获取支付方式信息
        String paymentMethodName = null;
        if (tx.getPaymentMethodId() != null) {
            try {
                PaymentMethodEntity paymentMethod = paymentMethodService.findById(tx.getPaymentMethodId());
                paymentMethodName = paymentMethod.getName();
            } catch (Exception e) {
                // 忽略
            }
        }

        // 获取创建人信息
        String createdByUserName = null;
        String createdByUserNickname = null;
        if (tx.getCreatedByUserId() != null) {
            try {
                UserEntity user = userRepository.findById(tx.getCreatedByUserId()).orElse(null);
                if (user != null) {
                    createdByUserName = user.getUsername();
                    createdByUserNickname = user.getNickname();
                }
            } catch (Exception e) {
                // 忽略
            }
        }

        // 获取附件数量
        long attachmentCount = 0;
        try {
            attachmentCount = attachmentService.countAttachments(tx.getId());
        } catch (Exception e) {
            // 忽略
        }

        return new AgentTransactionResp(
                tx.getId(),
                tx.getDescription(),
                tx.getAmount(),
                TransactionTypeEnum.getByCode(tx.getType()),
                tx.getTransactionDateTime(),
                tx.getLedgerId(),
                ledgerName,
                tx.getCategoryId(),
                categoryName,
                categoryIcon,
                tx.getPaymentMethodId(),
                paymentMethodName,
                tx.getCreatedByUserId(),
                createdByUserName,
                createdByUserNickname,
                attachmentCount,
                TransactionSourceEnum.getByCode(tx.getSource())
        );
    }

    /**
     * 批量构建 Agent 专用的交易响应对象
     * 使用批量查询优化性能，避免 N+1 问题
     */
    private List<AgentTransactionResp> buildAgentTransactionRespBatch(List<TransactionEntity> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return new ArrayList<>();
        }

        // 收集所有需要查询的 ID
        Set<Long> ledgerIds = new HashSet<>();
        Set<Long> categoryIds = new HashSet<>();
        Set<Long> paymentMethodIds = new HashSet<>();
        Set<Long> userIds = new HashSet<>();
        Set<Long> transactionIds = new HashSet<>();

        for (TransactionEntity tx : transactions) {
            if (tx.getLedgerId() != null) ledgerIds.add(tx.getLedgerId());
            if (tx.getCategoryId() != null) categoryIds.add(tx.getCategoryId());
            if (tx.getPaymentMethodId() != null) paymentMethodIds.add(tx.getPaymentMethodId());
            if (tx.getCreatedByUserId() != null) userIds.add(tx.getCreatedByUserId());
            transactionIds.add(tx.getId());
        }

        // 批量查询关联数据
        Map<Long, LedgerEntity> ledgerMap = ledgerIds.isEmpty() ? Collections.emptyMap() :
                ledgerRepository.findAllById(ledgerIds).stream()
                        .collect(Collectors.toMap(LedgerEntity::getId, e -> e));

        Map<Long, CategoryEntity> categoryMap = categoryIds.isEmpty() ? Collections.emptyMap() :
                categoryRepository.findAllById(categoryIds).stream()
                        .collect(Collectors.toMap(CategoryEntity::getId, e -> e));

        Map<Long, PaymentMethodEntity> paymentMethodMap = paymentMethodIds.isEmpty() ? Collections.emptyMap() :
                paymentMethodRepository.findAllById(paymentMethodIds).stream()
                        .collect(Collectors.toMap(PaymentMethodEntity::getId, e -> e));

        Map<Long, UserEntity> userMap = userIds.isEmpty() ? Collections.emptyMap() :
                userRepository.findAllById(userIds).stream()
                        .collect(Collectors.toMap(UserEntity::getId, e -> e));

        // 批量查询附件数量
        Map<Long, Long> attachmentCountMap = attachmentService.countAttachmentsByTransactionIds(new ArrayList<>(transactionIds));

        // 构建响应列表
        List<AgentTransactionResp> result = new ArrayList<>(transactions.size());
        for (TransactionEntity tx : transactions) {
            LedgerEntity ledger = tx.getLedgerId() != null ? ledgerMap.get(tx.getLedgerId()) : null;
            CategoryEntity category = tx.getCategoryId() != null ? categoryMap.get(tx.getCategoryId()) : null;
            PaymentMethodEntity paymentMethod = tx.getPaymentMethodId() != null ? paymentMethodMap.get(tx.getPaymentMethodId()) : null;
            UserEntity user = tx.getCreatedByUserId() != null ? userMap.get(tx.getCreatedByUserId()) : null;
            long attachmentCount = attachmentCountMap.getOrDefault(tx.getId(), 0L);

            result.add(new AgentTransactionResp(
                    tx.getId(),
                    tx.getDescription(),
                    tx.getAmount(),
                    TransactionTypeEnum.getByCode(tx.getType()),
                    tx.getTransactionDateTime(),
                    tx.getLedgerId(),
                    ledger != null ? ledger.getName() : null,
                    tx.getCategoryId(),
                    category != null ? category.getName() : null,
                    category != null ? category.getIcon() : null,
                    tx.getPaymentMethodId(),
                    paymentMethod != null ? paymentMethod.getName() : null,
                    tx.getCreatedByUserId(),
                    user != null ? user.getUsername() : null,
                    user != null ? user.getNickname() : null,
                    attachmentCount,
                    TransactionSourceEnum.getByCode(tx.getSource())
            ));
        }

        return result;
    }

    /**
     * 检查用户是否有交易编辑权限
     */
    private boolean hasTransactionEditPermission(Long ledgerId, Long userId) {
        try {
            LedgerEntity ledger = ledgerService.findById(ledgerId);
            
            // 所有者总是有权限
            if (ledger.getOwnerUserId().equals(userId)) {
                return true;
            }
            
            // 个人账本只有所有者可以编辑
            if (ledger.isPersonal()) {
                return false;
            }
            
            // 共享账本检查成员权限
            return ledgerMemberService.hasEditPermission(ledgerId, userId);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查用户是否可以查看交易
     */
    private boolean canViewTransaction(TransactionEntity transaction, Long userId) {
        // 创建者可以查看
        if (userId.equals(transaction.getCreatedByUserId())) {
            return true;
        }
        
        // 账本成员可以查看
        if (transaction.getLedgerId() != null) {
            try {
                LedgerEntity ledger = ledgerService.findById(transaction.getLedgerId());
                if (ledger.getOwnerUserId().equals(userId)) {
                    return true;
                }
                // 使用 hasViewPermission 检查是否是账本成员
                return ledgerMemberService.hasViewPermission(transaction.getLedgerId(), userId);
            } catch (Exception e) {
                return false;
            }
        }
        
        return false;
    }

    // ==================== 新增 Agent 专用 API ====================

    /**
     * 更新交易 - Agent 专用
     * 支持部分更新，只更新提供的字段
     */
    @PutMapping("/transactions/{id}")
    public JSONResult<AgentTransactionResp> updateTransaction(
            @PathVariable Long id,
            @RequestBody AgentUpdateTransactionReq request) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return JSONResult.fail("用户未登录");
        }

        try {
            TransactionEntity transaction = transactionService.findById(id);
            
            // 验证权限
            if (!canEditTransaction(transaction, currentUserId)) {
                return JSONResult.fail("无权限修改该交易");
            }

            // 更新提供的字段
            if (request.description() != null) {
                transaction.setDescription(request.description());
            }
            if (request.amount() != null) {
                transaction.setAmount(request.amount());
            }
            if (request.type() != null) {
                transaction.setType(request.type().getCode());
            }
            if (request.categoryId() != null) {
                transaction.setCategoryId(request.categoryId());
            }
            if (request.paymentMethodId() != null) {
                transaction.setPaymentMethodId(request.paymentMethodId());
            }
            if (request.transactionDateTime() != null) {
                transaction.setTransactionDateTime(request.transactionDateTime());
            }

            TransactionEntity updated = transactionService.update(transaction);
            return JSONResult.success(buildAgentTransactionResp(updated));
        } catch (Exception e) {
            return JSONResult.fail("更新失败: " + e.getMessage());
        }
    }

    /**
     * 删除交易 - Agent 专用
     * 逻辑删除，可恢复
     */
    @DeleteMapping("/transactions/{id}")
    public JSONResult<Void> deleteTransaction(@PathVariable Long id) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return JSONResult.fail("用户未登录");
        }

        try {
            TransactionEntity transaction = transactionService.findById(id);
            
            // 验证权限
            if (!canEditTransaction(transaction, currentUserId)) {
                return JSONResult.fail("无权限删除该交易");
            }

            transactionService.delete(id);
            return JSONResult.success(null);
        } catch (Exception e) {
            return JSONResult.fail("删除失败: " + e.getMessage());
        }
    }

    /**
     * 批量创建交易 - Agent 专用
     * 适合从图片/文字批量导入交易场景
     */
    @PostMapping("/transactions/batch-create")
    public JSONResult<AgentBatchResultResp> batchCreateTransactions(
            @RequestBody AgentBatchCreateTransactionReq request) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return JSONResult.fail("用户未登录");
        }

        // 验证账本权限
        if (!hasTransactionEditPermission(request.ledgerId(), currentUserId)) {
            return JSONResult.fail("无权限在该账本中创建交易");
        }

        if (request.transactions() == null || request.transactions().isEmpty()) {
            return JSONResult.fail("交易列表不能为空");
        }

        // 限制单次批量数量
        if (request.transactions().size() > 50) {
            return JSONResult.fail("单次最多创建50条交易");
        }

        List<AgentTransactionResp> successItems = new ArrayList<>();
        List<AgentBatchResultResp.FailedItem> failedItems = new ArrayList<>();

        for (int i = 0; i < request.transactions().size(); i++) {
            AgentBatchCreateTransactionReq.TransactionItem item = request.transactions().get(i);
            try {
                TransactionEntity transaction = transactionService.create(
                        item.description(),
                        item.amount(),
                        item.type().getCode(),
                        item.transactionDateTime() != null ? item.transactionDateTime() : LocalDateTime.now(),
                        request.ledgerId(),
                        item.categoryId(),
                        item.paymentMethodId(),
                        TransactionSourceEnum.AI.getCode()  // 批量创建也标记为 AI 来源
                );
                successItems.add(buildAgentTransactionResp(transaction));
            } catch (Exception e) {
                failedItems.add(new AgentBatchResultResp.FailedItem(i, item.description(), e.getMessage()));
            }
        }

        String message = String.format("批量创建完成：成功 %d 条，失败 %d 条",
                successItems.size(), failedItems.size());

        return JSONResult.success(new AgentBatchResultResp(
                successItems.size(),
                failedItems.size(),
                successItems,
                failedItems,
                message
        ));
    }

    /**
     * 获取统计报表 - Agent 专用
     * 支持按分类统计，返回收支汇总和各分类占比
     */
    @GetMapping("/statistics")
    public JSONResult<AgentStatisticsResp> getStatistics(
            @RequestParam(required = false) Long ledgerId,
            @RequestParam String startTime,
            @RequestParam String endTime,
            @RequestParam(required = false) String type) {
        
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return JSONResult.fail("用户未登录");
        }

        if (ledgerId != null && !canViewLedger(ledgerId, currentUserId)) {
            return JSONResult.fail("无权限查看该账本");
        }

        try {
            LocalDateTime start = parseDateTime(startTime);
            LocalDateTime end = parseDateTimeAsEnd(endTime);
            final Integer typeCode = (type != null && !type.isEmpty()) 
                    ? TransactionTypeEnum.valueOf(type).getCode() 
                    : null;

            // 构建查询条件
            Specification<TransactionEntity> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.isNull(root.get("deleteTime")));
                
                if (ledgerId != null) {
                    predicates.add(cb.equal(root.get("ledgerId"), ledgerId));
                } else {
                    predicates.add(cb.equal(root.get("createdByUserId"), currentUserId));
                }
                
                predicates.add(cb.greaterThanOrEqualTo(root.get("transactionDateTime"), start));
                predicates.add(cb.lessThanOrEqualTo(root.get("transactionDateTime"), end));
                
                if (typeCode != null) {
                    predicates.add(cb.equal(root.get("type"), typeCode));
                }
                
                return cb.and(predicates.toArray(new Predicate[0]));
            };

            List<TransactionEntity> transactions = transactionRepository.findAll(spec);

            // 计算汇总
            BigDecimal totalIncome = BigDecimal.ZERO;
            BigDecimal totalExpense = BigDecimal.ZERO;
            for (TransactionEntity tx : transactions) {
                if (tx.getType() == TransactionTypeEnum.INCOME.getCode()) {
                    totalIncome = totalIncome.add(tx.getAmount());
                } else if (tx.getType() == TransactionTypeEnum.EXPENSE.getCode()) {
                    totalExpense = totalExpense.add(tx.getAmount());
                }
            }

            // 按分类分组统计
            BigDecimal totalAmount = typeCode != null 
                    ? (typeCode == 1 ? totalIncome : totalExpense)
                    : totalIncome.add(totalExpense);
            
            Map<Long, List<TransactionEntity>> byCategory = transactions.stream()
                    .filter(t -> t.getCategoryId() != null)
                    .collect(Collectors.groupingBy(TransactionEntity::getCategoryId));

            final BigDecimal finalTotalAmount = totalAmount;
            List<AgentStatisticsResp.CategoryStat> categoryStats = byCategory.entrySet().stream()
                    .map(entry -> {
                        Long catId = entry.getKey();
                        List<TransactionEntity> txList = entry.getValue();
                        
                        BigDecimal amount = txList.stream()
                                .map(TransactionEntity::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        
                        String catName = "未知分类";
                        String catIcon = "📁";
                        try {
                            CategoryEntity cat = categoryService.findById(catId);
                            catName = cat.getName();
                            catIcon = cat.getIcon();
                        } catch (Exception ignored) {}
                        
                        double percentage = finalTotalAmount.compareTo(BigDecimal.ZERO) > 0
                                ? amount.divide(finalTotalAmount, 4, RoundingMode.HALF_UP)
                                        .multiply(BigDecimal.valueOf(100))
                                        .doubleValue()
                                : 0.0;
                        
                        return new AgentStatisticsResp.CategoryStat(
                                catId, catName, catIcon, amount, (long) txList.size(), percentage
                        );
                    })
                    .sorted((a, b) -> b.amount().compareTo(a.amount()))
                    .collect(Collectors.toList());

            return JSONResult.success(new AgentStatisticsResp(
                    totalIncome,
                    totalExpense,
                    totalIncome.subtract(totalExpense),
                    (long) transactions.size(),
                    categoryStats,
                    startTime,
                    endTime
            ));
        } catch (Exception e) {
            return JSONResult.fail("获取统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取分类列表 - Agent 专用
     * 返回用户可见的所有分类（系统分类 + 用户自定义分类）
     */
    @GetMapping("/categories")
    public JSONResult<List<AgentCategoryResp>> getCategories(
            @RequestParam(required = false) Long ledgerId,
            @RequestParam(required = false) String type) {
        
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return JSONResult.fail("用户未登录");
        }

        if (ledgerId != null && !canViewLedger(ledgerId, currentUserId)) {
            return JSONResult.fail("无权限查看该账本");
        }

        try {
            // 获取用户可见的所有分类
            var categoryResponses = categoryService.getAllCategories();
            
            // 筛选类型
            if (type != null && !type.isEmpty()) {
                int typeCode = TransactionTypeEnum.valueOf(type).getCode();
                categoryResponses = categoryResponses.stream()
                        .filter(c -> c.type().getCode() == typeCode)
                        .collect(Collectors.toList());
            }

            // 获取账本名称
            String ledgerName = "";
            if (ledgerId != null) {
                try {
                    ledgerName = ledgerService.findById(ledgerId).getName();
                } catch (Exception ignored) {}
            }

            final String finalLedgerName = ledgerName;
            
            List<AgentCategoryResp> result = categoryResponses.stream()
                    .map(cat -> new AgentCategoryResp(
                            cat.id(),
                            cat.name(),
                            cat.icon(),
                            cat.type().getCode(),
                            cat.type().getCode() == 1 ? "收入" : "支出",
                            ledgerId,
                            finalLedgerName,
                            null,  // parentId (not available in current CategoryResponse)
                            null,  // parentName
                            cat.sortOrder(),
                            0L     // transactionCount (expensive to calculate, skip for list)
                    ))
                    .collect(Collectors.toList());

            return JSONResult.success(result);
        } catch (Exception e) {
            return JSONResult.fail("获取分类失败: " + e.getMessage());
        }
    }

    /**
     * 创建分类 - Agent 专用
     */
    @PostMapping("/categories")
    public JSONResult<AgentCategoryResp> createCategory(@RequestBody AgentCreateCategoryReq request) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return JSONResult.fail("用户未登录");
        }

        try {
            // 使用现有的 CategoryService.createCategory 方法
            var createRequest = new org.jim.ledgerserver.ledger.dto.CreateCategoryRequest(
                    request.name(),
                    request.icon(),
                    null, // color
                    request.type(),
                    null, // sortOrder
                    null  // description
            );
            
            var categoryResponse = categoryService.createCategory(createRequest);

            // 获取账本名称
            String ledgerName = "";
            if (request.ledgerId() != null) {
                try {
                    ledgerName = ledgerService.findById(request.ledgerId()).getName();
                } catch (Exception ignored) {}
            }

            return JSONResult.success(new AgentCategoryResp(
                    categoryResponse.id(),
                    categoryResponse.name(),
                    categoryResponse.icon(),
                    categoryResponse.type().getCode(),
                    categoryResponse.type().getCode() == 1 ? "收入" : "支出",
                    request.ledgerId(),
                    ledgerName,
                    null,
                    null,
                    categoryResponse.sortOrder(),
                    0L
            ));
        } catch (Exception e) {
            return JSONResult.fail("创建分类失败: " + e.getMessage());
        }
    }

    /**
     * 获取支付方式列表 - Agent 专用
     */
    @GetMapping("/payment-methods")
    public JSONResult<List<PaymentMethodEntity>> getPaymentMethods() {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return JSONResult.fail("用户未登录");
        }

        try {
            List<PaymentMethodEntity> methods = paymentMethodService.findByUserId(currentUserId);
            return JSONResult.success(methods);
        } catch (Exception e) {
            return JSONResult.fail("获取支付方式失败: " + e.getMessage());
        }
    }

    /**
     * 创建支付方式 - Agent 专用
     */
    @PostMapping("/payment-methods")
    public JSONResult<PaymentMethodEntity> createPaymentMethod(
            @RequestParam String name,
            @RequestParam(required = false) String icon) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return JSONResult.fail("用户未登录");
        }

        try {
            PaymentMethodEntity method = paymentMethodService.create(
                    name, 
                    icon != null ? icon : "💳", 
                    "CUSTOM",  // type
                    currentUserId, 
                    false,     // isDefault
                    null       // sortOrder
            );
            return JSONResult.success(method);
        } catch (Exception e) {
            return JSONResult.fail("创建支付方式失败: " + e.getMessage());
        }
    }

    /**
     * 检查用户是否可以编辑交易
     */
    private boolean canEditTransaction(TransactionEntity transaction, Long userId) {
        // 创建者可以编辑
        if (userId.equals(transaction.getCreatedByUserId())) {
            return true;
        }
        
        // 账本成员可以编辑
        if (transaction.getLedgerId() != null) {
            return hasTransactionEditPermission(transaction.getLedgerId(), userId);
        }
        
        return false;
    }

    /**
     * 解析时间字符串（作为开始时间，当天 00:00:00）
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        return parseDateTimeInternal(dateTimeStr, false);
    }

    /**
     * 解析时间字符串作为结束时间（当天 23:59:59.999999999）
     */
    private LocalDateTime parseDateTimeAsEnd(String dateTimeStr) {
        return parseDateTimeInternal(dateTimeStr, true);
    }

    /**
     * 内部时间解析方法
     * @param dateTimeStr 时间字符串
     * @param asEndOfDay 如果是纯日期格式，是否解析为当天结束时间
     */
    private LocalDateTime parseDateTimeInternal(String dateTimeStr, boolean asEndOfDay) {
        if (dateTimeStr == null) return null;
        try {
            // 尝试完整的 LocalDateTime 格式 (2025-11-28T12:30:00)
            return LocalDateTime.parse(dateTimeStr);
        } catch (Exception e) {
            try {
                // 尝试 ZonedDateTime 格式
                return java.time.ZonedDateTime.parse(dateTimeStr).toLocalDateTime();
            } catch (Exception ex) {
                // 尝试日期格式 (2025-11-28)
                try {
                    java.time.LocalDate date = java.time.LocalDate.parse(dateTimeStr);
                    if (asEndOfDay) {
                        // 结束时间：当天 23:59:59.999999999
                        return date.atTime(java.time.LocalTime.MAX);
                    } else {
                        // 开始时间：当天 00:00:00
                        return date.atStartOfDay();
                    }
                } catch (Exception ex2) {
                    throw new RuntimeException("时间格式错误: " + dateTimeStr);
                }
            }
        }
    }

    // ==================== 增强分析 API ====================

    /**
     * 统一分析接口 - Agent 专用
     * 支持多种分析类型：summary/trend/category_breakdown/comparison/ranking
     */
    @PostMapping("/analyze")
    public JSONResult<AgentAnalysisResp> analyze(@RequestBody AgentAnalysisReq request) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return JSONResult.fail("用户未登录");
        }

        if (request.ledgerId() != null && !canViewLedger(request.ledgerId(), currentUserId)) {
            return JSONResult.fail("无权限查看该账本");
        }

        try {
            LocalDateTime start = parseDateTime(request.startTime());
            LocalDateTime end = parseDateTimeAsEnd(request.endTime());
            long days = java.time.temporal.ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate()) + 1;
            
            // 获取交易数据
            List<TransactionEntity> transactions = queryTransactionsForAnalysis(
                    request.ledgerId(), currentUserId, start, end, 
                    request.type(), request.categoryIds()
            );

            // 根据分析类型处理
            String analysisType = request.analysisType() != null ? request.analysisType().toLowerCase() : "summary";
            
            return switch (analysisType) {
                case "trend" -> JSONResult.success(buildTrendAnalysis(request, transactions, days));
                case "category_breakdown" -> JSONResult.success(buildCategoryBreakdown(request, transactions, days));
                case "comparison" -> JSONResult.success(buildComparison(request, currentUserId, transactions, days));
                case "ranking" -> JSONResult.success(buildRanking(request, transactions, days));
                default -> JSONResult.success(buildSummaryAnalysis(request, transactions, days));
            };
        } catch (Exception e) {
            return JSONResult.fail("分析失败: " + e.getMessage());
        }
    }

    /**
     * 查询分析用的交易数据
     */
    private List<TransactionEntity> queryTransactionsForAnalysis(
            Long ledgerId, Long userId,
            LocalDateTime start, LocalDateTime end,
            String type, List<Long> categoryIds
    ) {
        final Integer typeCode = (type != null && !type.isEmpty()) 
                ? TransactionTypeEnum.valueOf(type).getCode() 
                : null;

        Specification<TransactionEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deleteTime")));
            
            if (ledgerId != null) {
                predicates.add(cb.equal(root.get("ledgerId"), ledgerId));
            } else {
                predicates.add(cb.equal(root.get("createdByUserId"), userId));
            }
            
            predicates.add(cb.greaterThanOrEqualTo(root.get("transactionDateTime"), start));
            predicates.add(cb.lessThanOrEqualTo(root.get("transactionDateTime"), end));
            
            if (typeCode != null) {
                predicates.add(cb.equal(root.get("type"), typeCode));
            }
            
            if (categoryIds != null && !categoryIds.isEmpty()) {
                predicates.add(root.get("categoryId").in(categoryIds));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return transactionRepository.findAll(spec);
    }

    /**
     * 构建汇总分析
     */
    private AgentAnalysisResp buildSummaryAnalysis(
            AgentAnalysisReq request, 
            List<TransactionEntity> transactions, 
            long days
    ) {
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        
        for (TransactionEntity tx : transactions) {
            if (tx.getType() == TransactionTypeEnum.INCOME.getCode()) {
                totalIncome = totalIncome.add(tx.getAmount());
            } else {
                totalExpense = totalExpense.add(tx.getAmount());
            }
        }

        // 按分类统计
        List<AgentAnalysisResp.CategoryDetail> categoryBreakdown = buildCategoryDetails(transactions, days);

        return AgentAnalysisResp.summary(
                request.startTime(), request.endTime(),
                totalIncome, totalExpense,
                (long) transactions.size(), days,
                categoryBreakdown
        );
    }

    /**
     * 构建趋势分析
     */
    private AgentAnalysisResp buildTrendAnalysis(
            AgentAnalysisReq request,
            List<TransactionEntity> transactions,
            long days
    ) {
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        
        for (TransactionEntity tx : transactions) {
            if (tx.getType() == TransactionTypeEnum.INCOME.getCode()) {
                totalIncome = totalIncome.add(tx.getAmount());
            } else {
                totalExpense = totalExpense.add(tx.getAmount());
            }
        }

        String groupBy = request.groupBy() != null ? request.groupBy().toLowerCase() : "day";
        List<AgentAnalysisResp.TrendPoint> trendData = new ArrayList<>();

        // 按时间分组
        var grouped = transactions.stream()
                .collect(Collectors.groupingBy(tx -> {
                    LocalDateTime dt = tx.getTransactionDateTime();
                    return switch (groupBy) {
                        case "week" -> dt.toLocalDate().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).toString();
                        case "month" -> dt.getYear() + "-" + String.format("%02d", dt.getMonthValue());
                        default -> dt.toLocalDate().toString(); // day
                    };
                }));

        // 排序并构建趋势点
        grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String date = entry.getKey();
                    List<TransactionEntity> txList = entry.getValue();
                    
                    BigDecimal income = txList.stream()
                            .filter(t -> t.getType() == TransactionTypeEnum.INCOME.getCode())
                            .map(TransactionEntity::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal expense = txList.stream()
                            .filter(t -> t.getType() == TransactionTypeEnum.EXPENSE.getCode())
                            .map(TransactionEntity::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    String label = formatTrendLabel(date, groupBy);
                    
                    trendData.add(new AgentAnalysisResp.TrendPoint(
                            label, date, income, expense,
                            income.subtract(expense), (long) txList.size()
                    ));
                });

        return AgentAnalysisResp.trend(
                request.startTime(), request.endTime(), groupBy,
                totalIncome, totalExpense,
                (long) transactions.size(), days,
                trendData
        );
    }

    /**
     * 格式化趋势标签
     */
    private String formatTrendLabel(String date, String groupBy) {
        return switch (groupBy) {
            case "week" -> {
                var d = java.time.LocalDate.parse(date);
                yield String.format("%d月第%d周", d.getMonthValue(), (d.getDayOfMonth() - 1) / 7 + 1);
            }
            case "month" -> {
                String[] parts = date.split("-");
                yield parts[1] + "月";
            }
            default -> {
                String[] parts = date.split("-");
                yield parts[1] + "-" + parts[2];
            }
        };
    }

    /**
     * 构建分类明细
     */
    private AgentAnalysisResp buildCategoryBreakdown(
            AgentAnalysisReq request,
            List<TransactionEntity> transactions,
            long days
    ) {
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        
        for (TransactionEntity tx : transactions) {
            if (tx.getType() == TransactionTypeEnum.INCOME.getCode()) {
                totalIncome = totalIncome.add(tx.getAmount());
            } else {
                totalExpense = totalExpense.add(tx.getAmount());
            }
        }

        List<AgentAnalysisResp.CategoryDetail> categoryBreakdown = buildCategoryDetails(transactions, days);

        return AgentAnalysisResp.summary(
                request.startTime(), request.endTime(),
                totalIncome, totalExpense,
                (long) transactions.size(), days,
                categoryBreakdown
        );
    }

    /**
     * 构建分类详情列表
     */
    private List<AgentAnalysisResp.CategoryDetail> buildCategoryDetails(
            List<TransactionEntity> transactions, 
            long days
    ) {
        // 按类型分组计算总额
        BigDecimal totalExpense = transactions.stream()
                .filter(t -> t.getType() == TransactionTypeEnum.EXPENSE.getCode())
                .map(TransactionEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalIncome = transactions.stream()
                .filter(t -> t.getType() == TransactionTypeEnum.INCOME.getCode())
                .map(TransactionEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 按分类和类型分组
        var grouped = transactions.stream()
                .filter(t -> t.getCategoryId() != null)
                .collect(Collectors.groupingBy(t -> t.getCategoryId() + "_" + t.getType()));

        List<AgentAnalysisResp.CategoryDetail> details = new ArrayList<>();
        
        for (var entry : grouped.entrySet()) {
            String[] parts = entry.getKey().split("_");
            Long categoryId = Long.parseLong(parts[0]);
            int typeCode = Integer.parseInt(parts[1]);
            List<TransactionEntity> txList = entry.getValue();
            
            BigDecimal amount = txList.stream()
                    .map(TransactionEntity::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            String categoryName = "未知分类";
            String categoryIcon = "📁";
            try {
                CategoryEntity cat = categoryService.findById(categoryId);
                categoryName = cat.getName();
                categoryIcon = cat.getIcon();
            } catch (Exception ignored) {}
            
            String type = typeCode == TransactionTypeEnum.INCOME.getCode() ? "INCOME" : "EXPENSE";
            BigDecimal typeTotal = type.equals("INCOME") ? totalIncome : totalExpense;
            
            double percentage = typeTotal.compareTo(BigDecimal.ZERO) > 0
                    ? amount.divide(typeTotal, 4, java.math.RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .doubleValue()
                    : 0.0;
            
            BigDecimal dailyAvg = days > 0 
                    ? amount.divide(BigDecimal.valueOf(days), 2, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            
            details.add(new AgentAnalysisResp.CategoryDetail(
                    categoryId, categoryName, categoryIcon, type,
                    amount, (long) txList.size(), percentage, dailyAvg, null
            ));
        }
        
        // 按金额降序排序
        details.sort((a, b) -> b.amount().compareTo(a.amount()));
        return details;
    }

    /**
     * 构建对比分析
     */
    private AgentAnalysisResp buildComparison(
            AgentAnalysisReq request,
            Long userId,
            List<TransactionEntity> currentTransactions,
            long days
    ) {
        // 当前期间汇总
        BigDecimal currentIncome = BigDecimal.ZERO;
        BigDecimal currentExpense = BigDecimal.ZERO;
        for (TransactionEntity tx : currentTransactions) {
            if (tx.getType() == TransactionTypeEnum.INCOME.getCode()) {
                currentIncome = currentIncome.add(tx.getAmount());
            } else {
                currentExpense = currentExpense.add(tx.getAmount());
            }
        }

        // 获取对比期间数据
        List<TransactionEntity> previousTransactions = List.of();
        BigDecimal previousIncome = BigDecimal.ZERO;
        BigDecimal previousExpense = BigDecimal.ZERO;
        String compareStart = request.compareStartTime();
        String compareEnd = request.compareEndTime();
        
        // 如果没有指定对比期间，自动计算上一期
        if (compareStart == null || compareEnd == null) {
            LocalDateTime start = parseDateTime(request.startTime());
            LocalDateTime end = parseDateTimeAsEnd(request.endTime());
            long periodDays = java.time.temporal.ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate()) + 1;
            
            LocalDateTime prevEnd = start.minusDays(1);
            LocalDateTime prevStart = prevEnd.minusDays(periodDays - 1);
            compareStart = prevStart.toLocalDate().toString();
            compareEnd = prevEnd.toLocalDate().toString();
        }
        
        previousTransactions = queryTransactionsForAnalysis(
                request.ledgerId(), userId,
                parseDateTime(compareStart), parseDateTimeAsEnd(compareEnd),
                request.type(), request.categoryIds()
        );
        
        for (TransactionEntity tx : previousTransactions) {
            if (tx.getType() == TransactionTypeEnum.INCOME.getCode()) {
                previousIncome = previousIncome.add(tx.getAmount());
            } else {
                previousExpense = previousExpense.add(tx.getAmount());
            }
        }

        // 计算变化率
        Double incomeChangeRate = calculateChangeRate(currentIncome, previousIncome);
        Double expenseChangeRate = calculateChangeRate(currentExpense, previousExpense);
        BigDecimal currentBalance = currentIncome.subtract(currentExpense);
        BigDecimal previousBalance = previousIncome.subtract(previousExpense);
        Double balanceChangeRate = calculateChangeRate(currentBalance, previousBalance);

        // 构建期间汇总
        AgentAnalysisResp.PeriodSummary current = new AgentAnalysisResp.PeriodSummary(
                request.startTime(), request.endTime(), "当前期间",
                currentIncome, currentExpense, currentBalance, (long) currentTransactions.size()
        );
        AgentAnalysisResp.PeriodSummary previous = new AgentAnalysisResp.PeriodSummary(
                compareStart, compareEnd, "对比期间",
                previousIncome, previousExpense, previousBalance, (long) previousTransactions.size()
        );

        // 分类对比
        List<AgentAnalysisResp.CategoryComparison> categoryComparisons = buildCategoryComparisons(
                currentTransactions, previousTransactions
        );

        AgentAnalysisResp.ComparisonData comparison = new AgentAnalysisResp.ComparisonData(
                current, previous,
                incomeChangeRate, expenseChangeRate, balanceChangeRate,
                categoryComparisons
        );

        return AgentAnalysisResp.comparison(
                request.startTime(), request.endTime(),
                currentIncome, currentExpense,
                (long) currentTransactions.size(), days,
                comparison
        );
    }

    /**
     * 计算变化率
     */
    private Double calculateChangeRate(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        return current.subtract(previous)
                .divide(previous.abs(), 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    /**
     * 构建分类对比
     */
    private List<AgentAnalysisResp.CategoryComparison> buildCategoryComparisons(
            List<TransactionEntity> current,
            List<TransactionEntity> previous
    ) {
        // 当前期间按分类汇总
        Map<String, BigDecimal> currentByCategory = current.stream()
                .filter(t -> t.getCategoryId() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getCategoryId() + "_" + t.getType(),
                        Collectors.reducing(BigDecimal.ZERO, TransactionEntity::getAmount, BigDecimal::add)
                ));
        
        // 对比期间按分类汇总
        Map<String, BigDecimal> previousByCategory = previous.stream()
                .filter(t -> t.getCategoryId() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getCategoryId() + "_" + t.getType(),
                        Collectors.reducing(BigDecimal.ZERO, TransactionEntity::getAmount, BigDecimal::add)
                ));
        
        // 合并所有分类
        var allKeys = new java.util.HashSet<String>();
        allKeys.addAll(currentByCategory.keySet());
        allKeys.addAll(previousByCategory.keySet());
        
        List<AgentAnalysisResp.CategoryComparison> comparisons = new ArrayList<>();
        for (String key : allKeys) {
            String[] parts = key.split("_");
            Long categoryId = Long.parseLong(parts[0]);
            int typeCode = Integer.parseInt(parts[1]);
            
            BigDecimal currentAmount = currentByCategory.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal previousAmount = previousByCategory.getOrDefault(key, BigDecimal.ZERO);
            
            String categoryName = "未知分类";
            String categoryIcon = "📁";
            try {
                CategoryEntity cat = categoryService.findById(categoryId);
                categoryName = cat.getName();
                categoryIcon = cat.getIcon();
            } catch (Exception ignored) {}
            
            String type = typeCode == TransactionTypeEnum.INCOME.getCode() ? "INCOME" : "EXPENSE";
            Double changeRate = calculateChangeRate(currentAmount, previousAmount);
            
            comparisons.add(new AgentAnalysisResp.CategoryComparison(
                    categoryId, categoryName, categoryIcon, type,
                    currentAmount, previousAmount, changeRate
            ));
        }
        
        // 按当前金额降序排序
        comparisons.sort((a, b) -> b.currentAmount().compareTo(a.currentAmount()));
        return comparisons;
    }

    /**
     * 构建排行分析
     */
    private AgentAnalysisResp buildRanking(
            AgentAnalysisReq request,
            List<TransactionEntity> transactions,
            long days
    ) {
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        
        for (TransactionEntity tx : transactions) {
            if (tx.getType() == TransactionTypeEnum.INCOME.getCode()) {
                totalIncome = totalIncome.add(tx.getAmount());
            } else {
                totalExpense = totalExpense.add(tx.getAmount());
            }
        }

        int topN = request.topN() != null ? request.topN() : 10;
        BigDecimal total = totalIncome.add(totalExpense);
        
        // 按分类汇总排序
        var categoryStats = buildCategoryDetails(transactions, days);
        
        List<AgentAnalysisResp.RankingItem> ranking = new ArrayList<>();
        int rank = 1;
        for (var cat : categoryStats) {
            if (rank > topN) break;
            
            double percentage = total.compareTo(BigDecimal.ZERO) > 0
                    ? cat.amount().divide(total, 4, java.math.RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .doubleValue()
                    : 0.0;
            
            ranking.add(new AgentAnalysisResp.RankingItem(
                    rank++,
                    cat.categoryName(),
                    cat.categoryIcon(),
                    cat.amount(),
                    cat.count(),
                    percentage
            ));
        }

        return AgentAnalysisResp.ranking(
                request.startTime(), request.endTime(),
                totalIncome, totalExpense,
                (long) transactions.size(), days,
                ranking
        );
    }
}
