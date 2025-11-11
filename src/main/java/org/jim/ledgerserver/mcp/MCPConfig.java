package org.jim.ledgerserver.mcp;

import lombok.extern.slf4j.Slf4j;
import org.jim.ledgerserver.ledger.service.CategoryMCP;
import org.jim.ledgerserver.ledger.service.TransactionMCP;
import org.jim.ledgerserver.user.service.UserMCP;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author James Smith
 */
@Configuration
@Slf4j
public class MCPConfig {


/*    @Bean
    public ToolCallbackProvider mysqlToolCallbackProvider(UserMCP user, TransactionMCP transaction, CategoryMCP category) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(user, transaction, category)
                .build();
    }*/

}
