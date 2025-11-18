package org.jim.ledgerserver.ledger.controller;

import jakarta.annotation.Resource;
import org.jim.ledgerserver.common.JSONResult;
import org.jim.ledgerserver.common.util.UserContext;
import org.jim.ledgerserver.ledger.service.ReportService;
import org.jim.ledgerserver.ledger.vo.CategoryStatisticsResp;
import org.jim.ledgerserver.ledger.vo.ReportQueryReq;
import org.jim.ledgerserver.ledger.vo.TrendStatisticsResp;
import org.springframework.web.bind.annotation.*;

/**
 * 报表接口控制器
 * @author James Smith
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Resource
    private ReportService reportService;

    /**
     * 按分类统计
     * 用于生成饼图、柱状图等分类维度的图表
     * 
     * @param request 查询参数
     * @return 分类统计结果
     */
    @PostMapping("/by-category")
    public JSONResult<CategoryStatisticsResp> getStatisticsByCategory(@RequestBody ReportQueryReq request) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return JSONResult.fail("用户未登录");
        }

        CategoryStatisticsResp result = reportService.getStatisticsByCategory(request, currentUserId);
        return JSONResult.success(result);
    }

    /**
     * 按时间趋势统计
     * 用于生成折线图、面积图等时间序列图表
     * 
     * @param request 查询参数
     * @return 趋势统计结果
     */
    @PostMapping("/trend")
    public JSONResult<TrendStatisticsResp> getTrendStatistics(@RequestBody ReportQueryReq request) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return JSONResult.fail("用户未登录");
        }

        TrendStatisticsResp result = reportService.getTrendStatistics(request, currentUserId);
        return JSONResult.success(result);
    }
}
