package org.jim.ledgerserver.ledger.service;

import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.jim.ledgerserver.common.enums.TransactionTypeEnum;
import org.jim.ledgerserver.common.exception.BusinessException;
import org.jim.ledgerserver.common.util.UserContext;
import org.jim.ledgerserver.ledger.entity.LedgerEntity;
import org.jim.ledgerserver.ledger.entity.TransactionEntity;
import org.jim.ledgerserver.ledger.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 交易业务逻辑层
 * @author James Smith
 */
@Component
public class TransactionService {

    @Resource
    private TransactionRepository transactionRepository;

    @Resource
    private LedgerService ledgerService;

    @Resource
    private LedgerMemberService ledgerMemberService;

    /**
     * 创建交易
     * @param name 交易名称
     * @param description 交易描述
     * @param amount 交易金额
     * @param type 交易类型
     * @param transactionDateTime 交易时间
     * @param ledgerId 账本ID（可选）
     * @param categoryId 分类ID（可选）
     * @return 创建的交易实体
     */
    public TransactionEntity create(String name, String description, BigDecimal amount,
                                    Integer type, LocalDateTime transactionDateTime,
                                    Long ledgerId, Long categoryId, Long paymentMethodId) {
        if (amount == null) {
            throw new BusinessException("交易金额不能为空");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("交易金额必须大于0");
        }

        Long currentUserId = UserContext.getCurrentUserId();

        if (currentUserId == null) {
            throw new BusinessException("创建用户ID不能为空");
        }

        // 验证交易类型
        TransactionTypeEnum.fromCode(type);

        var transaction = new TransactionEntity();
        transaction.setName(name);
        transaction.setDescription(description);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setTransactionDateTime(transactionDateTime != null ? transactionDateTime : LocalDateTime.now());
        transaction.setLedgerId(ledgerId);
        transaction.setCreatedByUserId(currentUserId);
        transaction.setCategoryId(categoryId);
        transaction.setPaymentMethodId(paymentMethodId);

        return transactionRepository.save(transaction);
    }

    /**
     * 创建交易（兼容旧接口，不指定支付方式）
     */
    public TransactionEntity create(String name, String description, BigDecimal amount,
                                    Integer type, LocalDateTime transactionDateTime,
                                    Long ledgerId, Long categoryId) {
        return create(name, description, amount, type, transactionDateTime, ledgerId, categoryId, null);
    }

    /**
     * 创建交易（兼容旧接口，不指定分类）
     * @param name 交易名称
     * @param description 交易描述
     * @param amount 交易金额
     * @param type 交易类型
     * @param transactionDateTime 交易时间
     * @param ledgerId 账本ID（可选）
     * @return 创建的交易实体
     */
    public TransactionEntity create(String name, String description, BigDecimal amount,
                                    Integer type, LocalDateTime transactionDateTime,
                                    Long ledgerId) {
        return create(name, description, amount, type, transactionDateTime, ledgerId, null, null);
    }

    /**
     * 查询所有交易
     * @return 交易列表
     */
    public List<TransactionEntity> findAll() {
        return transactionRepository.findAll();
    }

    /**
     * 根据条件分页查询交易
     * @param ledgerId 账本ID（可选）
     * @param type 交易类型（可选）
     * @param categoryId 分类ID（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @param createdByUserId 创建用户ID
     * @param pageable 分页参数
     * @return 交易分页结果
     */
    public Page<TransactionEntity> queryTransactions(
            Long ledgerId,
            Integer type,
            Long categoryId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Long createdByUserId,
            Pageable pageable) {

        if (createdByUserId == null) {
            throw new BusinessException("创建用户ID不能为空");
        }

        // 验证交易类型（如果提供）
        if (type != null) {
            TransactionTypeEnum.fromCode(type);
        }

        // 验证时间范围（如果提供）
        if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
            throw new BusinessException("开始时间不能晚于结束时间");
        }

        // 构建动态查询条件
        Specification<TransactionEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 未删除的记录
            predicates.add(cb.isNull(root.get("deleteTime")));

            // 创建用户ID
            predicates.add(cb.equal(root.get("createdByUserId"), createdByUserId));

            // 账本ID筛选
            if (ledgerId != null) {
                predicates.add(cb.equal(root.get("ledgerId"), ledgerId));
            }

            // 交易类型筛选
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }

            // 分类ID筛选
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("categoryId"), categoryId));
            }

            // 时间范围筛选
            if (startTime != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("transactionDateTime"), startTime));
            }
            if (endTime != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("transactionDateTime"), endTime));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return transactionRepository.findAll(spec, pageable);
    }

    /**
     * 根据ID查询交易
     * @param id 交易ID
     * @return 交易实体
     */
    public TransactionEntity findById(Long id) {
        if (id == null) {
            throw new BusinessException("交易ID不能为空");
        }
        return transactionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("交易不存在"));
    }

    /**
     * 根据账本ID查询所有交易
     * @param ledgerId 账本ID
     * @return 交易列表
     */
    public List<TransactionEntity> findByLedgerId(Long ledgerId) {
        if (ledgerId == null) {
            throw new BusinessException("账本ID不能为空");
        }
        return transactionRepository.findByLedgerId(ledgerId);
    }

    /**
     * 根据账本ID分页查询交易
     * @param ledgerId 账本ID
     * @param pageable 分页参数
     * @return 交易分页结果
     */
    public Page<TransactionEntity> findByLedgerId(Long ledgerId, Pageable pageable) {
        if (ledgerId == null) {
            throw new BusinessException("账本ID不能为空");
        }
        return transactionRepository.findByLedgerId(ledgerId, pageable);
    }

    /**
     * 根据用户ID查询所有交易
     * @param createdByUserId 创建用户ID
     * @return 交易列表
     */
    public List<TransactionEntity> findByCreatedByUserId(Long createdByUserId) {
        if (createdByUserId == null) {
            throw new BusinessException("创建用户ID不能为空");
        }
        return transactionRepository.findByCreatedByUserId(createdByUserId);
    }

    /**
     * 根据用户ID分页查询交易
     * @param createdByUserId 创建用户ID
     * @param pageable 分页参数
     * @return 交易分页结果
     */
    public Page<TransactionEntity> findByCreatedByUserId(Long createdByUserId, Pageable pageable) {
        if (createdByUserId == null) {
            throw new BusinessException("创建用户ID不能为空");
        }
        return transactionRepository.findByCreatedByUserId(createdByUserId, pageable);
    }

    /**
     * 根据交易类型和用户ID查询交易
     * @param type 交易类型
     * @param createdByUserId 创建用户ID
     * @return 交易列表
     */
    public List<TransactionEntity> findByTypeAndCreatedByUserId(Integer type, Long createdByUserId) {
        TransactionTypeEnum.fromCode(type); // 验证类型
        if (createdByUserId == null) {
            throw new BusinessException("创建用户ID不能为空");
        }
        return transactionRepository.findByTypeAndCreatedByUserId(type, createdByUserId);
    }

    /**
     * 根据时间范围查询交易
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param createdByUserId 创建用户ID
     * @return 交易列表
     */
    public List<TransactionEntity> findByDateRange(LocalDateTime startTime, LocalDateTime endTime, Long createdByUserId) {
        if (startTime == null || endTime == null) {
            throw new BusinessException("开始时间和结束时间不能为空");
        }
        if (startTime.isAfter(endTime)) {
            throw new BusinessException("开始时间不能晚于结束时间");
        }
        if (createdByUserId == null) {
            throw new BusinessException("创建用户ID不能为空");
        }
        return transactionRepository.findByTransactionDateTimeBetweenAndCreatedByUserId(startTime, endTime, createdByUserId);
    }

    /**
     * 根据分类ID查询交易
     * @param categoryId 分类ID
     * @return 交易列表
     */
    public List<TransactionEntity> findByCategoryId(Long categoryId) {
        if (categoryId == null) {
            throw new BusinessException("分类ID不能为空");
        }
        return transactionRepository.findByCategoryId(categoryId);
    }

    /**
     * 根据分类ID和用户ID查询交易
     * @param categoryId 分类ID
     * @param createdByUserId 创建用户ID
     * @return 交易列表
     */
    public List<TransactionEntity> findByCategoryIdAndCreatedByUserId(Long categoryId, Long createdByUserId) {
        if (categoryId == null) {
            throw new BusinessException("分类ID不能为空");
        }
        if (createdByUserId == null) {
            throw new BusinessException("创建用户ID不能为空");
        }
        return transactionRepository.findByCategoryIdAndCreatedByUserId(categoryId, createdByUserId);
    }

    /**
     * 更新交易
     * @param id 交易ID
     * @param name 交易名称
     * @param description 交易描述
     * @param amount 交易金额
     * @param type 交易类型
     * @param transactionDateTime 交易时间
     * @param categoryId 分类ID
     * @return 更新后的交易实体
     */
    public TransactionEntity update(Long id, String name, String description, BigDecimal amount,
                                    Integer type, LocalDateTime transactionDateTime, Long categoryId) {
        var transaction = findById(id);

        // 使用增强的 switch 进行条件更新
        if (StringUtils.isNotBlank(name)) {
            transaction.setName(name);
        }

        if (description != null) {
            transaction.setDescription(description);
        }

        if (amount != null) {
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("交易金额必须大于0");
            }
            transaction.setAmount(amount);
        }

        if (type != null) {
            TransactionTypeEnum.fromCode(type); // 验证类型
            transaction.setType(type);
        }

        if (transactionDateTime != null) {
            transaction.setTransactionDateTime(transactionDateTime);
        }

        if (categoryId != null) {
            transaction.setCategoryId(categoryId);
        }

        return transactionRepository.save(transaction);
    }

    /**
     * 更新交易（兼容旧接口，不更新分类）
     * @param id 交易ID
     * @param name 交易名称
     * @param description 交易描述
     * @param amount 交易金额
     * @param type 交易类型
     * @param transactionDateTime 交易时间
     * @return 更新后的交易实体
     */
    public TransactionEntity update(Long id, String name, String description, BigDecimal amount,
                                    Integer type, LocalDateTime transactionDateTime) {
        return update(id, name, description, amount, type, transactionDateTime, null);
    }

    /**
     * 删除交易（逻辑删除）
     * @param id 交易ID
     */
    public void delete(Long id) {
        var transaction = findById(id);
        if (transaction.getDeleteTime() != null) {
            throw new BusinessException("交易已删除");
        }
        transaction.setDeleteTime(LocalDateTime.now());
        transactionRepository.save(transaction);
    }

    /**
     * 更新交易实体
     * @param transaction 交易实体
     * @return 更新后的交易实体
     */
    public TransactionEntity update(TransactionEntity transaction) {
        if (transaction.getId() == null) {
            throw new BusinessException("交易ID不能为空");
        }
        TransactionEntity existing = findById(transaction.getId());
        if (existing.getDeleteTime() != null) {
            throw new BusinessException("交易已删除");
        }
        return transactionRepository.save(transaction);
    }

    /**
     * 永久删除交易（物理删除）
     * @param id 交易ID
     */
    public void deletePermanently(Long id) {
        if (id == null) {
            throw new BusinessException("交易ID不能为空");
        }
        if (!transactionRepository.existsById(id)) {
            throw new BusinessException("交易不存在");
        }
        transactionRepository.deleteById(id);
    }

    /**
     * 计算账本的收入总额
     * @param ledgerId 账本ID
     * @return 收入总额
     */
    public BigDecimal calculateTotalIncome(Long ledgerId) {
        if (ledgerId == null) {
            throw new BusinessException("账本ID不能为空");
        }
        return transactionRepository.sumAmountByLedgerIdAndType(ledgerId, TransactionTypeEnum.INCOME.getCode());
    }

    /**
     * 计算账本的支出总额
     * @param ledgerId 账本ID
     * @return 支出总额
     */
    public BigDecimal calculateTotalExpense(Long ledgerId) {
        if (ledgerId == null) {
            throw new BusinessException("账本ID不能为空");
        }
        return transactionRepository.sumAmountByLedgerIdAndType(ledgerId, TransactionTypeEnum.EXPENSE.getCode());
    }

    /**
     * 计算账本的余额
     * @param ledgerId 账本ID
     * @return 余额（收入 - 支出）
     */
    public BigDecimal calculateBalance(Long ledgerId) {
        var income = calculateTotalIncome(ledgerId);
        var expense = calculateTotalExpense(ledgerId);
        return income.subtract(expense);
    }

    /**
     * 计算用户的收入总额
     * @param createdByUserId 创建用户ID
     * @return 收入总额
     */
    public BigDecimal calculateUserTotalIncome(Long createdByUserId) {
        if (createdByUserId == null) {
            throw new BusinessException("创建用户ID不能为空");
        }
        return transactionRepository.sumAmountByCreatedByUserIdAndType(createdByUserId, TransactionTypeEnum.INCOME.getCode());
    }

    /**
     * 计算用户的支出总额
     * @param createdByUserId 创建用户ID
     * @return 支出总额
     */
    public BigDecimal calculateUserTotalExpense(Long createdByUserId) {
        if (createdByUserId == null) {
            throw new BusinessException("创建用户ID不能为空");
        }
        return transactionRepository.sumAmountByCreatedByUserIdAndType(createdByUserId, TransactionTypeEnum.EXPENSE.getCode());
    }

    /**
     * 计算用户的余额
     * @param createdByUserId 创建用户ID
     * @return 余额（收入 - 支出）
     */
    public BigDecimal calculateUserBalance(Long createdByUserId) {
        var income = calculateUserTotalIncome(createdByUserId);
        var expense = calculateUserTotalExpense(createdByUserId);
        return income.subtract(expense);
    }

    /**
     * 将交易移动到指定账本
     * @param transactionId 交易ID
     * @param targetLedgerId 目标账本ID
     * @return 更新后的交易实体
     */
    @Transactional
    public TransactionEntity moveToLedger(Long transactionId, Long targetLedgerId) {
        if (transactionId == null) {
            throw new BusinessException("交易ID不能为空");
        }
        if (targetLedgerId == null) {
            throw new BusinessException("目标账本ID不能为空");
        }

        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException("当前用户未登录");
        }

        TransactionEntity transaction = findById(transactionId);
        if (transaction.getDeleteTime() != null) {
            throw new BusinessException("交易已删除");
        }

        if (targetLedgerId.equals(transaction.getLedgerId())) {
            throw new BusinessException("交易已在目标账本中");
        }

        LedgerEntity sourceLedger = null;
        if (transaction.getLedgerId() != null) {
            sourceLedger = ledgerService.findById(transaction.getLedgerId());
            ensureLedgerActive(sourceLedger, "原账本");
        }

        if (!currentUserId.equals(transaction.getCreatedByUserId())) {
            if (sourceLedger == null || !hasLedgerEditPermission(sourceLedger, currentUserId)) {
                throw new BusinessException("无权操作该交易");
            }
        }

        LedgerEntity targetLedger = ledgerService.findById(targetLedgerId);
        ensureLedgerActive(targetLedger, "目标账本");

        if (!hasLedgerEditPermission(targetLedger, currentUserId)) {
            throw new BusinessException("无权限移动到目标账本");
        }

        transaction.setLedgerId(targetLedgerId);
        return transactionRepository.save(transaction);
    }

    private void ensureLedgerActive(LedgerEntity ledger, String label) {
        if (ledger.getDeleteTime() != null) {
            throw new BusinessException(label + "已删除");
        }
    }

    private boolean hasLedgerEditPermission(LedgerEntity ledger, Long userId) {
        if (ledger == null || userId == null) {
            return false;
        }
        if (ledger.getOwnerUserId() != null && ledger.getOwnerUserId().equals(userId)) {
            return true;
        }
        return ledgerMemberService.hasEditPermission(ledger.getId(), userId);
    }
}
