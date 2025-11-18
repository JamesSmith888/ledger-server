package org.jim.ledgerserver.ledger.controller;

import jakarta.annotation.Resource;
import org.jim.ledgerserver.common.JSONResult;
import org.jim.ledgerserver.common.enums.TransactionTypeEnum;
import org.jim.ledgerserver.common.util.UserContext;
import org.jim.ledgerserver.ledger.entity.LedgerEntity;
import org.jim.ledgerserver.ledger.entity.TransactionEntity;
import org.jim.ledgerserver.ledger.service.LedgerService;
import org.jim.ledgerserver.ledger.service.LedgerMemberService;
import org.jim.ledgerserver.ledger.service.TransactionService;
import org.jim.ledgerserver.ledger.vo.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author James Smith
 */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Resource
    private TransactionService transactionService;

    @Resource
    private LedgerMemberService ledgerMemberService;

    @Resource
    private LedgerService ledgerService;

    /**
     * 创建交易记录
     */
    @PostMapping("/create")
    public JSONResult<Long> createTransaction(@RequestBody TransactionReq transaction) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return JSONResult.fail("用户未登录");
        }

        // 验证对账本的编辑权限
        if (!hasTransactionEditPermission(transaction.ledgerId(), currentUserId)) {
            return JSONResult.fail("无权限在该账本中创建交易");
        }

        TransactionEntity transactionEntity = transactionService.create(
                transaction.name(),
                transaction.description(),
                transaction.amount(),
                transaction.type().getCode(),
                transaction.transactionDateTime(),
                transaction.ledgerId(),
                transaction.categoryId(),
                transaction.paymentMethodId());
        return JSONResult.success(transactionEntity.getId());
    }

    /**
     * 列表查询（已废弃，建议使用 /query 接口）
     * @deprecated 请使用 {@link #queryTransactions(TransactionQueryReq)}
     */
    @Deprecated
    @RequestMapping("/getAll")
    public JSONResult<List<TransactionGetAllResp>> getAll() {
        List<TransactionEntity> transactions = transactionService.findAll();

        // TransactionEntity to TransactionGetAllResp
        List<TransactionGetAllResp> respList = transactions.stream()
                .map(tx ->{
                    return new TransactionGetAllResp(
                            tx.getId(),
                            tx.getName(),
                            tx.getDescription(),
                            tx.getAmount(),
                            TransactionTypeEnum.getByCode(tx.getType()),
                            tx.getTransactionDateTime(),
                            tx.getLedgerId(),
                            tx.getCreatedByUserId(),
                            tx.getCategoryId(),
                            tx.getPaymentMethodId()
                    );
                })
                .toList();

        return JSONResult.success(respList);
    }

    /**
     * 查询交易（支持多条件查询和分页）
     */
    @PostMapping("/query")
    public JSONResult<TransactionPageResp> queryTransactions(@RequestBody TransactionQueryReq request) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return JSONResult.fail("用户未登录");
        }

        // 构建排序
        Sort.Direction direction = "ASC".equalsIgnoreCase(request.getSortDirection())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Sort sort = Sort.by(direction, request.getSortBy());
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        // 查询数据
        Page<TransactionEntity> page = transactionService.queryTransactions(
                request.ledgerId(),
                request.type(),
                request.categoryId(),
                request.startTime(),
                request.endTime(),
                currentUserId,
                pageable
        );

        // 转换为响应对象
        List<TransactionGetAllResp> content = page.getContent().stream()
                .map(tx -> new TransactionGetAllResp(
                        tx.getId(),
                        tx.getName(),
                        tx.getDescription(),
                        tx.getAmount(),
                        TransactionTypeEnum.getByCode(tx.getType()),
                        tx.getTransactionDateTime(),
                        tx.getLedgerId(),
                        tx.getCreatedByUserId(),
                        tx.getCategoryId(),
                        tx.getPaymentMethodId()
                ))
                .toList();

        TransactionPageResp response = new TransactionPageResp(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.hasNext(),
                page.hasPrevious()
        );

        return JSONResult.success(response);
    }

    /**
     * 根据分类ID查询交易
     */
    @GetMapping("/category/{categoryId}")
    public JSONResult<List<TransactionEntity>> getTransactionsByCategoryId(@PathVariable Long categoryId) {
        List<TransactionEntity> transactions = transactionService.findByCategoryId(categoryId);
        return JSONResult.success(transactions);
    }

    /**
     * 将交易移动到指定账本
     */
    @PostMapping("/{transactionId}/move-ledger")
    public JSONResult<Void> moveToLedger(@PathVariable Long transactionId,
                                         @RequestBody TransactionMoveLedgerReq request) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return JSONResult.fail("用户未登录");
        }

        TransactionEntity transaction = transactionService.findById(transactionId);
        
        // 验证当前账本的编辑权限
        if (!currentUserId.equals(transaction.getCreatedByUserId()) &&
            !hasTransactionEditPermission(transaction.getLedgerId(), currentUserId)) {
            return JSONResult.fail("无权限移动该交易");
        }
        
        // 验证目标账本的编辑权限
        if (!hasTransactionEditPermission(request.targetLedgerId(), currentUserId)) {
            return JSONResult.fail("无权限将交易移动到目标账本");
        }

        transactionService.moveToLedger(transactionId, request.targetLedgerId());
        return JSONResult.success();
    }

    /**
     * 删除交易（逻辑删除）
     */
    @DeleteMapping("/{id}")
    public JSONResult<Void> deleteTransaction(@PathVariable Long id) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return JSONResult.fail("用户未登录");
        }

        TransactionEntity transaction = transactionService.findById(id);
        
        // 验证权限：创建者或有账本编辑权限的用户可以删除
        if (!currentUserId.equals(transaction.getCreatedByUserId()) &&
            !hasTransactionEditPermission(transaction.getLedgerId(), currentUserId)) {
            return JSONResult.fail("无权限删除该交易");
        }

        transactionService.delete(id);
        return JSONResult.success();
    }

    /**
     * 更新交易
     */
    @PutMapping("/{id}")
    public JSONResult<TransactionGetAllResp> updateTransaction(
            @PathVariable Long id,
            @RequestBody TransactionUpdateReq request
    ) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return JSONResult.fail("用户未登录");
        }

        TransactionEntity transaction = transactionService.findById(id);
        
        // 验证权限：创建者或有账本编辑权限的用户可以更新
        if (!currentUserId.equals(transaction.getCreatedByUserId()) &&
            !hasTransactionEditPermission(transaction.getLedgerId(), currentUserId)) {
            return JSONResult.fail("无权限更新该交易");
        }

        // 如果要修改账本，验证新账本的编辑权限
        if (request.ledgerId() != null && !request.ledgerId().equals(transaction.getLedgerId())) {
            if (!hasTransactionEditPermission(request.ledgerId(), currentUserId)) {
                return JSONResult.fail("无权限将交易移动到目标账本");
            }
        }

        // 更新字段
        if (request.type() != null) {
            transaction.setType(request.type().getCode());
        }
        if (request.amount() != null) {
            transaction.setAmount(request.amount());
        }
        if (request.categoryId() != null) {
            transaction.setCategoryId(request.categoryId());
        }
        if (request.description() != null) {
            transaction.setDescription(request.description());
        }
        if (request.transactionDateTime() != null) {
            transaction.setTransactionDateTime(request.transactionDateTime());
        }
        if (request.ledgerId() != null) {
            transaction.setLedgerId(request.ledgerId());
        }
        if (request.paymentMethodId() != null) {
            transaction.setPaymentMethodId(request.paymentMethodId());
        }

        TransactionEntity updated = transactionService.update(transaction);

        TransactionGetAllResp response = new TransactionGetAllResp(
                updated.getId(),
                updated.getName(),
                updated.getDescription(),
                updated.getAmount(),
                TransactionTypeEnum.getByCode(updated.getType()),
                updated.getTransactionDateTime(),
                updated.getLedgerId(),
                updated.getCreatedByUserId(),
                updated.getCategoryId(),
                updated.getPaymentMethodId()
        );

        return JSONResult.success(response);
    }

    /**
     * 检查用户是否有交易的编辑权限
     * 规则：
     * 1. 个人账本：只有所有者可以编辑
     * 2. 共享账本：所有者、管理员、记账员可以编辑
     * 
     * @param ledgerId 账本ID
     * @param userId 用户ID
     * @return 是否有编辑权限
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

}
