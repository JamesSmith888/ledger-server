package org.jim.ledgerserver.ledger.service;

import jakarta.annotation.Resource;
import org.jim.ledgerserver.ledger.entity.BudgetDetailEntity;
import org.jim.ledgerserver.ledger.entity.BudgetSettingEntity;
import org.jim.ledgerserver.ledger.entity.CategoryEntity;
import org.jim.ledgerserver.ledger.repository.BudgetDetailRepository;
import org.jim.ledgerserver.ledger.repository.BudgetSettingRepository;
import org.jim.ledgerserver.ledger.repository.CategoryRepository;
import org.jim.ledgerserver.ledger.repository.TransactionRepository;
import org.jim.ledgerserver.ledger.vo.budget.BudgetOverviewResp;
import org.jim.ledgerserver.ledger.vo.budget.BudgetSettingReq;
import org.jim.ledgerserver.ledger.vo.budget.CategoryBudgetReq;
import org.jim.ledgerserver.ledger.vo.budget.CategoryBudgetResp;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 预算服务
 * @author James Smith
 */
@Service
public class BudgetService {

    @Resource
    private BudgetSettingRepository budgetSettingRepository;

    @Resource
    private BudgetDetailRepository budgetDetailRepository;

    @Resource
    private TransactionRepository transactionRepository;

    @Resource
    private CategoryRepository categoryRepository;

    /**
     * 设置预算
     */
    @Transactional(rollbackFor = Exception.class)
    public void setBudget(BudgetSettingReq req) {
        Long ledgerId = req.ledgerId();

        // 1. 保存/更新总预算
        BudgetSettingEntity setting = budgetSettingRepository.findByLedgerId(ledgerId)
                .orElse(new BudgetSettingEntity().setLedgerId(ledgerId));
        
        setting.setTotalAmount(req.totalAmount());
        budgetSettingRepository.save(setting);

        // 2. 保存/更新分类预算
        // 先删除旧的分类预算 (简单粗暴，或者可以做增量更新)
        budgetDetailRepository.deleteByLedgerId(ledgerId);

        if (req.categoryBudgets() != null && !req.categoryBudgets().isEmpty()) {
            List<BudgetDetailEntity> details = req.categoryBudgets().stream()
                    .map(item -> new BudgetDetailEntity()
                            .setLedgerId(ledgerId)
                            .setCategoryId(item.categoryId())
                            .setAmount(item.amount()))
                    .collect(Collectors.toList());
            budgetDetailRepository.saveAll(details);
        }
    }

    /**
     * 获取预算概览
     */
    public BudgetOverviewResp getBudgetOverview(Long ledgerId) {
        // 1. 获取预算设置
        Optional<BudgetSettingEntity> settingOpt = budgetSettingRepository.findByLedgerId(ledgerId);
        if (settingOpt.isEmpty()) {
            return null; // 或者返回一个空的默认对象
        }
        BudgetSettingEntity setting = settingOpt.get();

        // 2. 获取本月时间范围
        LocalDate now = LocalDate.now();
        LocalDateTime startTime = now.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
        LocalDateTime endTime = now.with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX);

        // 3. 获取本月总支出
        BigDecimal totalExpense = transactionRepository.sumExpenseByLedgerIdAndDateRange(ledgerId, startTime, endTime);
        if (totalExpense == null) {
            totalExpense = BigDecimal.ZERO;
        }

        // 4. 计算总进度
        BigDecimal totalBudget = setting.getTotalAmount();
        BigDecimal remainingBudget = totalBudget.subtract(totalExpense);
        int progress = 0;
        if (totalBudget.compareTo(BigDecimal.ZERO) > 0) {
            progress = totalExpense.divide(totalBudget, 2, RoundingMode.HALF_UP).multiply(new BigDecimal(100)).intValue();
        }
        String status = getStatus(progress);

        // 5. 获取分类预算详情
        List<BudgetDetailEntity> details = budgetDetailRepository.findByLedgerId(ledgerId);
        
        // 6. 获取分类实际支出
        List<Object[]> categoryExpenses = transactionRepository.sumExpenseByCategoryAndDateRange(ledgerId, startTime, endTime);
        Map<Long, BigDecimal> categoryExpenseMap = categoryExpenses.stream()
                .collect(Collectors.toMap(obj -> (Long) obj[0], obj -> (BigDecimal) obj[1]));

        // 7. 组装分类预算响应
        List<CategoryBudgetResp> categoryResps = new ArrayList<>();
        for (BudgetDetailEntity detail : details) {
            BigDecimal catExpense = categoryExpenseMap.getOrDefault(detail.getCategoryId(), BigDecimal.ZERO);
            BigDecimal catBudget = detail.getAmount();
            BigDecimal catRemaining = catBudget.subtract(catExpense);
            int catProgress = 0;
            if (catBudget.compareTo(BigDecimal.ZERO) > 0) {
                catProgress = catExpense.divide(catBudget, 2, RoundingMode.HALF_UP).multiply(new BigDecimal(100)).intValue();
            }

            CategoryEntity category = categoryRepository.findById(detail.getCategoryId()).orElse(null);
            
            categoryResps.add(new CategoryBudgetResp()
                    .setCategoryId(detail.getCategoryId())
                    .setCategoryName(category != null ? category.getName() : "未知分类")
                    .setCategoryIcon(category != null ? category.getIcon() : "")
                    .setBudgetAmount(catBudget)
                    .setExpenseAmount(catExpense)
                    .setRemainingAmount(catRemaining)
                    .setProgress(catProgress)
                    .setStatus(getStatus(catProgress))
            );
        }

        return new BudgetOverviewResp()
                .setLedgerId(ledgerId)
                .setTotalBudget(totalBudget)
                .setTotalExpense(totalExpense)
                .setRemainingBudget(remainingBudget)
                .setProgress(progress)
                .setStatus(status)
                .setCategoryBudgets(categoryResps);
    }

    private String getStatus(int progress) {
        if (progress >= 100) {
            return "EXCEEDED";
        } else if (progress >= 80) {
            return "WARNING";
        } else {
            return "NORMAL";
        }
    }
}
