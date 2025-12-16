package org.jim.ledgerserver.ledger.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.jim.ledgerserver.common.JSONResult;
import org.jim.ledgerserver.common.enums.LedgerTypeEnum;
import org.jim.ledgerserver.common.util.UserContext;
import org.jim.ledgerserver.ledger.dto.CreateLedgerRequest;
import org.jim.ledgerserver.ledger.dto.LedgerResponse;
import org.jim.ledgerserver.ledger.dto.UpdateLedgerRequest;
import org.jim.ledgerserver.ledger.entity.LedgerEntity;
import org.jim.ledgerserver.ledger.service.LedgerService;
import org.jim.ledgerserver.ledger.service.LedgerMemberService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 账本控制器
 * @author James Smith
 */
@RestController
@RequestMapping("/api/ledgers")
public class LedgerController {

    @Resource
    private LedgerService ledgerService;
    
    @Resource
    private LedgerMemberService ledgerMemberService;

    /**
     * 创建个人账本
     */
    @PostMapping
    public JSONResult<LedgerResponse> createLedger(@Valid @RequestBody CreateLedgerRequest request) {
        Long currentUserId = UserContext.getCurrentUserId();
        LedgerEntity ledger = ledgerService.create(request.name(), request.description(), currentUserId);
        return JSONResult.success(convertToResponse(ledger));
    }

    /**
     * 创建共享账本
     */
    @PostMapping("/shared")
    public JSONResult<LedgerResponse> createSharedLedger(@Valid @RequestBody CreateLedgerRequest request) {
        Long currentUserId = UserContext.getCurrentUserId();
        LedgerEntity ledger = ledgerService.createSharedLedger(
            request.name(), 
            request.description(), 
            currentUserId,
            request.maxMembers(),
            request.isPublic()
        );
        return JSONResult.success(convertToResponse(ledger));
    }

    /**
     * 获取当前用户的所有账本（包括自己创建和参与的）
     */
    @GetMapping
    public JSONResult<List<LedgerResponse>> getMyLedgers() {
        Long currentUserId = UserContext.getCurrentUserId();
        List<LedgerEntity> ledgers = ledgerService.findAccessibleLedgersByUserId(currentUserId);
        
        // 批量查询成员数量
        List<Long> sharedLedgerIds = ledgers.stream()
                .filter(LedgerEntity::isShared)
                .map(LedgerEntity::getId)
                .toList();
        java.util.Map<Long, Long> memberCountMap = ledgerMemberService.countMembersByLedgerIds(sharedLedgerIds);
        
        List<LedgerResponse> responses = ledgers.stream()
                .map(ledger -> convertToResponse(ledger, memberCountMap))
                .collect(Collectors.toList());
        return JSONResult.success(responses);
    }

    /**
     * 分页获取当前用户的账本
     */
    @GetMapping("/page")
    public JSONResult<Page<LedgerResponse>> getMyLedgersPage(
            @PageableDefault(size = 10) Pageable pageable) {
        Long currentUserId = UserContext.getCurrentUserId();
        Page<LedgerEntity> ledgers = ledgerService.findAccessibleLedgersByUserId(currentUserId, pageable);
        
        // 批量查询成员数量
        List<Long> sharedLedgerIds = ledgers.getContent().stream()
                .filter(LedgerEntity::isShared)
                .map(LedgerEntity::getId)
                .toList();
        java.util.Map<Long, Long> memberCountMap = ledgerMemberService.countMembersByLedgerIds(sharedLedgerIds);
        
        Page<LedgerResponse> responses = ledgers.map(ledger -> convertToResponse(ledger, memberCountMap));
        return JSONResult.success(responses);
    }

    /**
     * 获取当前用户拥有的账本
     */
    @GetMapping("/owned")
    public JSONResult<List<LedgerResponse>> getOwnedLedgers() {
        Long currentUserId = UserContext.getCurrentUserId();
        List<LedgerEntity> ledgers = ledgerService.findByOwnerUserId(currentUserId);
        List<LedgerResponse> responses = ledgers.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return JSONResult.success(responses);
    }

    /**
     * 获取当前用户参与的共享账本
     */
    @GetMapping("/shared")
    public JSONResult<List<LedgerResponse>> getSharedLedgers() {
        Long currentUserId = UserContext.getCurrentUserId();
        List<LedgerEntity> ledgers = ledgerService.findSharedLedgersByUserId(currentUserId);
        List<LedgerResponse> responses = ledgers.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return JSONResult.success(responses);
    }

    /**
     * 根据ID获取账本详情
     */
    @GetMapping("/{id}")
    public JSONResult<LedgerResponse> getLedger(@PathVariable Long id) {
        Long currentUserId = UserContext.getCurrentUserId();
        LedgerEntity ledger = ledgerService.findById(id);
        
        // 验证访问权限
        if (!hasAccessPermission(ledger, currentUserId)) {
            return JSONResult.fail("无权限访问该账本");
        }
        
        return JSONResult.success(convertToResponse(ledger));
    }

    /**
     * 更新账本
     */
    @PutMapping("/{id}")
    public JSONResult<LedgerResponse> updateLedger(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLedgerRequest request) {
        Long currentUserId = UserContext.getCurrentUserId();
        LedgerEntity ledger = ledgerService.findById(id);
        
        // 验证管理权限
        if (!hasManagePermission(ledger, currentUserId)) {
            return JSONResult.fail("无权限修改该账本");
        }
        
        LedgerEntity updatedLedger = ledgerService.update(
            id, 
            request.name(), 
            request.description(),
            request.maxMembers(),
            request.isPublic()
        );
        
        return JSONResult.success(convertToResponse(updatedLedger));
    }

    /**
     * 删除账本
     */
    @DeleteMapping("/{id}")
    public JSONResult<Void> deleteLedger(@PathVariable Long id) {
        Long currentUserId = UserContext.getCurrentUserId();
        LedgerEntity ledger = ledgerService.findById(id);
        
        // 只有所有者可以删除账本
        if (!ledger.getOwnerUserId().equals(currentUserId)) {
            return JSONResult.fail("只有所有者可以删除账本");
        }
        
        ledgerService.delete(id);
        return JSONResult.success();
    }

    /**
     * 根据类型获取账本
     */
    @GetMapping("/type/{type}")
    public JSONResult<List<LedgerResponse>> getLedgersByType(@PathVariable String type) {
        LedgerTypeEnum typeEnum;
        try {
            typeEnum = LedgerTypeEnum.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return JSONResult.fail("无效的账本类型");
        }
        
        List<LedgerEntity> ledgers = ledgerService.findByType(typeEnum.getCode());
        List<LedgerResponse> responses = ledgers.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return JSONResult.success(responses);
    }

    /**
     * 转换为响应对象
     */
    private LedgerResponse convertToResponse(LedgerEntity ledger) {
        LedgerResponse response = new LedgerResponse();
        response.setId(ledger.getId());
        response.setName(ledger.getName());
        response.setDescription(ledger.getDescription());
        response.setOwnerUserId(ledger.getOwnerUserId());
        response.setType(ledger.getType());
        response.setTypeName(LedgerTypeEnum.getTypeDescription(ledger.getType()));
        response.setMaxMembers(ledger.getMaxMembers());
        response.setIsPublic(ledger.getIsPublic());
        response.setCreateTime(ledger.getCreateTime());
        response.setUpdateTime(ledger.getUpdateTime());
        
        // 如果是共享账本，添加成员数量信息
        if (ledger.isShared()) {
            long memberCount = ledgerMemberService.countMembers(ledger.getId());
            response.setMemberCount((int) memberCount);
        }
        
        return response;
    }

    private LedgerResponse convertToResponse(LedgerEntity ledger, java.util.Map<Long, Long> memberCountMap) {
        LedgerResponse response = new LedgerResponse();
        response.setId(ledger.getId());
        response.setName(ledger.getName());
        response.setDescription(ledger.getDescription());
        response.setOwnerUserId(ledger.getOwnerUserId());
        response.setType(ledger.getType());
        response.setTypeName(LedgerTypeEnum.getTypeDescription(ledger.getType()));
        response.setMaxMembers(ledger.getMaxMembers());
        response.setIsPublic(ledger.getIsPublic());
        response.setCreateTime(ledger.getCreateTime());
        response.setUpdateTime(ledger.getUpdateTime());
        
        // 如果是共享账本，添加成员数量信息
        if (ledger.isShared()) {
            long memberCount = memberCountMap.getOrDefault(ledger.getId(), 0L);
            response.setMemberCount((int) memberCount);
        }
        
        return response;
    }

    /**
     * 检查是否有访问权限
     */
    private boolean hasAccessPermission(LedgerEntity ledger, Long userId) {
        // 所有者总是有权限
        if (ledger.getOwnerUserId().equals(userId)) {
            return true;
        }
        
        // 个人账本只有所有者可以访问
        if (ledger.isPersonal()) {
            return false;
        }
        
        // 共享账本检查成员权限
        return ledgerMemberService.hasViewPermission(ledger.getId(), userId);
    }

    /**
     * 检查是否有管理权限
     */
    private boolean hasManagePermission(LedgerEntity ledger, Long userId) {
        // 所有者总是有管理权限
        if (ledger.getOwnerUserId().equals(userId)) {
            return true;
        }
        
        // 个人账本只有所有者可以管理
        if (ledger.isPersonal()) {
            return false;
        }
        
        // 共享账本检查管理权限
        return ledgerMemberService.hasManagePermission(ledger.getId(), userId);
    }
}