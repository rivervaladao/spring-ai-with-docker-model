package br.com.itss.chatbot.ai;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpError;

@RestController
public class McpSSEController {

    private static final Logger logger = LoggerFactory.getLogger(McpSSEController.class);

    private final List<McpSyncClient> mcpSyncClients;

    private final ChatClient chatClient;

    public McpSSEController(ChatModel chatModel, List<McpSyncClient> mcpSyncClients) {
        this.mcpSyncClients = mcpSyncClients;
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem(
                        """
                                Você é um agente de banco de dados. Quando precisar ler dados, SEMPRE use a ferramenta MCP `query`.
                                Formato dos argumentos, sempre use a view ordens_servico no banco de dados para responder as perguntas. 
                                Use o formato JSON abaixo:
                                {
                                  "sql": "SELECT ...;"
                                }
                                Responda só após executar a ferramenta e use o resultado retornado.
                                """)
                .defaultToolCallbacks(new SyncMcpToolCallbackProvider(mcpSyncClients))
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().build()).build())
                .build();

    }

    @GetMapping("/mcp/postgresql")
    public String querydb(@RequestParam(value = "sql", defaultValue = "liste pg_database") String sql) {
        logger.info("QUESTION: {}\n", sql);
        try {
            return this.chatClient.prompt().user(sql).call().content();
        } catch (McpError e) {
            logger.error("MCP tool call failed: {}", e.getMessage(), e);
            return "Error querying database: " + e.getMessage();
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage(), e);
            return "Unexpected error: " + e.getMessage();
        }
    }

}