package org.jim.ledgerserver.user.event;

import org.springframework.context.ApplicationEvent;

/**
 * 用户注册完成事件
 * @author James Smith
 */
public class UserRegisteredEvent extends ApplicationEvent {
    
    private final Long userId;
    private final String username;
    
    public UserRegisteredEvent(Object source, Long userId, String username) {
        super(source);
        this.userId = userId;
        this.username = username;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public String getUsername() {
        return username;
    }
}
