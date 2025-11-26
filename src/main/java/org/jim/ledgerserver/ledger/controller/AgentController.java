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
import org.jim.ledgerserver.ledger.service.*;
import org.jim.ledgerserver.ledger.vo.agent.AgentCreateTransactionReq;
import org.jim.ledgerserver.ledger.vo.agent.AgentQueryTransactionReq;
import org.jim.ledgerserver.ledger.vo.agent.AgentTransactionListResp;
import org.jim.ledgerserver.ledger.vo.agent.AgentTransactionResp;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

        // 创建交易
        TransactionEntity transaction = transactionService.create(
                request.name(),
                request.description(),
                request.amount(),
                request.type().getCode(),
                request.transactionDateTime() != null ? request.transactionDateTime() : LocalDateTime.now(),
                request.ledgerId(),
                request.categoryId(),
                request.paymentMethodId()
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

        // 转换为响应对象并计算汇总
        List<AgentTransactionResp> transactions = page.getContent().stream()
                .map(this::buildAgentTransactionResp)
                .collect(Collectors.toList());

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
        
        List<AgentTransactionResp> transactions = pageResult.getContent().stream()
                .map(this::buildAgentTransactionResp)
                .collect(Collectors.toList());

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
        
        List<AgentTransactionResp> transactions = page.getContent().stream()
                .map(this::buildAgentTransactionResp)
                .collect(Collectors.toList());

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
            
            // 关键词搜索
            if (request.keyword() != null && !request.keyword().trim().isEmpty()) {
                String pattern = "%" + request.keyword().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
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
                tx.getName(),
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
                attachmentCount
        );
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
}
