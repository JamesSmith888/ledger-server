package org.jim.ledgerserver.ledger.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.jim.ledgerserver.common.JSONResult;
import org.jim.ledgerserver.common.enums.LedgerMemberRoleEnum;
import org.jim.ledgerserver.common.util.UserContext;
import org.jim.ledgerserver.ledger.dto.AddMemberRequest;
import org.jim.ledgerserver.ledger.dto.LedgerMemberResponse;
import org.jim.ledgerserver.ledger.dto.UpdateMemberRoleRequest;
import org.jim.ledgerserver.ledger.entity.LedgerEntity;
import org.jim.ledgerserver.ledger.entity.LedgerMemberEntity;
import org.jim.ledgerserver.ledger.service.LedgerService;
import org.jim.ledgerserver.ledger.service.LedgerMemberService;
import org.jim.ledgerserver.user.entity.UserEntity;
import org.jim.ledgerserver.user.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 账本成员管理控制器
 * @author James Smith
 */
@RestController
@RequestMapping("/api/ledgers/{ledgerId}/members")
public class LedgerMemberController {

    @Resource
    private LedgerMemberService ledgerMemberService;
    
    @Resource
    private LedgerService ledgerService;
    
    @Resource
    private UserService userService;

    /**
     * 添加成员到账本
     */
    @PostMapping
    public JSONResult<LedgerMemberResponse> addMember(
            @PathVariable Long ledgerId,
            @Valid @RequestBody AddMemberRequest request) {
        Long currentUserId = UserContext.getCurrentUserId();
        
        // 验证账本存在
        LedgerEntity ledger = ledgerService.findById(ledgerId);
        if (ledger.isPersonal()) {
            return JSONResult.fail("个人账本不支持添加成员");
        }
        
        // 验证权限
        if (!ledgerMemberService.hasManagePermission(ledgerId, currentUserId)) {
            return JSONResult.fail("无权限添加成员");
        }
        
        LedgerMemberRoleEnum role = LedgerMemberRoleEnum.getByCode(request.role());
        if (role == null) {
            return JSONResult.fail("无效的成员角色");
        }
        
        LedgerMemberEntity member = ledgerMemberService.addMember(
            ledgerId, 
            request.userId(), 
            role, 
            currentUserId, 
            request.remark()
        );
        
        return JSONResult.success(convertToResponse(member));
    }

    /**
     * 获取账本的所有成员
     */
    @GetMapping
    public JSONResult<List<LedgerMemberResponse>> getMembers(@PathVariable Long ledgerId) {
        Long currentUserId = UserContext.getCurrentUserId();
        
        // 验证访问权限
        if (!ledgerMemberService.hasViewPermission(ledgerId, currentUserId)) {
            return JSONResult.fail("无权限查看成员列表");
        }
        
        List<LedgerMemberEntity> members = ledgerMemberService.findMembersByLedgerId(ledgerId);
        List<LedgerMemberResponse> responses = members.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        
        return JSONResult.success(responses);
    }

    /**
     * 分页获取账本成员
     */
    @GetMapping("/page")
    public JSONResult<Page<LedgerMemberResponse>> getMembersPage(
            @PathVariable Long ledgerId,
            @PageableDefault(size = 10) Pageable pageable) {
        Long currentUserId = UserContext.getCurrentUserId();
        
        // 验证访问权限
        if (!ledgerMemberService.hasViewPermission(ledgerId, currentUserId)) {
            return JSONResult.fail("无权限查看成员列表");
        }
        
        Page<LedgerMemberEntity> members = ledgerMemberService.findMembersByLedgerId(ledgerId, pageable);
        Page<LedgerMemberResponse> responses = members.map(this::convertToResponse);
        
        return JSONResult.success(responses);
    }

    /**
     * 更新成员角色
     */
    @PutMapping("/{userId}/role")
    public JSONResult<LedgerMemberResponse> updateMemberRole(
            @PathVariable Long ledgerId,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateMemberRoleRequest request) {
        Long currentUserId = UserContext.getCurrentUserId();
        
        LedgerMemberRoleEnum newRole = LedgerMemberRoleEnum.getByCode(request.role());
        if (newRole == null) {
            return JSONResult.fail("无效的成员角色");
        }
        
        LedgerMemberEntity member = ledgerMemberService.updateMemberRole(
            ledgerId, userId, newRole, currentUserId
        );
        
        return JSONResult.success(convertToResponse(member));
    }

    /**
     * 移除成员
     */
    @DeleteMapping("/{userId}")
    public JSONResult<Void> removeMember(
            @PathVariable Long ledgerId,
            @PathVariable Long userId) {
        Long currentUserId = UserContext.getCurrentUserId();
        
        ledgerMemberService.removeMember(ledgerId, userId, currentUserId);
        return JSONResult.success();
    }

    /**
     * 获取成员详情
     */
    @GetMapping("/{userId}")
    public JSONResult<LedgerMemberResponse> getMember(
            @PathVariable Long ledgerId,
            @PathVariable Long userId) {
        Long currentUserId = UserContext.getCurrentUserId();
        
        // 验证访问权限
        if (!ledgerMemberService.hasViewPermission(ledgerId, currentUserId)) {
            return JSONResult.fail("无权限查看成员信息");
        }
        
        LedgerMemberEntity member = ledgerMemberService.findMemberRelation(ledgerId, userId)
                .orElseThrow(() -> new RuntimeException("成员关系不存在"));
        
        return JSONResult.success(convertToResponse(member));
    }

    /**
     * 退出账本（用户主动离开）
     */
    @PostMapping("/leave")
    public JSONResult<Void> leaveLedger(@PathVariable Long ledgerId) {
        Long currentUserId = UserContext.getCurrentUserId();
        
        // 查找成员关系
        LedgerMemberEntity member = ledgerMemberService.findMemberRelation(ledgerId, currentUserId)
                .orElseThrow(() -> new RuntimeException("您不是该账本的成员"));
        
        // 所有者不能退出自己的账本
        if (member.isOwner()) {
            return JSONResult.fail("账本所有者不能退出账本");
        }
        
        ledgerMemberService.removeMember(ledgerId, currentUserId, currentUserId);
        return JSONResult.success();
    }

    /**
     * 转换为响应对象
     */
    private LedgerMemberResponse convertToResponse(LedgerMemberEntity member) {
        LedgerMemberResponse response = new LedgerMemberResponse();
        response.setId(member.getId());
        response.setLedgerId(member.getLedgerId());
        response.setUserId(member.getUserId());
        response.setRole(member.getRole());
        response.setRoleName(LedgerMemberRoleEnum.getRoleDescription(member.getRole()));
        response.setJoinedAt(member.getJoinedAt());
        response.setInvitedByUserId(member.getInvitedByUserId());
        response.setStatus(member.getStatus());
        response.setRemark(member.getRemark());
        
        // 获取用户信息
        try {
            UserEntity user = userService.findById(member.getUserId());
            response.setUsername(user.getUsername());
            response.setNickname(user.getNickname());
            response.setAvatarUrl(user.getAvatarUrl());
        } catch (Exception e) {
            // 用户信息获取失败时的降级处理
            response.setUsername("未知用户");
        }
        
        return response;
    }
}