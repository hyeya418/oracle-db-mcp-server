package com.example.oracle_db_mcp_server.config;

import com.example.oracle_db_mcp_server.tool.DbQueryTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpToolConfig {

    @Bean
    public ToolCallbackProvider dbTools(DbQueryTool dbQueryTool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(dbQueryTool)
                .build();
    }
}
