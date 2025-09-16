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

@RestController
public class McpSTDIOController {
	private static final Logger logger = LoggerFactory.getLogger(McpSTDIOController.class);

    private final List<McpSyncClient> mcpSyncClients;

    private final ChatClient tavilyChatClient;

    public McpSTDIOController(ChatModel chatModel, List<McpSyncClient> mcpSyncClients) {
        this.mcpSyncClients = mcpSyncClients;
        this.tavilyChatClient = ChatClient.builder(chatModel)
                .defaultSystem(
                        "Você é um assistente útil e pode realizar pesquisas, extração de paginas na web e responder às perguntas do usuário.")
                .defaultToolCallbacks(new SyncMcpToolCallbackProvider(mcpSyncClients))
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().build()).build())
                .build();           
    }

    @GetMapping("/mcp/tavily")
    public String search(@RequestParam(value = "message", defaultValue = "extair texto da pagina https://www.chucknorris.com.br/") String message) {
        logger.info("QUESTION: {}\n", message);        
        return this.tavilyChatClient.prompt().user(message).call().content();
    }

}