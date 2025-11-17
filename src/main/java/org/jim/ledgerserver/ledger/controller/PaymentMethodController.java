package org.jim.ledgerserver.ledger.controller;

import jakarta.annotation.Resource;
import org.jim.ledgerserver.common.JSONResult;
import org.jim.ledgerserver.common.util.UserContext;
import org.jim.ledgerserver.ledger.entity.PaymentMethodEntity;
import org.jim.ledgerserver.ledger.service.PaymentMethodService;
import org.jim.ledgerserver.ledger.vo.PaymentMethodReq;
import org.jim.ledgerserver.ledger.vo.PaymentMethodResp;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 支付方式控制器
 * @author James Smith
 */
@RestController
@RequestMapping("/api/payment-methods")
public class PaymentMethodController {

    @Resource
    private PaymentMethodService paymentMethodService;

    /**
     * 获取当前用户的所有支付方式
     */
    @GetMapping
    public JSONResult<List<PaymentMethodResp>> getPaymentMethods() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return JSONResult.fail("用户未登录");
        }

        List<PaymentMethodEntity> methods = paymentMethodService.findByUserId(userId);
        List<PaymentMethodResp> response = methods.stream()
                .map(m -> new PaymentMethodResp(
                        m.getId(),
                        m.getName(),
                        m.getIcon(),
                        m.getType(),
                        m.getIsDefault(),
                        m.getSortOrder()
                ))
                .toList();

        return JSONResult.success(response);
    }

    /**
     * 初始化默认支付方式（用于老用户）
     */
    @PostMapping("/init-defaults")
    public JSONResult<List<PaymentMethodResp>> initDefaultPaymentMethods() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return JSONResult.fail("用户未登录");
        }

        // 检查是否已有支付方式
        List<PaymentMethodEntity> existingMethods = paymentMethodService.findByUserId(userId);
        if (!existingMethods.isEmpty()) {
            return JSONResult.fail("已存在支付方式，无需初始化");
        }

        // 创建默认支付方式
        paymentMethodService.createDefaultPaymentMethods(userId);

        // 返回创建的支付方式
        List<PaymentMethodEntity> methods = paymentMethodService.findByUserId(userId);
        List<PaymentMethodResp> response = methods.stream()
                .map(m -> new PaymentMethodResp(
                        m.getId(),
                        m.getName(),
                        m.getIcon(),
                        m.getType(),
                        m.getIsDefault(),
                        m.getSortOrder()
                ))
                .toList();

        return JSONResult.success(response);
    }

    /**
     * 创建支付方式
     */
    @PostMapping
    public JSONResult<PaymentMethodResp> createPaymentMethod(@RequestBody PaymentMethodReq request) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return JSONResult.fail("用户未登录");
        }

        PaymentMethodEntity entity = paymentMethodService.create(
                request.name(),
                request.icon(),
                request.type(),
                userId,
                request.isDefault(),
                request.sortOrder()
        );

        PaymentMethodResp response = new PaymentMethodResp(
                entity.getId(),
                entity.getName(),
                entity.getIcon(),
                entity.getType(),
                entity.getIsDefault(),
                entity.getSortOrder()
        );

        return JSONResult.success(response);
    }

    /**
     * 更新支付方式
     */
    @PutMapping("/{id}")
    public JSONResult<PaymentMethodResp> updatePaymentMethod(
            @PathVariable Long id,
            @RequestBody PaymentMethodReq request
    ) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return JSONResult.fail("用户未登录");
        }

        PaymentMethodEntity entity = paymentMethodService.update(
                id,
                userId,
                request.name(),
                request.icon(),
                request.type(),
                request.isDefault(),
                request.sortOrder()
        );

        PaymentMethodResp response = new PaymentMethodResp(
                entity.getId(),
                entity.getName(),
                entity.getIcon(),
                entity.getType(),
                entity.getIsDefault(),
                entity.getSortOrder()
        );

        return JSONResult.success(response);
    }

    /**
     * 设置默认支付方式
     */
    @PostMapping("/{id}/set-default")
    public JSONResult<Void> setDefaultPaymentMethod(@PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return JSONResult.fail("用户未登录");
        }

        paymentMethodService.setDefault(id, userId);
        return JSONResult.success();
    }

    /**
     * 删除支付方式
     */
    @DeleteMapping("/{id}")
    public JSONResult<Void> deletePaymentMethod(@PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return JSONResult.fail("用户未登录");
        }

        paymentMethodService.delete(id, userId);
        return JSONResult.success();
    }
}
