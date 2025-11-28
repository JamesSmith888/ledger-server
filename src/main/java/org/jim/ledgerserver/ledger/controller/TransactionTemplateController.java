package org.jim.ledgerserver.ledger.controller;

import jakarta.annotation.Resource;
import org.jim.ledgerserver.common.JSONResult;
import org.jim.ledgerserver.common.enums.TransactionSourceEnum;
import org.jim.ledgerserver.common.util.UserContext;
import org.jim.ledgerserver.ledger.entity.TransactionTemplateEntity;
import org.jim.ledgerserver.ledger.service.TransactionService;
import org.jim.ledgerserver.ledger.service.TransactionTemplateService;
import org.jim.ledgerserver.ledger.vo.*;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 交易模板控制器
 * @author James Smith
 */
@RestController
@RequestMapping("/api/templates")
public class TransactionTemplateController {

    @Resource
    private TransactionTemplateService templateService;

    @Resource
    private TransactionService transactionService;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 创建交易模板
     */
    @PostMapping("/create")
    public JSONResult<TransactionTemplateResp> createTemplate(@RequestBody TransactionTemplateReq request) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return JSONResult.fail("用户未登录");
        }

        // 必填项校验
        if (request.name() == null || request.name().trim().isEmpty()) {
            return JSONResult.fail("模板名称不能为空");
        }
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            return JSONResult.fail("金额必须大于0");
        }
        if (request.type() == null) {
            return JSONResult.fail("交易类型不能为空");
        }

        TransactionTemplateEntity template = templateService.create(
                currentUserId,
                request.name(),
                request.amount(),
                request.type(),
                request.categoryId(),
                request.paymentMethodId(),
                request.description(),
                request.allowAmountEdit(),
                request.showInQuickPanel(),
                request.sortOrder(),
                request.icon(),
                request.color(),
                request.ledgerId()
        );

        TransactionTemplateResp resp = toTemplateResp(template);
        return JSONResult.success(resp);
    }

    /**
     * 获取所有模板
     */
    @GetMapping("/list")
    public JSONResult<List<TransactionTemplateResp>> listTemplates() {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return JSONResult.fail("用户未登录");
        }

        List<TransactionTemplateEntity> templates = templateService.findByUserId(currentUserId);
        List<TransactionTemplateResp> respList = templates.stream()
                .map(this::toTemplateResp)
                .collect(Collectors.toList());

        return JSONResult.success(respList);
    }

    /**
     * 获取快捷面板模板
     */
    @GetMapping("/quick-panel")
    public JSONResult<List<TransactionTemplateResp>> getQuickPanelTemplates() {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return JSONResult.fail("用户未登录");
        }

        List<TransactionTemplateEntity> templates = templateService.findQuickPanelTemplates(currentUserId);
        List<TransactionTemplateResp> respList = templates.stream()
                .map(this::toTemplateResp)
                .collect(Collectors.toList());

        return JSONResult.success(respList);
    }

    /**
     * 获取单个模板
     */
    @GetMapping("/{id}")
    public JSONResult<TransactionTemplateResp> getTemplate(@PathVariable Long id) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return JSONResult.fail("用户未登录");
        }

        TransactionTemplateEntity template = templateService.findByIdAndUserId(id, currentUserId);
        TransactionTemplateResp resp = toTemplateResp(template);

        return JSONResult.success(resp);
    }

    /**
     * 更新模板
     */
    @PutMapping("/{id}")
    public JSONResult<TransactionTemplateResp> updateTemplate(
            @PathVariable Long id,
            @RequestBody TransactionTemplateReq request
    ) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return JSONResult.fail("用户未登录");
        }

        // 必填项校验
        if (request.name() != null && request.name().trim().isEmpty()) {
            return JSONResult.fail("模板名称不能为空");
        }
        if (request.amount() != null && request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            return JSONResult.fail("金额必须大于0");
        }

        TransactionTemplateEntity template = templateService.update(
                id,
                currentUserId,
                request.name(),
                request.amount(),
                request.type(),
                request.categoryId(),
                request.paymentMethodId(),
                request.description(),
                request.allowAmountEdit(),
                request.showInQuickPanel(),
                request.sortOrder(),
                request.icon(),
                request.color(),
                request.ledgerId()
        );

        TransactionTemplateResp resp = toTemplateResp(template);
        return JSONResult.success(resp);
    }

    /**
     * 删除模板
     */
    @DeleteMapping("/{id}")
    public JSONResult<Void> deleteTemplate(@PathVariable Long id) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return JSONResult.fail("用户未登录");
        }

        templateService.delete(id, currentUserId);
        return JSONResult.success();
    }

    /**
     * 批量更新模板排序
     */
    @PostMapping("/sort-order")
    public JSONResult<Void> updateSortOrder(@RequestBody TemplateSortOrderReq request) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return JSONResult.fail("用户未登录");
        }

        templateService.updateSortOrder(currentUserId, request.templateIds());
        return JSONResult.success();
    }

    /**
     * 快速创建交易（基于模板）
     */
    @PostMapping("/{id}/quick-create")
    public JSONResult<TransactionGetAllResp> quickCreateTransaction(
            @PathVariable Long id,
            @RequestBody(required = false) QuickCreateTransactionReq request
    ) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return JSONResult.fail("用户未登录");
        }

        // 获取模板
        TransactionTemplateEntity template = templateService.findByIdAndUserId(id, currentUserId);

        // 使用请求中的金额，如果没有则使用模板金额
        BigDecimal amount = (request != null && request.amount() != null) 
                ? request.amount() 
                : template.getAmount();

        // 使用请求中的描述，如果没有则使用模板描述
        String description = (request != null && request.description() != null) 
                ? request.description() 
                : template.getDescription();

        // 使用请求中的交易时间，如果没有则使用当前时间
        LocalDateTime transactionDateTime = (request != null && request.transactionDateTime() != null)
                ? LocalDateTime.parse(request.transactionDateTime(), DATE_TIME_FORMATTER)
                : LocalDateTime.now();

        // 创建交易（使用模板名称作为交易描述）
        String transactionDescription = (description != null && !description.isEmpty()) ? description : template.getName();
        var transaction = transactionService.create(
                transactionDescription,
                amount,
                template.getType(),
                transactionDateTime,
                template.getLedgerId(),
                template.getCategoryId(),
                template.getPaymentMethodId()
        );

        // 转换为响应对象（使用 TransactionController 中的逻辑）
        // 这里简化处理，实际可以复用 TransactionController 的转换方法
        TransactionGetAllResp resp = new TransactionGetAllResp(
                transaction.getId(),
                transaction.getDescription(),
                transaction.getAmount(),
                org.jim.ledgerserver.common.enums.TransactionTypeEnum.getByCode(transaction.getType()),
                transaction.getTransactionDateTime(),
                transaction.getLedgerId(),
                transaction.getCreatedByUserId(),
                null, // createdByUserName - 可以后续优化
                null, // createdByUserNickname - 可以后续优化
                transaction.getCategoryId(),
                transaction.getPaymentMethodId(),
                0L,   // attachmentCount
                TransactionSourceEnum.getByCode(transaction.getSource())  // source
        );

        return JSONResult.success(resp);
    }

    /**
     * 将实体转换为响应对象
     */
    private TransactionTemplateResp toTemplateResp(TransactionTemplateEntity template) {
        return new TransactionTemplateResp(
                template.getId(),
                template.getUserId(),
                template.getName(),
                template.getAmount(),
                template.getType(),
                template.getCategoryId(),
                template.getPaymentMethodId(),
                template.getDescription(),
                template.getAllowAmountEdit(),
                template.getShowInQuickPanel(),
                template.getSortOrder(),
                template.getIcon(),
                template.getColor(),
                template.getLedgerId(),
                template.getCreateTime() != null ? template.getCreateTime().format(DATE_TIME_FORMATTER) : null,
                template.getUpdateTime() != null ? template.getUpdateTime().format(DATE_TIME_FORMATTER) : null
        );
    }
}
