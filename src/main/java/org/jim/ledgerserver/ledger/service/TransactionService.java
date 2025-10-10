package org.jim.ledgerserver.ledger.service;

import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.jim.ledgerserver.common.enums.TransactionTypeEnum;
import org.jim.ledgerserver.common.exception.BusinessException;
import org.jim.ledgerserver.ledger.entity.TransactionEntity;
import org.jim.ledgerserver.ledger.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 交易业务逻辑层
 * @author James Smith
 */
@Component
public class TransactionService {

    @Resource
    private TransactionRepository transactionRepository;

    /**
     * 创建交易
     * @param name 交易名称
     * @param description 交易描述
     * @param amount 交易金额
     * @param type 交易类型
     * @param transactionDateTime 交易时间
     * @param ledgerId 账本ID（可选）
     * @param createdByUserId 创建用户ID
     * @return 创建的交易实体
     */
    public TransactionEntity create(String name, String description, BigDecimal amount,
                                    Integer type, LocalDateTime transactionDateTime,
                                    Long ledgerId, Long createdByUserId) {
        if (StringUtils.isBlank(name)) {
            throw new BusinessException("交易名称不能为空");
        }
        if (amount == null) {
            throw new BusinessException("交易金额不能为空");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("交易金额必须大于0");
        }
        if (type == null) {
            throw new BusinessException("交易类型不能为空");
        }
        if (createdByUserId == null) {
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
        transaction.setCreatedByUserId(createdByUserId);

        return transactionRepository.save(transaction);
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
     * 更新交易
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

        return transactionRepository.save(transaction);
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
}
