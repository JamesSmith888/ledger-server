package org.jim.ledgerserver.ledger.service;

import jakarta.annotation.Resource;
import org.jim.ledgerserver.common.exception.BusinessException;
import org.jim.ledgerserver.ledger.entity.TransactionEntity;
import org.jim.ledgerserver.ledger.repository.TransactionRepository;
import org.jim.ledgerserver.ledger.vo.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 报表业务逻辑层
 * @author James Smith
 */
@Component
public class ReportService {

    @Resource
    private TransactionRepository transactionRepository;

    @Resource
    private CategoryService categoryService;

    @Resource
    private LedgerService ledgerService;

    @Resource
    private PaymentMethodService paymentMethodService;

    /**
     * 按分类统计（支持多维度）
     * @param request 查询参数
     * @param currentUserId 当前用户ID
     * @return 分类统计结果
     */
    public CategoryStatisticsResp getStatisticsByCategory(ReportQueryReq request, Long currentUserId) {
        validateRequest(request, currentUserId);

        // 查询符合条件的交易
        List<TransactionEntity> transactions = queryTransactions(request, currentUserId);

        if (transactions.isEmpty()) {
            return createEmptyCategoryStatistics(request);
        }

        // 计算总金额
        BigDecimal totalAmount = transactions.stream()
                .map(TransactionEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 根据维度进行分组统计
        List<StatisticsItemResp> items = switch (request.dimension().toLowerCase()) {
            case "category" -> groupByCategory(transactions, totalAmount);
            case "ledger" -> groupByLedger(transactions, totalAmount);
            case "paymentmethod" -> groupByPaymentMethod(transactions, totalAmount);
            default -> groupByCategory(transactions, totalAmount);
        };

        return new CategoryStatisticsResp(
                items,
                totalAmount,
                (long) transactions.size(),
                new CategoryStatisticsResp.TimeRange(
                        request.startTime().toString(),
                        request.endTime().toString()
                )
        );
    }

    /**
     * 按分类维度分组统计
     */
    private List<StatisticsItemResp> groupByCategory(List<TransactionEntity> transactions, BigDecimal totalAmount) {
        Map<Long, List<TransactionEntity>> grouped = transactions.stream()
                .filter(t -> t.getCategoryId() != null)
                .collect(Collectors.groupingBy(TransactionEntity::getCategoryId));

        return grouped.entrySet().stream()
                .map(entry -> {
                    Long id = entry.getKey();
                    List<TransactionEntity> items = entry.getValue();

                    // 获取分类信息
                    var category = categoryService.findById(id);
                    String name = category.getName();
                    String icon = category.getIcon();

                    return buildStatisticsItem(String.valueOf(id), name, icon, items, totalAmount);
                })
                .sorted((a, b) -> b.amount().compareTo(a.amount()))
                .toList();
    }

    /**
     * 按账本维度分组统计
     */
    private List<StatisticsItemResp> groupByLedger(List<TransactionEntity> transactions, BigDecimal totalAmount) {
        Map<Long, List<TransactionEntity>> grouped = transactions.stream()
                .filter(t -> t.getLedgerId() != null)
                .collect(Collectors.groupingBy(TransactionEntity::getLedgerId));

        return grouped.entrySet().stream()
                .map(entry -> {
                    Long id = entry.getKey();
                    List<TransactionEntity> items = entry.getValue();

                    // 获取账本信息（容错处理）
                    String name;
                    String icon = "📒"; // 默认账本图标
                    try {
                        var ledger = ledgerService.findById(id);
                        name = ledger.getName();
                    } catch (Exception e) {
                        name = "未知账本";
                    }

                    return buildStatisticsItem(String.valueOf(id), name, icon, items, totalAmount);
                })
                .sorted((a, b) -> b.amount().compareTo(a.amount()))
                .toList();
    }

    /**
     * 按支付方式维度分组统计
     */
    private List<StatisticsItemResp> groupByPaymentMethod(List<TransactionEntity> transactions, BigDecimal totalAmount) {
        Map<Long, List<TransactionEntity>> grouped = transactions.stream()
                .filter(t -> t.getPaymentMethodId() != null)
                .collect(Collectors.groupingBy(TransactionEntity::getPaymentMethodId));

        return grouped.entrySet().stream()
                .map(entry -> {
                    Long id = entry.getKey();
                    List<TransactionEntity> items = entry.getValue();

                    // 获取支付方式信息（容错处理）
                    String name;
                    String icon = "💳"; // 默认支付方式图标
                    try {
                        var paymentMethod = paymentMethodService.findById(id);
                        name = paymentMethod.getName();
                        if (paymentMethod.getIcon() != null) {
                            icon = paymentMethod.getIcon();
                        }
                    } catch (Exception e) {
                        name = "未知支付方式";
                    }

                    return buildStatisticsItem(String.valueOf(id), name, icon, items, totalAmount);
                })
                .sorted((a, b) -> b.amount().compareTo(a.amount()))
                .toList();
    }

    /**
     * 构建统计项
     */
    private StatisticsItemResp buildStatisticsItem(String id, String name, String icon,
                                                     List<TransactionEntity> transactions, BigDecimal totalAmount) {
        // 计算金额和数量
        BigDecimal amount = transactions.stream()
                .map(TransactionEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Long count = (long) transactions.size();

        // 计算占比
        Double percentage = totalAmount.compareTo(BigDecimal.ZERO) > 0
                ? amount.divide(totalAmount, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue()
                : 0.0;

        return new StatisticsItemResp(id, name, icon, amount, count, percentage);
    }

    /**
     * 按时间趋势统计
     * @param request 查询参数
     * @param currentUserId 当前用户ID
     * @return 趋势统计结果
     */
    public TrendStatisticsResp getTrendStatistics(ReportQueryReq request, Long currentUserId) {
        validateRequest(request, currentUserId);

        // 查询符合条件的交易
        List<TransactionEntity> transactions = queryTransactions(request, currentUserId);

        if (transactions.isEmpty()) {
            return createEmptyTrendStatistics(request);
        }

        // 按时间分组
        Map<String, List<TransactionEntity>> groupedByTime = groupByTime(transactions, request.groupBy());

        // 生成完整的时间序列（填充空白日期）
        List<String> timePoints = generateTimePoints(request.startTime(), request.endTime(), request.groupBy());

        // 构建趋势数据点
        List<TrendDataPoint> dataPoints = timePoints.stream()
                .map(timePoint -> {
                    List<TransactionEntity> timeTransactions = groupedByTime.getOrDefault(timePoint, Collections.emptyList());

                    // 分别计算收入和支出
                    BigDecimal income = timeTransactions.stream()
                            .filter(t -> t.getType() == 1) // 1=收入
                            .map(TransactionEntity::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal expense = timeTransactions.stream()
                            .filter(t -> t.getType() == 2) // 2=支出
                            .map(TransactionEntity::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal balance = income.subtract(expense);
                    Long count = (long) timeTransactions.size();

                    return new TrendDataPoint(timePoint, income, expense, balance, count);
                })
                .toList();

        // 计算汇总信息
        BigDecimal totalIncome = dataPoints.stream()
                .map(TrendDataPoint::income)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = dataPoints.stream()
                .map(TrendDataPoint::expense)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netBalance = totalIncome.subtract(totalExpense);
        Long totalCount = dataPoints.stream().mapToLong(TrendDataPoint::count).sum();

        // 计算平均值（避免除以0）
        int nonZeroPoints = (int) dataPoints.stream().filter(dp -> dp.count() > 0).count();
        BigDecimal avgIncome = nonZeroPoints > 0
                ? totalIncome.divide(BigDecimal.valueOf(nonZeroPoints), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal avgExpense = nonZeroPoints > 0
                ? totalExpense.divide(BigDecimal.valueOf(nonZeroPoints), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        TrendStatisticsResp.Summary summary = new TrendStatisticsResp.Summary(
                totalIncome,
                totalExpense,
                netBalance,
                totalCount,
                avgIncome,
                avgExpense
        );

        return new TrendStatisticsResp(dataPoints, summary, request.groupBy());
    }

    // ========== 私有辅助方法 ==========

    /**
     * 验证请求参数
     */
    private void validateRequest(ReportQueryReq request, Long currentUserId) {
        if (currentUserId == null) {
            throw new BusinessException("用户未登录");
        }
        if (request.startTime() == null || request.endTime() == null) {
            throw new BusinessException("开始时间和结束时间不能为空");
        }
        if (request.startTime().isAfter(request.endTime())) {
            throw new BusinessException("开始时间不能晚于结束时间");
        }
    }

    /**
     * 查询符合条件的交易
     */
    private List<TransactionEntity> queryTransactions(ReportQueryReq request, Long currentUserId) {
        Specification<TransactionEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 未删除的记录
            predicates.add(cb.isNull(root.get("deleteTime")));

            // 当前用户创建
            predicates.add(cb.equal(root.get("createdByUserId"), currentUserId));

            // 账本筛选
            if (request.ledgerId() != null) {
                predicates.add(cb.equal(root.get("ledgerId"), request.ledgerId()));
            }

            // 类型筛选
            if (request.type() != null) {
                predicates.add(cb.equal(root.get("type"), request.type()));
            }

            // 分类筛选
            if (request.categoryIds() != null && !request.categoryIds().isEmpty()) {
                predicates.add(root.get("categoryId").in(request.categoryIds()));
            }

            // 时间范围筛选
            predicates.add(cb.greaterThanOrEqualTo(root.get("transactionDateTime"), request.startTime()));
            predicates.add(cb.lessThanOrEqualTo(root.get("transactionDateTime"), request.endTime()));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return transactionRepository.findAll(spec);
    }

    /**
     * 按时间分组
     */
    private Map<String, List<TransactionEntity>> groupByTime(List<TransactionEntity> transactions, String groupBy) {
        DateTimeFormatter formatter = getDateFormatter(groupBy);

        return transactions.stream()
                .collect(Collectors.groupingBy(t ->
                        t.getTransactionDateTime().format(formatter)
                ));
    }

    /**
     * 生成完整的时间点列表（填充空白）
     */
    private List<String> generateTimePoints(LocalDateTime startTime, LocalDateTime endTime, String groupBy) {
        List<String> timePoints = new ArrayList<>();
        DateTimeFormatter formatter = getDateFormatter(groupBy);

        LocalDateTime current = truncateToGroupBy(startTime, groupBy);
        LocalDateTime end = truncateToGroupBy(endTime, groupBy);

        while (!current.isAfter(end)) {
            timePoints.add(current.format(formatter));
            current = incrementByGroupBy(current, groupBy);
        }

        return timePoints;
    }

    /**
     * 获取日期格式化器
     */
    private DateTimeFormatter getDateFormatter(String groupBy) {
        return switch (groupBy.toLowerCase()) {
            case "day" -> DateTimeFormatter.ofPattern("yyyy-MM-dd");
            case "week" -> DateTimeFormatter.ofPattern("yyyy-'W'ww");
            case "month" -> DateTimeFormatter.ofPattern("yyyy-MM");
            case "year" -> DateTimeFormatter.ofPattern("yyyy");
            default -> DateTimeFormatter.ofPattern("yyyy-MM");
        };
    }

    /**
     * 截断到分组粒度
     */
    private LocalDateTime truncateToGroupBy(LocalDateTime dateTime, String groupBy) {
        return switch (groupBy.toLowerCase()) {
            case "day" -> dateTime.truncatedTo(ChronoUnit.DAYS);
            case "week" -> dateTime.truncatedTo(ChronoUnit.DAYS).with(java.time.DayOfWeek.MONDAY);
            case "month" -> dateTime.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
            case "year" -> dateTime.withDayOfYear(1).truncatedTo(ChronoUnit.DAYS);
            default -> dateTime.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
        };
    }

    /**
     * 按分组粒度递增
     */
    private LocalDateTime incrementByGroupBy(LocalDateTime dateTime, String groupBy) {
        return switch (groupBy.toLowerCase()) {
            case "day" -> dateTime.plusDays(1);
            case "week" -> dateTime.plusWeeks(1);
            case "month" -> dateTime.plusMonths(1);
            case "year" -> dateTime.plusYears(1);
            default -> dateTime.plusMonths(1);
        };
    }

    /**
     * 创建空的分类统计结果
     */
    private CategoryStatisticsResp createEmptyCategoryStatistics(ReportQueryReq request) {
        return new CategoryStatisticsResp(
                Collections.emptyList(),
                BigDecimal.ZERO,
                0L,
                new CategoryStatisticsResp.TimeRange(
                        request.startTime().toString(),
                        request.endTime().toString()
                )
        );
    }

    /**
     * 创建空的趋势统计结果
     */
    private TrendStatisticsResp createEmptyTrendStatistics(ReportQueryReq request) {
        TrendStatisticsResp.Summary summary = new TrendStatisticsResp.Summary(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0L,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );

        return new TrendStatisticsResp(
                Collections.emptyList(),
                summary,
                request.groupBy()
        );
    }
}
