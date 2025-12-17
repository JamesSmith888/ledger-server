package org.jim.ledgerserver.ledger.service;

import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.jim.ledgerserver.common.enums.TransactionSourceEnum;
import org.jim.ledgerserver.common.enums.TransactionTypeEnum;
import org.jim.ledgerserver.common.exception.BusinessException;
import org.jim.ledgerserver.common.util.UserContext;
import org.jim.ledgerserver.ledger.entity.LedgerEntity;
import org.jim.ledgerserver.ledger.entity.TransactionEntity;
import org.jim.ledgerserver.ledger.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
     * @param description 交易描述
     * @param amount 交易金额
     * @param type 交易类型
     * @param transactionDateTime 交易时间
     * @param ledgerId 账本ID（可选）
     * @param categoryId 分类ID（可选）
     * @param paymentMethodId 支付方式ID（可选）
     * @param source 交易来源（可选，默认为手动）
     * @return 创建的交易实体
     */
    public TransactionEntity create(String description, BigDecimal amount,
                                    Integer type, LocalDateTime transactionDateTime,
                                    Long ledgerId, Long categoryId, Long paymentMethodId, Integer source) {
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
        transaction.setDescription(description);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setTransactionDateTime(transactionDateTime != null ? transactionDateTime : LocalDateTime.now());
        transaction.setLedgerId(ledgerId);
        transaction.setCreatedByUserId(currentUserId);
        transaction.setCategoryId(categoryId);
        transaction.setPaymentMethodId(paymentMethodId);
        // 设置来源，默认为手动(1)
        transaction.setSource(source != null ? source : TransactionSourceEnum.MANUAL.getCode());

        return transactionRepository.save(transaction);
    }

    /**
     * 创建交易（兼容旧接口，不指定来源，默认手动）
     */
    public TransactionEntity create(String description, BigDecimal amount,
                                    Integer type, LocalDateTime transactionDateTime,
                                    Long ledgerId, Long categoryId, Long paymentMethodId) {
        return create(description, amount, type, transactionDateTime, ledgerId, categoryId, paymentMethodId, TransactionSourceEnum.MANUAL.getCode());
    }

    /**
     * 创建交易（兼容旧接口，不指定分类）
     * @param description 交易描述
     * @param amount 交易金额
     * @param type 交易类型
     * @param transactionDateTime 交易时间
     * @param ledgerId 账本ID（可选）
     * @return 创建的交易实体
     */
    public TransactionEntity create(String description, BigDecimal amount,
                                    Integer type, LocalDateTime transactionDateTime,
                                    Long ledgerId) {
        return create(description, amount, type, transactionDateTime, ledgerId, null, null);
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
     * @param keyword 搜索关键词（可选，模糊匹配名称和描述）
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
            String keyword,
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

            // 账本ID筛选
            if (ledgerId != null) {
                predicates.add(cb.equal(root.get("ledgerId"), ledgerId));
            } else {
                // 创建用户ID
                predicates.add(cb.equal(root.get("createdByUserId"), createdByUserId));
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

            // 关键词模糊搜索（搜索描述）
            if (StringUtils.isNotBlank(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate descPredicate = cb.like(cb.lower(root.get("description")), pattern);
                predicates.add(descPredicate);
            }

            // 默认只查询父交易（parentId 为空）
            // 如果需要查询所有交易（包括子交易），可以在参数中增加一个开关，目前默认逻辑是聚合展示
            predicates.add(cb.isNull(root.get("parentId")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return transactionRepository.findAll(spec, pageable);
    }

    /**
     * 根据条件分页查询交易（兼容旧接口，不带关键词搜索）
     * @deprecated 请使用带 keyword 参数的方法
     */
    @Deprecated
    public Page<TransactionEntity> queryTransactions(
            Long ledgerId,
            Integer type,
            Long categoryId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Long createdByUserId,
            Pageable pageable) {
        return queryTransactions(ledgerId, type, categoryId, startTime, endTime, createdByUserId, null, pageable);
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
     * 根据分类ID查询未删除的交易
     * @param categoryId 分类ID
     * @return 交易列表
     */
    public List<TransactionEntity> findByCategoryId(Long categoryId) {
        if (categoryId == null) {
            throw new BusinessException("分类ID不能为空");
        }
        return transactionRepository.findByCategoryIdAndDeleteTimeIsNull(categoryId);
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
     * @param description 交易描述
     * @param amount 交易金额
     * @param type 交易类型
     * @param transactionDateTime 交易时间
     * @param categoryId 分类ID
     * @return 更新后的交易实体
     */
    public TransactionEntity update(Long id, String description, BigDecimal amount,
                                    Integer type, LocalDateTime transactionDateTime, Long categoryId) {
        var transaction = findById(id);

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
     * @param description 交易描述
     * @param amount 交易金额
     * @param type 交易类型
     * @param transactionDateTime 交易时间
     * @return 更新后的交易实体
     */
    public TransactionEntity update(Long id, String description, BigDecimal amount,
                                    Integer type, LocalDateTime transactionDateTime) {
        return update(id, description, amount, type, transactionDateTime, null);
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

    /**
     * 解析时间字符串，支持 ISO_LOCAL_DATE_TIME 和 ISO_ZONED_DATE_TIME
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null) return null;
        try {
            // 尝试解析 ISO_LOCAL_DATE_TIME (e.g. 2023-11-24T10:00:00)
            return LocalDateTime.parse(dateTimeStr);
        } catch (Exception e) {
            try {
                // 尝试解析 ISO_ZONED_DATE_TIME (e.g. 2023-11-24T10:00:00.000Z)
                return java.time.ZonedDateTime.parse(dateTimeStr).toLocalDateTime();
            } catch (Exception ex) {
                throw new BusinessException("时间格式错误: " + dateTimeStr);
            }
        }
    }

    /**
     * 获取每日统计数据（用于热力图）
     * @param ledgerId 账本ID（可选）
     * @param startTimeStr 开始时间字符串
     * @param endTimeStr 结束时间字符串
     * @param userId 用户ID
     * @return 每日统计列表
     */
    public List<org.jim.ledgerserver.ledger.vo.DailyStatisticsResp> getDailyStatistics(
            Long ledgerId,
            String startTimeStr,
            String endTimeStr,
            Long userId) {

        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }

        LocalDateTime startTime = parseDateTime(startTimeStr);
        LocalDateTime endTime = parseDateTime(endTimeStr);

        if (startTime.isAfter(endTime)) {
            throw new BusinessException("开始时间不能晚于结束时间");
        }

        // 构建查询条件
        Specification<TransactionEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 未删除的记录
            predicates.add(cb.isNull(root.get("deleteTime")));

            // 账本ID筛选
            if (ledgerId != null) {
                predicates.add(cb.equal(root.get("ledgerId"), ledgerId));
            } else {
                // 创建用户ID
                predicates.add(cb.equal(root.get("createdByUserId"), userId));
            }

            // 时间范围筛选
            predicates.add(cb.greaterThanOrEqualTo(root.get("transactionDateTime"), startTime));
            predicates.add(cb.lessThanOrEqualTo(root.get("transactionDateTime"), endTime));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<TransactionEntity> transactions = transactionRepository.findAll(spec);

        // 按日期分组统计
        java.util.Map<String, org.jim.ledgerserver.ledger.vo.DailyStatisticsResp> dailyMap = new java.util.HashMap<>();

        transactions.forEach(tx -> {
            String date = tx.getTransactionDateTime().toLocalDate().toString();

            dailyMap.computeIfAbsent(date, k ->
                    new org.jim.ledgerserver.ledger.vo.DailyStatisticsResp(
                            k,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            0
                    )
            );

            org.jim.ledgerserver.ledger.vo.DailyStatisticsResp existing = dailyMap.get(date);

            BigDecimal newIncome = existing.income();
            BigDecimal newExpense = existing.expense();
            int newCount = existing.count() + 1;

            // 根据交易类型累加
            if (tx.getType() == 1) { // 收入
                newIncome = newIncome.add(tx.getAmount());
            } else if (tx.getType() == 2) { // 支出
                newExpense = newExpense.add(tx.getAmount());
            }

            dailyMap.put(date, new org.jim.ledgerserver.ledger.vo.DailyStatisticsResp(
                    date,
                    newIncome,
                    newExpense,
                    newCount
            ));
        });

        return new ArrayList<>(dailyMap.values());
    }

    /**
     * 获取月度汇总统计（用于列表页顶部汇总区域）
     * @param ledgerId 账本ID（可选）
     * @param startTimeStr 开始时间字符串
     * @param endTimeStr 结束时间字符串
     * @param userId 用户ID
     * @return 月度汇总统计
     */
    public org.jim.ledgerserver.ledger.vo.MonthlySummaryResp getMonthlySummary(
            Long ledgerId,
            String startTimeStr,
            String endTimeStr,
            Long userId) {

        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }

        LocalDateTime startTime = parseDateTime(startTimeStr);
        LocalDateTime endTime = parseDateTime(endTimeStr);

        if (startTime.isAfter(endTime)) {
            throw new BusinessException("开始时间不能晚于结束时间");
        }

        // 构建查询条件
        Specification<TransactionEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 未删除的记录
            predicates.add(cb.isNull(root.get("deleteTime")));

            // 账本ID筛选
            if (ledgerId != null) {
                predicates.add(cb.equal(root.get("ledgerId"), ledgerId));
            } else {
                // 创建用户ID
                predicates.add(cb.equal(root.get("createdByUserId"), userId));
            }

            // 时间范围筛选
            predicates.add(cb.greaterThanOrEqualTo(root.get("transactionDateTime"), startTime));
            predicates.add(cb.lessThanOrEqualTo(root.get("transactionDateTime"), endTime));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<TransactionEntity> transactions = transactionRepository.findAll(spec);

        // 计算汇总数据
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        int totalCount = transactions.size();

        for (TransactionEntity tx : transactions) {
            if (tx.getType() == 1) { // 收入
                totalIncome = totalIncome.add(tx.getAmount());
            } else if (tx.getType() == 2) { // 支出
                totalExpense = totalExpense.add(tx.getAmount());
            }
        }

        BigDecimal balance = totalIncome.subtract(totalExpense);

        return new org.jim.ledgerserver.ledger.vo.MonthlySummaryResp(
                totalIncome,
                totalExpense,
                balance,
                totalCount
        );
    }

    /**
     * 查询Top N交易（不查询总数，性能优化）
     * @param ledgerId 账本ID（可选）
     * @param type 交易类型（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @param createdByUserId 创建用户ID
     * @param limit 限制数量
     * @return 交易列表
     */
    public List<TransactionEntity> findTopTransactions(
            Long ledgerId,
            Integer type,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Long createdByUserId,
            int limit) {

        if (createdByUserId == null) {
            throw new BusinessException("创建用户ID不能为空");
        }

        // 构建动态查询条件
        Specification<TransactionEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 未删除的记录
            predicates.add(cb.isNull(root.get("deleteTime")));

            // 账本ID筛选
            if (ledgerId != null) {
                predicates.add(cb.equal(root.get("ledgerId"), ledgerId));
            } else {
                // 创建用户ID
                predicates.add(cb.equal(root.get("createdByUserId"), createdByUserId));
            }

            // 交易类型筛选
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
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

        // 按金额降序
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "amount"));

        // 使用 Slice 避免 count 查询，或者直接 getContent
        return transactionRepository.findAll(spec, pageable).getContent();
    }

    /**
     * 追加交易（创建子交易）
     * 用于将新的交易追加到现有交易上，实现交易聚合功能
     * 
     * @param parentId 父交易ID
     * @param amount 追加金额
     * @param description 描述（可选）
     * @param transactionDateTime 交易时间（可选，默认为当前时间）
     * @return 创建的子交易实体
     */
    public TransactionEntity appendTransaction(Long parentId, BigDecimal amount, 
                                              String description, LocalDateTime transactionDateTime) {
        if (parentId == null) {
            throw new BusinessException("父交易ID不能为空");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("追加金额必须大于0");
        }

        // 查询父交易
        TransactionEntity parentTransaction = findById(parentId);
        if (parentTransaction.getDeleteTime() != null) {
            throw new BusinessException("父交易已删除，无法追加");
        }
        
        // 检查父交易是否本身就是子交易（防止多层嵌套）
        if (parentTransaction.getParentId() != null) {
            throw new BusinessException("不支持在子交易上继续追加，请在原始交易上追加");
        }
        
        // 验证权限：只有父交易的创建者可以追加
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException("当前用户未登录");
        }
        if (!currentUserId.equals(parentTransaction.getCreatedByUserId())) {
            throw new BusinessException("只有交易创建者可以追加记录");
        }

        // 创建子交易，继承父交易的属性
        TransactionEntity childTransaction = new TransactionEntity();
        childTransaction.setParentId(parentId);
        childTransaction.setAmount(amount);
        childTransaction.setDescription(description != null ? description : parentTransaction.getDescription());
        childTransaction.setType(parentTransaction.getType());
        childTransaction.setTransactionDateTime(transactionDateTime != null ? transactionDateTime : LocalDateTime.now());
        childTransaction.setLedgerId(parentTransaction.getLedgerId());
        childTransaction.setCreatedByUserId(currentUserId);
        childTransaction.setCategoryId(parentTransaction.getCategoryId());
        childTransaction.setPaymentMethodId(parentTransaction.getPaymentMethodId());
        childTransaction.setSource(TransactionSourceEnum.MANUAL.getCode());

        return transactionRepository.save(childTransaction);
    }

    /**
     * 查询交易的所有子交易
     * 
     * @param parentId 父交易ID
     * @return 子交易列表
     */
    public List<TransactionEntity> findChildTransactions(Long parentId) {
        if (parentId == null) {
            throw new BusinessException("父交易ID不能为空");
        }
        
        // 使用 Specification 查询未删除的子交易
        Specification<TransactionEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deleteTime")));
            predicates.add(cb.equal(root.get("parentId"), parentId));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        return transactionRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "transactionDateTime"));
    }

    /**
     * 计算聚合交易的总金额（父交易 + 所有子交易）
     * 
     * @param parentId 父交易ID
     * @return 总金额
     */
    public BigDecimal calculateAggregatedAmount(Long parentId) {
        TransactionEntity parent = findById(parentId);
        List<TransactionEntity> children = findChildTransactions(parentId);
        
        BigDecimal total = parent.getAmount();
        for (TransactionEntity child : children) {
            total = total.add(child.getAmount());
        }
        
        return total;
    }

    /**
     * 批量查询父交易的子交易统计信息
     * @param parentIds 父交易ID列表
     * @return 统计结果 Map<ParentId, [TotalAmount, Count]>
     */
    public java.util.Map<Long, Object[]> getChildStatsByParentIds(List<Long> parentIds) {
        if (parentIds == null || parentIds.isEmpty()) {
            return new java.util.HashMap<>();
        }
        
        List<Object[]> results = transactionRepository.countAndSumByParentIds(parentIds);
        java.util.Map<Long, Object[]> statsMap = new java.util.HashMap<>();
        
        for (Object[] row : results) {
            Long parentId = (Long) row[0];
            // row[1] is sum(amount), row[2] is count(*)
            statsMap.put(parentId, new Object[]{row[1], row[2]});
        }
        
        return statsMap;
    }
}
