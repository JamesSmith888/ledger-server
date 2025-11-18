package org.jim.ledgerserver.ledger.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.jim.ledgerserver.common.JSONResult;
import org.jim.ledgerserver.common.enums.LedgerMemberRoleEnum;
import org.jim.ledgerserver.common.util.UserContext;
import org.jim.ledgerserver.ledger.dto.*;
import org.jim.ledgerserver.ledger.entity.InviteCodeEntity;
import org.jim.ledgerserver.ledger.entity.LedgerEntity;
import org.jim.ledgerserver.ledger.entity.LedgerMemberEntity;
import org.jim.ledgerserver.ledger.service.InviteCodeService;
import org.jim.ledgerserver.ledger.service.LedgerService;
import org.jim.ledgerserver.ledger.service.LedgerMemberService;
import org.jim.ledgerserver.user.entity.UserEntity;
import org.jim.ledgerserver.user.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 账本邀请控制器
 * 处理邀请码生成、验证、使用等功能
 * 
 * @author James Smith
 */
@RestController
@RequestMapping("/api/ledgers")
public class LedgerInviteController {

    @Resource
    private InviteCodeService inviteCodeService;

    @Resource
    private LedgerService ledgerService;

    @Resource
    private LedgerMemberService ledgerMemberService;

    @Resource
    private UserService userService;

    /**
     * 生成邀请码
     * POST /api/ledgers/{ledgerId}/invites
     */
    @PostMapping("/{ledgerId}/invites")
    public JSONResult<InviteCodeResponse> createInviteCode(
            @PathVariable Long ledgerId,
            @Valid @RequestBody CreateInviteCodeRequest request) {
        Long currentUserId = UserContext.getCurrentUserId();

        LedgerMemberRoleEnum role = LedgerMemberRoleEnum.getByCode(request.role());
        if (role == null) {
            return JSONResult.fail("无效的角色代码");
        }

        InviteCodeEntity inviteCode = inviteCodeService.generateInviteCode(
                ledgerId,
                currentUserId,
                role,
                request.maxUses(),
                request.expireHours()
        );

        return JSONResult.success(convertToResponse(inviteCode));
    }

    /**
     * 获取账本的所有邀请码
     * GET /api/ledgers/{ledgerId}/invites
     */
    @GetMapping("/{ledgerId}/invites")
    public JSONResult<List<InviteCodeResponse>> getInviteCodes(
            @PathVariable Long ledgerId,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        Long currentUserId = UserContext.getCurrentUserId();

        // 验证访问权限
        if (!ledgerMemberService.hasManagePermission(ledgerId, currentUserId)) {
            return JSONResult.fail("无权限查看邀请码");
        }

        List<InviteCodeEntity> inviteCodes = inviteCodeService.getInviteCodes(ledgerId, includeInactive);
        List<InviteCodeResponse> responses = inviteCodes.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return JSONResult.success(responses);
    }

    /**
     * 验证邀请码
     * GET /api/invites/validate/{code}
     */
    @GetMapping("/invites/validate/{code}")
    public JSONResult<InviteValidateResponse> validateInviteCode(@PathVariable String code) {
        InviteValidateResponse response = new InviteValidateResponse();

        try {
            InviteCodeEntity inviteCode = inviteCodeService.validateInviteCode(code);
            LedgerEntity ledger = ledgerService.findById(inviteCode.getLedgerId());
            UserEntity inviter = userService.findById(inviteCode.getCreatedByUserId());
            long memberCount = ledgerMemberService.countMembers(ledger.getId());

            response.setIsValid(true);
            response.setLedgerId(ledger.getId());
            response.setLedgerName(ledger.getName());
            response.setLedgerDescription(ledger.getDescription());
            response.setInviterName(inviter != null ? inviter.getUsername() : "未知用户");
            response.setRole(inviteCode.getRole());
            response.setRoleName(LedgerMemberRoleEnum.getRoleDescription(inviteCode.getRole()));
            response.setExpireTime(inviteCode.getExpireTime());
            response.setMemberCount((int) memberCount);
            response.setMaxMembers(ledger.getMaxMembers());

        } catch (Exception e) {
            response.setIsValid(false);
            response.setErrorMessage(e.getMessage());
        }

        return JSONResult.success(response);
    }

    /**
     * 使用邀请码加入账本
     * POST /api/invites/accept/{code}
     */
    @PostMapping("/invites/accept/{code}")
    public JSONResult<LedgerMemberResponse> acceptInvite(@PathVariable String code) {
        Long currentUserId = UserContext.getCurrentUserId();

        try {
            LedgerMemberEntity member = inviteCodeService.acceptInvite(code, currentUserId);
            LedgerMemberResponse response = convertToMemberResponse(member);
            return JSONResult.success(response);
        } catch (Exception e) {
            return JSONResult.fail(e.getMessage());
        }
    }

    /**
     * 禁用邀请码
     * DELETE /api/ledgers/{ledgerId}/invites/{inviteId}
     */
    @DeleteMapping("/{ledgerId}/invites/{inviteId}")
    public JSONResult<Void> disableInviteCode(
            @PathVariable Long ledgerId,
            @PathVariable Long inviteId) {
        Long currentUserId = UserContext.getCurrentUserId();

        try {
            inviteCodeService.disableInviteCode(inviteId, currentUserId);
            return JSONResult.success();
        } catch (Exception e) {
            return JSONResult.fail(e.getMessage());
        }
    }

    /**
     * 直接邀请用户（通过用户ID）
     * POST /api/ledgers/{ledgerId}/members/direct-invite
     */
    @PostMapping("/{ledgerId}/members/direct-invite")
    public JSONResult<LedgerMemberResponse> directInvite(
            @PathVariable Long ledgerId,
            @Valid @RequestBody DirectInviteRequest request) {
        Long currentUserId = UserContext.getCurrentUserId();

        try {
            // 验证权限
            if (!ledgerMemberService.hasManagePermission(ledgerId, currentUserId)) {
                return JSONResult.fail("无权限邀请成员");
            }

            // 验证角色
            LedgerMemberRoleEnum role = LedgerMemberRoleEnum.getByCode(request.role());
            if (role == null) {
                return JSONResult.fail("无效的角色代码");
            }

            // 添加成员
            LedgerMemberEntity member = ledgerMemberService.addMember(
                    ledgerId,
                    request.userId(),
                    role,
                    currentUserId,
                    "直接邀请"
            );

            LedgerMemberResponse response = convertToMemberResponse(member);
            return JSONResult.success(response);

        } catch (Exception e) {
            return JSONResult.fail(e.getMessage());
        }
    }

    /**
     * 转换为邀请码响应对象
     */
    private InviteCodeResponse convertToResponse(InviteCodeEntity entity) {
        InviteCodeResponse response = new InviteCodeResponse();
        response.setId(entity.getId());
        response.setCode(entity.getCode());
        response.setLedgerId(entity.getLedgerId());
        response.setCreatedByUserId(entity.getCreatedByUserId());
        response.setRole(entity.getRole());
        response.setRoleName(LedgerMemberRoleEnum.getRoleDescription(entity.getRole()));
        response.setMaxUses(entity.getMaxUses());
        response.setUsedCount(entity.getUsedCount());
        response.setExpireTime(entity.getExpireTime());
        response.setIsExpired(entity.isExpired());
        response.setIsExhausted(entity.isExhausted());
        response.setStatus(entity.getStatus());
        response.setStatusName(entity.isValid() ? "有效" : "禁用");
        response.setCreateTime(entity.getCreateTime());
        response.setUpdateTime(entity.getUpdateTime());

        // 可以添加完整链接（如果有前端域名配置）
        // response.setInviteUrl("https://yourapp.com/invite/" + entity.getCode());

        // 填充账本名称和创建者名称
        try {
            LedgerEntity ledger = ledgerService.findById(entity.getLedgerId());
            response.setLedgerName(ledger.getName());

            UserEntity creator = userService.findById(entity.getCreatedByUserId());
            if (creator != null) {
                response.setCreatedByUserName(creator.getUsername());
            }
        } catch (Exception e) {
            // 忽略查询失败
        }

        return response;
    }

    /**
     * 转换为成员响应对象
     */
    private LedgerMemberResponse convertToMemberResponse(LedgerMemberEntity entity) {
        LedgerMemberResponse response = new LedgerMemberResponse();
        response.setId(entity.getId());
        response.setLedgerId(entity.getLedgerId());
        response.setUserId(entity.getUserId());
        response.setRole(entity.getRole());
        response.setRoleName(LedgerMemberRoleEnum.getRoleDescription(entity.getRole()));
        response.setJoinedAt(entity.getJoinedAt());
        response.setStatus(entity.getStatus());

        // 填充用户名
        try {
            UserEntity user = userService.findById(entity.getUserId());
            if (user != null) {
                response.setUsername(user.getUsername());
            }
        } catch (Exception e) {
            // 忽略查询失败
        }

        return response;
    }
}
