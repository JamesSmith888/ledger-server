package org.jim.ledgerserver.ledger.controller;

import jakarta.annotation.Resource;
import org.jim.ledgerserver.common.JSONResult;
import org.jim.ledgerserver.common.enums.TransactionTypeEnum;
import org.jim.ledgerserver.common.util.UserContext;
import org.jim.ledgerserver.ledger.entity.TransactionEntity;
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

    /**
     * 创建交易记录
     */
    @PostMapping("/create")
    public JSONResult<Long> createTransaction(@RequestBody TransactionReq transaction) {

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
        
        // 验证权限：只有创建者可以删除
        if (!currentUserId.equals(transaction.getCreatedByUserId())) {
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
        
        // 验证权限：只有创建者可以更新
        if (!currentUserId.equals(transaction.getCreatedByUserId())) {
            return JSONResult.fail("无权限更新该交易");
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

}
