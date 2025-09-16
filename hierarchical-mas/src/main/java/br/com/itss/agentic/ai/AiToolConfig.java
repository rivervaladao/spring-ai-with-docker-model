package br.com.itss.agentic.ai;

import br.com.itss.agentic.ai.tools.ChartTools;
import br.com.itss.agentic.ai.tools.DocumentTools;
import br.com.itss.agentic.ai.tools.ResearchTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiToolConfig {

    @Bean
    ChatClient.Builder chatClientBuilder(ChatModel chatModel,
                                         ResearchTools researchTools,
                                         DocumentTools documentTools,
                                         ChartTools chartTools) {
        return ChatClient.builder(chatModel)
                .defaultTools(researchTools, documentTools, chartTools);
    }
}
