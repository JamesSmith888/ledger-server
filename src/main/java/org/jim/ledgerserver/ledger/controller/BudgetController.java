package org.jim.ledgerserver.ledger.controller;

import jakarta.annotation.Resource;
import org.jim.ledgerserver.common.JSONResult;
import org.jim.ledgerserver.common.util.UserContext;
import org.jim.ledgerserver.ledger.service.BudgetService;
import org.jim.ledgerserver.ledger.vo.budget.BudgetOverviewResp;
import org.jim.ledgerserver.ledger.vo.budget.BudgetSettingReq;
import org.springframework.web.bind.annotation.*;

/**
 * 预算控制器
 * @author James Smith
 */
@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    @Resource
    private BudgetService budgetService;

    /**
     * 设置预算
     */
    @PostMapping("/setting")
    public JSONResult<Void> setBudget(@RequestBody BudgetSettingReq req) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return JSONResult.fail("用户未登录");
        }
        // TODO: 校验用户是否有权限操作该账本 (这里简化处理，假设前端传的 ledgerId 是合法的)
        
        budgetService.setBudget(req);
        return JSONResult.success();
    }

    /**
     * 获取预算概览
     */
    @GetMapping("/overview")
    public JSONResult<BudgetOverviewResp> getBudgetOverview(
            @RequestParam Long ledgerId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return JSONResult.fail("用户未登录");
        }
        
        BudgetOverviewResp resp = budgetService.getBudgetOverview(ledgerId, year, month);
        return JSONResult.success(resp);
    }
}
