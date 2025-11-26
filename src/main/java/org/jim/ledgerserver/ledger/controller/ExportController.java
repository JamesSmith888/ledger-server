package org.jim.ledgerserver.ledger.controller;

import jakarta.annotation.Resource;
import org.jim.ledgerserver.common.JSONResult;
import org.jim.ledgerserver.common.util.UserContext;
import org.jim.ledgerserver.ledger.service.ExportService;
import org.jim.ledgerserver.ledger.vo.export.ExportDataReq;
import org.jim.ledgerserver.ledger.vo.export.ExportPreviewResp;
import org.jim.ledgerserver.ledger.vo.export.ExportResult;
import org.springframework.web.bind.annotation.*;

/**
 * 数据导出控制器
 * 提供用户数据导出功能
 * 
 * @author James Smith
 */
@RestController
@RequestMapping("/api/export")
public class ExportController {

    @Resource
    private ExportService exportService;

    /**
     * 导出数据
     * 支持导出为JSON、CSV、Excel格式
     * 
     * @param request 导出请求参数
     * @return 导出结果
     */
    @PostMapping("/data")
    public JSONResult<ExportResult> exportData(@RequestBody ExportDataReq request) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return JSONResult.fail("用户未登录");
        }

        try {
            ExportResult result = exportService.exportData(currentUserId, request);
            return JSONResult.success(result);
        } catch (Exception e) {
            return JSONResult.fail("导出失败: " + e.getMessage());
        }
    }

    /**
     * 获取导出数据预览
     * 用于展示将要导出的数据统计信息
     * 
     * @param request 导出请求参数
     * @return 导出预览信息
     */
    @PostMapping("/preview")
    public JSONResult<ExportPreviewResp> getExportPreview(@RequestBody ExportDataReq request) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return JSONResult.fail("用户未登录");
        }

        try {
            ExportPreviewResp preview = exportService.getExportPreview(currentUserId, request);
            return JSONResult.success(preview);
        } catch (Exception e) {
            return JSONResult.fail("获取预览失败: " + e.getMessage());
        }
    }
}
