package org.jim.ledgerserver.ledger.listener;

import jakarta.annotation.Resource;
import org.jim.ledgerserver.ledger.entity.LedgerEntity;
import org.jim.ledgerserver.ledger.service.LedgerService;
import org.jim.ledgerserver.user.entity.UserEntity;
import org.jim.ledgerserver.user.event.UserRegisteredEvent;
import org.jim.ledgerserver.user.repository.UserRepository;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户注册事件监听器
 * 负责在用户注册后创建默认账本
 * @author James Smith
 */
@Component
public class UserRegistrationListener {
    
    @Resource
    private LedgerService ledgerService;
    
    @Resource
    private UserRepository userRepository;
    
    /**
     * 处理用户注册事件：创建默认账本
     */
    @EventListener
    @Async
    @Transactional
    public void handleUserRegistered(UserRegisteredEvent event) {
        Long userId = event.getUserId();
        
        try {
            // 创建默认账本
            String defaultLedgerName = "默认账本";
            LedgerEntity defaultLedger = ledgerService.create(defaultLedgerName, null, userId);
            
            // 设置用户的默认账本ID
            UserEntity user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                user.setDefaultLedgerId(defaultLedger.getId());
                userRepository.save(user);
            }
        } catch (Exception e) {
            // 记录日志但不影响注册流程
            System.err.println("为用户 " + event.getUsername() + " 创建默认账本失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
