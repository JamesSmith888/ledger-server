package org.jim.ledgerserver.user.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jim.ledgerserver.user.entity.UserEntity;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * @author James Smith
 */
@Component
@Slf4j
public class UserMCP {

    @Resource
    private UserService userService;


    @Tool(description = """
            Purpose: Minimal user registration with username and password
            
            Prerequisites:
            - NONE - Direct registration process
            
            Parameters:
            - username: User's desired username (required, must be unique)
            - password: User's password (required, will be encrypted)
            
            Returns:
            - Success: Registration successful message with user info
            - Failure: Error message if registration fails
            
            Error Handling:
            - If username already exists:
              1. Return "用户名重复" error message
              2. Automatically suggest alternative usernames based on the current username
              3. Wait for user confirmation of the suggested username
              4. Retry registration with the confirmed username
            
            Post-Registration:
            - After successful registration, automatically call the login MCP tool to complete the login process
            
            Workflow:
            1. Validate username and password are provided
            2. Attempt registration
            3. If username conflict: suggest alternatives → get user confirmation → retry
            4. If successful: auto-login with the registered credentials
            """)
    public String register(String username, String password) {
        log.info("Registering user: {}", username);
        UserEntity register = userService.register(username, password);
        log.info("User registered : {}", register);
        return "注册成功: " + register.getUsername();
    }
}
