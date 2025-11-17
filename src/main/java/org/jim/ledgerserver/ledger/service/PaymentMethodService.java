package org.jim.ledgerserver.ledger.service;

import jakarta.annotation.Resource;
import org.jim.ledgerserver.common.exception.BusinessException;
import org.jim.ledgerserver.ledger.entity.PaymentMethodEntity;
import org.jim.ledgerserver.ledger.repository.PaymentMethodRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付方式服务
 * @author James Smith
 */
@Service
public class PaymentMethodService {

    @Resource
    private PaymentMethodRepository paymentMethodRepository;

    /**
     * 创建支付方式
     */
    @Transactional
    public PaymentMethodEntity create(String name, String icon, String type, Long userId, Boolean isDefault, Integer sortOrder) {
        // 如果设置为默认，先取消其他默认支付方式
        if (Boolean.TRUE.equals(isDefault)) {
            clearDefaultPaymentMethod(userId);
        }

        PaymentMethodEntity paymentMethod = new PaymentMethodEntity();
        paymentMethod.setName(name);
        paymentMethod.setIcon(icon);
        paymentMethod.setType(type);
        paymentMethod.setUserId(userId);
        paymentMethod.setIsDefault(isDefault != null ? isDefault : false);
        paymentMethod.setSortOrder(sortOrder != null ? sortOrder : 0);

        return paymentMethodRepository.save(paymentMethod);
    }

    /**
     * 批量创建默认支付方式（用于新用户注册）
     */
    @Transactional
    public void createDefaultPaymentMethods(Long userId) {
        // 默认支付方式列表
        String[][] defaultMethods = {
                {"现金", "💵", "CASH"},
                {"支付宝", "🟦", "ALIPAY"},
                {"微信", "💚", "WECHAT"},
                {"银行卡", "💳", "BANK_CARD"}
        };

        for (int i = 0; i < defaultMethods.length; i++) {
            String[] method = defaultMethods[i];
            create(method[0], method[1], method[2], userId, i == 0, i); // 第一个设为默认
        }
    }

    /**
     * 查询用户的所有支付方式
     */
    public List<PaymentMethodEntity> findByUserId(Long userId) {
        return paymentMethodRepository.findByUserIdAndDeleteTimeIsNull(userId);
    }

    /**
     * 根据ID查询支付方式
     */
    public PaymentMethodEntity findById(Long id) {
        return paymentMethodRepository.findById(id)
                .orElseThrow(() -> new BusinessException("支付方式不存在"));
    }

    /**
     * 根据ID和用户ID查询（权限校验）
     */
    public PaymentMethodEntity findByIdAndUserId(Long id, Long userId) {
        return paymentMethodRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BusinessException("支付方式不存在或无权访问"));
    }

    /**
     * 获取用户的默认支付方式
     */
    public PaymentMethodEntity getDefaultByUserId(Long userId) {
        return paymentMethodRepository.findDefaultByUserId(userId)
                .orElse(null);
    }

    /**
     * 更新支付方式
     */
    @Transactional
    public PaymentMethodEntity update(Long id, Long userId, String name, String icon, String type, Boolean isDefault, Integer sortOrder) {
        PaymentMethodEntity paymentMethod = findByIdAndUserId(id, userId);

        if (paymentMethod.getDeleteTime() != null) {
            throw new BusinessException("支付方式已删除");
        }

        // 如果设置为默认，先取消其他默认支付方式
        if (Boolean.TRUE.equals(isDefault) && !Boolean.TRUE.equals(paymentMethod.getIsDefault())) {
            clearDefaultPaymentMethod(userId);
        }

        if (name != null) paymentMethod.setName(name);
        if (icon != null) paymentMethod.setIcon(icon);
        if (type != null) paymentMethod.setType(type);
        if (isDefault != null) paymentMethod.setIsDefault(isDefault);
        if (sortOrder != null) paymentMethod.setSortOrder(sortOrder);

        return paymentMethodRepository.save(paymentMethod);
    }

    /**
     * 设置默认支付方式
     */
    @Transactional
    public void setDefault(Long id, Long userId) {
        PaymentMethodEntity paymentMethod = findByIdAndUserId(id, userId);
        
        if (paymentMethod.getDeleteTime() != null) {
            throw new BusinessException("支付方式已删除");
        }

        // 先取消其他默认支付方式
        clearDefaultPaymentMethod(userId);

        // 设置为默认
        paymentMethod.setIsDefault(true);
        paymentMethodRepository.save(paymentMethod);
    }

    /**
     * 取消所有默认支付方式
     */
    private void clearDefaultPaymentMethod(Long userId) {
        List<PaymentMethodEntity> methods = findByUserId(userId);
        methods.forEach(method -> {
            if (Boolean.TRUE.equals(method.getIsDefault())) {
                method.setIsDefault(false);
                paymentMethodRepository.save(method);
            }
        });
    }

    /**
     * 删除支付方式（逻辑删除）
     */
    @Transactional
    public void delete(Long id, Long userId) {
        PaymentMethodEntity paymentMethod = findByIdAndUserId(id, userId);
        
        if (paymentMethod.getDeleteTime() != null) {
            throw new BusinessException("支付方式已删除");
        }

        paymentMethod.setDeleteTime(LocalDateTime.now());
        paymentMethodRepository.save(paymentMethod);
    }
}
