package org.jim.ledgerserver.ledger.controller;

import jakarta.annotation.Resource;
import org.jim.ledgerserver.common.JSONResult;
import org.jim.ledgerserver.common.enums.TransactionTypeEnum;
import org.jim.ledgerserver.common.util.UserContext;
import org.jim.ledgerserver.ledger.entity.LedgerEntity;
import org.jim.ledgerserver.ledger.entity.TransactionAttachmentEntity;
import org.jim.ledgerserver.ledger.entity.TransactionEntity;
import org.jim.ledgerserver.ledger.service.LedgerService;
import org.jim.ledgerserver.ledger.service.LedgerMemberService;
import org.jim.ledgerserver.ledger.service.TransactionAttachmentService;
import org.jim.ledgerserver.ledger.service.TransactionService;
import org.jim.ledgerserver.ledger.vo.*;
import org.jim.ledgerserver.user.entity.UserEntity;
import org.jim.ledgerserver.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

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

    @Resource
    private UserRepository userRepository;

    @Resource
    private TransactionAttachmentService attachmentService;

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
                .map(this::toTransactionResp)
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

        // 批量获取用户信息和附件数量，避免N+1查询
        List<TransactionEntity> transactions = page.getContent();
        
        // 收集所有唯一的用户ID
        List<Long> userIds = transactions.stream()
                .map(TransactionEntity::getCreatedByUserId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        
        // 批量查询用户信息
        java.util.Map<Long, UserEntity> userMap = new java.util.HashMap<>();
        if (!userIds.isEmpty()) {
            List<UserEntity> users = userRepository.findAllById(userIds);
            users.forEach(user -> userMap.put(user.getId(), user));
        }
        
        // 批量查询附件数量
        List<Long> transactionIds = transactions.stream()
                .map(TransactionEntity::getId)
                .toList();
        java.util.Map<Long, Long> attachmentCountMap = attachmentService.countAttachmentsByTransactionIds(transactionIds);

        // 转换为响应对象（使用批量查询的数据）
        List<TransactionGetAllResp> content = transactions.stream()
                .map(tx -> toTransactionResp(tx, userMap, attachmentCountMap))
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
     * 获取每日统计数据（用于热力图）
     * @param ledgerId 账本ID（可选）
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 每日统计列表
     */
    @GetMapping("/daily-statistics")
    public JSONResult<List<DailyStatisticsResp>> getDailyStatistics(
            @RequestParam(required = false) Long ledgerId,
            @RequestParam String startTime,
            @RequestParam String endTime
    ) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return JSONResult.fail("用户未登录");
        }

        List<DailyStatisticsResp> statistics = transactionService.getDailyStatistics(
                ledgerId,
                startTime,
                endTime,
                currentUserId
        );

        return JSONResult.success(statistics);
    }

    /**
     * 获取月度汇总统计（用于列表页顶部汇总区域）
     * @param ledgerId 账本ID（可选）
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 月度汇总统计
     */
    @GetMapping("/monthly-summary")
    public JSONResult<MonthlySummaryResp> getMonthlySummary(
            @RequestParam(required = false) Long ledgerId,
            @RequestParam String startTime,
            @RequestParam String endTime
    ) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return JSONResult.fail("用户未登录");
        }

        MonthlySummaryResp summary = transactionService.getMonthlySummary(
                ledgerId,
                startTime,
                endTime,
                currentUserId
        );

        return JSONResult.success(summary);
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

        TransactionGetAllResp response = toTransactionResp(updated);

        return JSONResult.success(response);
    }

    /**
     * 将 TransactionEntity 转换为 TransactionGetAllResp，包含创建人信息和附件数量
     * （旧版本，兼容性保留，建议使用批量查询版本）
     */
    private TransactionGetAllResp toTransactionResp(TransactionEntity tx) {
        String createdByUserName = null;
        String createdByUserNickname = null;
        
        // 获取创建人信息
        if (tx.getCreatedByUserId() != null) {
            try {
                UserEntity user = userRepository.findById(tx.getCreatedByUserId()).orElse(null);
                if (user != null) {
                    createdByUserName = user.getUsername();
                    createdByUserNickname = user.getNickname();
                }
            } catch (Exception e) {
                // 忽略用户查询异常，不影响交易数据返回
            }
        }
        
        // 获取附件数量
        long attachmentCount = 0;
        try {
            attachmentCount = attachmentService.countAttachments(tx.getId());
        } catch (Exception e) {
            // 忽略附件查询异常，不影响交易数据返回
        }
        
        return new TransactionGetAllResp(
                tx.getId(),
                tx.getName(),
                tx.getDescription(),
                tx.getAmount(),
                TransactionTypeEnum.getByCode(tx.getType()),
                tx.getTransactionDateTime(),
                tx.getLedgerId(),
                tx.getCreatedByUserId(),
                createdByUserName,
                createdByUserNickname,
                tx.getCategoryId(),
                tx.getPaymentMethodId(),
                attachmentCount
        );
    }

    /**
     * 将 TransactionEntity 转换为 TransactionGetAllResp（批量查询优化版本）
     * @param tx 交易实体
     * @param userMap 用户信息映射
     * @param attachmentCountMap 附件数量映射
     */
    private TransactionGetAllResp toTransactionResp(
            TransactionEntity tx,
            java.util.Map<Long, UserEntity> userMap,
            java.util.Map<Long, Long> attachmentCountMap) {
        
        String createdByUserName = null;
        String createdByUserNickname = null;
        
        // 从批量查询结果中获取用户信息
        if (tx.getCreatedByUserId() != null) {
            UserEntity user = userMap.get(tx.getCreatedByUserId());
            if (user != null) {
                createdByUserName = user.getUsername();
                createdByUserNickname = user.getNickname();
            }
        }
        
        // 从批量查询结果中获取附件数量
        long attachmentCount = attachmentCountMap.getOrDefault(tx.getId(), 0L);
        
        return new TransactionGetAllResp(
                tx.getId(),
                tx.getName(),
                tx.getDescription(),
                tx.getAmount(),
                TransactionTypeEnum.getByCode(tx.getType()),
                tx.getTransactionDateTime(),
                tx.getLedgerId(),
                tx.getCreatedByUserId(),
                createdByUserName,
                createdByUserNickname,
                tx.getCategoryId(),
                tx.getPaymentMethodId(),
                attachmentCount
        );
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

    // ==================== 附件相关接口 ====================

    /**
     * 上传交易附件
     */
    @PostMapping("/{transactionId}/attachments")
    public JSONResult<AttachmentMetadataResp> uploadAttachment(
            @PathVariable Long transactionId,
            @RequestParam("file") MultipartFile file
    ) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return JSONResult.fail("用户未登录");
        }

        TransactionEntity transaction = transactionService.findById(transactionId);
        
        // 验证权限：创建者或有账本编辑权限的用户可以上传附件
        if (!currentUserId.equals(transaction.getCreatedByUserId()) &&
            !hasTransactionEditPermission(transaction.getLedgerId(), currentUserId)) {
            return JSONResult.fail("无权限上传附件");
        }

        TransactionAttachmentEntity attachment = attachmentService.uploadAttachment(transactionId, file);
        
        AttachmentMetadataResp resp = new AttachmentMetadataResp(
                attachment.getId(),
                attachment.getTransactionId(),
                attachment.getFileName(),
                attachment.getFileType(),
                attachment.getFileSize(),
                attachment.getWidth(),
                attachment.getHeight(),
                attachment.getUploadedByUserId(),
                attachment.getCreateTime(),
                attachment.getThumbnailData() != null
        );

        return JSONResult.success(resp);
    }

    /**
     * 获取交易附件列表（仅元数据）
     */
    @GetMapping("/{transactionId}/attachments")
    public JSONResult<List<AttachmentMetadataResp>> getAttachments(@PathVariable Long transactionId) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return JSONResult.fail("用户未登录");
        }

        List<TransactionAttachmentEntity> attachments = attachmentService.getAttachmentMetadata(transactionId);
        
        List<AttachmentMetadataResp> respList = attachments.stream()
                .map(a -> new AttachmentMetadataResp(
                        a.getId(),
                        a.getTransactionId(),
                        a.getFileName(),
                        a.getFileType(),
                        a.getFileSize(),
                        a.getWidth(),
                        a.getHeight(),
                        a.getUploadedByUserId(),
                        a.getCreateTime(),
                        a.getThumbnailData() != null
                ))
                .collect(Collectors.toList());

        return JSONResult.success(respList);
    }

    /**
     * 下载附件（完整文件）
     */
    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<byte[]> downloadAttachment(@PathVariable Long attachmentId) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        TransactionAttachmentEntity attachment = attachmentService.getAttachment(attachmentId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(attachment.getFileType()));
        headers.setContentDispositionFormData("attachment", attachment.getFileName());
        headers.setContentLength(attachment.getFileSize());

        return new ResponseEntity<>(attachment.getFileData(), headers, HttpStatus.OK);
    }

    /**
     * 获取附件缩略图
     */
    @GetMapping("/attachments/{attachmentId}/thumbnail")
    public ResponseEntity<byte[]> getThumbnail(@PathVariable Long attachmentId) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        TransactionAttachmentEntity attachment = attachmentService.getAttachment(attachmentId);

        if (attachment.getThumbnailData() == null) {
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);
        headers.setCacheControl("max-age=86400"); // 缓存1天

        return new ResponseEntity<>(attachment.getThumbnailData(), headers, HttpStatus.OK);
    }

    /**
     * 删除附件
     */
    @DeleteMapping("/attachments/{attachmentId}")
    public JSONResult<Void> deleteAttachment(@PathVariable Long attachmentId) {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return JSONResult.fail("用户未登录");
        }

        attachmentService.deleteAttachment(attachmentId);
        return JSONResult.success();
    }

}
