package org.jim.ledgerserver.ledger.service;

import jakarta.annotation.Resource;
import org.jim.ledgerserver.common.exception.BusinessException;
import org.jim.ledgerserver.ledger.entity.TransactionTemplateEntity;
import org.jim.ledgerserver.ledger.repository.TransactionTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 交易模板服务
 * @author James Smith
 */
@Service
public class TransactionTemplateService {

    @Resource
    private TransactionTemplateRepository templateRepository;

    /**
     * 创建交易模板
     */
    @Transactional
    public TransactionTemplateEntity create(
            Long userId,
            String name,
            BigDecimal amount,
            Integer type,
            Long categoryId,
            Long paymentMethodId,
            String description,
            Boolean allowAmountEdit,
            Boolean showInQuickPanel,
            Integer sortOrder,
            String icon,
            String color,
            Long ledgerId
    ) {
        TransactionTemplateEntity template = new TransactionTemplateEntity();
        template.setUserId(userId);
        template.setName(name);
        template.setAmount(amount);
        template.setType(type);
        template.setCategoryId(categoryId);
        template.setPaymentMethodId(paymentMethodId);
        template.setDescription(description);
        template.setAllowAmountEdit(allowAmountEdit != null ? allowAmountEdit : true);
        template.setShowInQuickPanel(showInQuickPanel != null ? showInQuickPanel : false);
        template.setSortOrder(sortOrder != null ? sortOrder : 0);
        template.setIcon(icon);
        template.setColor(color);
        template.setLedgerId(ledgerId);

        return templateRepository.save(template);
    }

    /**
     * 查询用户的所有模板
     */
    public List<TransactionTemplateEntity> findByUserId(Long userId) {
        return templateRepository.findByUserIdAndDeleteTimeIsNull(userId);
    }

    /**
     * 查询用户在快捷面板显示的模板
     */
    public List<TransactionTemplateEntity> findQuickPanelTemplates(Long userId) {
        return templateRepository.findQuickPanelTemplates(userId);
    }

    /**
     * 根据ID查询模板
     */
    public TransactionTemplateEntity findById(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new BusinessException("模板不存在"));
    }

    /**
     * 根据ID和用户ID查询（权限校验）
     */
    public TransactionTemplateEntity findByIdAndUserId(Long id, Long userId) {
        return templateRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BusinessException("模板不存在或无权访问"));
    }

    /**
     * 更新交易模板
     */
    @Transactional
    public TransactionTemplateEntity update(
            Long id,
            Long userId,
            String name,
            BigDecimal amount,
            Integer type,
            Long categoryId,
            Long paymentMethodId,
            String description,
            Boolean allowAmountEdit,
            Boolean showInQuickPanel,
            Integer sortOrder,
            String icon,
            String color,
            Long ledgerId
    ) {
        TransactionTemplateEntity template = findByIdAndUserId(id, userId);

        if (template.getDeleteTime() != null) {
            throw new BusinessException("模板已删除");
        }

        if (name != null) template.setName(name);
        if (amount != null) template.setAmount(amount);
        if (type != null) template.setType(type);
        if (categoryId != null) template.setCategoryId(categoryId);
        if (paymentMethodId != null) template.setPaymentMethodId(paymentMethodId);
        if (description != null) template.setDescription(description);
        if (allowAmountEdit != null) template.setAllowAmountEdit(allowAmountEdit);
        if (showInQuickPanel != null) template.setShowInQuickPanel(showInQuickPanel);
        if (sortOrder != null) template.setSortOrder(sortOrder);
        if (icon != null) template.setIcon(icon);
        if (color != null) template.setColor(color);
        if (ledgerId != null) template.setLedgerId(ledgerId);

        return templateRepository.save(template);
    }

    /**
     * 删除模板（逻辑删除）
     */
    @Transactional
    public void delete(Long id, Long userId) {
        TransactionTemplateEntity template = findByIdAndUserId(id, userId);
        
        if (template.getDeleteTime() != null) {
            throw new BusinessException("模板已删除");
        }

        template.setDeleteTime(LocalDateTime.now());
        templateRepository.save(template);
    }

    /**
     * 批量更新模板排序
     */
    @Transactional
    public void updateSortOrder(Long userId, List<Long> templateIds) {
        for (int i = 0; i < templateIds.size(); i++) {
            Long templateId = templateIds.get(i);
            TransactionTemplateEntity template = findByIdAndUserId(templateId, userId);
            template.setSortOrder(i);
            templateRepository.save(template);
        }
    }
}
