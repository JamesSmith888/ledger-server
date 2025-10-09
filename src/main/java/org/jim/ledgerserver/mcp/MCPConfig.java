package org.jim.ledgerserver.mcp;

import io.modelcontextprotocol.client.McpSyncClient;
import org.jim.ledgerserver.user.service.UserMCP;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author James Smith
 */
@Configuration
public class MCPConfig {


    @Bean
    public ToolCallbackProvider mysqlToolCallbackProvider(UserMCP optionService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(optionService)
                .build();
    }


    @Bean
    public ChatClient chatClient(List<McpSyncClient> mcpSyncClients, GoogleGenAiChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultToolCallbacks(SyncMcpToolCallbackProvider.syncToolCallbacks(mcpSyncClients))
                .build();
    }
}
